package com.man_behind.checkmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.man_behind.checkmate.data.model.Checklist
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Exports a [Checklist] to a PDF file using [PdfDocument] and Canvas drawing.
 *
 * Everything is drawn as vector graphics (text, lines, shapes) so the output is
 * always crisp regardless of zoom level. Images are the only rasterized content.
 *
 * No WebView — runs entirely on a background thread, so [export] can be called
 * from any coroutine without wrapping in suspendCancellableCoroutine.
 *
 * Usage:
 *   exporter.export(checklist, outputFile) { success -> ... }
 *   // onComplete is always called on the main thread
 */
class ChecklistPdfExporter(private val context: Context) {

    // ── Page geometry — A4 at 72 dpi (1 unit = 1 PDF point = 1/72 inch) ─────
    private val PW = 595          // page width  (210 mm)
    private val PH = 842          // page height (297 mm)
    private val M  = 36f          // margin (~0.5 in)
    private val CW = PW - M * 2  // content width

    // ── Palette ───────────────────────────────────────────────────────────────
    private val ORANGE        = Color.rgb(0xE0, 0x5A, 0x1E)
    private val ORANGE_DARK   = Color.rgb(0xC0, 0x4A, 0x10)
    private val ORANGE_TINT   = Color.rgb(0xFF, 0xF4, 0xEF)
    private val GUIDE_BG      = Color.rgb(0x4A, 0x37, 0x28)  // dark warm brown, matches RISQ guide boxes
    private val TEXT          = Color.rgb(0x1A, 0x1A, 0x1A)
    private val MUTED         = Color.rgb(0x66, 0x66, 0x66)
    private val BORDER        = Color.rgb(0xCC, 0xCC, 0xCC)
    private val BORDER_LIGHT  = Color.rgb(0xE8, 0xE8, 0xE8)
    private val DOC_BG        = Color.rgb(0xDB, 0xEA, 0xFE)
    private val DOC_FG        = Color.rgb(0x1D, 0x4E, 0xD8)
    private val INSP_BG       = Color.rgb(0xDC, 0xFC, 0xE7)
    private val INSP_FG       = Color.rgb(0x15, 0x80, 0x3D)

    // ── Typefaces ─────────────────────────────────────────────────────────────
    private val REGULAR = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val BOLD    = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val ITALIC  = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

    // ── Intermediate data classes ─────────────────────────────────────────────
    // Decouple the Renderer from the domain model so it works even if the
    // domain classes change shape.

    private data class RenderOption(val id: Long, val position: Int, val text: String)

    private data class RenderItem(
        val number: String,
        val question: String,
        val guidelines: String?,
        val fromDocumentation: Boolean,
        val onInspection: Boolean,
        val options: List<RenderOption>,
        val selectedOptionId: Long?,
        val comment: String,
        val actionTaken: String,
        val imageUris: List<String>,
    )

    private data class RenderSection(
        val number: Int,
        val name: String,
        val comments: String,
        val items: List<RenderItem>,
    )

    // ── Public API ────────────────────────────────────────────────────────────

    fun export(checklist: Checklist, outputFile: File, onComplete: (Boolean) -> Unit) {
        thread(name = "pdf-export") {
            val success = runCatching {
                val sections = checklist.sections
                    .sortedBy { it.position }
                    .mapIndexed { si, section ->
                        RenderSection(
                            number   = si + 1,
                            name     = section.name,
                            comments = section.comments,
                            items    = section.items
                                .sortedBy { it.position }
                                .mapIndexed { ii, item ->
                                    RenderItem(
                                        number            = "${si + 1}.${ii + 1}",
                                        question          = item.question,
                                        guidelines        = item.guidelines,
                                        fromDocumentation = item.fromDocumentation,
                                        onInspection      = item.onInspection,
                                        options           = item.options.map {
                                            RenderOption(it.id, it.position, it.text)
                                        },
                                        selectedOptionId  = item.selectedOptionId,
                                        comment           = item.comment,
                                        actionTaken       = item.actionTaken,
                                        imageUris         = item.images.map { it.uri.toString() },
                                    )
                                }
                        )
                    }

                val pdf = Renderer(checklist.name, checklist.comments, sections).build()
                FileOutputStream(outputFile).use { pdf.writeTo(it) }
                pdf.close()
            }.isSuccess

            Handler(Looper.getMainLooper()).post { onComplete(success) }
        }
    }

    /** No WebView to clean up — kept for call-site compatibility. */
    fun release() = Unit

    // ─────────────────────────────────────────────────────────────────────────
    // Renderer
    // ─────────────────────────────────────────────────────────────────────────

    private inner class Renderer(
        private val checklistName: String,
        private val checklistComments: String,
        private val sections: List<RenderSection>,
    ) {
        // ── Page state ────────────────────────────────────────────────────────
        private val pdf     = PdfDocument()
        private var page:  PdfDocument.Page? = null
        private var canvas: Canvas            = Canvas()
        private var y       = M
        private var pageNum = 0

        // Item card state — set when starting a card, read when finishing it
        private var cardTop = 0f

        // Inner item margins
        private val IL = M + 10f         // inner left  (inside card)
        private val IR = PW - M - 8f    // inner right
        private val IW = IR - IL        // inner width

        init { newPage() }

        // ── Page management ───────────────────────────────────────────────────

        private fun newPage() {
            page?.let {
                drawPageFooter()
                pdf.finishPage(it)
            }
            pageNum++
            val info = PdfDocument.PageInfo.Builder(PW, PH, pageNum).create()
            page = pdf.startPage(info).also { canvas = it.canvas }
            canvas.drawColor(Color.WHITE)
            y = M
            if (pageNum > 1) drawPageHeader()
        }

        /** Starts a new page if fewer than [needed] points remain. */
        private fun need(needed: Float) {
            if (y + needed > PH - M - 16f) newPage()
        }

        private fun drawPageHeader() {
            val p = tp(7.5f, ITALIC, MUTED)
            canvas.drawText(checklistName, M, y + 8f, p)
            canvas.drawLine(M, y + 12f, PW - M, y + 12f, stroke(BORDER_LIGHT, 0.5f))
            y += 20f
        }

        private fun drawPageFooter() {
            val bottom = PH - M / 2f
            canvas.drawLine(M, bottom - 10f, PW - M, bottom - 10f, stroke(BORDER_LIGHT, 0.5f))
            val p = tp(7.5f, REGULAR, MUTED)
            val rightText = "Page $pageNum"
            canvas.drawText(rightText, PW - M - p.measureText(rightText), bottom, p)
        }

        fun finish(): PdfDocument {
            page?.let { drawPageFooter(); pdf.finishPage(it); page = null }
            return pdf
        }

        // ── Build ─────────────────────────────────────────────────────────────

        fun build(): PdfDocument {
            drawTitleBlock()
            sections.forEach { section ->
                drawSectionHeader(section)
                section.items.forEach { drawItem(it) }
            }
            return finish()
        }

        // ── Title block ───────────────────────────────────────────────────────

        private fun drawTitleBlock() {
            // Large orange title
            val titleP = tp(22f, BOLD, ORANGE)
            val titleL = sl(checklistName, titleP, CW.toInt())
            canvas.save()
            canvas.translate(M, y)
            titleL.draw(canvas)
            canvas.restore()
            y += titleL.height + 5f

            if (checklistComments.isNotBlank()) {
                val cp = tp(9f, REGULAR, MUTED)
                val cl = sl(checklistComments, cp, CW.toInt())
                canvas.save()
                canvas.translate(M, y)
                cl.draw(canvas)
                canvas.restore()
                y += cl.height + 5f
            }

            // Thick orange rule under the title
            canvas.drawRect(M, y, PW - M, y + 2.5f, fill(ORANGE))
            y += 18f
        }

        // ── Section header ────────────────────────────────────────────────────

        private fun drawSectionHeader(section: RenderSection) {
            val label = "${section.number}.  ${section.name}"
            val lp    = tp(11f, BOLD, Color.WHITE)
            val ll    = sl(label, lp, (CW - 20f).toInt())
            val boxH  = ll.height + 14f

            need(boxH + (if (section.comments.isNotBlank()) 20f else 0f) + 16f)

            // Rounded orange pill
            canvas.drawRoundRect(RectF(M, y, PW - M, y + boxH), 4f, 4f, fill(ORANGE))
            canvas.save()
            canvas.translate(M + 10f, y + 7f)
            ll.draw(canvas)
            canvas.restore()
            y += boxH

            if (section.comments.isNotBlank()) {
                y += 5f
                val cp = tp(8.5f, ITALIC, MUTED)
                val cl = sl(section.comments, cp, CW.toInt())
                canvas.save()
                canvas.translate(M + 4f, y)
                cl.draw(canvas)
                canvas.restore()
                y += cl.height
            }
            y += 10f
        }

        // ── Item card ─────────────────────────────────────────────────────────

        private fun drawItem(item: RenderItem) {
            val est = estimateItemHeight(item)
            // If the whole card fits, keep it on one page. If it's taller than a
            // full page (very long item), just start a new page and let it overflow.
            if (est <= PH - M * 2 - 40f) need(est)

            cardTop   = y
            var iy    = y + 10f       // inner Y cursor

            // ── Question header row ──────────────────────────────────────────

            val qNumP  = tp(10.5f, BOLD, ORANGE)
            val qNumW  = qNumP.measureText("${item.number}  ").coerceAtLeast(26f)

            // Reserve space for badges on the right
            var badgesReserved = 0f
            if (item.fromDocumentation) badgesReserved += measureBadge("Doc",   7.5f) + 4f
            if (item.onInspection)      badgesReserved += measureBadge("Insp.", 7.5f) + 4f

            val qTextW  = (IW - qNumW - badgesReserved - 4f).toInt().coerceAtLeast(60)
            val qTextP  = tp(10.5f, BOLD, TEXT)
            val qTextL  = sl(item.question, qTextP, qTextW)
            val qRowH   = qTextL.height.toFloat().coerceAtLeast(15f)

            // Question number
            canvas.drawText(item.number, IL, iy + qNumP.textSize, qNumP)

            // Question text
            canvas.save()
            canvas.translate(IL + qNumW, iy)
            qTextL.draw(canvas)
            canvas.restore()

            // Badges — right aligned
            var bx = IR - 2f
            if (item.onInspection) {
                bx -= drawBadge("Insp.", bx, iy + 2f, INSP_BG, INSP_FG, 7.5f)
                bx -= 4f
            }
            if (item.fromDocumentation) {
                drawBadge("Doc", bx, iy + 2f, DOC_BG, DOC_FG, 7.5f)
            }

            iy += qRowH + 9f

            // ── Guide to Inspection box ──────────────────────────────────────

            if (!item.guidelines.isNullOrBlank()) {
                val headerP  = tp(8f, BOLD, Color.WHITE)
                val bodyP    = tp(8f, REGULAR, Color.rgb(0xE8, 0xD8, 0xCC))
                val bodyL    = sl(item.guidelines, bodyP, (IW - 12f).toInt())
                val guideH   = 14f + bodyL.height + 10f

                val gr = RectF(IL, iy, IR, iy + guideH)
                canvas.drawRoundRect(gr, 3f, 3f, fill(GUIDE_BG))

                canvas.drawText("Guide to Inspection:", IL + 6f, iy + 11f, headerP)
                canvas.save()
                canvas.translate(IL + 6f, iy + 14f)
                bodyL.draw(canvas)
                canvas.restore()

                iy += guideH + 9f
            }

            // ── Options ──────────────────────────────────────────────────────

            if (item.options.isNotEmpty()) {
                item.options.sortedBy { it.position }.forEach { option ->
                    iy = drawOption(option.text, option.id == item.selectedOptionId, iy)
                }
                iy += 4f
            }

            // ── Comment ──────────────────────────────────────────────────────

            if (item.comment.isNotBlank()) {
                val sep = RectF(IL, iy, IR, iy + 0.6f)
                canvas.drawRect(sep, fill(BORDER_LIGHT))
                iy += 6f
                iy = drawFieldRow("Comment:", item.comment, iy)
                iy += 4f
            }

            // ── Action taken ─────────────────────────────────────────────────

            if (item.actionTaken.isNotBlank()) {
                if (item.comment.isBlank()) {
                    val sep = RectF(IL, iy, IR, iy + 0.6f)
                    canvas.drawRect(sep, fill(BORDER_LIGHT))
                    iy += 6f
                }
                iy = drawFieldRow("Action taken:", item.actionTaken, iy)
                iy += 4f
            }

            // ── Images ───────────────────────────────────────────────────────

            if (item.imageUris.isNotEmpty()) {
                iy = drawImages(item.imageUris, iy)
            }

            iy += 10f  // bottom inner padding

            // ── Card chrome — drawn last because we now know the card height ──

            // Outer border
            canvas.drawRoundRect(
                RectF(M, cardTop, PW - M, iy),
                3f, 3f,
                stroke(BORDER, 0.8f)
            )
            // Left orange accent
            val accentPath = Path().apply {
                // Follows the left rounded corner so it aligns with the border
                moveTo(M + 3f, cardTop + 3f)
                lineTo(M + 3f, iy - 3f)
            }
            canvas.drawPath(accentPath, mk(ORANGE, 4f, cap = Paint.Cap.ROUND))

            y = iy + 8f
        }

        // ── Option ────────────────────────────────────────────────────────────

        private fun drawOption(text: String, selected: Boolean, sy: Float): Float {
            val boxSz  = 10.5f
            val textP  = if (selected) tp(9.5f, BOLD, ORANGE_DARK) else tp(9.5f, REGULAR, TEXT)
            val textL  = sl(text, textP, (IW - boxSz - 10f).toInt().coerceAtLeast(1))
            val rowH   = textL.height.toFloat().coerceAtLeast(boxSz + 2f)
            val boxY   = sy + (rowH - boxSz) / 2f
            val boxR   = RectF(IL, boxY, IL + boxSz, boxY + boxSz)

            if (selected) {
                // Tinted fill + orange border
                canvas.drawRoundRect(boxR, 2f, 2f, fill(ORANGE_TINT))
                canvas.drawRoundRect(boxR, 2f, 2f, stroke(ORANGE, 1.3f))

                // Tick mark — drawn as a path for crispness
                val tick = Path().apply {
                    moveTo(IL + 2f,        boxY + boxSz * 0.50f)
                    lineTo(IL + boxSz * 0.42f, boxY + boxSz - 2.3f)
                    lineTo(IL + boxSz - 2f, boxY + 2.3f)
                }
                canvas.drawPath(tick, mk(ORANGE, 1.6f, cap = Paint.Cap.ROUND, join = Paint.Join.ROUND))
            } else {
                canvas.drawRoundRect(boxR, 2f, 2f, stroke(MUTED, 0.9f))
            }

            canvas.save()
            canvas.translate(IL + boxSz + 7f, sy)
            textL.draw(canvas)
            canvas.restore()

            return sy + rowH + 4f
        }

        // ── Field row ─────────────────────────────────────────────────────────

        private fun drawFieldRow(label: String, value: String, sy: Float): Float {
            val lp = tp(9f, BOLD, MUTED)
            val lw = lp.measureText(label) + 5f
            val vp = tp(9f, REGULAR, TEXT)
            val vl = sl(value, vp, (IW - lw).toInt().coerceAtLeast(1))

            canvas.drawText(label, IL, sy + lp.textSize, lp)
            canvas.save()
            canvas.translate(IL + lw, sy)
            vl.draw(canvas)
            canvas.restore()

            return sy + vl.height.toFloat()
        }

        // ── Images ────────────────────────────────────────────────────────────

        private fun drawImages(uris: List<String>, sy: Float): Float {
            val gap   = 6f
            val maxW  = ((IW - gap) / 2f).coerceAtMost(160f)
            val maxH  = 115f
            var curX  = IL
            var curY  = sy + 4f
            var rowH  = 0f

            uris.forEach { uri ->
                val bmp = loadBitmap(uri, maxW.toInt(), maxH.toInt()) ?: return@forEach
                val bW  = bmp.width.toFloat()
                val bH  = bmp.height.toFloat()

                // Wrap to next row if this image doesn't fit
                if (curX + bW > IR && curX > IL) {
                    curY += rowH + gap; curX = IL; rowH = 0f
                }

                // Border + image
                canvas.drawRoundRect(
                    RectF(curX - 1f, curY - 1f, curX + bW + 1f, curY + bH + 1f),
                    3f, 3f, stroke(BORDER, 0.7f)
                )
                canvas.drawBitmap(
                    bmp, null,
                    RectF(curX, curY, curX + bW, curY + bH),
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                )

                rowH = rowH.coerceAtLeast(bH)
                curX += bW + gap
            }

            return curY + rowH + 6f
        }

        // ── Badge ─────────────────────────────────────────────────────────────

        /**
         * Draws a badge with its right edge at [x].
         * Returns the width of the drawn badge.
         */
        private fun drawBadge(
            text: String, x: Float, y: Float,
            bg: Int, fg: Int, textSize: Float
        ): Float {
            val p   = tp(textSize, BOLD, fg)
            val tw  = p.measureText(text)
            val ph  = 5f; val pv = 2.5f
            val bw  = tw + ph * 2f
            val bh  = textSize + pv * 2f + 1f
            val rx  = x - bw
            canvas.drawRoundRect(RectF(rx, y, rx + bw, y + bh), bh / 2f, bh / 2f, fill(bg))
            canvas.drawText(text, rx + ph, y + pv + textSize, p)
            return bw
        }

        private fun measureBadge(text: String, textSize: Float) =
            tp(textSize, BOLD, TEXT).measureText(text) + 10f

        // ── Item height estimation (drives pagination) ─────────────────────────

        private fun estimateItemHeight(item: RenderItem): Float {
            var h = 28f  // top padding + question row baseline

            val qNumW = 28f
            val qW    = (IW - qNumW - 50f).toInt().coerceAtLeast(60)
            h += sl(item.question, tp(10.5f, BOLD, TEXT), qW).height + 9f

            if (!item.guidelines.isNullOrBlank()) {
                h += sl(item.guidelines, tp(8f, REGULAR, TEXT), (IW - 12f).toInt()).height + 33f
            }

            if (item.options.isNotEmpty()) {
                item.options.forEach { opt ->
                    h += sl(opt.text, tp(9.5f, REGULAR, TEXT), (IW - 20f).toInt()).height + 4f
                }
                h += 4f
            }

            if (item.comment.isNotBlank()) {
                h += sl(item.comment, tp(9f, REGULAR, TEXT), (IW - 90f).toInt()).height + 13f
            }

            if (item.actionTaken.isNotBlank()) {
                h += sl(item.actionTaken, tp(9f, REGULAR, TEXT), (IW - 110f).toInt()).height + 13f
            }

            if (item.imageUris.isNotEmpty()) h += 130f

            h += 10f  // bottom padding
            return h
        }

        // ── Paint factories ───────────────────────────────────────────────────

        private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        private fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = w
            style = Paint.Style.STROKE
        }

        private fun mk(
            color: Int,
            w: Float,
            cap:  Paint.Cap  = Paint.Cap.ROUND,
            join: Paint.Join = Paint.Join.ROUND,
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = w
            style = Paint.Style.STROKE
            strokeCap = cap
            strokeJoin = join
        }

        private fun tp(size: Float, tf: Typeface, color: Int) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize  = size
                typeface  = tf
                this.color = color
            }

        private fun sl(text: String, paint: TextPaint, width: Int): StaticLayout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0.5f, 1.15f)
                .setIncludePad(false)
                .build()
    }

    // ── Bitmap loading ────────────────────────────────────────────────────────

    private fun loadBitmap(uriStr: String, maxW: Int, maxH: Int): Bitmap? = runCatching {
        val uri = Uri.parse(uriStr)
        val src = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file"    -> File(uri.path!!).inputStream()
            else      -> return null
        }?.use { BitmapFactory.decodeStream(it) } ?: return null

        // Scale down to fit within maxW × maxH, never upscale
        val scale = min(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
            .coerceAtMost(1f)

        if (scale >= 1f) src
        else Bitmap.createScaledBitmap(
            src,
            (src.width  * scale).toInt(),
            (src.height * scale).toInt(),
            true
        )
    }.getOrNull()
}
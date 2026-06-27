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
import androidx.core.net.toUri

class ChecklistPdfExporter(private val context: Context) {

    // ── Page geometry — A4 at 72 dpi ─────────────────────────────────────────
    private val PW = 595
    private val PH = 842
    private val M  = 36f
    private val CW = PW - M * 2

    // ── Palette ───────────────────────────────────────────────────────────────
    private val ORANGE        = Color.rgb(0xE0, 0x5A, 0x1E)
    private val ORANGE_TINT   = Color.rgb(0xFF, 0xF4, 0xEF)
    private val TEXT          = Color.rgb(0x1A, 0x1A, 0x1A)
    private val MUTED         = Color.rgb(0x66, 0x66, 0x66)
    private val BORDER        = Color.rgb(0xCC, 0xCC, 0xCC)
    private val BORDER_LIGHT  = Color.rgb(0xE8, 0xE8, 0xE8)
    private val GREEN         = Color.rgb(0x15, 0x80, 0x3D)
    private val RED           = Color.rgb(0xCC, 0x1A, 0x1A)

    // ── Typefaces ─────────────────────────────────────────────────────────────
    private val REGULAR = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val BOLD    = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val ITALIC  = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

    // ── Intermediate data classes ─────────────────────────────────────────────

    private data class RenderOption(val id: Long, val position: Int, val text: String)

    private data class RenderItem(
        val number: String,
        val question: String,
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
                                        number           = "${si + 1}.${ii + 1}",
                                        question         = item.question,
                                        options          = item.options.map {
                                            RenderOption(it.id, it.position, it.text)
                                        },
                                        selectedOptionId = item.selectedOptionId,
                                        comment          = item.comment,
                                        actionTaken      = item.actionTaken,
                                        imageUris        = item.images.map { it.uri.toString() },
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

    fun release() = Unit

    // ─────────────────────────────────────────────────────────────────────────
    // Renderer
    // ─────────────────────────────────────────────────────────────────────────

    private inner class Renderer(
        private val checklistName: String,
        private val checklistComments: String,
        private val sections: List<RenderSection>,
    ) {
        private val pdf     = PdfDocument()
        private var page:   PdfDocument.Page? = null
        private var canvas: Canvas             = Canvas()
        private var y       = M
        private var pageNum = 0

        // Item card inner bounds
        private val IL = M + 10f
        private val IR = PW - M - 8f
        private val IW = IR - IL

        // Image display size in PDF points — large enough for 2 side by side
        private val IMG_DISPLAY_W = ((IW - 8f) / 2f)   // half inner width minus gap
        private val IMG_DISPLAY_H = 180f

        // Bitmap pixel budget — ~150 DPI relative to PDF points (72 dpi base)
        private val IMG_LOAD_PX_W = (IMG_DISPLAY_W * 2.1f).toInt()  // ≈ 530 px
        private val IMG_LOAD_PX_H = (IMG_DISPLAY_H * 2.1f).toInt()  // ≈ 378 px

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
            val label = "Page $pageNum"
            canvas.drawText(label, PW - M - p.measureText(label), bottom, p)
        }

        fun finish(): PdfDocument {
            page?.let { drawPageFooter(); pdf.finishPage(it); page = null }
            return pdf
        }

        // ── Build ─────────────────────────────────────────────────────────────

        fun build(): PdfDocument {
            drawTitleBlock()
            sections.forEachIndexed { index, section ->
                if (index > 0)
                    newPage()                        // every section starts on a fresh page
                drawSectionHeader(section)
                section.items.forEach { drawItem(it) }
                drawSectionComments(section.comments)
            }
            return finish()
        }

        // ── Title block ───────────────────────────────────────────────────────

        private fun drawTitleBlock() {
            val titleP = tp(22f, BOLD, ORANGE)
            val titleL = sl(checklistName, titleP, CW.toInt())
            canvas.save(); canvas.translate(M, y); titleL.draw(canvas); canvas.restore()
            y += titleL.height + 5f

            if (checklistComments.isNotBlank()) {
                val cp = tp(9f, REGULAR, MUTED)
                val cl = sl(checklistComments, cp, CW.toInt())
                canvas.save(); canvas.translate(M, y); cl.draw(canvas); canvas.restore()
                y += cl.height + 5f
            }

            canvas.drawRect(M, y, PW - M, y + 2.5f, fill(ORANGE))
            y += 18f
        }

        // ── Section header ────────────────────────────────────────────────────

        private fun drawSectionHeader(section: RenderSection) {
            val label = "${section.number}.  ${section.name}"
            val lp    = tp(16f, BOLD, Color.WHITE)         // larger font than before
            val ll    = sl(label, lp, (CW - 20f).toInt())
            val boxH  = ll.height + 18f

            val r = RectF(M, y, PW - M, y + boxH)
            canvas.drawRoundRect(r, 4f, 4f, fill(ORANGE))
            canvas.save(); canvas.translate(M + 10f, y + 9f); ll.draw(canvas); canvas.restore()
            y += boxH + 16f
        }

        // ── Section comments (always drawn at the end of a section) ───────────

        private fun drawSectionComments(comments: String) {
            need(if (comments.isNotBlank()) 60f else 30f)

            y += 10f
            val headingP = tp(11f, BOLD, TEXT)
            canvas.drawText("Section Comments", M, y + headingP.textSize, headingP)
            y += headingP.textSize + 6f

            if (comments.isNotBlank()) {
                val cp = tp(9.5f, REGULAR, TEXT)
                val cl = sl(comments, cp, CW.toInt())
                canvas.save(); canvas.translate(M, y); cl.draw(canvas); canvas.restore()
                y += cl.height
            }

            y += 8f
        }

        // ── Item card ─────────────────────────────────────────────────────────

        private fun drawItem(item: RenderItem) {
            val est = estimateItemHeight(item)
            if (est <= PH - M * 2 - 40f) need(est)

            val cardTop = y
            var iy      = y + 10f

            // ── Question header ──────────────────────────────────────────────
            val qNumP = tp(10.5f, BOLD, ORANGE)
            val qNumW = qNumP.measureText("${item.number}  ").coerceAtLeast(26f)
            val qTextW = (IW - qNumW - 4f).toInt().coerceAtLeast(60)
            val qTextL = sl(item.question, tp(10.5f, BOLD, TEXT), qTextW)
            val qRowH  = qTextL.height.toFloat().coerceAtLeast(15f)

            canvas.drawText(item.number, IL, iy + qNumP.textSize, qNumP)
            canvas.save(); canvas.translate(IL + qNumW, iy); qTextL.draw(canvas); canvas.restore()
            iy += qRowH + 9f

            // ── Options ──────────────────────────────────────────────────────
            if (item.options.isNotEmpty()) {
                item.options.sortedBy { it.position }.forEach { option ->
                    iy = drawOption(option.text, option.id == item.selectedOptionId, iy)
                }
                iy += 4f
            }

            // Determine comment colour from the selected option text
            val selectedText = item.options.find { it.id == item.selectedOptionId }?.text ?: ""
            val commentColor = when {
                selectedText.contains("yes", ignoreCase = true) -> GREEN
                selectedText.contains("no",  ignoreCase = true) -> RED
                else                                            -> TEXT
            }

            // ── Comment ──────────────────────────────────────────────────────
            if (item.comment.isNotBlank()) {
                val sep = RectF(IL, iy, IR, iy + 0.6f)
                canvas.drawRect(sep, fill(BORDER_LIGHT))
                iy += 6f
                iy = drawFieldRow("Comment:", item.comment, iy, commentColor)
                iy += 4f
            }

            // ── Action taken ─────────────────────────────────────────────────
            if (item.actionTaken.isNotBlank()) {
                val sep = RectF(IL, iy, IR, iy + 0.6f)
                canvas.drawRect(sep, fill(BORDER_LIGHT))
                iy += 6f
                iy = drawFieldRow("Action taken:", item.actionTaken, iy, TEXT)
                iy += 4f
            }

            // ── Images ───────────────────────────────────────────────────────
            if (item.imageUris.isNotEmpty()) {
                iy = drawImages(item.imageUris, iy)
            }

            iy += 10f

            // ── Card chrome ──────────────────────────────────────────────────
            canvas.drawRoundRect(RectF(M, cardTop, PW - M, iy), 3f, 3f, stroke(BORDER, 0.8f))
            val accentPath = Path().apply {
                moveTo(M + 3f, cardTop + 3f)
                lineTo(M + 3f, iy - 3f)
            }
            canvas.drawPath(accentPath, mk(ORANGE, 4f, cap = Paint.Cap.ROUND))

            y = iy + 8f
        }

        // ── Option row ────────────────────────────────────────────────────────

        private fun drawOption(text: String, selected: Boolean, sy: Float): Float {
            val boxSz = 10.5f
            // Selected: bold black. Unselected: regular black.
            val textP = if (selected) tp(9.5f, BOLD, TEXT) else tp(9.5f, REGULAR, TEXT)
            val textL = sl(text, textP, (IW - boxSz - 10f).toInt().coerceAtLeast(1))
            val rowH  = textL.height.toFloat().coerceAtLeast(boxSz + 2f)
            val boxY  = sy + (rowH - boxSz) / 2f
            val boxR  = RectF(IL, boxY, IL + boxSz, boxY + boxSz)

            if (selected) {
                canvas.drawRoundRect(boxR, 2f, 2f, fill(ORANGE_TINT))
                canvas.drawRoundRect(boxR, 2f, 2f, stroke(ORANGE, 1.3f))
                val tick = Path().apply {
                    moveTo(IL + 2f,             boxY + boxSz * 0.50f)
                    lineTo(IL + boxSz * 0.42f,  boxY + boxSz - 2.3f)
                    lineTo(IL + boxSz - 2f,     boxY + 2.3f)
                }
                canvas.drawPath(tick, mk(ORANGE, 1.6f, cap = Paint.Cap.ROUND, join = Paint.Join.ROUND))
            } else {
                canvas.drawRoundRect(boxR, 2f, 2f, stroke(MUTED, 0.9f))
            }

            canvas.save(); canvas.translate(IL + boxSz + 7f, sy); textL.draw(canvas); canvas.restore()
            return sy + rowH + 4f
        }

        // ── Field row ─────────────────────────────────────────────────────────

        private fun drawFieldRow(label: String, value: String, sy: Float, valueColor: Int): Float {
            val lp = tp(9f, BOLD, MUTED)
            val lw = lp.measureText(label) + 5f
            val vp = tp(9f, REGULAR, valueColor)
            val vl = sl(value, vp, (IW - lw).toInt().coerceAtLeast(1))

            canvas.drawText(label, IL, sy + lp.textSize, lp)
            canvas.save(); canvas.translate(IL + lw, sy); vl.draw(canvas); canvas.restore()
            return sy + vl.height.toFloat()
        }

        // ── Images ────────────────────────────────────────────────────────────

        private fun drawImages(uris: List<String>, sy: Float): Float {
            val gap  = 8f
            var curX = IL
            var curY = sy + 4f
            var rowH = 0f

            uris.forEach { uri ->
                val bmp = loadBitmap(uri, IMG_LOAD_PX_W, IMG_LOAD_PX_H) ?: return@forEach

                // Scale bitmap to fit display slot, preserving aspect ratio
                val scale = min(IMG_DISPLAY_W / bmp.width, IMG_DISPLAY_H / bmp.height)
                val dW    = bmp.width  * scale
                val dH    = bmp.height * scale

                // Wrap to a new row (2 images per row)
                if (curX + dW > IR && curX > IL) {
                    curY += rowH + gap; curX = IL; rowH = 0f
                }

                canvas.drawRoundRect(
                    RectF(curX - 1f, curY - 1f, curX + dW + 1f, curY + dH + 1f),
                    3f, 3f, stroke(BORDER, 0.7f)
                )
                canvas.drawBitmap(
                    bmp, null,
                    RectF(curX, curY, curX + dW, curY + dH),
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                )

                rowH = rowH.coerceAtLeast(dH)
                curX += dW + gap
            }

            return curY + rowH + 6f
        }

        // ── Item height estimation ─────────────────────────────────────────────

        private fun estimateItemHeight(item: RenderItem): Float {
            var h = 28f
            val qNumW = 28f
            h += sl(item.question, tp(10.5f, BOLD, TEXT), (IW - qNumW - 4f).toInt()).height + 9f

            item.options.forEach { opt ->
                h += sl(opt.text, tp(9.5f, REGULAR, TEXT), (IW - 20f).toInt()).height + 4f
            }
            if (item.options.isNotEmpty()) h += 4f

            if (item.comment.isNotBlank()) {
                h += sl(item.comment, tp(9f, REGULAR, TEXT), (IW - 90f).toInt()).height + 13f
            }
            if (item.actionTaken.isNotBlank()) {
                h += sl(item.actionTaken, tp(9f, REGULAR, TEXT), (IW - 110f).toInt()).height + 13f
            }
            if (item.imageUris.isNotEmpty()) {
                h += IMG_DISPLAY_H + 20f   // rough estimate: one row of images
            }

            h += 10f
            return h
        }

        // ── Paint / layout factories ──────────────────────────────────────────

        private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.FILL
        }

        private fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; strokeWidth = w; style = Paint.Style.STROKE
        }

        private fun mk(
            color: Int, w: Float,
            cap:  Paint.Cap  = Paint.Cap.ROUND,
            join: Paint.Join = Paint.Join.ROUND,
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; strokeWidth = w
            style = Paint.Style.STROKE; strokeCap = cap; strokeJoin = join
        }

        private fun tp(size: Float, tf: Typeface, color: Int) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size; typeface = tf; this.color = color
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

    private fun loadBitmap(uriStr: String, maxPxW: Int, maxPxH: Int): Bitmap? = runCatching {
        val uri = uriStr.toUri()
        val stream = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file"    -> File(uri.path!!).inputStream()
            else      -> return null
        } ?: return null

        // Two-pass decode: first read dimensions, then decode at the right sample size
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        stream.use { BitmapFactory.decodeStream(it, null, opts) }

        val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, maxPxW, maxPxH)

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val stream2 = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file"    -> File(uri.path!!).inputStream()
            else      -> return null
        } ?: return null

        stream2.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    }.getOrNull()

    /**
     * Calculates the largest power-of-2 sample size such that the decoded
     * bitmap is at least [targetW] × [targetH] pixels (so we don't lose detail).
     */
    private fun calculateSampleSize(srcW: Int, srcH: Int, targetW: Int, targetH: Int): Int {
        var sampleSize = 1
        var halfW = srcW / 2
        var halfH = srcH / 2
        while (halfW >= targetW && halfH >= targetH) {
            sampleSize *= 2
            halfW /= 2
            halfH /= 2
        }
        return sampleSize
    }
}
package com.man_behind.checkmate.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistItem
import java.io.File
import java.io.FileOutputStream

/**
 * Exports a [Checklist] to a PDF file using [android.graphics.pdf.PdfDocument].
 * Uses native Canvas drawing to avoid WebView dependencies and crashes.
 */
class ChecklistPdfExporter2(private val context: Context) {

    // A4 dimensions at 72 DPI (Standard PDF points)
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val contentWidth = pageWidth - (2 * margin)

    // Colors
    private val colorPrimary = Color.parseColor("#e05a1e")
    private val colorText = Color.parseColor("#1a1a1a")
    private val colorSecondary = Color.parseColor("#555555")
    private val colorBorder = Color.parseColor("#dddddd")

    private var currentY = margin
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var pdfDocument = PdfDocument()
    private var pageNumber = 0

    fun export(checklist: Checklist, outputFile: File, onComplete: (Boolean) -> Unit) {
        try {
            pdfDocument = PdfDocument()
            pageNumber = 0
            startNewPage()

            // 1. Title
            drawText(checklist.name, 22f, colorPrimary, isBold = true)
            if (checklist.comments.isNotBlank()) {
                drawText(checklist.comments, 12f, colorSecondary)
            }
            drawDivider()

            // 2. Sections
            checklist.sections.sortedBy { it.position }.forEachIndexed { sIdx, section ->
                ensureSpace(60f)
                drawSectionHeader("${sIdx + 1}. ${section.name}")

                if (section.comments.isNotBlank()) {
                    drawText(section.comments, 11f, colorSecondary, italic = true)
                }

                // 3. Items
                section.items.sortedBy { it.position }.forEachIndexed { iIdx, item ->
                    drawItem(item, "${sIdx + 1}.${iIdx + 1}")
                }
            }

            pdfDocument.finishPage(currentPage)
            FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        } finally {
            pdfDocument.close()
        }
    }

    private fun startNewPage() {
        if (currentPage != null) pdfDocument.finishPage(currentPage)
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        currentPage = pdfDocument.startPage(pageInfo)
        canvas = currentPage?.canvas
        currentY = margin
    }

    private fun ensureSpace(needed: Float) {
        if (currentY + needed > pageHeight - margin) {
            startNewPage()
        }
    }

    private fun drawText(text: String, size: Float, color: Int, isBold: Boolean = false, italic: Boolean = false) {
        val paint = TextPaint().apply {
            this.color = color
            this.textSize = size
            this.isAntiAlias = true
            if (isBold && italic) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            else if (isBold) typeface = Typeface.DEFAULT_BOLD
            else if (italic) typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

        ensureSpace(layout.height.toFloat())
        canvas?.save()
        canvas?.translate(margin, currentY)
        layout.draw(canvas)
        canvas?.restore()
        currentY += layout.height + 8f
    }

    private fun drawSectionHeader(title: String) {
        val paint = Paint().apply { color = colorPrimary; isAntiAlias = true }
        val textPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val rectHeight = 30f
        val rect = RectF(margin, currentY, margin + contentWidth, currentY + rectHeight)
        canvas?.drawRoundRect(rect, 4f, 4f, paint)

        val fontMetrics = textPaint.fontMetrics
        val textY = currentY + (rectHeight - (fontMetrics.ascent + fontMetrics.descent)) / 2
        canvas?.drawText(title, margin + 10f, textY, textPaint)

        currentY += rectHeight + 10f
    }

    private fun drawItem(item: ChecklistItem, number: String) {
        val titlePaint = TextPaint().apply { textSize = 11f; color = colorText; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val textPaint = TextPaint().apply { textSize = 10f; color = colorText; isAntiAlias = true }
        val secondaryPaint = TextPaint().apply { textSize = 9f; color = colorSecondary; isAntiAlias = true }

        // Question + Badges
        val badges = mutableListOf<String>()
        if (item.fromDocumentation) badges.add("Doc")
        if (item.onInspection) badges.add("Inspection")
        val badgeText = if (badges.isNotEmpty()) " [${badges.joinToString(", ")}]" else ""
        val questionLayout = StaticLayout.Builder.obtain("$number ${item.question}$badgeText", 0, ("$number ${item.question}$badgeText").length, titlePaint, (contentWidth - 24).toInt()).build()

        // Guidelines
        val guideLayout = item.guidelines?.takeIf { it.isNotBlank() }?.let {
            StaticLayout.Builder.obtain("Guide: $it", 0, ("Guide: $it").length, secondaryPaint, (contentWidth - 24).toInt()).build()
        }

        // Options
        val optionLayouts = item.options.sortedBy { it.position }.map { option ->
            val isSelected = option.id == item.selectedOptionId
            val prefix = if (isSelected) "☑ " else "☐ "
            StaticLayout.Builder.obtain("$prefix${option.text}", 0, ("$prefix${option.text}").length, if (isSelected) titlePaint else textPaint, (contentWidth - 36).toInt()).build()
        }

        // Comment & Action Taken
        val commentLayout = item.comment.takeIf { it.isNotBlank() }?.let { StaticLayout.Builder.obtain("Comment: $it", 0, ("Comment: $it").length, textPaint, (contentWidth - 24).toInt()).build() }
        val actionLayout = item.actionTaken.takeIf { it.isNotBlank() }?.let { StaticLayout.Builder.obtain("Action: $it", 0, ("Action: $it").length, textPaint, (contentWidth - 24).toInt()).build() }

        // Calculate heights
        var textHeight = 10f + questionLayout.height + 4f
        guideLayout?.let { textHeight += it.height + 4f }
        optionLayouts.forEach { textHeight += it.height + 2f }
        if (optionLayouts.isNotEmpty()) textHeight += 4f
        commentLayout?.let { textHeight += it.height + 4f }
        actionLayout?.let { textHeight += it.height + 4f }
        textHeight += 10f

        ensureSpace(textHeight + 20f)
        val startY = currentY

        canvas?.save()
        canvas?.translate(margin + 12f, currentY + 10f)
        questionLayout.draw(canvas)
        canvas?.translate(0f, questionLayout.height + 4f)
        guideLayout?.let { it.draw(canvas); canvas?.translate(0f, it.height + 4f) }
        optionLayouts.forEach { it.draw(canvas); canvas?.translate(0f, it.height + 2f) }
        if (optionLayouts.isNotEmpty()) canvas?.translate(0f, 4f)
        commentLayout?.let { it.draw(canvas); canvas?.translate(0f, it.height + 4f) }
        actionLayout?.let { it.draw(canvas); canvas?.translate(0f, it.height + 4f) }
        canvas?.restore()

        currentY += textHeight

        // Images
        item.images.forEach { img -> drawBitMap(img.uri) }

        // Draw card border
        val borderPaint = Paint().apply { color = colorBorder; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        canvas?.drawRect(margin, startY, margin + contentWidth, currentY, borderPaint)
        val accentPaint = Paint().apply { color = colorPrimary; style = Paint.Style.FILL; isAntiAlias = true }
        canvas?.drawRect(margin, startY, margin + 4f, currentY, accentPaint)

        currentY += 12f
    }

    private fun drawBitMap(uri: Uri) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(stream) ?: return
            val scaled = scaleBitmap(bitmap, 180)
            ensureSpace(scaled.height.toFloat() + 20f)
            canvas?.drawBitmap(scaled, margin + 12f, currentY, null)
            currentY += scaled.height + 10f
        } catch (e: Exception) {}
    }

    private fun scaleBitmap(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val aspectRatio = src.height.toFloat() / src.width.toFloat()
        return Bitmap.createScaledBitmap(src, maxWidth, (maxWidth * aspectRatio).toInt(), true)
    }

    private fun drawDivider() {
        val paint = Paint().apply { color = colorPrimary; strokeWidth = 2f; isAntiAlias = true }
        canvas?.drawLine(margin, currentY, margin + contentWidth, currentY, paint)
        currentY += 15f
    }

    fun release() {}
}

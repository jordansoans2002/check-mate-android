package com.man_behind.checkmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.man_behind.checkmate.data.local.db.entity.ChecklistWithDetails
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import androidx.core.graphics.scale
import com.man_behind.checkmate.data.model.Checklist

/**
 * Exports a [ChecklistWithDetails] to a PDF file using [android.graphics.pdf.PdfDocument].
 *
 * No third-party dependencies, no PrintDocumentAdapter callbacks.
 * The WebView renders HTML into memory; each page is drawn onto a PdfDocument canvas.
 *
 * Usage (must be called on the main thread):
 *
 *   val exporter = ChecklistPdfExporter(context)
 *   exporter.export(checklistWithDetails, outputFile) { success ->
 *       // runs on the main thread
 *   }
 *   // later, e.g. in ViewModel.onCleared():
 *   exporter.release()
 */
class ChecklistPdfExporter(private val context: Context) {

    private var webView: WebView? = null

    // A4 at 96 dpi (standard WebView / CSS pixel density)
    private val pageWidthPx = 794   // 210 mm → 794 px @ 96 dpi
    private val pageHeightPx = 1123  // 297 mm → 1123 px @ 96 dpi

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Render [checklist] to [outputFile] as a multi-page A4 PDF.
     * [onComplete] is called on the main thread.
     */
    fun export(
        checklist: Checklist,
        outputFile: File,
        onComplete: (success: Boolean) -> Unit
    ) {
        val html = buildHtml(checklist)

        // WebView MUST be created on the main thread.
        // We give it an explicit layout size so contentHeight is meaningful.
        val wv =  try {
            WebView(context).also { webView = it }
        } catch (e: Exception) {
            onComplete(false)
            return
        }

        wv.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = true
            allowContentAccess = true
            // Match our target page width so the CSS layout is stable
            useWideViewPort = true
            loadWithOverviewMode = false
        }

        // Size the WebView to exactly one page wide; height is arbitrary —
        // we'll read contentHeight after the page finishes loading.
        wv.measure(
            View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        wv.layout(0, 0, pageWidthPx, pageHeightPx)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Re-measure now that HTML content is available
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val totalHeight = view.measuredHeight.takeIf { it > 0 }
                    ?: (view.contentHeight * view.scale).toInt()

                view.layout(0, 0, pageWidthPx, totalHeight)
                renderToPdf(view, totalHeight, outputFile, onComplete)
            }
        }

        wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }

    /** Destroy the internal WebView. Call from Fragment.onDestroyView / ViewModel.onCleared. */
    fun release() {
        webView?.destroy()
        webView = null
    }

    // -------------------------------------------------------------------------
    // PDF rendering  (no PrintDocumentAdapter — works on all API levels)
    // -------------------------------------------------------------------------

    private fun renderToPdf(
        webView: WebView,
        totalHeightPx: Int,
        outputFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        val pageCount = ceil(totalHeightPx.toFloat() / pageHeightPx).toInt().coerceAtLeast(1)
        val pdf = PdfDocument()

        try {
            repeat(pageCount) { pageIndex ->
                val pageInfo =
                    PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)

                // Translate the canvas upward so that this page's slice is visible
                page.canvas.translate(0f, -(pageIndex * pageHeightPx).toFloat())
                webView.draw(page.canvas)

                pdf.finishPage(page)
            }

            FileOutputStream(outputFile).use { pdf.writeTo(it) }
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        } finally {
            pdf.close()
        }
    }

    // -------------------------------------------------------------------------
    // HTML generation
    // -------------------------------------------------------------------------

    private fun buildHtml(data: Checklist): String {
        val sb = StringBuilder()
        sb.append(htmlHead(data.name))
        sb.append("<body>")

        // ── Title block ───────────────────────────────────────────────────────
        sb.append("""<div class="title-block"><h1>${data.name.escapeHtml()}</h1>""")
        if (data.comments.isNotBlank()) {
            sb.append("""<p class="checklist-comments">${data.comments.escapeHtml()}</p>""")
        }
        sb.append("</div>")

        // ── Sections ──────────────────────────────────────────────────────────
        data.sections.sortedBy { it.position }.forEachIndexed { sIdx, section ->
            sb.append("""<div class="section">""")
            sb.append("""<h2 class="section-title">${section.name.escapeHtml()}</h2>""")

            if (section.comments.isNotBlank()) {
                sb.append("""<p class="section-comments">${section.comments.escapeHtml()}</p>""")
            }

            section.items.sortedBy { it.position }
                .forEachIndexed { iIdx, item ->
                    val qNum = "${sIdx + 1}.${iIdx + 1}"

                    sb.append("""<div class="item">""")

                    // Header: number + question + badges
                    sb.append("""<div class="item-header">""")
                    sb.append("""<span class="q-number">$qNum</span>""")
                    sb.append("""<span class="q-text">${item.question.escapeHtml()}</span>""")
                    if (item.fromDocumentation || item.onInspection) {
                        sb.append("""<div class="q-badges">""")
                        if (item.fromDocumentation) sb.append("""<span class="badge badge-doc">Doc</span>""")
                        if (item.onInspection) sb.append("""<span class="badge badge-insp">Inspection</span>""")
                        sb.append("</div>")
                    }
                    sb.append("</div>") // item-header

                    // Guidelines
                    if (!item.guidelines.isNullOrBlank()) {
                        sb.append("""<div class="guide-box"><b>Guide:</b> ${item.guidelines.escapeHtml()}</div>""")
                    }

                    // Options
                    val sortedOptions = item.options.sortedBy { it.position }
                    if (sortedOptions.isNotEmpty()) {
                        sb.append("""<ul class="options">""")
                        sortedOptions.forEach { option ->
                            val selected = option.id == item.selectedOptionId
                            val cls = if (selected) "checked" else "unchecked"
                            val symbol = if (selected) "&#10003;" else "&nbsp;"
                            sb.append("""<li class="option $cls"><span class="checkbox">$symbol</span><span class="option-text">${option.text.escapeHtml()}</span></li>""")
                        }
                        sb.append("</ul>")
                    }

                    // Comment (only when non-blank)
                    if (item.comment.isNotBlank()) {
                        sb.append("""<div class="field-row"><span class="field-label">Comment:</span><span class="field-value">${item.comment.escapeHtml()}</span></div>""")
                    }

                    // Action taken (only when non-blank)
                    if (item.actionTaken.isNotBlank()) {
                        sb.append("""<div class="field-row"><span class="field-label">Action taken:</span><span class="field-value">${item.actionTaken.escapeHtml()}</span></div>""")
                    }

                    // Images
                    if (item.images.isNotEmpty()) {
                        sb.append("""<div class="images">""")
                        item.images.forEach { imgEntity ->
                            uriToBase64(imgEntity.uri.toString())?.let { b64 ->
                                sb.append("""<img src="data:image/jpeg;base64,$b64" class="item-image" alt=""/>""")
                            }
                        }
                        sb.append("</div>")
                    }

                    sb.append("</div>") // item
                }

            sb.append("</div>") // section
        }

        sb.append("</body></html>")
        return sb.toString()
    }

    // -------------------------------------------------------------------------
    // CSS / HTML head
    // -------------------------------------------------------------------------

    private fun htmlHead(title: String) = """
        <!DOCTYPE html><html lang="en"><head>
        <meta charset="UTF-8"/>
        <title>${title.escapeHtml()}</title>
        <style>
        *{box-sizing:border-box;margin:0;padding:0}
        body{font-family:Arial,Helvetica,sans-serif;font-size:11pt;color:#1a1a1a;background:#fff;padding:16px}

        /* Title */
        .title-block{border-bottom:3px solid #e05a1e;margin-bottom:20px;padding-bottom:10px}
        .title-block h1{font-size:18pt;color:#e05a1e;font-weight:bold}
        .checklist-comments{margin-top:6px;color:#555;font-size:10pt}

        /* Section */
        .section{margin-bottom:20px}
        .section-title{font-size:13pt;font-weight:bold;color:#fff;background:#e05a1e;padding:6px 10px;border-radius:3px;margin-bottom:8px}
        .section-comments{font-size:10pt;color:#555;margin-bottom:6px;padding-left:4px}

        /* Item card */
        .item{border:1px solid #ddd;border-left:4px solid #e05a1e;border-radius:3px;padding:10px 12px;margin-bottom:10px}
        .item-header{display:flex;align-items:flex-start;gap:8px;margin-bottom:8px}
        .q-number{font-weight:bold;color:#e05a1e;white-space:nowrap;min-width:30px;font-size:11pt}
        .q-text{font-weight:bold;flex:1;line-height:1.4}
        .q-badges{display:flex;gap:4px;flex-shrink:0}
        .badge{font-size:8pt;padding:2px 6px;border-radius:10px;font-weight:bold}
        .badge-doc{background:#dbeafe;color:#1d4ed8}
        .badge-insp{background:#dcfce7;color:#15803d}

        /* Guide */
        .guide-box{background:#f9f5f0;border:1px solid #f0d9c8;border-radius:3px;padding:6px 10px;font-size:10pt;color:#555;margin-bottom:8px;line-height:1.4}

        /* Options */
        .options{list-style:none;margin-bottom:8px}
        .option{display:flex;align-items:center;gap:8px;padding:3px 0;font-size:10.5pt}
        .checkbox{display:inline-flex;align-items:center;justify-content:center;width:16px;height:16px;border:1.5px solid #555;border-radius:2px;font-size:12pt;flex-shrink:0}
        .option.checked .checkbox{border-color:#e05a1e;background:#fff4ef;color:#e05a1e;font-weight:bold}
        .option.checked .option-text{font-weight:bold;color:#c04a10}

        /* Comment / Action */
        .field-row{display:flex;gap:6px;margin-bottom:5px;font-size:10.5pt;line-height:1.4}
        .field-label{font-weight:bold;color:#555;white-space:nowrap}
        .field-value{flex:1}

        /* Images */
        .images{display:flex;flex-wrap:wrap;gap:8px;margin-top:8px}
        .item-image{max-width:220px;max-height:180px;border:1px solid #ccc;border-radius:4px;object-fit:cover}
        </style></head>
    """.trimIndent()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a content:// or file:// URI and returns a Base64 JPEG string,
     * or null if the image cannot be loaded.
     */
    private fun uriToBase64(uriString: String): String? = runCatching {
        val uri = Uri.parse(uriString)
        val stream = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> File(uri.path!!).inputStream()
            else -> return null
        } ?: return null

        val bitmap = stream.use { BitmapFactory.decodeStream(it) } ?: return null
        val scaled = scaleBitmap(bitmap, maxDimension = 1024)

        ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }.toByteArray().let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }.getOrNull()

    private fun scaleBitmap(src: Bitmap, maxDimension: Int): Bitmap {
        val max = maxOf(src.width, src.height)
        if (max <= maxDimension) return src
        val scale = maxDimension.toFloat() / max
        return src.scale((src.width * scale).toInt(), (src.height * scale).toInt())
    }

    private fun String.escapeHtml() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
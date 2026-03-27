package com.t2dm.predia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val TAG_NATIVE_PDF = "NATIVE_PDF"

/** A4 @ 72 DPI */
private const val PAGE_W = 595
private const val PAGE_H = 842

private const val MARGIN_PT = 51f
private val CONTENT_W get() = PAGE_W - 2 * MARGIN_PT

/**
 * Builds valid PDF bytes using [PdfDocument] with full lifecycle:
 * startPage → draw (white + content) → finishPage → writeTo → flush → document.close().
 */
object NativePdfReportGenerator {

    fun generateFromBridgeJson(metaJson: String, chartPngBytes: ByteArray?): ByteArray {
        val obj = JSONObject(metaJson)
        val kind = obj.optString("kind", "risk")
        return when (kind) {
            "shap" -> generateShapReport(
                patientName = obj.optString("patientName", "Patient"),
                introBody = obj.optString("introBody", ""),
                recommendationsText = obj.optString("recommendationsText", ""),
                chartPngBytes = chartPngBytes
            )
            else -> generateRiskReport(
                patientName = obj.optString("patientName", "Patient"),
                summaryBlock = obj.optString("summaryBlock", ""),
                explanationText = obj.optString("explanationText", ""),
                chartPngBytes = chartPngBytes
            )
        }
    }

    private fun generateRiskReport(
        patientName: String,
        summaryBlock: String,
        explanationText: String,
        chartPngBytes: ByteArray?
    ): ByteArray {
        val titlePaint = titleTextPaint()
        val sectionPaint = sectionTextPaint()
        val bodyPaint = bodyTextPaint()

        return buildPdfBytes { ctx ->
            val patientPaint = bodyTextPaint().apply { textSize = 11.5f }
            var y = MARGIN_PT
            ctx.setY(y)
            y = drawTextBlock(ctx, "Diabetes Risk Report", titlePaint, y, bold = true)
            y += 6f
            y = drawTextBlock(ctx, "Patient: $patientName", patientPaint, y)
            y += 10f
            y = drawTextBlock(ctx, "1. Summary", sectionPaint, y)
            y += 4f
            y = drawMultilineBody(ctx, summaryBlock, bodyPaint, y)
            y += 8f
            y = drawTextBlock(ctx, "2. Chart", sectionPaint, y)
            y += 6f
            y = drawChartOrPlaceholder(ctx, chartPngBytes, y)
            y += 10f
            y = drawTextBlock(ctx, "3. Explanation", sectionPaint, y)
            y += 6f
            drawMultilineBody(ctx, explanationText, bodyPaint, y)
        }
    }

    private fun generateShapReport(
        patientName: String,
        introBody: String,
        recommendationsText: String,
        chartPngBytes: ByteArray?
    ): ByteArray {
        val titlePaint = titleTextPaint()
        val sectionPaint = sectionTextPaint()
        val bodyPaint = bodyTextPaint()

        return buildPdfBytes { ctx ->
            val patientPaint = bodyTextPaint().apply { textSize = 11.5f }
            var y = MARGIN_PT
            ctx.setY(y)
            y = drawTextBlock(ctx, "Model Explanation Report", titlePaint, y, bold = true)
            y += 6f
            y = drawTextBlock(ctx, "Patient: $patientName", patientPaint, y)
            y += 10f
            y = drawTextBlock(ctx, "1. Introduction", sectionPaint, y)
            y += 4f
            y = drawMultilineBody(ctx, introBody, bodyPaint, y)
            y += 8f
            y = drawTextBlock(ctx, "2. SHAP bar chart", sectionPaint, y)
            y += 6f
            y = drawChartOrPlaceholder(ctx, chartPngBytes, y)
            y += 10f
            y = drawTextBlock(ctx, "3. Recommendations", sectionPaint, y)
            y += 6f
            drawMultilineBody(ctx, recommendationsText, bodyPaint, y)
        }
    }

    private inline fun buildPdfBytes(drawPages: (PdfDrawContext) -> Unit): ByteArray {
        val document = PdfDocument()
        try {
            val ctx = PdfDrawContext(document)
            ctx.startFirstPage()
            drawPages(ctx)
            ctx.finishCurrentPageIfNeeded()
            val baos = ByteArrayOutputStream()
            Log.i(TAG_NATIVE_PDF, "PdfDocument.writeTo(ByteArrayOutputStream) starting")
            document.writeTo(baos)
            baos.flush()
            Log.i(
                TAG_NATIVE_PDF,
                "writeTo() completed size=${baos.size()} bytes; closing document next"
            )
            return baos.toByteArray()
        } finally {
            document.close()
            Log.i(TAG_NATIVE_PDF, "document.close() completed")
        }
    }

    private class PdfDrawContext(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var pageIndex = 0

        private var currentY = MARGIN_PT

        fun canvas(): Canvas = page?.canvas ?: error("No active page")

        fun y(): Float = currentY

        fun setY(y: Float) {
            currentY = y
        }

        fun startFirstPage() {
            startNewPage()
        }

        fun startNewPage() {
            finishCurrentPageIfNeeded()
            pageIndex++
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex).create()
            val p = document.startPage(info)
            page = p
            currentY = MARGIN_PT
            p.canvas.drawColor(Color.WHITE)
            Log.d(TAG_NATIVE_PDF, "startPage index=$pageIndex ${PAGE_W}x${PAGE_H} drawColor(WHITE)")
        }

        fun finishCurrentPageIfNeeded() {
            val p = page ?: return
            document.finishPage(p)
            Log.d(TAG_NATIVE_PDF, "finishPage index=$pageIndex")
            page = null
        }

        fun ensureVerticalSpace(neededPt: Float) {
            if (currentY + neededPt <= PAGE_H - MARGIN_PT) return
            Log.d(TAG_NATIVE_PDF, "page break: needed=$neededPt y=$currentY")
            startNewPage()
        }
    }

    private fun drawTextBlock(
        ctx: PdfDrawContext,
        text: String,
        paint: TextPaint,
        yStart: Float,
        bold: Boolean = false
    ): Float {
        ctx.setY(yStart)
        if (bold) {
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val layout = staticLayout(text, paint, CONTENT_W.toInt())
        val h = layout.height.toFloat() + 2f
        ctx.ensureVerticalSpace(h)
        val y = ctx.y()
        val canvas = ctx.canvas()
        canvas.save()
        canvas.translate(MARGIN_PT, y)
        layout.draw(canvas)
        canvas.restore()
        val ny = y + h
        ctx.setY(ny)
        if (bold) {
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        return ny
    }

    private fun drawMultilineBody(ctx: PdfDrawContext, text: String, paint: TextPaint, yStart: Float): Float {
        var y = yStart
        ctx.setY(y)
        val parts = text.split("\n")
        for (i in parts.indices) {
            var p = parts[i].trim()
            if (p.isEmpty()) {
                y += 5f
                ctx.setY(y)
                continue
            }
            val layout = staticLayout(p, paint, CONTENT_W.toInt())
            val h = layout.height.toFloat()
            ctx.ensureVerticalSpace(h + 8f)
            y = ctx.y()
            val canvas = ctx.canvas()
            canvas.save()
            canvas.translate(MARGIN_PT, y)
            layout.draw(canvas)
            canvas.restore()
            y += h + 6f
            ctx.setY(y)
        }
        return y
    }

    private fun drawChartOrPlaceholder(ctx: PdfDrawContext, png: ByteArray?, yStart: Float): Float {
        var y = yStart
        ctx.setY(y)
        val bmp: Bitmap? = if (png != null && png.isNotEmpty()) {
            BitmapFactory.decodeByteArray(png, 0, png.size)?.also {
                Log.i(TAG_NATIVE_PDF, "chart bitmap decoded ${it.width}x${it.height}")
            }
        } else null

        if (bmp == null) {
            val paint = bodyTextPaint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                color = Color.rgb(100, 100, 100)
            }
            return drawTextBlock(ctx, "Chart could not be captured.", paint, y)
        }

        val cw = CONTENT_W
        val scale = cw / bmp.width
        val drawH = bmp.height * scale
        ctx.ensureVerticalSpace(drawH + 4f)
        y = ctx.y()
        val c = ctx.canvas()
        val dst = RectF(MARGIN_PT, y, MARGIN_PT + cw, y + drawH)
        c.drawBitmap(bmp, null, dst, null)
        bmp.recycle()
        y += drawH + 8f
        ctx.setY(y)
        return y
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
    }

    private fun titleTextPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 20, 20)
        textSize = 18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun sectionTextPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 20, 20)
        textSize = 13f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun bodyTextPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(51, 51, 51)
        textSize = 10.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
}

package com.t2dm.predia

import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private const val TAG_PDF = "PDF_EXPORT"
private const val TAG_EXPORT = "EXPORT"
private const val TAG_WEB_CONSOLE = "WebViewConsole"
private const val TAG_APP_LAUNCH = "APP_LAUNCH"

/** HTTPS origin mapped by [WebViewAssetLoader] to `android_asset/` — avoids file:// ES module failures in WebView. */
private const val APP_WEB_ENTRY_URL = "https://appassets.androidplatform.net/assets/www/index.html"

/**
 * Top-level (non-inner) [JavascriptInterface] — some WebView builds mishandle inner-class bridges.
 * All PDF steps log with [TAG_PDF]; shared file write logs with [TAG_EXPORT].
 */
class WebExportBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun processDownload(url: String, fileName: String) {
        Log.d(TAG_EXPORT, "processDownload enter fileName=$fileName urlLen=${url.length}")
        activity.runOnUiThread { activity.handleProcessDownloadFromJs(url, fileName) }
    }

    /** Native [android.graphics.pdf.PdfDocument] path — chart PNG as chunked base64 + JSON metadata. */
    @JavascriptInterface
    fun beginNativePdfSave(fileName: String) {
        Log.i(TAG_PDF, "beginNativePdfSave fileName=$fileName thread=${Thread.currentThread().name}")
        activity.onBridgeBeginNativePdfSave(fileName)
    }

    @JavascriptInterface
    fun appendNativePdfChartChunk(chunk: String) {
        activity.onBridgeAppendNativePdfChartChunk(chunk)
    }

    @JavascriptInterface
    fun setNativePdfMetadata(json: String) {
        activity.onBridgeSetNativePdfMetadata(json)
    }

    @JavascriptInterface
    fun finishNativePdfSave() {
        Log.i(TAG_PDF, "finishNativePdfSave (bridge) thread=${Thread.currentThread().name}")
        activity.onBridgeFinishNativePdfSave()
    }

    /** PDF / JS-side diagnostics (marker not found, chunk counts, etc.) */
    @JavascriptInterface
    fun pdfBridgeLog(message: String) {
        Log.w(TAG_PDF, "JS: ${message.take(4000)}")
    }

    /** Any caught exception from injected download hook or PDF IIFE */
    @JavascriptInterface
    fun exportJsFailure(message: String) {
        Log.e(TAG_PDF, "exportJsFailure: ${message.take(4000)}")
        activity.onExportJsFailure(message.take(2000))
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader

    private var pendingSaveBytes: ByteArray? = null
    private var pendingSaveMimeType: String? = null

    private val nativePdfLock = Any()
    private val nativePdfChartB64 = StringBuilder(65536)
    private var nativePdfFileName: String? = null
    private var nativePdfMetaJson: String? = null
    private val nativePdfChunkCount = AtomicInteger(0)
    private val pdfExecutor = Executors.newSingleThreadExecutor()
    private var pdfPreparingDialog: AlertDialog? = null

    private val saveDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytes = pendingSaveBytes
        val mime = pendingSaveMimeType
        Log.d(TAG_EXPORT, "CreateDocument result uri=$uri pendingBytes=${bytes?.size} mime=$mime")
        pendingSaveBytes = null
        pendingSaveMimeType = null
        if (uri == null) {
            Log.w(TAG_EXPORT, "Save cancelled or no URI (user dismissed picker?)")
            Toast.makeText(this, "Save cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        if (bytes == null) {
            Log.e(TAG_EXPORT, "BUG: bytes null while uri present — state lost?")
            Toast.makeText(this, "Save failed: internal error (no data).", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        try {
            val out = contentResolver.openOutputStream(uri)
            if (out == null) {
                Log.e(TAG_EXPORT, "openOutputStream returned null for $uri")
                Toast.makeText(this, "Could not open file for writing.", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            out.use { stream: OutputStream ->
                stream.write(bytes)
            }
            Log.i(TAG_EXPORT, "Write OK fileSizeBytes=${bytes.size} uri=$uri")
            Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG_EXPORT, "CRASH writing export", e)
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG_APP_LAUNCH, "=== ENTERING MainActivity onCreate ===")
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.app_dark_bg)
            }

            assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
                .build()
            Log.d(TAG_APP_LAUNCH, "WebViewAssetLoader ready (maps /assets/ → android_asset/)")

            webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.mediaPlaybackRequiresUserGesture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            addJavascriptInterface(WebExportBridge(this@MainActivity), "AndroidDownload")

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                    message?.let { m ->
                        val text = "${m.message()} — ${m.sourceId()}:${m.lineNumber()}"
                        when (m.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG_WEB_CONSOLE, text)
                            ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG_WEB_CONSOLE, text)
                            else -> Log.d(TAG_WEB_CONSOLE, text)
                        }
                    }
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val resp = assetLoader.shouldInterceptRequest(request.url)
                    if (resp == null && request.url.toString().contains("appassets.androidplatform.net")) {
                        Log.w(TAG_APP_LAUNCH, "shouldInterceptRequest: no handler for ${request.url}")
                    }
                    return resp
                }

                /**
                 * Only main-frame navigations are overridden. Returning true for subresource
                 * requests (scripts/modules) breaks loading on some WebView builds — blank blue screen.
                 */
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val r = request ?: return false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !r.isForMainFrame) {
                        return false
                    }
                    val url = r.url?.toString() ?: return false
                    if (isAppBundledContentUrl(url)) {
                        return false
                    }
                    Log.d(TAG_APP_LAUNCH, "Blocking non-app navigation: $url")
                    return true
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val isMain = request?.isForMainFrame == true
                        Log.e(
                            TAG_APP_LAUNCH,
                            "onReceivedError mainFrame=$isMain url=${request?.url} code=${error?.errorCode} desc=${error?.description}"
                        )
                    }
                    super.onReceivedError(view, request, error)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG_APP_LAUNCH, "WebView onPageFinished url=$url")
                    view?.evaluateJavascript(
                        "(function(){try{var r=document.getElementById('root');return r&&(r.childElementCount>0||((r.textContent||'').trim().length>0))?'root_has_content':'root_empty';}catch(e){return 'eval_err:'+e;}})();"
                    ) { value ->
                        Log.d(TAG_APP_LAUNCH, "JS root check: $value (expect \"root_has_content\" when login UI is up)")
                        if (value == "\"root_empty\"" || value == "null") {
                            Log.w(TAG_APP_LAUNCH, "Login screen not yet painted — if this persists, check WebViewConsole for JS errors")
                        } else {
                            Log.d(TAG_APP_LAUNCH, "Login / app UI visible (root populated)")
                        }
                    }
                    view?.loadUrl(
                        """
                        javascript:(function() {
                            function handleDownloadLink(a) {
                                var href = (a && a.href) ? a.href : '';
                                var name = (a && a.download) ? (a.download + '').trim() : 'export';
                                if (!name || name === '') name = 'export';
                                if (href.startsWith('data:')) {
                                    try { AndroidDownload.processDownload(href, name); } catch (e) {
                                        try { AndroidDownload.exportJsFailure('data-link:' + e); } catch (z) {}
                                    }
                                    return true;
                                }
                                if (href.startsWith('blob:')) {
                                    fetch(href).then(function(r) { return r.blob(); }).then(function(blob) {
                                        var reader = new FileReader();
                                        reader.onloadend = function() {
                                            try { AndroidDownload.processDownload(reader.result, name); } catch (e) {
                                                try { AndroidDownload.exportJsFailure('blob-link:' + e); } catch (z) {}
                                            }
                                        };
                                        reader.readAsDataURL(blob);
                                    }).catch(function(err) {
                                        try { AndroidDownload.exportJsFailure('blob-fetch:' + err); } catch (z) {}
                                    });
                                    return true;
                                }
                                return false;
                            }
                            document.addEventListener('click', function(ev) {
                                var t = ev.target;
                                while (t && t !== document) {
                                    if (t.tagName && t.tagName.toLowerCase() === 'a') {
                                        var href = t.href || '';
                                        if (href.startsWith('data:') || href.startsWith('blob:')) {
                                            ev.preventDefault();
                                            ev.stopPropagation();
                                            if (handleDownloadLink(t)) return false;
                                        }
                                        break;
                                    }
                                    t = t.parentNode;
                                }
                            }, true);
                            var origCreate = document.createElement;
                            document.createElement = function(tagName) {
                                var el = origCreate.call(document, tagName);
                                if (tagName.toLowerCase() === 'a') {
                                    var origClick = el.click;
                                    el.click = function() {
                                        if (handleDownloadLink(this)) return;
                                        if (origClick) origClick.call(this);
                                    };
                                }
                                return el;
                            };
                        })();
                        """.trimIndent()
                    )
                }
            }

            setDownloadListener { url, _, _, mimetype, _ ->
                Log.d(TAG_EXPORT, "DownloadListener urlLen=${url.length} mime=$mimetype")
                handleProcessDownloadFromJs(url, mimetype, "export")
            }
        }

            setContentView(webView)
            Log.d(TAG_APP_LAUNCH, "setContentView(WebView) done")

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })

            Log.d(TAG_APP_LAUNCH, "Loading bundled web UI: $APP_WEB_ENTRY_URL")
            webView.loadUrl(APP_WEB_ENTRY_URL)
            Log.d(TAG_APP_LAUNCH, "loadUrl submitted (async)")
        } catch (e: Throwable) {
            Log.e(TAG_APP_LAUNCH, "CRASH in MainActivity.onCreate — UI will not load", e)
            throw e
        }
    }

    private fun isAppBundledContentUrl(url: String): Boolean {
        return url.startsWith("file:///android_asset/www") ||
            url.startsWith("https://appassets.androidplatform.net/assets/www")
    }

    override fun onDestroy() {
        dismissPdfPreparingDialog()
        pdfExecutor.shutdown()
        super.onDestroy()
    }

    // --- Called from WebExportBridge (native PdfDocument path) ---

    fun onBridgeBeginNativePdfSave(fileName: String) {
        synchronized(nativePdfLock) {
            nativePdfChartB64.setLength(0)
            nativePdfFileName = fileName.trim().ifBlank { "export.pdf" }
            nativePdfMetaJson = null
            nativePdfChunkCount.set(0)
        }
        showPdfPreparingDialog()
    }

    fun onBridgeAppendNativePdfChartChunk(chunk: String) {
        if (chunk.isEmpty()) return
        val totalLen = synchronized(nativePdfLock) {
            nativePdfChartB64.append(chunk)
            val n = nativePdfChunkCount.incrementAndGet()
            if (n == 1 || n % 32 == 0) {
                Log.d(
                    TAG_PDF,
                    "appendNativePdfChartChunk #$n chunkLen=${chunk.length} cumulativeB64Len=${nativePdfChartB64.length}"
                )
            }
            nativePdfChartB64.length
        }
        if (totalLen > 40_000_000) {
            Log.e(TAG_PDF, "Chart base64 buffer too large, aborting")
            synchronized(nativePdfLock) {
                nativePdfChartB64.setLength(0)
                nativePdfFileName = null
                nativePdfMetaJson = null
            }
            onExportJsFailure("Chart image too large to export.")
        }
    }

    fun onBridgeSetNativePdfMetadata(json: String) {
        synchronized(nativePdfLock) {
            nativePdfMetaJson = json
            Log.i(TAG_PDF, "setNativePdfMetadata len=${json.length}")
        }
    }

    fun onBridgeFinishNativePdfSave() {
        val name: String
        val chartB64: String
        val meta: String?
        synchronized(nativePdfLock) {
            name = nativePdfFileName ?: "export.pdf"
            nativePdfFileName = null
            chartB64 = nativePdfChartB64.toString()
            nativePdfChartB64.setLength(0)
            meta = nativePdfMetaJson
            nativePdfMetaJson = null
        }
        Log.i(TAG_PDF, "finishNativePdfSave name=$name chartB64Chars=${chartB64.length} metaLen=${meta?.length}")
        if (meta.isNullOrBlank()) {
            dismissPdfPreparingDialog()
            runOnUiThread {
                Log.e(TAG_PDF, "missing metadata JSON")
                Toast.makeText(this, "PDF error: missing report data.", Toast.LENGTH_LONG).show()
            }
            return
        }
        pdfExecutor.execute {
            try {
                val chartBytes: ByteArray? = if (chartB64.isNotBlank()) {
                    val cleaned = chartB64.replace(Regex("\\s"), "")
                    Log.d(TAG_PDF, "decode chart PNG base64 cleanedLen=${cleaned.length}")
                    val png = Base64.decode(cleaned, Base64.DEFAULT)
                    Log.i(TAG_PDF, "chart PNG decoded bytes=${png.size}")
                    if (png.isEmpty()) null else png
                } else {
                    Log.w(TAG_PDF, "no chart base64 — placeholder in PDF")
                    null
                }
                val pdfBytes = NativePdfReportGenerator.generateFromBridgeJson(meta, chartBytes)
                Log.i(
                    TAG_PDF,
                    "native PDF generated bytes=${pdfBytes.size} startsWithPdf=${pdfBytes.startsWithPdfMagic()}"
                )
                if (pdfBytes.isEmpty() || !pdfBytes.startsWithPdfMagic()) {
                    runOnUiThread {
                        dismissPdfPreparingDialog()
                        Toast.makeText(this, "PDF error: generation failed.", Toast.LENGTH_LONG).show()
                    }
                    return@execute
                }
                runOnUiThread {
                    dismissPdfPreparingDialog()
                    try {
                        exportBytesViaDocumentPicker(
                            bytes = pdfBytes,
                            mimeType = "application/pdf",
                            displayName = ensureFileName(name, "application/pdf"),
                            logTagPdf = true
                        )
                    } catch (e: Exception) {
                        Log.e(TAG_PDF, "CRASH launching save UI", e)
                        Toast.makeText(this, "PDF error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG_PDF, "CRASH native PDF pipeline", e)
                runOnUiThread {
                    dismissPdfPreparingDialog()
                    Toast.makeText(this, "PDF error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onExportJsFailure(message: String) {
        dismissPdfPreparingDialog()
        runOnUiThread {
            Toast.makeText(this, "Export: $message", Toast.LENGTH_LONG).show()
        }
    }

    // --- CSV / generic data URL from JS ---

    fun handleProcessDownloadFromJs(url: String, suggestedFileName: String) {
        handleProcessDownloadFromJs(url, "", suggestedFileName)
    }

    private fun handleProcessDownloadFromJs(url: String, mimetype: String, suggestedFileName: String) {
        try {
            if (url.startsWith("data:")) {
                Log.d(TAG_EXPORT, "handleProcessDownload data URL len=${url.length}")
                val parsed = parseDataUrl(url)
                if (parsed == null || parsed.bytes.isEmpty()) {
                    Log.e(TAG_EXPORT, "parseDataUrl failed or empty")
                    Toast.makeText(this, "Download error: invalid data", Toast.LENGTH_SHORT).show()
                    return
                }
                val mimeType = when {
                    parsed.mimeType.isNotBlank() -> parsed.mimeType
                    mimetype.isNotBlank() -> mimetype
                    suggestedFileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    suggestedFileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
                    else -> "application/octet-stream"
                }
                val finalFileName = ensureFileName(suggestedFileName, mimeType)
                exportBytesViaDocumentPicker(parsed.bytes, mimeType, finalFileName, logTagPdf = false)
            } else if (url.startsWith("blob:")) {
                Log.d(TAG_EXPORT, "blob URL ignored in native layer")
            } else {
                val fallbackMime = if (mimetype.isBlank()) "application/octet-stream" else mimetype
                val finalFileName = ensureFileName(suggestedFileName, fallbackMime)
                Toast.makeText(this, "Downloading $finalFileName…", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG_EXPORT, "CRASH handleProcessDownloadFromJs", e)
            Toast.makeText(this, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportBytesViaDocumentPicker(
        bytes: ByteArray,
        mimeType: String,
        displayName: String,
        logTagPdf: Boolean
    ) {
        val tag = if (logTagPdf) TAG_PDF else TAG_EXPORT
        Log.i(
            tag,
            "exportBytesViaDocumentPicker displayName=$displayName mime=$mimeType fileSizeBytes=${bytes.size}"
        )
        if (bytes.isEmpty()) {
            Log.e(tag, "refused empty export")
            Toast.makeText(this, "Nothing to save.", Toast.LENGTH_SHORT).show()
            return
        }
        pendingSaveBytes = bytes
        pendingSaveMimeType = mimeType
        try {
            Log.i(tag, "CreateDocument.launch($displayName)")
            saveDocumentLauncher.launch(displayName)
        } catch (e: Exception) {
            Log.e(tag, "CRASH CreateDocument.launch", e)
            pendingSaveBytes = null
            pendingSaveMimeType = null
            Toast.makeText(this, "Could not open save dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPdfPreparingDialog() {
        runOnUiThread {
            if (pdfPreparingDialog?.isShowing == true) return@runOnUiThread
            pdfPreparingDialog = AlertDialog.Builder(this)
                .setTitle("Preparing PDF")
                .setMessage("Please wait…")
                .setCancelable(false)
                .create()
            pdfPreparingDialog?.show()
            Log.d(TAG_PDF, "Preparing dialog shown")
        }
    }

    private fun dismissPdfPreparingDialog() {
        runOnUiThread {
            pdfPreparingDialog?.dismiss()
            pdfPreparingDialog = null
            Log.d(TAG_PDF, "Preparing dialog dismissed")
        }
    }

    private data class ParsedDataUrl(
        val bytes: ByteArray,
        val mimeType: String
    )

    private fun parseDataUrl(url: String): ParsedDataUrl? {
        val commaIndex = url.indexOf(',')
        if (commaIndex <= 5 || commaIndex >= url.length - 1) return null

        val metadata = url.substring(5, commaIndex)
        val payload = url.substring(commaIndex + 1)
        val isBase64 = metadata.contains(";base64", ignoreCase = true)
        val mimeType = metadata.substringBefore(';').ifBlank { "application/octet-stream" }

        val bytes = if (isBase64) {
            Base64.decode(payload.trim(), Base64.DEFAULT)
        } else {
            URLDecoder.decode(payload, StandardCharsets.UTF_8.name()).toByteArray(StandardCharsets.UTF_8)
        }

        return ParsedDataUrl(bytes = bytes, mimeType = mimeType)
    }

    private fun ensureFileName(rawName: String, mimeType: String): String {
        var baseName = rawName.trim().ifBlank { "export" }
        val extension = when {
            mimeType.contains("pdf", ignoreCase = true) -> "pdf"
            mimeType.contains("csv", ignoreCase = true) -> "csv"
            else -> null
        }
        if (extension != null) {
            val dotExt = ".$extension"
            val dupExt = dotExt + dotExt
            while (baseName.lowercase().endsWith(dupExt)) {
                baseName = baseName.dropLast(dotExt.length)
            }
            if (baseName.lowercase().endsWith(dotExt)) return baseName
        }
        if (baseName.contains('.')) return baseName
        val fallbackExt = extension ?: "bin"
        return "$baseName.$fallbackExt"
    }

    private fun ByteArray.startsWithPdfMagic(): Boolean {
        val sig = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        if (size < sig.size) return false
        for (i in sig.indices) {
            if (this[i] != sig[i]) return false
        }
        return true
    }
}

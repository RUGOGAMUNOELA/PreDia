package com.t2dm.predia

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    // Pending file to write when user picks location (like CamScanner: Save → choose folder → done)
    private var pendingSaveBytes: ByteArray? = null
    private var pendingSaveMimeType: String? = null

    private val saveDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytes = pendingSaveBytes
        val mime = pendingSaveMimeType
        pendingSaveBytes = null
        pendingSaveMimeType = null
        if (uri == null || bytes == null) return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                out.write(bytes)
            }
            Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Match status bar to app dark background (no purple/blue bar at top)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.app_dark_bg)
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            
            // Critical for some modern JS PDF/CSV libraries
            settings.mediaPlaybackRequiresUserGesture = false

            addJavascriptInterface(AndroidDownloadInterface(), "AndroidDownload")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return !url.startsWith("file:///android_asset/www")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Intercept ANY click on blob/data links (not just createElement-created ones) so Save PDF / Export CSV work
                    view?.loadUrl("""
                        javascript:(function() {
                            function handleDownloadLink(a) {
                                var href = (a && a.href) ? a.href : '';
                                var name = (a && a.download) ? (a.download + '').trim() : 'export';
                                if (!name || name === '') name = 'export';
                                if (href.startsWith('data:')) {
                                    try { AndroidDownload.processDownload(href, name); } catch (e) {}
                                    return true;
                                }
                                if (href.startsWith('blob:')) {
                                    fetch(href).then(function(r) { return r.blob(); }).then(function(blob) {
                                        var reader = new FileReader();
                                        reader.onloadend = function() {
                                            try { AndroidDownload.processDownload(reader.result, name); } catch (e) {}
                                        };
                                        reader.readAsDataURL(blob);
                                    }).catch(function() {});
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
                    """.trimIndent())
                }
            }

            setDownloadListener { url, _, _, mimetype, _ ->
                handleDownload(url, mimetype, "export")
            }
        }

        setContentView(webView)

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

        webView.loadUrl("file:///android_asset/www/index.html")
    }

    inner class AndroidDownloadInterface {
        @JavascriptInterface
        fun processDownload(url: String, fileName: String) {
            runOnUiThread {
                handleDownload(url, "", fileName)
            }
        }
    }

    private fun handleDownload(url: String, mimetype: String, suggestedFileName: String) {
        try {
            val extension = if (url.contains("pdf") || suggestedFileName.endsWith(".pdf")) "pdf" else "csv"
            val finalFileName = if (suggestedFileName.contains(".")) suggestedFileName else "$suggestedFileName.$extension"
            
            if (url.startsWith("data:")) {
                // Data URL format: data:[<mediatype>][;base64],<data> — payload is after the comma
                val commaIndex = url.indexOf(',', 5)
                val base64Data = if (commaIndex in (6..url.length - 1)) {
                    url.substring(commaIndex + 1).trim()
                } else {
                    url.substringAfter("base64,", "").trim()
                }
                if (base64Data.isEmpty()) {
                    Toast.makeText(this, "Download error: invalid data", Toast.LENGTH_SHORT).show()
                    return
                }
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                if (decodedBytes.isEmpty()) {
                    Toast.makeText(this, "Download error: empty file", Toast.LENGTH_SHORT).show()
                    return
                }
                saveAndOpenFile(decodedBytes, finalFileName, if (extension == "pdf") "application/pdf" else "text/csv")
            } else if (url.startsWith("blob:")) {
                // Blob URLs can't be read in native code. JS must convert to data URL and call processDownload again.
                // Don't try to decode blob URL (would throw) and don't show misleading error.
                return
            } else {
                Toast.makeText(this, "Downloading $finalFileName...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveAndOpenFile(bytes: ByteArray, fileName: String, mimeType: String) {
        pendingSaveBytes = bytes
        pendingSaveMimeType = mimeType
        saveDocumentLauncher.launch(fileName)
    }
}

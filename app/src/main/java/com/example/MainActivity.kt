package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var webView: WebView? = null
    private var customView: View? = null
    private var originalSystemUiVisibility: Int = 0

    companion object {
        private const val PLATFORM_URL = "https://zoomdz.com"
        private val INTERNAL_DOMAINS = listOf(
            "zoomdz.com",
            "www.zoomdz.com",
            "zooooooom-mown.vercel.app"
        )
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback != null) {
            val results = if (result.resultCode == RESULT_OK) {
                val dataString = result.data?.dataString
                val clipData = result.data?.clipData
                if (clipData != null) {
                    val count = clipData.itemCount
                    Array(count) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    arrayOf(Uri.parse(dataString))
                } else {
                    null
                }
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!cameraGranted || !audioGranted) {
            Toast.makeText(this, "يرجى منح صلاحيات الكاميرا والمايك لدعم البث المباشر", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.parseColor("#0B172A")

        requestPermissions()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ComposeColor(0xFF0B172A)
                ) {
                    ZoomDzApp()
                }
            }
        }
    }

    @Composable
    private fun ZoomDzApp() {
        var isLoading by remember { mutableStateOf(true) }
        val context = remember { this }

        Box(modifier = Modifier.fillMaxSize()) {
            ZoomDzWebView(
                url = PLATFORM_URL,
                onPageFinished = { isLoading = false }
            )

            if (isLoading) {
                LoadingScreen()
            }
        }
    }

    @Composable
    private fun LoadingScreen() {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ComposeColor(0xFF0B172A),
                            ComposeColor(0xFF1E3A8A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(ComposeColor(0xFF1E40AF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = ComposeColor.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = ComposeColor(0xFF60A5FA),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ZoomDz",
                    color = ComposeColor.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "جاري التحميل...",
                    color = ComposeColor(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        }
    }

    private fun isInternalUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        return INTERNAL_DOMAINS.any { host.equals(it, ignoreCase = true) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun ZoomDzWebView(url: String, onPageFinished: (String) -> Unit) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false

                            if (isInternalUrl(targetUrl) || targetUrl.startsWith("file://") || targetUrl.contains("localhost")) {
                                return false
                            }

                            if (targetUrl.startsWith("intent://")) {
                                try {
                                    val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                    if (intent.resolveActivity(ctx.packageManager) != null) {
                                        ctx.startActivity(intent)
                                        return true
                                    }
                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                    if (fallbackUrl != null) {
                                        view?.loadUrl(fallbackUrl)
                                        return true
                                    }
                                } catch (e: Exception) {
                                    return true
                                }
                            }

                            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ctx.startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    return false
                                }
                            }

                            return false
                        }

                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            finishedUrl?.let { onPageFinished(it) }
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                onPageFinished(url)
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            this@MainActivity.filePathCallback?.onReceiveValue(null)
                            this@MainActivity.filePathCallback = filePathCallback

                            val intent = fileChooserParams?.createIntent()
                            if (intent != null) {
                                try {
                                    fileChooserLauncher.launch(intent)
                                } catch (e: Exception) {
                                    this@MainActivity.filePathCallback = null
                                    return false
                                }
                            }
                            return true
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }

                        // Fullscreen support for video/live streams
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            (window.decorView as FrameLayout).addView(
                                view,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                            originalSystemUiVisibility = window.decorView.systemUiVisibility
                            window.decorView.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                        }

                        override fun onHideCustomView() {
                            (window.decorView as? FrameLayout)?.removeView(customView)
                            customView = null
                            window.decorView.systemUiVisibility = originalSystemUiVisibility
                        }
                    }

                    // Download support
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            request.allowScanningByMediaScanner()
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                Uri.parse(downloadUrl).lastPathSegment ?: "zoomdz_download"
                            )
                            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(ctx, "جاري تنزيل الملف...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                ctx.startActivity(i)
                            } catch (ex: Exception) {
                                Toast.makeText(ctx, "تعذر تنزيل الملف", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        textZoom = 100
                    }

                    // Inject a small JS bridge to handle share/copy from the web platform
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun shareText(text: String) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            startActivity(Intent.createChooser(intent, "مشاركة عبر"))
                        }

                        @JavascriptInterface
                        fun copyToClipboard(text: String) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ZoomDz", text))
                            Toast.makeText(this@MainActivity, "تم النسخ", Toast.LENGTH_SHORT).show()
                        }
                    }, "ZoomDzNative")

                    loadUrl(url)
                }
            },
            update = { view ->
                webView = view
            }
        )
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customView != null) {
            (window.decorView as? FrameLayout)?.removeView(customView)
            customView = null
            window.decorView.systemUiVisibility = originalSystemUiVisibility
            return
        }
        val tempWebView = webView
        if (tempWebView != null && tempWebView.canGoBack()) {
            tempWebView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            removeJavascriptInterface("ZoomDzNative")
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}

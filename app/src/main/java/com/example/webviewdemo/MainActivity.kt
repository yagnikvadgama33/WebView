package com.example.webviewdemo

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.webviewdemo.ui.theme.WebViewDemoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var downloadManager: DownloadManager
    private lateinit var webView: WebView
    private var isPermissionGranted = false

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    isPermissionGranted = true
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }

        // Register back press callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        // File picker launcher
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    filePathCallback?.onReceiveValue(arrayOf(uri))
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

        setContent {
            WebViewDemoTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("WebView Demo") },
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WebViewScreen(innerPadding)
                }
            }
        }
    }

    @Composable
    fun WebViewScreen(paddingValues: PaddingValues) {
        val context = LocalContext.current
        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var showProgressDialog by remember { mutableStateOf(false) }
        var progress by remember { mutableIntStateOf(0) }

        if (showProgressDialog) {
            DownloadProgressDialog(progress = progress)
        }

        AndroidView(
            modifier = Modifier.padding(paddingValues),
            factory = {
                webView = WebView(context).apply {
                    this.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        domStorageEnabled = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false
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

                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }
                            filePickerLauncher.launch(intent)
                            return true
                        }
                    }
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                // Request permission if not granted
                                permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                return@setDownloadListener
                            }
                        }

                        // Proceed with the download
                        val request = DownloadManager.Request(Uri.parse(url.trim().replace("blob:",""))).apply {
                            setMimeType(mimeType)
                            addRequestHeader("User-Agent", userAgent)
                            setDescription("Downloading file...")
                            setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                URLUtil.guessFileName(url, contentDisposition, mimeType)
                            )
                        }

                        val downloadId = downloadManager.enqueue(request)
                        showProgressDialog = true

                        // Monitor download progress
                        CoroutineScope(Dispatchers.IO).launch {
                            var downloading = true
                            while (downloading) {
                                val query = DownloadManager.Query().setFilterById(downloadId)
                                val cursor = downloadManager.query(query)
                                if (cursor.moveToFirst()) {
                                    val status =
                                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                        downloading = false
                                        progress = 100
                                    } else if (status == DownloadManager.STATUS_FAILED) {
                                        downloading = false
                                        progress = -1
                                    } else {
                                        val totalSize =
                                            cursor.getLong(
                                                cursor.getColumnIndexOrThrow(
                                                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                                )
                                            )
                                        val downloadedSize =
                                            cursor.getLong(
                                                cursor.getColumnIndexOrThrow(
                                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                                )
                                            )

                                        if (totalSize > 0) {
                                            progress =
                                                ((downloadedSize * 100) / totalSize).toInt()
                                        }
                                    }
                                }
                                cursor.close()
                                delay(500)
                            }
                            // Ensure UI updates on the main thread when download finishes
                            withContext(Dispatchers.Main) {
                                showProgressDialog = false
                            }
                        }
                    }
                }
                webView
            },
            update = {
                it.loadUrl("https://convertio.co/")
            }
        )
    }

    @Composable
    fun DownloadProgressDialog(progress: Int) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Downloading...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                if (progress >= 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Progress: $progress%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = "Download failed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                if (progress == 100) {
                    Toast.makeText(LocalContext.current, "Download Complete", Toast.LENGTH_SHORT)
                        .show()
                } else if (progress == -1) {
                    Toast.makeText(LocalContext.current, "Download Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium
        )
    }
}








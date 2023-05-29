package com.salvier.online.pokies.austrmob


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var uploadMessage: ValueCallback<Array<Uri>>? = null
    val REQUEST_SELECT_FILE = 100
    private val FILECHOOSER_RESULTCODE = 1
    val pmmCookies: CookieManager = CookieManager.getInstance()
    val ONESIGNAL_APP_ID = "2e4b0e8a-b8c7-4990-a1d0-63150127d8d3"

    private var progress: Progress? = null
    private var isLoaded: Boolean = false
    private var doubleBackToExitPressedOnce = false
    private var webURL = "https://kwekerijloos.nl/ro/voorpagina-ro/"

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        // OneSignal Initialization
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        setContentView(R.layout.activity_main)
        val webView = findViewById<WebView>(R.id.webView)
        CookieManager.setAcceptFileSchemeCookies(true)

        webView.settings.apply {
            useWideViewPort = true
            javaScriptEnabled = true
            mixedContentMode = 0
            loadWithOverviewMode = true
            allowFileAccess = true
            domStorageEnabled = true
            defaultTextEncodingName = "utf-8"
            databaseEnabled = true
            allowFileAccessFromFileURLs = true
            setAppCacheEnabled(true)
            javaScriptCanOpenWindowsAutomatically = true

        }

        if (!isOnline()) {
            val infoTV = findViewById<TextView>(R.id.infoTV)
            showToast(getString(R.string.no_internet))
            infoTV.text = getString(R.string.no_internet)
            showNoNetSnackBar()
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        if (isOnline() && !isLoaded) loadWebView()
        super.onResume()
    }

    private fun loadWebView() {
        val webView = findViewById<WebView>(R.id.webView)
        val infoTV = findViewById<TextView>(R.id.infoTV)
        infoTV.text = ""
        webView.loadUrl(webURL)
        webView.webViewClient = object : WebViewClient() {
            fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams):Boolean {
                var mFilePathCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("*/*")
                val PICKFILE_REQUEST_CODE = 100
                startActivityForResult(intent, PICKFILE_REQUEST_CODE)
                return true
            }


            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("tg:")) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                } else {
                    view?.loadUrl(url)
                }
                return super.shouldOverrideUrlLoading(view, request)

            }


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                setProgressDialogVisibility(false)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoaded = true
                setProgressDialogVisibility(false)
                super.onPageFinished(view, url)

                Log.e("onPageFinished", "$url")
                getSharedPreferences(packageName,
                    MODE_PRIVATE).edit().putString("finished", url).apply()
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                val infoTV = findViewById<TextView>(R.id.infoTV)
                isLoaded = false
                val errorMessage = "Got Error!"
                showToast(errorMessage)
                infoTV.text = errorMessage
                setProgressDialogVisibility(false)
                super.onReceivedError(view, request, error)
            }
        }
    }
    override fun onBackPressed() {
        if (webView.isFocused && webView.canGoBack()) {
            webView.goBack()
        }else {
            Log.d("TAG", "can`t go back")
        }
        this.doubleBackToExitPressedOnce = true

        Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessage == null) return
            uploadMessage!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    intent
                )
            )
            uploadMessage = null
        }
    }

    private fun setProgressDialogVisibility(visible: Boolean) {
        if (visible) progress = Progress(this, R.string.please_wait, cancelable = true)
        progress?.apply { if (visible) show() else dismiss() }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showNoNetSnackBar() {
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootView)
        val snack =
            Snackbar.make(rootView, getString(R.string.no_internet), Snackbar.LENGTH_INDEFINITE)
        snack.setAction(getString(R.string.settings)) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        snack.show()
    }
}
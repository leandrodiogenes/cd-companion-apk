package io.github.leandrodiogenes.cdcompanion

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.app.AlertDialog
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var wsClient: WsClient
    private var connected = false
    private var isFullscreen = false
    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = findViewById(R.id.webView)
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isFullscreen) toggleFullscreen()
                return true
            }
        })
        setupWebView()

        wsClient = WsClient(
            onMessage = { json -> runOnUiThread { forwardToJs(json) } },
            onStatusChange = { isConnected ->
                connected = isConnected
                runOnUiThread {
                    invalidateOptionsMenu()
                    val status = JSONObject()
                    status.put("type", "status")
                    status.put("connected", isConnected)
                    forwardToJs(status.toString())
                    if (isConnected) {
                        val opts = JSONObject()
                        opts.put("cmd", "client_options")
                        opts.put("clientName", "android")
                        wsClient.send(opts.toString())
                    }
                }
            }
        )
        reconnectFromPrefs()

        toggleFullscreen()

        checkForUpdate()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun sendCmd(json: String) {
                wsClient.send(json)
            }

            @JavascriptInterface
            fun onReady() {
                // JS sinaliza que está pronto — nenhuma ação necessária
            }
        }, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val host = android.net.Uri.parse(url).host ?: return true
                if (host == "mapgenie.io" || host.endsWith(".mapgenie.io")) return false
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectJs()
            }
        }

        webView.loadUrl("https://mapgenie.io/crimson-desert/maps/pywel")
    }

    private fun injectJs() {
        val css = assets.open("overlay.css").bufferedReader().use { it.readText() }
        val escapedCss = css.replace("\\", "\\\\").replace("`", "\\`")
        webView.evaluateJavascript(
            """(function(){
                var s=document.createElement('style');
                s.id='cdp-injected-css';
                if(!document.getElementById('cdp-injected-css'))document.head.appendChild(s);
                s.textContent=`$escapedCss`;
            })()""", null
        )
        val js = assets.open("companion.js").bufferedReader().use { it.readText() }
        webView.evaluateJavascript(js) {
            // JS injetado — envia status atual para sincronizar o overlay
            val status = JSONObject()
            status.put("type", "status")
            status.put("connected", connected)
            forwardToJs(status.toString())
        }
    }

    private fun forwardToJs(json: String) {
        val b64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webView.evaluateJavascript(
            "if(window.cdOnMessage)window.cdOnMessage(atob('$b64'))",
            null
        )
    }

    private fun reconnectFromPrefs() {
        val prefs = getSharedPreferences("cdcompanion", MODE_PRIVATE)
        val host = prefs.getString("ws_host", "10.0.0.9") ?: "10.0.0.9"
        val port = prefs.getInt("ws_port", 7891)
        wsClient.connect(host, port)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_status)?.setIcon(
            if (connected) R.drawable.ic_connected else R.drawable.ic_disconnected
        )
        menu.findItem(R.id.action_fullscreen)?.setTitle(
            if (isFullscreen) R.string.exit_fullscreen else R.string.fullscreen
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_fullscreen -> {
                toggleFullscreen()
                true
            }
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity::class.java), REQUEST_SETTINGS)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkForUpdate() {
        UpdateChecker.check(this, BuildConfig.VERSION_NAME) { tag, url ->
            AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage("Version $tag is available. Update now?")
                .setCancelable(true)
                .setPositiveButton("Update") { _, _ ->
                    if (!packageManager.canRequestPackageInstalls()) {
                        startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = android.net.Uri.parse("package:$packageName")
                            }
                        )
                        return@setPositiveButton
                    }
                    val toast = Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG)
                    toast.show()
                    UpdateChecker.downloadAndInstall(this, url) { toast.cancel() }
                }
                .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun toggleFullscreen() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
            supportActionBar?.show()
            isFullscreen = false
            getSharedPreferences("cdcompanion", MODE_PRIVATE).edit().putBoolean("fullscreen", false).apply()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            supportActionBar?.hide()
            isFullscreen = true
            getSharedPreferences("cdcompanion", MODE_PRIVATE).edit().putBoolean("fullscreen", true).apply()
            Toast.makeText(this, "Double tap anywhere to exit fullscreen", Toast.LENGTH_LONG).show()
        }
        invalidateOptionsMenu()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
            wsClient.disconnect()
            reconnectFromPrefs()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        val url = webView.url ?: ""
        if (!url.contains("mapgenie.io")) {
            webView.loadUrl("https://mapgenie.io/crimson-desert/maps/pywel")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wsClient.disconnect()
    }

    companion object {
        private const val REQUEST_SETTINGS = 1001
    }
}

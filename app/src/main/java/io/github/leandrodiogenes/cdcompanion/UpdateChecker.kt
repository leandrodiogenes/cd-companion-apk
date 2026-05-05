package io.github.leandrodiogenes.cdcompanion

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UpdateChecker {
    private const val RELEASES_URL =
        "https://api.github.com/repos/leandrodiogenes/cd-companion-apk/releases/latest"

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    fun check(context: Context, currentVersion: String, onUpdate: (tag: String, url: String) -> Unit) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val json = JSONObject(body)
                    val tag = json.getString("tag_name").trimStart('v')
                    if (tag == currentVersion) return
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            val url = asset.getString("browser_download_url")
                            handler.post { onUpdate(tag, url) }
                            return
                        }
                    }
                } catch (_: Exception) {}
            }
        })
    }

    fun downloadAndInstall(context: Context, url: String, onDone: () -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post { onDone() }
            }

            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes() ?: run { handler.post { onDone() }; return }
                val dir = File(context.cacheDir, "apk").apply { mkdirs() }
                val file = File(dir, "update.apk")
                FileOutputStream(file).use { it.write(bytes) }
                handler.post {
                    onDone()
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        })
    }
}

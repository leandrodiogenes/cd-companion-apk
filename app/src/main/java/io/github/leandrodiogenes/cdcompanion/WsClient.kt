package io.github.leandrodiogenes.cdcompanion

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WsClient(
    private val onMessage: (String) -> Unit,
    private val onStatusChange: (Boolean) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var url: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var stopped = false
    private var delayIndex = 0
    private val delays = longArrayOf(3000L, 5000L, 10000L)

    fun connect(host: String, port: Int) {
        stopped = false
        delayIndex = 0
        url = "ws://$host:$port"
        openSocket()
    }

    fun disconnect() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        ws?.close(1000, null)
        ws = null
    }

    fun send(json: String) {
        ws?.send(json)
    }

    private fun openSocket() {
        if (stopped) return
        val request = Request.Builder().url(url).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                delayIndex = 0
                handler.post { onStatusChange(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handler.post { onMessage(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handler.post {
                    onStatusChange(false)
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.post {
                    onStatusChange(false)
                    if (!stopped) scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (stopped) return
        val delay = delays[minOf(delayIndex, delays.size - 1)]
        delayIndex++
        handler.postDelayed({ openSocket() }, delay)
    }
}

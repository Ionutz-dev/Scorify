package com.example.scorifynative.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket Manager for real-time server updates
 * Listens for CREATE, UPDATE, DELETE events from the server
 */
class WebSocketManager(private val serverUrl: String) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES) // No timeout for WebSocket
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messageFlow = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow.asSharedFlow()

    private var reconnectAttempts = 0
    private var isManualClose = false

    /**
     * Connect to WebSocket server
     */
    fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket already connected")
            return
        }

        isManualClose = false
        val wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://")

        Log.d(TAG, "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")
                reconnectAttempts = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")

                try {
                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    scope.launch {
                        _messageFlow.emit(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                this@WebSocketManager.webSocket = null

                if (!isManualClose) {
                    attemptReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                this@WebSocketManager.webSocket = null

                if (!isManualClose) {
                    attemptReconnect()
                }
            }
        })
    }

    /**
     * Attempt to reconnect with exponential backoff
     */
    private fun attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached, giving up")
            return
        }

        reconnectAttempts++
        val delay = RECONNECT_DELAY_MS * reconnectAttempts

        Log.d(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

        scope.launch {
            delay(delay)
            if (!isManualClose) {
                connect()
            }
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        isManualClose = true
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
    }

    /**
     * Check if WebSocket is connected
     */
    fun isConnected(): Boolean {
        return webSocket != null
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
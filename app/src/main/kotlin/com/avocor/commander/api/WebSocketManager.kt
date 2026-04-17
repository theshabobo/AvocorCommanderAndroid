package com.avocor.commander.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val scope: CoroutineScope
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var reconnectJob: Job? = null
    private var currentUrl: String? = null
    private var currentToken: String? = null
    private var reconnectAttempt = 0
    private val maxReconnectDelay = 30_000L // 30 seconds

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    fun connect(serverUrl: String, token: String) {
        disconnect()

        val wsUrl = serverUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        currentUrl = "$wsUrl/ws?token=$token"
        currentToken = token
        reconnectAttempt = 0

        doConnect()
    }

    private fun doConnect() {
        val url = currentUrl ?: return
        _connectionState.value = ConnectionState.CONNECTING

        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempt = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, WsEvent::class.java)
                    scope.launch {
                        _events.emit(event)
                    }
                } catch (e: Exception) {
                    // Try parsing as generic map
                    try {
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        val map: Map<String, Any> = gson.fromJson(text, mapType)
                        val type = map["type"] as? String ?: "unknown"
                        @Suppress("UNCHECKED_CAST")
                        val data = map["data"] as? Map<String, Any>
                        scope.launch {
                            _events.emit(WsEvent(type, data))
                        }
                    } catch (_: Exception) {
                        // Ignore unparseable messages
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                if (code != 1000) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = minOf(
                1000L * (1L shl minOf(reconnectAttempt, 5)),
                maxReconnectDelay
            )
            reconnectAttempt++
            delay(delay)
            doConnect()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
        _connectionState.value = ConnectionState.DISCONNECTED
        currentUrl = null
        currentToken = null
    }
}

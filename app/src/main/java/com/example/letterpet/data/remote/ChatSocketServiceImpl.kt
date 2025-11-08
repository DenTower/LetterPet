package com.example.letterpet.data.remote

import com.example.letterpet.data.remote.dto.MessageRequest
import com.example.letterpet.data.remote.dto.ServerEvent
import com.example.letterpet.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ChatSocketServiceImpl(
    private val client: HttpClient
): ChatSocketService {
    private var socket: WebSocketSession? = null

    private val _connectionEvents = MutableSharedFlow<ChatSocketService.ConnectionEvent>()
    override val connectionEvents = _connectionEvents.asSharedFlow()

    override suspend fun initSession(username: String): Resource<Unit> {
        return try {
            socket = client.webSocketSession {
                url("${ChatSocketService.Endpoints.ChatSocket.url}?username=$username")
            }
            socket?.coroutineContext[Job]?.invokeOnCompletion { cause ->
                val event = if (cause == null) {
                    ChatSocketService.ConnectionEvent.ClosedNormally
                } else {
                    ChatSocketService.ConnectionEvent.Disconnected(cause)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    _connectionEvents.emit(event)
                }
            }

            if(socket?.isActive == true) {
                Resource.Success(Unit)
            } else {
                throw Exception("Couldn't establish a connection.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.IO).launch {
                _connectionEvents.emit(ChatSocketService.ConnectionEvent.NotEstablished)
            }
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun sendMessages(text: String, chatId: String) {
        try {
            val message = Json.encodeToString(MessageRequest(text, chatId))
            socket?.send(Frame.Text(message))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun observeEvents(): Flow<ServerEvent> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filterIsInstance<Frame.Text>()
                ?.mapNotNull { frame ->
                    val jsonString = frame.readText()
                    Json.decodeFromString<ServerEvent>(jsonString)
                } ?: flow { }
        } catch (e: Exception) {
            e.printStackTrace()
            flow { }
        }
    }

    override suspend fun closeSession() {
        socket?.close()
    }
}
package com.example.letterpet.data.remote

import com.example.letterpet.data.remote.dto.ServerEvent
import com.example.letterpet.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChatSocketService {

    val connectionEvents: SharedFlow<ConnectionEvent>

    suspend fun initSession(
        username: String
    ): Resource<Unit>

    suspend fun sendMessages(text: String, chatId: String)

    fun observeEvents(): Flow<ServerEvent>

    suspend fun closeSession()

    companion object {
//        const val BASE_URL = "ws://10.0.2.2:8080"
        const val BASE_URL = "ws://192.168.0.100:8080"
    }

    sealed class Endpoints(val url: String) {
        object ChatSocket: Endpoints("$BASE_URL/chat-socket")
    }

    sealed class ConnectionEvent {
        object NotEstablished : ConnectionEvent()
        object ClosedNormally : ConnectionEvent()
        data class Disconnected(val error: Throwable?) : ConnectionEvent()
    }
}
package com.example.letterpet.data.remote

import com.example.letterpet.data.remote.dto.MessageDto
import com.example.letterpet.domain.model.Message
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.collections.emptyList

class MessageServiceImpl(
    private val client: HttpClient
): MessageService {

    override suspend fun getAllMessages(): List<Message> {
        return try {
            client.get(MessageService.Endpoints.GetAllMessages.url)
                .body<List<MessageDto>>()
                .map { it.toMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
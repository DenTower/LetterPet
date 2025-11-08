package com.example.letterpet.data.remote.dto

import com.example.letterpet.domain.model.Message
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class MessageResponse(
    val text: String,
    val timestamp: Long,
    val username: String,
    val id: String,
    val chatId: String
) {
    fun toMessage(): Message {
        val date = Date(timestamp)
        return Message(
            text = text,
            date = date,
            username = username,
            chatId = chatId
        )
    }
}

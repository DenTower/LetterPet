package com.example.letterpet.data.remote.dto

import com.example.letterpet.domain.model.Chat
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val createdBy: String,
    val lastMessageId: String? = null,
) {
    fun toChat(): Chat {
        return Chat(
            name = name,
            id = id,
            isGroup = isGroup,
            createdBy = createdBy,
            lastMessageId = lastMessageId
        )
    }
}

package com.example.letterpet.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerEvent {
    @Serializable
    @SerialName("NewMessage")
    data class NewMessage(val message: MessageResponse) : ServerEvent()
    @Serializable
    @SerialName("NewChat")
    data class NewChat(val chat: ChatResponse) : ServerEvent()
}
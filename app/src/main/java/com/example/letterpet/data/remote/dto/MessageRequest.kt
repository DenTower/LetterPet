package com.example.letterpet.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val text: String,
    val chatId: String
)

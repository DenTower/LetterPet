package com.example.letterpet.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val createdBy: String,
    val name: String,
    val isGroup: Boolean
)

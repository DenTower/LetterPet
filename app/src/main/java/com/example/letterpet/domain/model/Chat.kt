package com.example.letterpet.domain.model

data class Chat(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val createdBy: String,
    val lastMessageId: String? = null
)

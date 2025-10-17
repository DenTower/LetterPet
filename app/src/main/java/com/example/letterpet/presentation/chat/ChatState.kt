package com.example.letterpet.presentation.chat

import com.example.letterpet.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false
)

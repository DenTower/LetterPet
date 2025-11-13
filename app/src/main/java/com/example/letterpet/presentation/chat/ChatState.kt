package com.example.letterpet.presentation.chat

import com.example.letterpet.domain.model.Chat
import com.example.letterpet.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val membersOfChats: Map<String, List<String>> = emptyMap(),
    val selectedChat: Chat? = null,
    val isLoading: Boolean = false,
    val isConnected: Boolean = false
)

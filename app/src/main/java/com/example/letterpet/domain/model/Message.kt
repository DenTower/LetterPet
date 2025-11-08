package com.example.letterpet.domain.model

import java.util.Date

data class Message(
    val text: String,
    val date: Date,
    val username: String,
    val chatId: String
)

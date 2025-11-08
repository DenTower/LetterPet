package com.example.letterpet.data.remote

import com.example.letterpet.data.remote.dto.ChatRequest
import com.example.letterpet.domain.model.Chat
import com.example.letterpet.domain.model.Message
import com.example.letterpet.util.Resource

interface MessageService {

    suspend fun getAllMessages(username: String): List<Message>
    suspend fun getAllChatsForUser(username: String): List<Chat>
    suspend fun createChat(chatRequest: ChatRequest): Resource<Chat>
    suspend fun deleteChat(chatId: String) // TODO use in viewModel
    suspend fun getAllChatMembers(chatId: String): List<String> // TODO use in viewModel
    suspend fun addMemberToChat(username: String, chatId: String): Resource<Unit>
    suspend fun removeMemberFromChat(username: String, chatId: String): Boolean // TODO use in viewModel

    companion object {
        const val BASE_URL = "http://192.168.0.101:8080"
    }

    sealed class Endpoints(val url: String) {
        object GetAllMessages: Endpoints("$BASE_URL/{username}/messages")
        object GetAllChatsForUser: Endpoints("$BASE_URL/{username}/chats")
        object CreateChat: Endpoints("$BASE_URL/new/chat")
        object DeleteChat: Endpoints("$BASE_URL/chat/{chatId}")
        object GetAllChatMembers: Endpoints("$BASE_URL/chat/{chatId}/members")
        object AddMemberToChat: Endpoints("$BASE_URL/new/member")
        object RemoveMemberFromChat: Endpoints("$BASE_URL/chat/{chatId}/members/{username}")
    }
}
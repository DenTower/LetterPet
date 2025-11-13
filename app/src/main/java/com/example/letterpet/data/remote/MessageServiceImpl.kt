package com.example.letterpet.data.remote

import com.example.letterpet.data.remote.dto.ChatRequest
import com.example.letterpet.data.remote.dto.ChatResponse
import com.example.letterpet.data.remote.dto.MessageResponse
import com.example.letterpet.domain.model.Chat
import com.example.letterpet.domain.model.Message
import com.example.letterpet.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.collections.emptyList

class MessageServiceImpl(
    private val client: HttpClient
): MessageService {

    override suspend fun getAllMessages(username: String): List<Message> {
        return try {
            val url = MessageService.Endpoints.GetAllMessages.url
                .replace("{username}", username)

            client.get(url)
                .body<List<MessageResponse>>()
                .map { it.toMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllChatsForUser(username: String): List<Chat> {
        return try {
            val url = MessageService.Endpoints.GetAllChatsForUser.url
                .replace("{username}", username)

            client.get(url)
                .body<List<ChatResponse>>()
                .map { it.toChat() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createChat(chatRequest: ChatRequest): Resource<Chat> {
        return try {
            val url = MessageService.Endpoints.CreateChat.url

            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(chatRequest)
            }


            Resource.Success(response.body<ChatResponse>().toChat())

        } catch (e: Exception) {
            Resource.Error("Error creating chat: ${e.message}")
        }
    }

    override suspend fun deleteChat(chatId: String): Resource<Unit> {
        return try {
            val url = MessageService.Endpoints.DeleteChat.url
                .replace("{chatId}", chatId)

            client.delete(url)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Error deleting chat: ${e.message}")
        }
    }

    override suspend fun getChatMembers(chatId: String): List<String> {
        return try {
            val url = MessageService.Endpoints.GetAllChatMembers.url
                .replace("{chatId}", chatId)

            client.get(url)
                .body<List<String>>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addMemberToChat(username: String, chatId: String): Resource<Unit> {
        return try {
            val url = MessageService.Endpoints.AddMemberToChat.url

            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("username" to username, "chatId" to chatId))
            }
            if(response.status == HttpStatusCode.OK) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.status.description)
            }
        } catch (e: Exception) {
            Resource.Error("Error adding member: ${e.message}")
        }
    }

    override suspend fun removeMemberFromChat(username: String, chatId: String): Resource<Unit> {
        return try {
            val url = MessageService.Endpoints.RemoveMemberFromChat.url
                .replace("{username}", username)
                .replace("{chatId}", chatId)

            val response = client.delete(url)
            if(response.status == HttpStatusCode.OK) {
                return Resource.Success(Unit)
            } else {
                return Resource.Error(response.status.description)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error("Error removing member: ${e.message}")
        }
    }
}
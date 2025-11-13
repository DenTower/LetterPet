package com.example.letterpet.presentation.chat

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.letterpet.data.remote.ChatSocketService
import com.example.letterpet.data.remote.MessageService
import com.example.letterpet.data.remote.dto.ChatRequest
import com.example.letterpet.data.remote.dto.ServerEvent
import com.example.letterpet.domain.model.Chat
import com.example.letterpet.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageService: MessageService,
    private val chatSocketService: ChatSocketService,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {

    private val _messageText = mutableStateOf("")
    val messageText: State<String> = _messageText

    private val _state = mutableStateOf(ChatState())
    val state: State<ChatState> = _state

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private val _onOpenChat = MutableSharedFlow<String>()
    val onOpenChat = _onOpenChat.asSharedFlow()

    init {
        connectToChat()
        observeConnection()
    }

    fun onMessageChange(message: String) {
        _messageText.value = message
    }

    fun disconnectFromChat() {
        if(state.value.isConnected) {
            viewModelScope.launch {
                chatSocketService.closeSession()
                Log.d("MyLog", "disconnectFromChat")
                _state.value = state.value.copy(isConnected = false)
            }
        }
    }

    fun sendMessage(chatId: String) {
        viewModelScope.launch {
            if(messageText.value.isNotBlank()) {
                chatSocketService.sendMessages(messageText.value, chatId)
                _messageText.value = ""
            }
        }
    }

    fun createChat(isGroup: Boolean, chatName: String) {
        savedStateHandle.get<String>("username")?.let { username ->
            viewModelScope.launch {
                val result = messageService.createChat(
                    ChatRequest(
                        createdBy = username,
                        name = chatName,
                        isGroup = isGroup
                    )
                )

                when (result) {
                    is Resource.Success -> {
                        _state.value = state.value.copy(chats = state.value.chats + result.data!!)
                    }

                    is Resource.Error -> {
                        emitError(result.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val result = messageService.deleteChat(chatId)

            when (result) {
                is Resource.Success -> {
                    val newList = state.value.chats.toMutableList().apply {
                        removeIf { it.id == chatId }
                    }
                    _state.value = state.value.copy(chats = newList)
                }

                is Resource.Error -> {
                    emitError(result.message ?: "Unknown error")
                }

            }
        }
    }

    fun getChatMembers(chatId: String) {
        viewModelScope.launch {
            if(!state.value.membersOfChats.containsKey(chatId)) {
                val chatMembers = messageService.getChatMembers(chatId)

                val newMap = state.value.membersOfChats.toMutableMap().apply {
                    put(chatId, chatMembers)
                }

                _state.value = state.value.copy(membersOfChats = newMap)
            }
        }
    }

    fun addMemberToChat(username: String, chatId: String) {
        viewModelScope.launch {
            val result = messageService.addMemberToChat(
                username = username,
                chatId = chatId
            )

            when (result) {
                is Resource.Success -> {
                    val currentList = state.value.membersOfChats.getOrDefault(chatId, emptyList())
                    val newList = currentList.toMutableList().apply {
                        add(username)
                    }
                    val newMap = state.value.membersOfChats.toMutableMap().apply {
                        put(chatId, newList)
                    }

                    _state.value = state.value.copy(membersOfChats = newMap)
                }

                is Resource.Error -> {
                    emitError(result.message ?: "Unknown error")
                }
            }
        }

    }

    fun removeMemberFromChat(usernameForRemoving: String, chatId: String) {
        savedStateHandle.get<String>("username")?.let { username ->
            viewModelScope.launch {
                val result = messageService.removeMemberFromChat(
                    username = usernameForRemoving,
                    chatId = chatId
                )

                when (result) {
                    is Resource.Success -> {

                        if(usernameForRemoving == username) {
                            val newList = state.value.chats.toMutableList().apply {
                                removeIf { it.id == chatId }
                            }
                            _state.value = state.value.copy(chats = newList)
                        }
                        val currentList =
                            state.value.membersOfChats.getOrDefault(chatId, emptyList())
                        val newList = currentList.toMutableList().apply {
                            remove(usernameForRemoving)
                        }
                        val newMap = state.value.membersOfChats.toMutableMap().apply {
                            put(chatId, newList)
                        }

                        _state.value = state.value.copy(membersOfChats = newMap)
                    }

                    is Resource.Error -> {
                        emitError(result.message ?: "Unknown error")
                    }
                }
            }
        }
    }


    fun onChatSelected(chat: Chat) {
        viewModelScope.launch {
            _state.value = state.value.copy(selectedChat = chat)
            _onOpenChat.emit(chat.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectFromChat()
    }

    private fun connectToChat() {
        if(!state.value.isConnected) {
            savedStateHandle.get<String>("username")?.let { username ->
                loadInitialData(username)
                initChatSocketSession(username)
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            chatSocketService.connectionEvents.collect { event ->
                _state.value = state.value.copy(isConnected = false)
                when (event) {
                    is ChatSocketService.ConnectionEvent.Disconnected,
                    ChatSocketService.ConnectionEvent.NotEstablished -> {
                        delay(2000)
                        Log.d("MyLog", "Reconnecting...")
                        connectToChat()
                        emitError("No connection. Reconnecting...")
                    }

                    else -> {}
                }
            }
        }
    }

    private fun loadInitialData(username: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val chats = messageService.getAllChatsForUser(username)
            val messages = messageService.getAllMessages(username)

            _state.value = state.value.copy(
                messages = messages,
                chats = chats,
                isLoading = false
            )
        }
    }

    private fun initChatSocketSession(username: String) {
        viewModelScope.launch {
            val result = chatSocketService.initSession(username)
            when (result) {
                is Resource.Success -> {
                    Log.d("MyLog", "connectedToChat")
                    _state.value = state.value.copy(isConnected = true)
                    chatSocketService.observeEvents()
                        .onEach { event ->
                            handleServerEvent(event)
                        }
                        .catch { e ->
                            e.printStackTrace()
                            emitError("Error while handling events: ${e.message}")
                        }
                        .launchIn(viewModelScope)
                }

                is Resource.Error -> {

                }
            }
        }
    }

    private fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.NewMessage -> {
                val newList = state.value.messages.toMutableList().apply {
                    add(0, event.message.toMessage())
                }
                _state.value = state.value.copy(messages = newList)
            }

            is ServerEvent.NewChat -> {
                val newList = state.value.chats.toMutableList().apply {
                    add(event.chat.toChat())
                }
                _state.value = state.value.copy(chats = newList)
            }

            is ServerEvent.DeleteChat -> {
                val newList = state.value.chats.toMutableList().apply {
                    removeIf { it.id == event.chatId }
                }
                _state.value = state.value.copy(chats = newList)
            }
        }
    }

    private suspend fun emitError(message: String?) {
        _toastEvent.emit(message ?: "Unknown error")
    }
}
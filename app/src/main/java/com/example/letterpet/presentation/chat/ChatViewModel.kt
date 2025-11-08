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
                    is ChatSocketService.ConnectionEvent.Disconnected, ChatSocketService.ConnectionEvent.NotEstablished -> {
                        delay(2000)
                        Log.d("MyLog", "Reconnecting...")
                        connectToChat()
                    }
                    else -> {}
                }
            }
        }
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
                        pushChatToState(result.data!!)
                    }

                    is Resource.Error -> {
                        emitError(result.message ?: "Unknown error")
                    }
                }
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
                    // TODO connect chat and member on client
                }

                is Resource.Error -> {
                    emitError(result.message ?: "Unknown error")
                }
            }
        }

    }

    fun onChatSelected(chatId: String) {
        viewModelScope.launch {
            _onOpenChat.emit(chatId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectFromChat()
    }

    private fun loadInitialData(username: String) {
        getAllChats(username)
        getAllMessages(username)
    }

    private fun getAllChats(username: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = messageService.getAllChatsForUser(username)
            _state.value = state.value.copy(
                chats = result,
                isLoading = false
            )
        }
    }

    private fun getAllMessages(username: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = messageService.getAllMessages(username)
            _state.value = state.value.copy(
                messages = result,
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
                    emitError(result.message)
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
                pushChatToState(event.chat.toChat())
            }
        }
    }

    private fun pushChatToState(chat: Chat) {
        val newList = state.value.chats.toMutableList().apply {
            add(chat)
        }
        _state.value = state.value.copy(chats = newList)
    }

    private suspend fun emitError(message: String?) {
        _toastEvent.emit(message ?: "Unknown error")
    }
}
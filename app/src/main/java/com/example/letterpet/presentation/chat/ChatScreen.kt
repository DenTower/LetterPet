package com.example.letterpet.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    username: String?,
    viewModel: ChatViewModel,
    onPopBackNavigation: () -> Unit
) {
    val state = viewModel.state.value
    val chat = state.selectedChat!!
    val chatMessages = state.messages.filter { it.chatId == chat.id }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var usernameToAdd by remember { mutableStateOf("") }
    if(showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMemberToChat(usernameToAdd, chat.id)
                    showAddMemberDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel") }
            },
            title = { Text("Add Member") },
            text = {
                TextField(
                    value = usernameToAdd,
                    onValueChange = { usernameToAdd = it },
                    label = { Text("Username") }
                )
            }
        )
    }

    var showDeleteChatDialog by remember { mutableStateOf(false) }
    if(showDeleteChatDialog) {
        var btnEnable by remember { mutableStateOf(false) }
        var confirmationString by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChat(chat.id)
                        showDeleteChatDialog = false
                        onPopBackNavigation()
                    },
                    enabled = btnEnable
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete chat") },
            text = {
                TextField(
                    value = confirmationString,
                    onValueChange = {
                        confirmationString = it
                        btnEnable = it == chat.name
                    },
                    label = { Text("Write \"${chat.name}\" to confirm") }
                )
            }
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(chat.name) },
                    actions = {
                        IconButton(onClick = {
                            viewModel.getChatMembers(chat.id)
                            coroutineScope.launch {
                                if(drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(padding)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true
                ) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    itemsIndexed(chatMessages) { i, message ->
                        val isOwnMessage = message.username == username
                        Box(
                            contentAlignment = if(isOwnMessage) {
                                Alignment.CenterEnd
                            } else {
                                Alignment.CenterStart
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val ownMessageColor = MaterialTheme.colorScheme.onPrimary
                            val otherMessageColor = MaterialTheme.colorScheme.surfaceContainer
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 300.dp)
                                    .drawBehind {
                                        val nextMsgUsername =
                                            chatMessages.getOrNull(i - 1)?.username ?: ""
                                        if(nextMsgUsername != message.username) {
                                            val cornerRadius = 10.dp.toPx()
                                            val triangleHeight = 5.dp.toPx()
                                            val triangleWidth = 30.dp.toPx()

                                            val trianglePath = Path().apply {
                                                if(isOwnMessage) {
                                                    moveTo(size.width, size.height - cornerRadius)
                                                    lineTo(size.width, size.height + triangleHeight)
                                                    lineTo(
                                                        size.width - triangleWidth,
                                                        size.height - cornerRadius
                                                    )
                                                    close()
                                                } else {
                                                    moveTo(0f, size.height - cornerRadius)
                                                    lineTo(0f, size.height + triangleHeight)
                                                    lineTo(
                                                        triangleWidth,
                                                        size.height - cornerRadius
                                                    )
                                                    close()
                                                }
                                            }
                                            drawPath(
                                                path = trianglePath,
                                                color = if(isOwnMessage) ownMessageColor else otherMessageColor
                                            )
                                        }
                                    }
                                    .background(
                                        color = if(isOwnMessage) ownMessageColor else otherMessageColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                val previousMsgUsername =
                                    chatMessages.getOrNull(i + 1)?.username ?: ""
                                if(previousMsgUsername != message.username && !isOwnMessage) {
                                    Text(
                                        text = message.username,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = message.text,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                val time = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .apply { timeZone = TimeZone.getDefault() }
                                    .format(message.date)
                                Text(
                                    text = time,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.align(Alignment.End),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = viewModel.messageText.value,
                        onValueChange = viewModel::onMessageChange,
                        placeholder = {
                            Text(text = "Enter a message")
                        },
                        modifier = Modifier
                            .weight(1f),
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.sendMessage(chat.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopEnd),
            visible = drawerState.isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Column(
                Modifier
                    .width(250.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
            ) {
                TopAppBar(
                    title = {
                        Text("Options", style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if(drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if(chat.createdBy == username) {
                        IconButton(
                            onClick = {
                                showDeleteChatDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "DeleteChat",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                TopAppBar(
                    title = {
                        Text("Members", style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                showAddMemberDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                )
                LazyColumn {
                    items(
                        state.membersOfChats.getOrDefault(
                            chat.id,
                            emptyList()
                        )
                    ) { memberUsername ->
                        MemberItem(
                            username = memberUsername,
                            rolename = if(memberUsername == chat.createdBy) "Owner" else "Member",
                            onRemove = {
                                if((username == chat.createdBy || username == memberUsername) &&
                                    (memberUsername != chat.createdBy)
                                ) {
                                    viewModel.removeMemberFromChat(memberUsername, chat.id)
                                    if(username == memberUsername) {
                                        onPopBackNavigation()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("ChatScreen")
@Composable
fun ChatScreenPreview(menuIsShown: Boolean = true) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    Box {
        // Основной контент
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat") },
                    actions = {
                        Row {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if(drawerState.isClosed) drawerState.open()
                                    else drawerState.close()
                                }
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "ChatOptions")
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if(drawerState.isClosed) drawerState.open()
                                    else drawerState.close()
                                }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("Main chat content", modifier = Modifier.align(Alignment.Center))
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopEnd),
            visible = menuIsShown,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(250.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .shadow(4.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TopAppBar(
                    title = {
                        Text("Options", style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        IconButton(onClick = {
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }

                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "DeleteChat")
                    }
                    IconButton(
                        onClick = {
                        }
                    ) {
                        Icon(Icons.Default.Create, contentDescription = "RenameChat")
                    }
                    IconButton(
                        onClick = {
                        }
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "AddToFavorite")
                    }
                }

                TopAppBar(
                    title = {
                        Text("Members", style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        IconButton(
                            onClick = {
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                )

                LazyColumn {
                    items(10) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Item $it".repeat(it),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )

                                Text(
                                    text = "Role"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.example.letterpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.letterpet.presentation.chat.ChatScreen
import com.example.letterpet.presentation.chat.MainScreen
import com.example.letterpet.presentation.chat.ChatViewModel
import com.example.letterpet.presentation.auth.AuthScreen
import com.example.letterpet.ui.theme.LetterPetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LetterPetTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "auth_graph",
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    navigation(startDestination = "auth_screen", route = "auth_graph") {
                        composable("auth_screen") {
                            AuthScreen(onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("auth_graph") { inclusive = true }
                                }
                            })
                        }
                    }

                    navigation(startDestination = "main_screen/{username}", route = "chat_graph") {
                        composable(
                            route = "main_screen/{username}",
                            arguments = listOf(
                                navArgument(name = "username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username")
                            val viewModel: ChatViewModel = hiltViewModel(backStackEntry)

                            MainScreen(
                                username = username,
                                onNavigate = navController::navigate,
                                viewModel = viewModel
                            )
                        }

                        composable(
                            route = "chat_screen/{username}/{chatId}",
                            arguments = listOf(
                                navArgument(name = "username") { type = NavType.StringType },
                                navArgument(name = "chatId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId")!!
                            val username = backStackEntry.arguments?.getString("username")

                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("main_screen/$username")
                            }
                            val viewModel: ChatViewModel = hiltViewModel(parentEntry)

                            ChatScreen(
                                chatId = chatId,
                                username = username,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

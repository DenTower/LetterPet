package com.example.letterpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.letterpet.presentation.chat.ChatScreen
import com.example.letterpet.presentation.user.UserScreen
import com.example.letterpet.ui.theme.LetterPetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LetterPetTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "user_screen"
                ) {
                    composable(
                        route = "user_screen"
                    ) {
                        UserScreen(onNavigate = navController::navigate)
                    }

                    composable(
                        route = "chat_screen/{username}",
                        arguments = listOf(
                            navArgument(name = "username") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) {
                        val username = it.arguments?.getString("username")
                        ChatScreen(username = username)
                    }
                }
            }
        }
    }
}

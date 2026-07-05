package com.nothing.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.nothing.assistant.theme.AssistantTheme
import com.nothing.assistant.ui.chat.ChatScreen
import com.nothing.assistant.ui.chat.ChatViewModel
import com.nothing.assistant.ui.onboarding.OnboardingScreen
import com.nothing.assistant.ui.settings.SettingsScreen

/**
 * Root composable for the Wear OS app.
 *
 * Navigation flow:
 *   No API key → Onboarding → Chat
 *   Has API key → Chat
 *   Chat gear icon → Settings → Back to Chat
 */
@Composable
fun WearApp(
    appContainer: AppContainer,
    launchWithMic: Boolean = false,
) {
    val navController = rememberSwipeDismissableNavController()
    val viewModel = remember {
        ChatViewModel(
            apiKeyStore = appContainer.apiKeyStore,
            chatDao = appContainer.chatDao,
        )
    }

    // Determine where to start
    val startDestination = if (appContainer.apiKeyStore.hasApiKey()) "chat" else "onboarding"

    AssistantTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            // Onboarding — API key entry
            composable("onboarding") {
                OnboardingScreen(
                    apiKeyStore = appContainer.apiKeyStore,
                    onKeyValidated = {
                        viewModel.setApiKey(
                            appContainer.apiKeyStore.getApiKey() ?: return@OnboardingScreen
                        )
                        navController.navigate("chat") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // Main chat screen
            composable("chat") {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    launchMicOnStart = launchWithMic,
                )
            }

            // Settings screen
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    apiKeyStore = appContainer.apiKeyStore,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

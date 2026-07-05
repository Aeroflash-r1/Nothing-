package com.nothing.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.nothing.assistant.data.ApiKeyStore
import com.nothing.assistant.ui.chat.ChatViewModel

/**
 * Settings screen accessible from the chat screen via the gear icon.
 *
 * Features:
 * - Gemini model selection
 * - API key management (view/remove)
 * - Haptic feedback toggle
 * - Clear chat history with confirmation
 */
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    apiKeyStore: ApiKeyStore,
    onNavigateBack: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showKeyReplace by remember { mutableStateOf(false) }

    ScreenScaffold(
        timeText = { TimeText() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = "Settings",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            // ─── Gemini Model ───
            Card(
                onClick = { showModelPicker = !showModelPicker },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Gemini Model",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = viewModel.selectedModel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                    )
                }
            }

            if (showModelPicker) {
                val models = listOf(
                    "gemini-2.5-flash-lite",
                    "gemini-3.1-flash-lite",
                    "gemini-3.5-flash"
                )
                models.forEach { model ->
                    CompactButton(
                        onClick = {
                            viewModel.selectedModel = model
                            showModelPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = model,
                            fontSize = 10.sp,
                            color = if (viewModel.selectedModel == model)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── API Key ───
            Card(
                onClick = { showKeyReplace = !showKeyReplace },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "API Key",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = if (apiKeyStore.hasApiKey()) "Key is set" else "No key set",
                        color = if (apiKeyStore.hasApiKey())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                    )
                }
            }

            if (showKeyReplace) {
                Button(
                    onClick = {
                        apiKeyStore.clearApiKey()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove API Key", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Haptics Toggle ───
            Card(
                onClick = { viewModel.hapticsEnabled = !viewModel.hapticsEnabled },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Haptic feedback",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = if (viewModel.hapticsEnabled) "ON" else "OFF",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Clear History ───
            Card(
                onClick = { showClearConfirm = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Clear Chat History",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (showClearConfirm) {
                Card(
                    onClick = {},
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Delete all messages? This cannot be undone.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.clearHistory()
                                    showClearConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Yes, clear all", fontSize = 11.sp)
                            }
                            CompactButton(onClick = { showClearConfirm = false }) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Back ───
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Back to Chat", fontSize = 13.sp)
            }
        }
    }
}

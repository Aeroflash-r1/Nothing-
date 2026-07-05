package com.nothing.assistant.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import android.app.RemoteInput
import androidx.wear.input.RemoteInputIntentHelper
import com.nothing.assistant.R
import com.nothing.assistant.data.ChatMessage

/**
 * Main chat screen with message list, text/voice input, and settings access.
 *
 * Uses TransformingLazyColumn for curved-edge-aware message list.
 * Mic permission is requested at first use; denied → mic button disabled.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    launchMicOnStart: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val listState = rememberTransformingLazyColumnState()
    var showContextMenuFor by remember { mutableStateOf<ChatMessage?>(null) }
    var micPermissionDenied by remember { mutableStateOf(false) }

    // Auto-scroll to latest message
    val lastMessageId = messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Wear text input launcher
    val textInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bundle = RemoteInput.getResultsFromIntent(result.data)
        val text = bundle?.getCharSequence(REMOTE_INPUT_KEY)?.toString()
        if (text != null) viewModel.sendMessage(text)
    }

    // Voice input launcher
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val text = data
                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (text != null) viewModel.sendMessage(text)
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionDenied = false
            voiceInputLauncher.launch(createVoiceInputIntent())
        } else {
            // Check if "don't ask again" was checked
            if (activity != null && !activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                micPermissionDenied = true
            }
        }
    }

    // Launch mic on start if triggered from Tile
    LaunchedEffect(launchMicOnStart) {
        if (launchMicOnStart) {
            requestMicAndLaunch(activity, permissionLauncher, voiceInputLauncher)
        }
    }

    ScreenScaffold(
        timeText = {
            TimeText(modifier = Modifier.padding(top = 4.dp))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Message list
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages yet.\nTap the mic or text field to start.",
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier.padding(vertical = 4.dp),
                        onLongClick = { showContextMenuFor = message }
                    )
                }

                // Thinking indicator
                if (uiState.isThinking) {
                    item {
                        ThinkingIndicator(
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }
                }

                // Error message
                if (uiState.errorMessage != null) {
                    item {
                        ErrorBanner(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.retryLastMessage() },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
            }

            // Context menu overlay
            showContextMenuFor?.let { msg ->
                ContextMenuOverlay(
                    message = msg,
                    onCopy = {
                        copyToClipboard(context, msg.text)
                        showContextMenuFor = null
                    },
                    onDelete = {
                        viewModel.deleteMessage(msg)
                        showContextMenuFor = null
                    },
                    onDismiss = { showContextMenuFor = null }
                )
            }

            // Mic permission hint
            if (micPermissionDenied) {
                Text(
                    text = "Voice unavailable — enable mic in system settings",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // Bottom input bar
            InputBar(
                onTextInput = {
                    val remoteInput = RemoteInput.Builder(REMOTE_INPUT_KEY)
                        .setLabel(context.getString(R.string.chat_hint))
                        .build()
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
                    textInputLauncher.launch(intent)
                },
                onVoiceInput = {
                    requestMicAndLaunch(activity, permissionLauncher, voiceInputLauncher)
                },
                onNavigateToSettings = onNavigateToSettings,
                isListening = uiState.isListening,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Message Bubble ───

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
) {
    val isUser = message.role == "user"

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = if (isUser) 24.dp else 4.dp, end = if (isUser) 4.dp else 24.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                onClick = {},
                onLongClick = onLongClick,
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 12.dp,
                ),
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.85f else 0.9f)
            ) {
                Text(
                    text = message.text,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ─── Thinking Indicator ───

@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thinking",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\u2022\u2022\u2022",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Error Banner ───

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(onClick = onRetry) {
                        Text("Retry", fontSize = 11.sp)
                    }
                    CompactButton(onClick = onDismiss) {
                        Text("Dismiss", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ─── Context Menu Overlay ───

@Composable
private fun ContextMenuOverlay(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = onDismiss,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Message options",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy", fontSize = 12.sp)
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
                CompactButton(onClick = onDismiss) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Input Bar ───

@Composable
private fun InputBar(
    onTextInput: () -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings gear
        CompactButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier.size(18.dp)
            )
        }

        // Text input — launches Wear's native keyboard
        Button(
            onClick = onTextInput,
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = "Tap to type",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // Mic button
        val micBg = if (isListening)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondaryContainer

        Button(
            onClick = onVoiceInput,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = micBg)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Voice input",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Utility helpers ───

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

private fun createVoiceInputIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }
}

internal const val REMOTE_INPUT_KEY = "text_input"

private fun requestMicAndLaunch(
    activity: Activity?,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    voiceLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    if (activity == null) return
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED
    ) {
        voiceLauncher.launch(createVoiceInputIntent())
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

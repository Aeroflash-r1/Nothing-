package com.nothing.assistant.ui.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import android.app.RemoteInput
import androidx.wear.input.RemoteInputIntentHelper
import com.nothing.assistant.data.ApiKeyStore
import com.nothing.assistant.data.GeminiClient
import com.nothing.assistant.ui.chat.REMOTE_INPUT_KEY
import kotlinx.coroutines.launch

/**
 * First-launch screen where the user enters their Gemini API key.
 * Validates the key before saving.
 */
@Composable
fun OnboardingScreen(
    apiKeyStore: ApiKeyStore,
    onKeyValidated: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Wear text input launcher — uses Compose-safe lifecycle handling
    val textInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bundle = RemoteInput.getResultsFromIntent(result.data)
        val text = bundle?.getCharSequence(REMOTE_INPUT_KEY)?.toString()
        if (text != null) {
            apiKey = text.trim()
            errorMessage = null
        }
    }

    ScreenScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Nothing Assistant",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter your Gemini API key to get started",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API Key label
            Text(
                text = "API Key",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Text field button — launches Wear's native keyboard
            Button(
                onClick = {
                    val remoteInput = RemoteInput.Builder(REMOTE_INPUT_KEY)
                        .setLabel(context.getString(com.nothing.assistant.R.string.api_key_hint))
                        .build()
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, arrayOf(remoteInput))
                    textInputLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (apiKey.isNotEmpty()) {
                        if (apiKey.length > 20) "${apiKey.take(8)}...${apiKey.takeLast(8)}"
                        else apiKey
                    } else {
                        "Tap to enter API key"
                    },
                    color = if (apiKey.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Validate button
            Button(
                onClick = {
                    if (apiKey.isBlank()) return@Button
                    isValidating = true
                    errorMessage = null
                    scope.launch {
                        val client = GeminiClient(apiKey.trim())
                        val valid = client.validateApiKey()
                        isValidating = false
                        if (valid) {
                            apiKeyStore.saveApiKey(apiKey.trim())
                            onKeyValidated()
                        } else {
                            errorMessage = "Invalid API key — check and try again"
                        }
                    }
                },
                enabled = apiKey.isNotBlank() && !isValidating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isValidating) "Validating\u2026" else "Save & Continue",
                    fontSize = 13.sp
                )
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Get your key from Google AI Studio\n-> ai.google.dev",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

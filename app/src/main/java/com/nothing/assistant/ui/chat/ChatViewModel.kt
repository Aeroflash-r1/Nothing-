package com.nothing.assistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.assistant.data.ApiKeyStore
import com.nothing.assistant.data.ChatDao
import com.nothing.assistant.data.ChatMessage
import com.nothing.assistant.data.GeminiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val errorMessage: String? = null,
    val isListening: Boolean = false,
)

/**
 * ViewModel for the chat screen.
 * Manages conversation state, Gemini API calls, and Room persistence.
 * All in-flight network requests are cancelled when the scope is cleared.
 */
class ChatViewModel(
    private val apiKeyStore: ApiKeyStore,
    val chatDao: ChatDao,
) : ViewModel() {

    private var geminiClient: GeminiClient? = null
    private var activeRequestJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Observe all messages from Room (oldest first). */
    val messages: StateFlow<List<ChatMessage>> = chatDao.observeAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Configurable preferences */
    var selectedModel: String = DEFAULT_MODEL
    var hapticsEnabled: Boolean = true

    init {
        val key = apiKeyStore.getApiKey()
        if (key != null) {
            geminiClient = GeminiClient(key)
        }
    }

    /** Initialize (or re-initialize) the Gemini client with a new key. */
    fun setApiKey(key: String) {
        apiKeyStore.saveApiKey(key)
        geminiClient = GeminiClient(key)
    }

    fun hasApiKey(): Boolean = apiKeyStore.hasApiKey()

    /**
     * Send a text message: persists the user message and calls Gemini.
     * Cancels any previous in-flight request first.
     */
    fun sendMessage(text: String) {
        val client = geminiClient
        if (client == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "API key not set. Go to Settings.")
            return
        }
        if (text.isBlank()) return

        // Cancel any in-flight request
        activeRequestJob?.cancel()

        viewModelScope.launch {
            // Persist user message
            val userMsg = ChatMessage(role = "user", text = text.trim())
            chatDao.insert(userMsg)

            _uiState.value = _uiState.value.copy(
                isThinking = true,
                errorMessage = null
            )

            // Build conversation context from recent messages
            val recentMessages = chatDao.loadRecentMessages(limit = 50)
            val messagePairs = recentMessages.map { it.role to it.text }

            // Streaming request — collect tokens and persist on completion
            val assistantTextBuilder = StringBuilder()

            activeRequestJob = viewModelScope.launch {
                try {
                    client.generateContentStreaming(
                        model = selectedModel,
                        messages = messagePairs,
                        onToken = { token ->
                            assistantTextBuilder.append(token)
                        }
                    )

                    val fullResponse = assistantTextBuilder.toString()
                    if (fullResponse.isNotBlank()) {
                        chatDao.insert(ChatMessage(role = "model", text = fullResponse))
                    }

                    _uiState.value = _uiState.value.copy(isThinking = false)
                } catch (e: GeminiClient.GeminiException) {
                    val msg = when {
                        e.httpCode == 400 || e.httpCode == 403 ->
                            "Invalid API key or request forbidden"
                        e.httpCode == 429 ->
                            "Rate limited — please wait a moment"
                        e.httpCode == 0 && e.message?.contains("blocked", ignoreCase = true) == true ->
                            "Response blocked by safety filters"
                        e.httpCode == 0 ->
                            "Couldn't reach Gemini — check connection"
                        else -> "Error ${e.httpCode}: ${e.message}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        errorMessage = msg
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        errorMessage = "Couldn't reach Gemini — check connection"
                    )
                }
            }
        }
    }

    /** Retry after an error — re-sends the last user message. */
    fun retryLastMessage() {
        val msgs = messages.value
        val lastUserMsg = msgs.lastOrNull { it.role == "user" } ?: return
        sendMessage(lastUserMsg.text)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch { chatDao.delete(message) }
    }

    fun clearHistory() {
        viewModelScope.launch { chatDao.deleteAll() }
    }

    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }

    /** Cancel any in-flight Gemini request (e.g. when leaving the screen). */
    fun cancelActiveRequest() {
        activeRequestJob?.cancel()
        _uiState.value = _uiState.value.copy(isThinking = false)
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveRequest()
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash-lite"
    }
}

package com.nothing.assistant

import android.content.Context
import com.nothing.assistant.data.ApiKeyStore
import com.nothing.assistant.data.ChatDao
import com.nothing.assistant.data.ChatDatabase

/**
 * Simple manual dependency injection container.
 * Avoids heavy DI frameworks — keeps cold-start fast and APK small.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Encrypted storage for the Gemini API key. */
    val apiKeyStore: ApiKeyStore = ApiKeyStore(appContext)

    /** Room database for chat history. */
    private val chatDatabase: ChatDatabase = ChatDatabase.getInstance(appContext)

    /** DAO for chat message operations. */
    val chatDao: ChatDao = chatDatabase.chatDao()
}

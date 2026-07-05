package com.nothing.assistant.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Securely stores the Gemini API key using EncryptedSharedPreferences
 * backed by Android Keystore.
 *
 * The key is NEVER stored in plain text, Room, DataStore, or logs.
 */
class ApiKeyStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Save (or overwrite) the Gemini API key. */
    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    /** Retrieve the stored API key, or null if none has been saved. */
    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    /** Check whether an API key is currently stored. */
    fun hasApiKey(): Boolean = getApiKey() != null

    /** Remove the stored API key. */
    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        private const val FILE_NAME = "api_key_encrypted.xml"
        private const val KEY_API_KEY = "gemini_api_key"
    }
}

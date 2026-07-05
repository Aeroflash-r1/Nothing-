package com.nothing.assistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single chat message, stored locally in Room.
 *
 * All data stays on-device — never synced anywhere.
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** "user" or "model" */
    val role: String,
    /** The message text content */
    val text: String,
    /** Epoch millis when the message was created */
    val timestampMillis: Long = System.currentTimeMillis()
)

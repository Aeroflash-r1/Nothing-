package com.nothing.assistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    /** Observe all messages ordered by timestamp (oldest first). */
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun observeAllMessages(): Flow<List<ChatMessage>>

    /** Load the most recent N messages for lazy-loading on launch. */
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC LIMIT :limit")
    suspend fun loadRecentMessages(limit: Int = 50): List<ChatMessage>

    /** Load messages older than the given timestamp (pagination). */
    @Query("SELECT * FROM chat_messages WHERE timestampMillis < :before ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun loadOlderMessages(before: Long, limit: Int = 20): List<ChatMessage>

    /** Insert a single message. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage): Long

    /** Delete a single message by ID. */
    @Delete
    suspend fun delete(message: ChatMessage)

    /** Delete all messages (clear history). */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}

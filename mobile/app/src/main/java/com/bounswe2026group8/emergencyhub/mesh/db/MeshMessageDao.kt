package com.bounswe2026group8.emergencyhub.mesh.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeshMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MeshMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MeshMessage>)

    @Query("SELECT * FROM mesh_messages ORDER BY createdAt DESC")
    suspend fun getAllMessages(): List<MeshMessage>

    @Query("SELECT id FROM mesh_messages")
    suspend fun getAllMessageIds(): List<String>

    @Query("SELECT * FROM mesh_messages WHERE syncedToServer = 0")
    suspend fun getUnsyncedMessages(): List<MeshMessage>

    @Query("UPDATE mesh_messages SET syncedToServer = 1 WHERE id = :messageId")
    suspend fun markSynced(messageId: String)

    /**
     * Delete expired posts AND any comments whose parent post is being expired,
     * even if those comments are themselves still fresh — keeps the DB clean
     * and avoids leaving permanent orphan comments hidden by the UI.
     */
    @Query(
        "DELETE FROM mesh_messages " +
            "WHERE createdAt < :cutoffMillis " +
            "   OR parentPostId IN (" +
            "       SELECT id FROM mesh_messages " +
            "       WHERE parentPostId IS NULL AND createdAt < :cutoffMillis" +
            "   )"
    )
    suspend fun deleteExpired(cutoffMillis: Long)

    // --- Forum queries ---

    /** Top-level posts only, newest first. */
    @Query("SELECT * FROM mesh_messages WHERE parentPostId IS NULL ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<MeshMessage>

    /** Comments on a post, oldest first (chat thread order). */
    @Query("SELECT * FROM mesh_messages WHERE parentPostId = :postId ORDER BY createdAt ASC")
    suspend fun getCommentsForPost(postId: String): List<MeshMessage>

    @Query("SELECT COUNT(*) FROM mesh_messages WHERE parentPostId = :postId")
    suspend fun getCommentCount(postId: String): Int

    @Query("SELECT * FROM mesh_messages WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): MeshMessage?
}

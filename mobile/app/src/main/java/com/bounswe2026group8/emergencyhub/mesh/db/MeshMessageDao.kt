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

    @Query("DELETE FROM mesh_messages WHERE createdAt < :cutoffMillis")
    suspend fun deleteExpired(cutoffMillis: Long)
}

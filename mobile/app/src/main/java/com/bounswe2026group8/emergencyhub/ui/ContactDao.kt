package com.bounswe2026group8.emergencyhub.offline.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    // "suspend" means these run in the background so they don't freeze the app
    @Insert
    suspend fun insertContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    // A custom SQL query to grab all the saved numbers
    @Query("SELECT * FROM custom_contacts")
    suspend fun getAllContacts(): List<EmergencyContact>
}
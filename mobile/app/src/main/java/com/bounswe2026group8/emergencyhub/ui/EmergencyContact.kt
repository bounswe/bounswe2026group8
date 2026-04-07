package com.bounswe2026group8.emergencyhub.offline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_contacts")
data class EmergencyContact(
    val alias: String,
    val phoneNumber: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0 // Room will automatically number our rows 1, 2, 3...
)
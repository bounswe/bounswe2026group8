package com.bounswe2026group8.emergencyhub.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessageDao

// Define the entities (tables) that belong in this database and the version number
@Database(entities = [EmergencyContact::class, MeshMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Connects the DAO to the database
    abstract fun contactDao(): ContactDao
    abstract fun meshMessageDao(): MeshMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If the instance exists, return it. Otherwise, build it.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "emergency_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
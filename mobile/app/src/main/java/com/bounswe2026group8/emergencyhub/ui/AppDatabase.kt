package com.bounswe2026group8.emergencyhub.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Define the entities (tables) that belong in this database and the version number
@Database(entities = [EmergencyContact::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Connects the DAO to the database
    abstract fun contactDao(): ContactDao

    companion object {
        private const val DATABASE_NAME = "offline_contacts_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If the instance exists, return it. Otherwise, build it.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Offline contacts are local-only, so resetting stale schemas is safer than crashing.
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

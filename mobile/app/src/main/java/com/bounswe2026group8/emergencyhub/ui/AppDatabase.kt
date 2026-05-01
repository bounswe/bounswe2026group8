package com.bounswe2026group8.emergencyhub.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessageDao

// Define the entities (tables) that belong in this database and the version number
@Database(entities = [EmergencyContact::class, MeshMessage::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Connects the DAO to the database
    abstract fun contactDao(): ContactDao
    abstract fun meshMessageDao(): MeshMessageDao

    companion object {
        private const val DATABASE_NAME = "offline_contacts_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2 → v3: add nullable location columns to mesh_messages.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN locAccuracyMeters REAL")
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN locCapturedAt INTEGER")
            }
        }

        // v3 → v4: convert flat messaging into forum (posts + comments in one table).
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN postType TEXT")
                db.execSQL("ALTER TABLE mesh_messages ADD COLUMN parentPostId TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            // If the instance exists, return it. Otherwise, build it.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Offline contacts are local-only, so resetting stale schemas is safer than crashing.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ServiceRequestEntity::class,
        ServiceOfferEntity::class,
        ChatMessageEntity::class,
        ReviewEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HirafiDatabase : RoomDatabase() {
    abstract fun hirafiDao(): HirafiDao

    companion object {
        @Volatile
        private var INSTANCE: HirafiDatabase? = null

        fun getInstance(context: Context): HirafiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HirafiDatabase::class.java,
                    "hirafi_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

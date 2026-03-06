package com.fedflaee.smarthealth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HeartRateEntity::class,
        DailySummaryEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun heartRateDao(): HeartRateDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_health_db"
                )
                    .fallbackToDestructiveMigration() // Important for version change
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
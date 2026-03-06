package com.fedflaee.smarthealth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: HeartRateEntity)

    // Get all records (used for analytics / year mode)
    @Query("""
        SELECT * FROM heart_rate_table 
        ORDER BY timestamp DESC
    """)
    fun getAllHeartRates(): Flow<List<HeartRateEntity>>

    // Get records between two timestamps (used for daily summary)
    @Query("""
        SELECT * FROM heart_rate_table 
        WHERE timestamp BETWEEN :start AND :end
    """)
    suspend fun getHeartRatesBetween(
        start: Long,
        end: Long
    ): List<HeartRateEntity>
}
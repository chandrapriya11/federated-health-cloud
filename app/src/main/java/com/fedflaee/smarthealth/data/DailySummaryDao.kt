package com.fedflaee.smarthealth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary_table WHERE year = :year AND weekOfYear = :week")
    fun getWeeklySummaries(year: Int, week: Int): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary_table WHERE year = :year AND month = :month")
    fun getMonthlySummaries(year: Int, month: Int): Flow<List<DailySummaryEntity>>
    @Query("SELECT * FROM daily_summary_table WHERE date = :date LIMIT 1")
    suspend fun getSummaryByDate(date: String): DailySummaryEntity?
}
package com.fedflaee.smarthealth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summary_table")
data class DailySummaryEntity(

    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd

    val year: Int,
    val weekOfYear: Int,
    val month: Int,

    val avgHeartRate: Float,
    val maxHeartRate: Int,
    val minHeartRate: Int
)
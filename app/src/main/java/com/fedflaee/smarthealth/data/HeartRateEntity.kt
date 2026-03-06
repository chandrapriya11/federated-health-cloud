package com.fedflaee.smarthealth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_table")
data class HeartRateEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bpm: Int,

    val timestamp: Long
)
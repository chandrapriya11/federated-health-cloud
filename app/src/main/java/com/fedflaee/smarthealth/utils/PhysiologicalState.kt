package com.fedflaee.smarthealth.utils

data class PhysiologicalState(

    var heartRate: Double = 72.0,
    var spo2: Double = 98.0,
    var temperature: Double = 36.8,
    var respiratoryRate: Double = 16.0,

    var systolicBP: Double = 120.0,
    var diastolicBP: Double = 80.0,

    var steps: Int = 0,

    var activity: ActivityState = ActivityState.REST,
    var sleepState: SleepState = SleepState.AWAKE,

    var severity: SeverityLevel = SeverityLevel.NORMAL,

    var lastUpdateTimestamp: Long = System.currentTimeMillis()
)
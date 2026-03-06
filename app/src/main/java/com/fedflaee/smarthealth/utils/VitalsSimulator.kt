package com.fedflaee.smarthealth.utils

import android.util.Log

object VitalsSimulator {

    private const val TAG = "VitalsSimulator"

    init {
        /* Initialize engine with default profile */
        VitalsSimulatorEngine.initialize(
            AgeGroup.ADULT
        )
        VitalsSimulatorEngine.initializeRandomUser()
    }

    fun generateVitals(): SimulatedVitals {

        val engineVitals =
            VitalsSimulatorEngine.generateNext()

        val bpString =
            "${engineVitals.systolicBP}/${engineVitals.diastolicBP}"


        val status =
            when (engineVitals.severity) {

                "NORMAL" -> 0

                "ABNORMAL" -> 1

                "SEVERE" -> 2

                "CRITICAL" -> 3

                "OUTLIER" -> 4

                else -> 0
            }

        val vitals = SimulatedVitals(

            spo2 = engineVitals.spo2,

            heartRate = engineVitals.heartRate,

            respiratoryRate = engineVitals.respiratoryRate,

            bp = bpString,

            temperature = engineVitals.temperature,

            steps = engineVitals.steps,

            activity = engineVitals.activity,

            status = status,

            severity = engineVitals.severity,

            sleepState = engineVitals.sleepState,

            ageGroup = engineVitals.ageGroup,

            timeOfDay = engineVitals.timeOfDay,

            isAbnormal = engineVitals.isAbnormal,
            age = engineVitals.age,
            height = engineVitals.height,
            weight = engineVitals.weight

        )

        Log.d(
            TAG,
            "VitalsSimulator output: $vitals"
        )

        return vitals
    }
}

/* Existing app data model remains unchanged */

data class SimulatedVitals(

    val spo2: Int,

    val heartRate: Int,

    val respiratoryRate: Int,

    val bp: String,

    val temperature: Float,

    val steps: Int,

    val activity: String,

    val status: Int,
    val age: Int,
    val height: Double,
    val weight: Double,

    /* NEW context fields (safe defaults) */

    val severity: String = "NORMAL",

    val sleepState: String = "AWAKE",

    val ageGroup: String = "ADULT",

    val timeOfDay: String = "DAY",

    val isAbnormal: Boolean = false
)
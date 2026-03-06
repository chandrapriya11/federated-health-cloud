package com.fedflaee.smarthealth.model

/**
 * HealthVitals
 *
 * This is the primary physiological record model used across:
 *
 * - App simulator
 * - UI
 * - Server payload
 * - Federated Learning training
 * - Anomaly detection
 *
 * Each instance represents ONE timestamped physiological reading.
 */

data class HealthVitals(

    /* -------- Core physiological vitals -------- */

    val heartRate: Int,
    val spo2: Int,
    val temperature: Float,
    val respiratoryRate: Int,
    val systolicBP: Int,
    val diastolicBP: Int,
    val steps: Int,
    /* -------- Contextual state -------- */

    val activity: String,
    val sleepState: String,
    val severity: String,
    val isAbnormal: Boolean,
    /* -------- User physiological profile -------- */

    val ageGroup: String,
    /* -------- Circadian context -------- */
    val timeOfDay: String,
    /* -------- Temporal tracking -------- */
    val timestamp: Long,

    /* -------- Optional device/session context (future use) -------- */
    val deviceConnected: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)
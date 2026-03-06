package com.fedflaee.smarthealth.model

/**
 * HealthPayload
 *
 * This is the data sent from the mobile client to the server.
 *
 * It is designed to support:
 *
 * - Federated Learning training
 * - Anomaly detection
 * - Context-aware modeling
 * - Smartwatch / sensor simulation
 *
 * Backward compatible with your current server structure.
 */

data class HealthPayload(
    /* -------- Device identity -------- */
    val deviceId: String,
    /* -------- Temporal -------- */
    val timestamp: Long,
    /* -------- Core vitals -------- */
    val heartRate: Int,
    val spo2: Int,
    val temperature: Float,
    val respiratoryRate: Int,
    val systolicBP: Int,
    val diastolicBP: Int,
    val steps: Int,
    /* -------- Health status -------- */
    val status: Int,              // 0 Normal, 1 Abnormal, 2 Severe, 3 Critical, 4 Outlier
    val severity: String,
    val isAbnormal: Boolean,
    /* -------- Context -------- */
    val activity: String,
    val sleepState: String,
    val ageGroup: String,
    val timeOfDay: String,

    /* -------- Location -------- */
    val latitude: Double?,
    val longitude: Double?,

    /* -------- Device state -------- */
    val deviceConnected: Boolean = false,
    val age: Int,
    val height: Double,
    val weight: Double,
)
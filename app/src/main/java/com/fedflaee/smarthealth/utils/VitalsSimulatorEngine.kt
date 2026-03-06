package com.fedflaee.smarthealth.utils

import android.util.Log
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

object VitalsSimulatorEngine {

    private const val TAG = "VitalsSimulatorEngine"

    /* Persistent physiological state */
    private var state = PhysiologicalState()

    /* User profile context */
    private var ageGroup: AgeGroup = AgeGroup.ADULT

    /* Physiological baseline means */
    private const val HR_MEAN_ADULT = 72.0
    private const val HR_MEAN_CHILD = 90.0
    private const val HR_MEAN_ELDERLY = 68.0

    private const val SPO2_MEAN = 98.0
    private const val TEMP_MEAN = 36.8
    private const val RR_MEAN = 16.0

    /* Ornstein-Uhlenbeck parameters */
    private const val THETA = 0.15
    private const val SIGMA = 1.5
    /* ---- Simulated user profile ---- */

    private var userAge: Int = 45
    private var userHeight: Double = 1.70
    private var userWeight: Double = 70.0

    /* Initialize simulator with user profile */
    fun initialize(age: AgeGroup) {
        ageGroup = age
        state = PhysiologicalState()
    }

    /* MAIN GENERATOR FUNCTION */
    fun generateNext(): SimulatedVitalsEngine {

        val now = System.currentTimeMillis()

        val dt = ((now - state.lastUpdateTimestamp) / 1000.0)
            .coerceAtLeast(1.0)

        val timeOfDay = getTimeOfDay()

        val sleepState = determineSleepState(timeOfDay)

        val activity = nextActivity(state.activity, sleepState)

        val severity = nextSeverity(state.severity)

        /* Update vitals using physiological evolution model */

        state.heartRate = updateOU(
            state.heartRate,
            getHRMean(),
            dt
        ) + activityHRModifier(activity)+ severityHRModifier(severity)

        state.spo2 = updateOU(
            state.spo2,
            SPO2_MEAN,
            dt
        ) + severitySpO2Modifier(severity)

        state.temperature = updateOU(
            state.temperature,
            TEMP_MEAN,
            dt
        ) + severityTempModifier(severity)

        state.respiratoryRate = updateOU(
            state.respiratoryRate,
            RR_MEAN,
            dt
        ) +
                activityRRModifier(activity) +
                severityRespModifier(severity)
        /* ---------------- Sleep physiological modifiers ---------------- */

        if (sleepState == SleepState.SLEEP) {

            /* Heart rate drops during sleep */
            state.heartRate -= Random.nextDouble(5.0, 15.0)

            /* Respiratory rate drops */
            state.respiratoryRate -= Random.nextDouble(2.0, 5.0)

            /* Body temperature slightly drops */
            state.temperature -= Random.nextDouble(0.2, 0.6)

            /* SpO₂ slightly stabilizes (usually improves in rest) */
            state.spo2 += Random.nextDouble(0.0, 1.0)
        }

        /* Step handling with sleep reset */

        val newSteps = stepIncrement(activity, sleepState)

        state.steps =
            if (sleepState == SleepState.SLEEP) {

                /* Reset steps during sleep */

                newSteps

            } else {

                /* Accumulate during awake */

                state.steps + newSteps
            }

        /* Update state */
        state.activity = activity
        state.sleepState = sleepState
        state.severity = severity
        state.lastUpdateTimestamp = now

        /* ---------------- Medical safety clamps ---------------- */

        /* Heart rate medically plausible ranges */

        state.heartRate =
            state.heartRate.coerceIn(
                35.0,   // severe bradycardia lower bound
                210.0   // extreme tachycardia upper bound
            )

        /* SpO₂ range */

        state.spo2 =
            state.spo2.coerceIn(
                70.0,   // critical hypoxia lower bound
                100.0
            )

        /* Temperature range */

        state.temperature =
            state.temperature.coerceIn(
                34.0,   // hypothermia
                42.0    // extreme fever upper bound
            )

        /* Respiratory rate range */

        state.respiratoryRate =
            state.respiratoryRate.coerceIn(
                6.0,    // severe respiratory depression
                45.0    // severe respiratory distress
            )
        val abnormal = severity != SeverityLevel.NORMAL

        val result = SimulatedVitalsEngine(
            timestamp = now,
            heartRate = state.heartRate.toInt().coerceIn(30, 220),
            spo2 = state.spo2.toInt().coerceIn(60, 100),
            temperature = state.temperature.toFloat(),
            respiratoryRate = state.respiratoryRate.toInt().coerceIn(5, 45),
            systolicBP = estimateSystolicBP(state.heartRate),
            diastolicBP = estimateDiastolicBP(state.heartRate),
            steps = state.steps,
            activity = activity.name,
            sleepState = sleepState.name,
            severity = severity.name,
            ageGroup = ageGroup.name,
            timeOfDay = timeOfDay.name,
            isAbnormal = abnormal,
            age = userAge,
            height = userHeight,
            weight = userWeight
        )

        Log.d(TAG, "Generated vitals: $result")

        return result
    }

    /* Ornstein-Uhlenbeck update equation */
    private fun updateOU(
        x: Double,
        mean: Double,
        dt: Double
    ): Double {

        val noise = gaussianNoise() * SIGMA * sqrt(dt)

        return x + THETA * (mean - x) * dt + noise
    }

    /* Gaussian noise using Box-Muller transform */
    private fun gaussianNoise(): Double {

        val u1 = Random.nextDouble().coerceAtLeast(1e-10)
        val u2 = Random.nextDouble()

        return sqrt(-2.0 * ln(u1)) *
                cos(2.0 * Math.PI * u2)
    }

    /* Age-based HR mean */
    private fun getHRMean(): Double =
        when (ageGroup) {
            AgeGroup.CHILD -> HR_MEAN_CHILD
            AgeGroup.ADULT -> HR_MEAN_ADULT
            AgeGroup.ELDERLY -> HR_MEAN_ELDERLY
        }

    /* Activity modifiers */

    private fun activityHRModifier(
        activity: ActivityState
    ): Double =
        when (activity) {
            ActivityState.REST -> 0.0
            ActivityState.WALK -> 15.0
            ActivityState.RUN -> 35.0
        }

    private fun activityRRModifier(
        activity: ActivityState
    ): Double =
        when (activity) {
            ActivityState.REST -> 0.0
            ActivityState.WALK -> 4.0
            ActivityState.RUN -> 10.0
        }

    private fun stepIncrement(
        activity: ActivityState,
        sleepState: SleepState
    ): Int {

        if (sleepState == SleepState.SLEEP) return 0

        return when (activity) {
            ActivityState.REST -> Random.nextInt(0, 10)
            ActivityState.WALK -> Random.nextInt(20, 60)
            ActivityState.RUN -> Random.nextInt(80, 150)
        }
    }

    /* Severity modifiers */
    fun initializeRandomUser() {

        ageGroup =
            listOf(
                AgeGroup.CHILD,
                AgeGroup.ADULT,
                AgeGroup.ELDERLY
            ).random()

        userAge = when (ageGroup) {
            AgeGroup.CHILD -> Random.nextInt(6, 17)
            AgeGroup.ADULT -> Random.nextInt(18, 60)
            AgeGroup.ELDERLY -> Random.nextInt(60, 85)
        }

        userHeight = when (ageGroup) {
            AgeGroup.CHILD -> Random.nextDouble(1.1, 1.6)
            AgeGroup.ADULT -> Random.nextDouble(1.55, 1.90)
            AgeGroup.ELDERLY -> Random.nextDouble(1.50, 1.85)
        }

        val bmi = Random.nextDouble(18.0, 32.0)

        userWeight = bmi * userHeight * userHeight

        state = PhysiologicalState()

        Log.d(TAG, "Initialized user: age=$userAge height=$userHeight weight=$userWeight")
    }
    private fun severityHRModifier(
        severity: SeverityLevel
    ): Double =
        when (severity) {

            SeverityLevel.NORMAL ->
                Random.nextDouble(-5.0, 5.0)

            SeverityLevel.ABNORMAL ->
                Random.nextDouble(5.0, 20.0)

            SeverityLevel.SEVERE ->
                Random.nextDouble(20.0, 50.0)

            SeverityLevel.CRITICAL ->
                Random.nextDouble(40.0, 90.0)

            SeverityLevel.OUTLIER ->
                Random.nextDouble(-60.0, 120.0)
        }

    private fun severitySpO2Modifier(
        severity: SeverityLevel
    ): Double =
        when (severity) {

            /* Normal oxygen saturation */

            SeverityLevel.NORMAL ->
                Random.nextDouble(-1.0, 1.0)

            /* Mild hypoxia */

            SeverityLevel.ABNORMAL ->
                Random.nextDouble(-4.0, -1.5)

            /* Moderate hypoxia */

            SeverityLevel.SEVERE ->
                Random.nextDouble(-10.0, -4.0)

            /* Severe hypoxia */

            SeverityLevel.CRITICAL ->
                Random.nextDouble(-20.0, -8.0)

            /* Sensor failure or extreme anomaly */

            SeverityLevel.OUTLIER ->
                Random.nextDouble(-30.0, 5.0)
        }

    private fun severityTempModifier(
        severity: SeverityLevel
    ): Double =
        when (severity) {

            /* Normal temperature */

            SeverityLevel.NORMAL ->
                Random.nextDouble(-0.2, 0.2)

            /* Mild fever */

            SeverityLevel.ABNORMAL ->
                Random.nextDouble(0.3, 1.0)

            /* Moderate fever */

            SeverityLevel.SEVERE ->
                Random.nextDouble(0.8, 2.0)

            /* High fever */

            SeverityLevel.CRITICAL ->
                Random.nextDouble(1.5, 3.5)

            /* Extreme or sensor anomaly */

            SeverityLevel.OUTLIER ->
                Random.nextDouble(-3.0, 5.0)
        }
    /* Respiratory severity modifier */

    private fun severityRespModifier(
        severity: SeverityLevel
    ): Double =
        when (severity) {

            SeverityLevel.NORMAL ->
                Random.nextDouble(-1.0, 1.0)

            SeverityLevel.ABNORMAL ->
                Random.nextDouble(2.0, 6.0)

            SeverityLevel.SEVERE ->
                Random.nextDouble(5.0, 12.0)

            SeverityLevel.CRITICAL ->
                Random.nextDouble(10.0, 20.0)

            SeverityLevel.OUTLIER ->
                Random.nextDouble(-5.0, 25.0)
        }
    /* BP estimation from HR */

    private fun estimateSystolicBP(
        hr: Double
    ): Int {

        val sys =
            110 + (hr - 60) * 0.5

        return sys.toInt().coerceIn(
            80,   // hypotension
            220   // hypertensive crisis
        )
    }

    private fun estimateDiastolicBP(
        hr: Double
    ): Int {

        val dia =
            70 + (hr - 60) * 0.3

        return dia.toInt().coerceIn(
            40,
            130
        )
    }

    /* Sleep model */

    private fun determineSleepState(
        timeOfDay: TimeOfDay
    ): SleepState =
        if (timeOfDay == TimeOfDay.NIGHT &&
            Random.nextDouble() < 0.7
        )
            SleepState.SLEEP
        else
            SleepState.AWAKE

    /* Activity state transitions */

    private fun nextActivity(
        current: ActivityState,
        sleep: SleepState
    ): ActivityState {

        if (sleep == SleepState.SLEEP)
            return ActivityState.REST

        return when (current) {

            ActivityState.REST ->
                listOf(
                    ActivityState.REST,
                    ActivityState.WALK
                ).random()

            ActivityState.WALK ->
                listOf(
                    ActivityState.REST,
                    ActivityState.WALK,
                    ActivityState.RUN
                ).random()

            ActivityState.RUN ->
                listOf(
                    ActivityState.WALK,
                    ActivityState.REST
                ).random()
        }
    }

    /* Severity transitions */

    private fun nextSeverity(
        current: SeverityLevel
    ): SeverityLevel {

        val r = Random.nextDouble()

        return when (current) {

            SeverityLevel.NORMAL ->
                if (r < 0.08)
                    SeverityLevel.ABNORMAL
                else
                    SeverityLevel.NORMAL

            SeverityLevel.ABNORMAL ->
                when {
                    r < 0.10 ->
                        SeverityLevel.SEVERE

                    r < 0.30 ->
                        SeverityLevel.NORMAL

                    else ->
                        SeverityLevel.ABNORMAL
                }

            SeverityLevel.SEVERE ->
                when {
                    r < 0.08 ->
                        SeverityLevel.CRITICAL

                    r < 0.40 ->
                        SeverityLevel.ABNORMAL

                    else ->
                        SeverityLevel.SEVERE
                }

            SeverityLevel.CRITICAL ->
                if (r < 0.50)
                    SeverityLevel.SEVERE
                else
                    SeverityLevel.CRITICAL

            SeverityLevel.OUTLIER ->
                SeverityLevel.SEVERE
        }
    }

    /* Circadian rhythm */

    private fun getTimeOfDay(): TimeOfDay {

        val hour =
            java.util.Calendar.getInstance()
                .get(java.util.Calendar.HOUR_OF_DAY)

        return when (hour) {

            in 5..11 ->
                TimeOfDay.MORNING

            in 12..16 ->
                TimeOfDay.AFTERNOON

            in 17..20 ->
                TimeOfDay.EVENING

            else ->
                TimeOfDay.NIGHT
        }
    }
}

/* Output model */

data class SimulatedVitalsEngine(

    val timestamp: Long,

    val heartRate: Int,
    val spo2: Int,
    val temperature: Float,
    val respiratoryRate: Int,

    val systolicBP: Int,
    val diastolicBP: Int,

    val steps: Int,

    val activity: String,
    val sleepState: String,
    val severity: String,

    val ageGroup: String,
    val timeOfDay: String,

    val isAbnormal: Boolean,
    val age: Int,
    val height: Double,
    val weight: Double
)
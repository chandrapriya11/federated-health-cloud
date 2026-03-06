package com.fedflaee.smarthealth.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fedflaee.smarthealth.data.AppDatabase
import com.fedflaee.smarthealth.data.HeartRateEntity
import com.fedflaee.smarthealth.model.HealthPayload
import com.fedflaee.smarthealth.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val heartRateDao = database.heartRateDao()

    private var sendJob: Job? = null
    private var currentHeartRate: Int = 0

    private val _allHeartRates = MutableStateFlow<List<HeartRateEntity>>(emptyList())
    val allHeartRates: StateFlow<List<HeartRateEntity>> = _allHeartRates

    private val _simulatedVitals =
        MutableStateFlow(VitalsSimulator.generateVitals())
    val simulatedVitals: StateFlow<SimulatedVitals> = _simulatedVitals

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate

    private val appContext = getApplication<Application>()
    // ------------------------------------------------------------
    // INIT
    // ------------------------------------------------------------

    init {
        viewModelScope.launch {
            heartRateDao.getAllHeartRates().collect {
                _allHeartRates.value = it
            }
        }
    }

    // ------------------------------------------------------------
    // DATE SELECTION
    // ------------------------------------------------------------

    fun selectDate(millis: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        _selectedDate.value = calendar
    }

    // ------------------------------------------------------------
    // WEEK RANGE LABEL
    // ------------------------------------------------------------

    fun getCurrentWeekRangeLabel(): String {

        val calendar = _selectedDate.value.clone() as Calendar
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val start = calendar.clone() as Calendar
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val end = calendar.clone() as Calendar

        val startDay = start.get(Calendar.DAY_OF_MONTH)
        val endDay = end.get(Calendar.DAY_OF_MONTH)

        val startMonth = start.getDisplayName(
            Calendar.MONTH,
            Calendar.SHORT,
            Locale.getDefault()
        )

        val endMonth = end.getDisplayName(
            Calendar.MONTH,
            Calendar.SHORT,
            Locale.getDefault()
        )

        val year = end.get(Calendar.YEAR)

        return "$startDay $startMonth – $endDay $endMonth $year"
    }

    fun getCurrentYearLabel(): String {
        return _selectedDate.value.get(Calendar.YEAR).toString()
    }

    // ------------------------------------------------------------
    // WEEK MODE
    // ------------------------------------------------------------

    fun getWeeklyAverageHeartRates(): List<Float> {

        val result = MutableList(7) { 0f }

        val calendar = _selectedDate.value.clone() as Calendar
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {

            val dayStart = calendar.clone() as Calendar
            dayStart.add(Calendar.DAY_OF_MONTH, i)

            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)

            val startMillis = dayStart.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000

            val records = _allHeartRates.value.filter {
                it.timestamp in startMillis until endMillis
            }

            if (records.isNotEmpty()) {
                result[i] = records.map { it.bpm }.average().toFloat()
            }
        }

        return result
    }

    fun getWeekAnalysis(): String {

        val weeklyData = getWeeklyAverageHeartRates()
        val valid = weeklyData.filter { it > 0 }

        if (valid.isEmpty()) return "No data available for this week."

        val avg = valid.average()

        return when {
            avg < 60 -> "This week shows low heart rate trend."
            avg in 60.0..100.0 -> "This week heart rate is within normal healthy range."
            else -> "This week shows elevated heart rate trend."
        }
    }

    // ------------------------------------------------------------
    // YEAR MODE
    // ------------------------------------------------------------

    fun getMonthlyAveragesForSelectedYear(): List<Float> {

        val selectedYear = _selectedDate.value.get(Calendar.YEAR)
        val monthlyBuckets = MutableList(12) { mutableListOf<Int>() }

        _allHeartRates.value.forEach { record ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = record.timestamp

            if (cal.get(Calendar.YEAR) == selectedYear) {
                val month = cal.get(Calendar.MONTH)
                monthlyBuckets[month].add(record.bpm)
            }
        }

        return monthlyBuckets.map {
            if (it.isNotEmpty()) it.average().toFloat() else 0f
        }
    }

    fun getYearSummary(): Triple<Float, Pair<String, Float>, Pair<String, Float>> {

        val monthlyData = getMonthlyAveragesForSelectedYear()

        val validData = monthlyData.mapIndexed { index, value ->
            Pair(index, value)
        }.filter { it.second > 0 }

        if (validData.isEmpty())
            return Triple(0f, Pair("", 0f), Pair("", 0f))

        val avgYear = validData.map { it.second }.average().toFloat()
        val highest = validData.maxByOrNull { it.second }!!
        val lowest = validData.minByOrNull { it.second }!!

        val monthNames = listOf(
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
        )

        return Triple(
            avgYear,
            Pair(monthNames[highest.first], highest.second),
            Pair(monthNames[lowest.first], lowest.second)
        )
    }

    // ------------------------------------------------------------
    // ✅ ADDED: TODAY FORMATTED DATE (FOR UI DISPLAY)
    // ------------------------------------------------------------

    fun getTodayFormatted(): String {
        return SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
    }

    // ------------------------------------------------------------
    // ✅ ADDED: CHECK IF SELECTED WEEK IS CURRENT WEEK
    // ------------------------------------------------------------

    fun isCurrentWeekSelected(): Boolean {

        val selected = _selectedDate.value
        val today = Calendar.getInstance()

        return selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selected.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
    }

    // ------------------------------------------------------------
    // TODAY INDEX
    // ------------------------------------------------------------

    fun getTodayIndexInWeek(): Int {

        if (!isCurrentWeekSelected()) return -1

        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> -1
        }
    }

    // ------------------------------------------------------------
    // NAVIGATION
    // ------------------------------------------------------------

    fun previousWeek() {
        _selectedDate.value.add(Calendar.WEEK_OF_YEAR, -1)
        _selectedDate.value = _selectedDate.value.clone() as Calendar
    }

    fun nextWeek() {
        _selectedDate.value.add(Calendar.WEEK_OF_YEAR, 1)
        _selectedDate.value = _selectedDate.value.clone() as Calendar
    }

    fun previousYear() {
        _selectedDate.value.add(Calendar.YEAR, -1)
        _selectedDate.value = _selectedDate.value.clone() as Calendar
    }

    fun nextYear() {
        _selectedDate.value.add(Calendar.YEAR, 1)
        _selectedDate.value = _selectedDate.value.clone() as Calendar
    }

    // ------------------------------------------------------------
    // HEART RATE UPDATE
    // ------------------------------------------------------------

    fun updateHeartRate(heartRate: Int) {

        if (heartRate <= 0) return

        currentHeartRate = heartRate

        viewModelScope.launch {
            heartRateDao.insertHeartRate(
                HeartRateEntity(
                    bpm = heartRate,
                    timestamp = System.currentTimeMillis()
                )
            )

            _selectedDate.value = _selectedDate.value.clone() as Calendar
        }
        // 🔥 Download Global Model in background (non-blocking)



    }

    // ------------------------------------------------------------
    // SENDING PIPELINE (UNCHANGED)
    // ------------------------------------------------------------

    fun startSending(context: Context) {

        if (sendJob != null) return

        sendJob = viewModelScope.launch {
            while (true) {
                try {
                    val engineVitals =
                        VitalsSimulatorEngine.generateNext()

                    _simulatedVitals.value =
                        VitalsSimulator.generateVitals()

                    sendPayload(context, engineVitals)

                } catch (e: Exception) {
                    Log.e("HealthViewModel", "Error", e)
                }

                delay(3000)
            }
        }
        // 🔥 Download Global Model in background (non-blocking)

        val appContext = getApplication<Application>()

        ApiClient.getGlobalModel(appContext) { model ->

            Log.d("FL_FLOW", "Global model received from server")

            LocalModelStorage.saveGlobalModel(
                appContext,
                model.weights,
                model.bias,
                model.round
            )

            Log.d("FL_FLOW", "Global model saved locally")
        }

    }

    private fun sendPayload(
        context: Context,
        engineVitals: SimulatedVitalsEngine
    ) {
        val (lat, lon) =
            LocationUtils.getLastKnownLocation(context)

        val deviceId =
            DeviceIdUtils.getDeviceId(context)

        val payload = HealthPayload(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            heartRate = currentHeartRate,


            spo2 = engineVitals.spo2,
            temperature = engineVitals.temperature,
            respiratoryRate = engineVitals.respiratoryRate,
            systolicBP = engineVitals.systolicBP,
            diastolicBP = engineVitals.diastolicBP,
            steps = engineVitals.steps,
            status = if (engineVitals.isAbnormal) 1 else 0,
            severity = engineVitals.severity,
            isAbnormal = engineVitals.isAbnormal,
            activity = engineVitals.activity,
            sleepState = engineVitals.sleepState,
            ageGroup = engineVitals.ageGroup,
            timeOfDay = engineVitals.timeOfDay,
            latitude = lat,
            longitude = lon,
            deviceConnected = currentHeartRate > 0,
            age = engineVitals.age,
            height = engineVitals.height,
            weight = engineVitals.weight,
        )

        // Save dataset
        LocalDatasetManager.saveVitals(context, payload)

        val datasetSize =
            LocalDatasetManager.getDatasetSize(context)

// STRICT 25 sample window
        if (datasetSize >= 25 && datasetSize % 25 == 0) {

            ApiClient.getCurrentRound { serverRound ->

                if (serverRound == null) {
                    Log.d("FL", "Could not fetch server round")
                    return@getCurrentRound
                }

                if (RoundManager.hasSubmittedThisRound(context, serverRound)) {
                    Log.d("FL", "Already submitted this round")
                    return@getCurrentRound
                }

                if (!LocalModelTrainer.shouldTrain(context)) {
                    Log.d("FL", "ShouldTrain returned false")
                    return@getCurrentRound
                }

                val result = LocalModelTrainer.trainModel(context)

                if (result != null) {

                    val (maskedWeights, mask) = result

                    val globalModel =
                        LocalModelStorage.loadGlobalModel(context)

                    if (globalModel != null) {

                        val json = JSONObject().apply {
                            put("masked_weights", JSONArray(maskedWeights.toList()))
                            put("mask", JSONArray(mask.toList()))
                        }

                        val encryptedFile =
                            AESEncryptionManager.encryptBytes(
                                context,
                                json.toString().toByteArray()
                            )

                        ApiClient.sendModelUpdate(
                            context,
                            encryptedFile,
                            serverRound
                        )

                    } else {
                        Log.e("FL", "Global model not found")
                    }

                    Log.d(
                        "FederatedLearning",
                        "Round attempt at sample $datasetSize"
                    )
                }
            }
        }
    }
    private fun mapSeverityToStatus(
        severity: String
    ): Int {

        return when (severity) {
            "NORMAL" -> 0
            "ABNORMAL" -> 1
            "SEVERE" -> 2
            "CRITICAL" -> 3
            "OUTLIER" -> 4
            else -> 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        sendJob?.cancel()
        sendJob = null
    }
}


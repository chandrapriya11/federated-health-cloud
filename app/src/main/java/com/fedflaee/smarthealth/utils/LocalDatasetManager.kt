package com.fedflaee.smarthealth.utils

import android.content.Context
import android.util.Log
import com.fedflaee.smarthealth.model.HealthPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter


object LocalDatasetManager {

    private const val TAG = "LocalDatasetManager"
    private const val FILE_NAME = "local_training_dataset.json"

    private fun getDatasetFile(context: Context): File {
        val dir = context.getExternalFilesDir(null)
            ?: throw Exception("External storage not available")
        return File(dir, FILE_NAME)
    }

    fun saveVitals(
        context: Context,
        payload: HealthPayload
    ) {
        try {

            val file = getDatasetFile(context)

            val jsonArray =
                if (file.exists()) {
                    val text = file.readText()
                    if (text.isEmpty()) JSONArray()
                    else JSONArray(text)
                } else {
                    JSONArray()
                }

            val json = JSONObject()

            json.put("timestamp", payload.timestamp)
            json.put("heart_rate", payload.heartRate)
            json.put("spo2", payload.spo2)
            json.put("temperature", payload.temperature)
            json.put("respiratory_rate", payload.respiratoryRate)
            json.put("systolic_bp", payload.systolicBP)
            json.put("diastolic_bp", payload.diastolicBP)
            json.put("status", payload.status)
            json.put("age", payload.age)
            json.put("height", payload.height)

            val bmi = payload.weight / (payload.height * payload.height)
            json.put("bmi", bmi)

            jsonArray.put(json)

            FileWriter(file, false).use { writer ->
                writer.write(jsonArray.toString())
            }
            Log.d(TAG, "Saved sample. Total=${jsonArray.length()}")

        } catch (e: Exception) {
            Log.e(TAG, "Save failed", e)
        }
    }

    fun getDatasetSize(context: Context): Int {
        return try {
            val file = getDatasetFile(context)
            if (!file.exists()) return 0
            val text = file.readText()
            if (text.isEmpty()) return 0
            JSONArray(text).length()
        } catch (e: Exception) {
            Log.e(TAG, "Size read failed", e)
            0
        }
    }

    fun getAllSamples(context: Context): JSONArray {
        return try {
            val file = getDatasetFile(context)
            if (!file.exists()) return JSONArray()
            val text = file.readText()
            if (text.isEmpty()) return JSONArray()
            JSONArray(text)
        } catch (e: Exception) {
            Log.e(TAG, "Get all samples failed", e)
            JSONArray()
        }
    }

    fun getBatch(
        context: Context,
        startIndex: Int,
        batchSize: Int
    ): JSONArray {

        return try {

            val full = getAllSamples(context)

            val end =
                (startIndex + batchSize)
                    .coerceAtMost(full.length())

            val batch = JSONArray()

            for (i in startIndex until end) {
                batch.put(full.getJSONObject(i))
            }

            Log.d(
                TAG,
                "Batch fetched start=$startIndex size=${batch.length()}"
            )

            batch

        } catch (e: Exception) {

            Log.e(TAG, "Batch fetch failed", e)
            JSONArray()
        }
    }

    fun clearDataset(context: Context) {
        try {
            val file = getDatasetFile(context)
            if (file.exists()) file.delete()
            Log.d(TAG, "Dataset cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Clear failed", e)
        }
    }
}
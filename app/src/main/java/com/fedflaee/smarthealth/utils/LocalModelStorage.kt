package com.fedflaee.smarthealth.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object LocalModelStorage {

    private const val TAG = "LocalModelStorage"

    private const val GLOBAL_MODEL_FILE = "global_model.json"
    private const val PREFS = "model_prefs"
    private const val KEY_VERSION = "model_version"
    private const val INPUT_SIZE = 8
    private const val H1 = 128
    private const val H2 = 64
    private const val H3 = 32
    private const val OUTPUT_SIZE = 1
    // =====================================================
    // SAVE GLOBAL MODEL (FROM SERVER)
    // =====================================================

    fun saveGlobalModel(
        context: Context,
        weights: List<Double>,
        bias: FloatArray,
        round: Int
    ) {

        try {

            val file =
                File(
                    context.filesDir,
                    GLOBAL_MODEL_FILE
                )

            val json = JSONObject()

            json.put("round", round)
            val biasArray = JSONArray()
            for (b in bias) {
                biasArray.put(b)
            }
            json.put("bias", biasArray)

            val weightArray = JSONArray()
            for (w in weights) {
                weightArray.put(w)
            }

            json.put("weights", weightArray)

            FileWriter(file).use {
                it.write(json.toString(4))
            }

            Log.d(TAG, "Global model saved locally")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save global model", e)
        }
    }

    // =====================================================
    // LOAD GLOBAL MODEL
    // =====================================================

    fun loadGlobalModel(
        context: Context
    ): Pair<FloatArray, FloatArray>? {

        try {

            val file =
                File(
                    context.filesDir,
                    GLOBAL_MODEL_FILE
                )

            if (!file.exists()) {
                Log.d(TAG, "No global model found")
                return null
            }

            val json =
                JSONObject(file.readText())

            val weightsJson =
                json.getJSONArray("weights")

            val weights =
                FloatArray(weightsJson.length())

            for (i in 0 until weightsJson.length()) {
                weights[i] =
                    weightsJson.getDouble(i).toFloat()
            }

            val biasJson = json.getJSONArray("bias")

            val bias = FloatArray(biasJson.length())

            for (i in 0 until biasJson.length()) {
                bias[i] = biasJson.getDouble(i).toFloat()
            }

            val combined = FloatArray(weights.size + bias.size)

            System.arraycopy(weights, 0, combined, 0, weights.size)
            System.arraycopy(bias, 0, combined, weights.size, bias.size)
            val expectedSize =
                INPUT_SIZE * H1 +
                        H1 * H2 +
                        H2 * H3 +
                        H3 * OUTPUT_SIZE +
                        H1 + H2 + H3 + OUTPUT_SIZE

            if (combined.size != expectedSize) {
                Log.e(TAG, "MODEL SIZE MISMATCH. Expected=$expectedSize Got=${combined.size}")
                return null
            }

            return Pair(combined, FloatArray(0))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load global model", e)
            return null
        }
    }

    // =====================================================
    // EXISTING SAVE LOCAL TRAINED WEIGHTS
    // =====================================================

    fun saveWeights(
        context: Context,
        weights: FloatArray,
        bias: FloatArray
    ) {

        try {

            val round =
                RoundManager.incrementRound(context)

            Log.d(TAG, "Saving weights for Round $round")

            val jsonDir =
                File(
                    context.getExternalFilesDir(null),
                    "local_model_weights"
                )

            if (!jsonDir.exists())
                jsonDir.mkdirs()

            val jsonFile =
                File(
                    jsonDir,
                    "round_$round.json"
                )

            val json =
                JSONObject()

            json.put("round", round)
            json.put("timestamp", System.currentTimeMillis())

            val weightArray =
                JSONArray()

            for (w in weights) {

                val safe =
                    if (w.isNaN() || w.isInfinite())
                        0.0
                    else
                        w.toDouble()

                weightArray.put(safe)
            }

            json.put("weights", weightArray)

            FileWriter(jsonFile).use { writer ->
                writer.write(json.toString(4))
            }

            Log.d(TAG, "Weights JSON saved")
            val combined = FloatArray(weights.size + bias.size)

            System.arraycopy(weights, 0, combined, 0, weights.size)
            System.arraycopy(bias, 0, combined, weights.size, bias.size)


        } catch (e: Exception) {

            Log.e(TAG, "Save failed", e)
        }
    }

    fun saveDownloadedGlobalModel(
        context: Context,
        modelFile: File,
        version: Int? = null
    ) {

        val destination = File(
            context.filesDir,
            "current_global_model.joblib"
        )

        modelFile.copyTo(destination, overwrite = true)

        version?.let {
            saveModelVersion(context, it)
        }

        Log.d("MODEL_STORAGE", "Global model saved locally")
    }
    fun saveModelVersion(
        context: Context,
        version: Int
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(KEY_VERSION, version)
            .apply()
    }

    fun getModelVersion(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(KEY_VERSION, 0)
    }
    fun saveMaskedWeights(
        context: Context,
        maskedWeights: FloatArray,
        mask: FloatArray
    ) {

        val json = org.json.JSONObject().apply {
            put("masked_weights", org.json.JSONArray(maskedWeights.toList()))
            put("mask", org.json.JSONArray(mask.toList()))
        }

        val jsonBytes = json.toString().toByteArray()

        val encryptedFile =
            AESEncryptionManager.encryptBytes(
                context,
                jsonBytes
            )
    }
}
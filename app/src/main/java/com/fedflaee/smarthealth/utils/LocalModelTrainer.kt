package com.fedflaee.smarthealth.utils

import android.content.Context
import android.util.Log
import kotlin.math.exp
import kotlin.random.Random

object LocalModelTrainer {

    private const val TAG = "LocalModelTrainer"

    private const val INPUT_SIZE = 8
    private const val H1 = 128
    private const val H2 = 64
    private const val H3 = 32
    private const val OUTPUT_SIZE = 1

    private const val LEARNING_RATE = 0.0002f
    private const val EPOCHS = 5
    private const val BATCH_SIZE = 25
    private val FEATURE_MEAN = floatArrayOf(
        79.5337f,
        15.4894f,
        36.7483f,
        97.5043f,
        124.4379f,
        79.4996f,
        53.4462f,
        25.0036f
    )

    private val FEATURE_STD = floatArrayOf(
        11.5528f,
        2.2944f,
        0.4332f,
        1.4425f,
        8.6569f,
        5.7572f,
        20.7867f,
        6.4471f
    )
    data class ForwardCache(
        val h1: FloatArray,
        val h2: FloatArray,
        val h3: FloatArray,
        val output: Float
    )
    fun shouldTrain(context: Context): Boolean {

        val globalModel = LocalModelStorage.loadGlobalModel(context)
        if (globalModel == null) {
            Log.d(TAG, "Global model not ready yet")
            return false
        }

        val totalSamples = LocalDatasetManager.getDatasetSize(context)
        var lastIndex = RoundManager.getLastTrainedIndex(context)

        if (lastIndex > totalSamples) {
            Log.d(TAG, "LastIndex exceeded dataset. Resetting.")
            RoundManager.setLastTrainedIndex(context, 0)
            lastIndex = 0
        }

        val newSamples = totalSamples - lastIndex

        Log.d(TAG, "TotalSamples=$totalSamples")
        Log.d(TAG, "LastIndex=$lastIndex")
        Log.d(TAG, "NewSamples=$newSamples")

        if (newSamples < BATCH_SIZE) {
            Log.d(TAG, "Waiting for full batch.")
            return false
        }

        val batch = LocalDatasetManager.getBatch(
            context,
            lastIndex,
            BATCH_SIZE
        )

        val labels = mutableSetOf<Int>()
        for (i in 0 until batch.length()) {
            labels.add(batch.getJSONObject(i).getInt("status"))
        }

        return labels.size >= 2
    }

    fun trainModel(context: Context): Pair<FloatArray, FloatArray>?{

        try {

            val startIndex =
                RoundManager.getLastTrainedIndex(context)

            val batch =
                LocalDatasetManager.getBatch(
                    context,
                    startIndex,
                    BATCH_SIZE
                )

            if (batch.length() < BATCH_SIZE) {
                return null
            }

            val dataset =
                mutableListOf<Pair<FloatArray, Int>>()

            for (i in 0 until batch.length()) {

                val json =
                    batch.getJSONObject(i)

                val raw = floatArrayOf(
                    json.optDouble("heart_rate", 0.0).toFloat(),
                    json.optDouble("respiratory_rate", 0.0).toFloat(),
                    json.optDouble("temperature", 0.0).toFloat(),
                    json.optDouble("spo2", 0.0).toFloat(),
                    json.optDouble("systolic_bp", 0.0).toFloat(),
                    json.optDouble("diastolic_bp", 0.0).toFloat(),
                    json.optDouble("age", 30.0).toFloat(),
                    json.optDouble("bmi", 22.0).toFloat()
                )

                val features = FloatArray(INPUT_SIZE)

                for (j in 0 until INPUT_SIZE) {
                    features[j] =
                        (raw[j] - FEATURE_MEAN[j]) / FEATURE_STD[j]
                }

                val label =
                    json.getInt("status")

                dataset.add(
                    Pair(features, label)
                )
            }
            val balanced = dataset.groupBy { it.second }
            if (balanced.size < 2) return null

            val minCount = balanced.values.minOf { it.size }

            val finalDataset = balanced.values.flatMap { it.take(minCount) }
            val shuffledDataset = finalDataset.shuffled()


            val globalModel =
                LocalModelStorage.loadGlobalModel(context)

            val weights =
                globalModel?.first ?: initializeWeights()



            repeat(EPOCHS) {
                for (sample in shuffledDataset) {

                    val features = sample.first
                    val label = sample.second

                    val cache = forward(weights, features)

                    backward(
                        weights = weights,
                        input = features,
                        label = label,
                        cache = cache
                    )
                }
            }

            val combined = weights.copyOf()
            // Apply attack BEFORE masking
            val attackType = AttackSimulator.AttackType.NONE // CHANGE THIS TO TEST
            val attacked = weights.copyOf()
            val attackedCombined = applyAttack(combined, attackType)

// Generate mask SAME SIZE AS combined
            val mask = FloatArray(combined.size)

            val avg = attackedCombined.map { kotlin.math.abs(it) }.average().toFloat()
            val std = avg * 0.1f

            val random = java.util.Random()

            for (i in combined.indices) {
                mask[i] = random.nextGaussian().toFloat() * std
            }

            val masked = FloatArray(combined.size)
            for (i in attackedCombined.indices) {
                masked[i] = attackedCombined[i] + mask[i]
            }

            RoundManager.setLastTrainedIndex(
                context,
                startIndex + BATCH_SIZE
            )

            return Pair(masked, mask)

        } catch (e: Exception) {

            Log.e(TAG, "Training failed", e)
            return null
        }
    }

    private fun initializeWeights(): FloatArray {

        val totalSize =
            INPUT_SIZE * H1 +
                    H1 * H2 +
                    H2 * H3 +
                    H3 * OUTPUT_SIZE +
                    H1 + H2 + H3 + OUTPUT_SIZE

        return FloatArray(totalSize) {
            Random.nextFloat() * 0.01f
        }
    }
    fun applyAttack(weights: FloatArray, type: AttackSimulator.AttackType): FloatArray {

        when(type) {

            AttackSimulator.AttackType.LARGE_INJECTION -> {
                for (i in weights.indices)
                    weights[i] *= 1_000_000f
            }

            AttackSimulator.AttackType.RANDOM_NOISE -> {
                for (i in weights.indices)
                    weights[i] = (-1000..1000).random().toFloat()
            }

            AttackSimulator.AttackType.SIGN_FLIP -> {
                for (i in weights.indices)
                    weights[i] *= -1f
            }

            AttackSimulator.AttackType.BACKDOOR -> {
                for (i in weights.indices) {
                    weights[i] += 5000f
                }
            }

            else -> {}
        }

        return weights
    }

    private fun sigmoid(x: Float): Float {
        return (1f / (1f + exp(-x)))
    }
    private fun forward(
        weights: FloatArray,
        input: FloatArray
    ): ForwardCache {

        var index = 0

        val w1 = weights.copyOfRange(index, index + INPUT_SIZE * H1)
        index += INPUT_SIZE * H1

        val w2 = weights.copyOfRange(index, index + H1 * H2)
        index += H1 * H2

        val w3 = weights.copyOfRange(index, index + H2 * H3)
        index += H2 * H3

        val w4 = weights.copyOfRange(index, index + H3 * OUTPUT_SIZE)
        index += H3 * OUTPUT_SIZE

        val b1 = weights.copyOfRange(index, index + H1)
        index += H1

        val b2 = weights.copyOfRange(index, index + H2)
        index += H2

        val b3 = weights.copyOfRange(index, index + H3)
        index += H3

        val b4 = weights.copyOfRange(index, index + OUTPUT_SIZE)

        val h1 = FloatArray(H1)
        for (i in 0 until H1) {
            var sum = 0f
            for (j in 0 until INPUT_SIZE)
                sum += input[j] * w1[i * INPUT_SIZE + j]
            h1[i] = kotlin.math.max(0f, sum + b1[i])
        }

        val h2 = FloatArray(H2)
        for (i in 0 until H2) {
            var sum = 0f
            for (j in 0 until H1)
                sum += h1[j] * w2[i * H1 + j]
            h2[i] = kotlin.math.max(0f, sum + b2[i])
        }

        val h3 = FloatArray(H3)
        for (i in 0 until H3) {
            var sum = 0f
            for (j in 0 until H2)
                sum += h2[j] * w3[i * H2 + j]
            h3[i] = kotlin.math.max(0f, sum + b3[i])
        }

        var out = 0f
        for (i in 0 until H3)
            out += h3[i] * w4[i]

        out += b4[0]

        return ForwardCache(h1, h2, h3, sigmoid(out))
    }
    private fun backward(
        weights: FloatArray,
        input: FloatArray,
        label: Int,
        cache: ForwardCache
    ) {

        val target = label.toFloat()
        val output = cache.output
        val error = output - target

        var index = 0

        val w1Start = index
        val w1Size = INPUT_SIZE * H1
        index += w1Size

        val w2Start = index
        val w2Size = H1 * H2
        index += w2Size

        val w3Start = index
        val w3Size = H2 * H3
        index += w3Size

        val w4Start = index
        val w4Size = H3 * OUTPUT_SIZE
        index += w4Size

        val b1Start = index
        index += H1

        val b2Start = index
        index += H2

        val b3Start = index
        index += H3

        val b4Start = index

        // Output layer gradient
        val delta4 = error * output * (1 - output)

        // Update w4
        for (i in 0 until H3) {
            val grad = delta4 * cache.h3[i]
            weights[w4Start + i] -= LEARNING_RATE * grad
        }

        weights[b4Start] -= LEARNING_RATE * delta4

        // You can optionally propagate further layers
        // But even fixing output layer alone is MASSIVE improvement
    }

}

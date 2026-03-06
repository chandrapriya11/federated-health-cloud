package com.fedflaee.smarthealth.utils

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import android.util.Base64
import com.fedflaee.smarthealth.security.RSAKeyManager

data class GlobalModelResponse(
    val weights: List<Double>,
    val bias: FloatArray,
    val round: Int
)

object ApiClient {

    private const val TAG = "API_CLIENT"
    private const val BASE_URL = "http://10.179.86.56:5000"

    private val client =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()

    // =====================================================
    // REGISTER DEVICE (UPDATED FOR JWT + AES KEY)
    // =====================================================

    fun registerDevice(
        context: Context,
        onSuccess: (() -> Unit)? = null
    ) {

        RSAKeyManager.generateKeyPairIfNeeded()

        val publicKey =
            RSAKeyManager.getPublicKey()

        val json = JSONObject().apply {

            put(
                "device_id",
                DeviceIdUtils.getDeviceId(context)
            )

            put(
                "public_key",
                publicKey
            )
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/register")
            .post(body)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Registration failed", e)
                }

                override fun onResponse(call: Call, response: Response) {

                    response.use {

                        if (!it.isSuccessful) {
                            Log.e(TAG, "Registration error: ${it.code}")
                            return
                        }

                        val bodyStr = it.body?.string() ?: return
                        val json = JSONObject(bodyStr)

                        val accessToken = json.getString("access_token")

                        val encryptedAESBase64 =
                            json.getString("aes_key_encrypted")

                        // Save JWT
                        SessionManager.saveToken(context, accessToken)

                        val encryptedAES =
                            Base64.decode(
                                encryptedAESBase64,
                                Base64.NO_WRAP
                            )

                        val aesKeyBytes = try {

                            RSAKeyManager.decryptAESKey(
                                encryptedAES
                            )

                        } catch (e: Exception) {

                            Log.e(TAG, "RSA AES decrypt failed", e)
                            return
                        }

                        AESEncryptionManager.saveServerKey(
                            context,
                            aesKeyBytes
                        )

                        Log.d(TAG, "Device registered. JWT + AES key saved.")
                        Log.d("REG_DEBUG", "Response code: ${it.code}")
                        Log.d("REG_DEBUG", "Body: $bodyStr")
                        onSuccess?.invoke()
                    }
                }
            })
    }




    // =====================================================
    // SEND MODEL UPDATE (UPDATED FOR JWT HEADER)
    // =====================================================

    fun sendModelUpdate(
        context: Context,
        encryptedFile: File,
        round: Int
    ) {

        val token = SessionManager.getToken(context)

        if (token == null) {
            Log.e(TAG, "No JWT token. Cannot upload model.")
            return
        }

        Log.d(TAG, "Uploading model to: $BASE_URL/model_update")
        Log.d(TAG, "File size: ${encryptedFile.length()} bytes")

        val requestBody = encryptedFile.readBytes()
            .toRequestBody("application/octet-stream".toMediaType())
        val (lat, lon) = LocationUtils.getLastKnownLocation(context)

        val request = Request.Builder()
            .url("$BASE_URL/model_update")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-Round", round.toString())
            .addHeader("X-Device-Id", DeviceIdUtils.getDeviceId(context))
            .addHeader("X-Latitude", lat?.toString() ?: "")
            .addHeader("X-Longitude", lon?.toString() ?: "")
            .post(requestBody)
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "MODEL UPDATE FAILED", e)
                }

                override fun onResponse(call: Call, response: Response) {

                    response.use {

                        val bodyString = it.body?.string() ?: "{}"

                        Log.d(TAG, "MODEL UPDATE RESPONSE CODE: ${it.code}")
                        Log.d(TAG, "MODEL UPDATE RESPONSE BODY: $bodyString")

                        if (!it.isSuccessful) {
                            Log.e(TAG, "Update failed")
                            return
                        }

                        val json = JSONObject(bodyString)

                        if (json.optString("status") == "rejected") {
                            Log.w(TAG, "Server rejected update for round $round")
                            return
                        }

                        Log.d(TAG, "Update accepted for round $round")
                        RoundManager.markSubmitted(context, round)

                        getGlobalModel(context) { modelResponse ->
                            if (modelResponse != null) {
                                LocalModelStorage.saveGlobalModel(
                                    context,
                                    modelResponse.weights,
                                    modelResponse.bias,
                                    modelResponse.round
                                )
                            }
                        }
                    }
                }
            })
    }
    fun getGlobalModel(
        context: Context,
        onSuccess: (GlobalModelResponse) -> Unit
    ) {

        val token = SessionManager.getToken(context)
        Log.d("JWT_DEBUG", "Token used for global model: $token")

        if (token == null) {
            Log.e(TAG, "No JWT token available for global model request")
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/global_model")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to fetch global model", e)
                }

                override fun onResponse(call: Call, response: Response) {

                    response.use {

                        if (!it.isSuccessful) {
                            Log.e(TAG, "Global model fetch error: ${it.code}")
                            return
                        }

                        val bodyStr = it.body?.string() ?: return

                        try {

                            val encryptedJson = JSONObject(bodyStr)

                            val nonce = Base64.decode(
                                encryptedJson.getString("nonce"),
                                Base64.NO_WRAP
                            )

                            val ciphertext = Base64.decode(
                                encryptedJson.getString("ciphertext"),
                                Base64.NO_WRAP
                            )

                            val tag = Base64.decode(
                                encryptedJson.getString("tag"),
                                Base64.NO_WRAP
                            )

                            val decryptedBytes =
                                AESEncryptionManager.decryptFromServer(
                                    context,
                                    nonce,
                                    tag,
                                    ciphertext
                                )

                            val decryptedJson =
                                JSONObject(String(decryptedBytes))

                            val weightsJson =
                                decryptedJson.getJSONArray("weights")

                            val weights =
                                mutableListOf<Double>()

                            for (i in 0 until weightsJson.length()) {
                                weights.add(weightsJson.getDouble(i))
                            }

                            val biasJson = decryptedJson.getJSONArray("bias")

                            val bias = FloatArray(biasJson.length())

                            for (i in 0 until biasJson.length()) {
                                bias[i] = biasJson.getDouble(i).toFloat()
                            }

                            val round =
                                decryptedJson.getInt("round")

                            onSuccess(
                                GlobalModelResponse(
                                    weights,
                                    bias,
                                    round
                                )
                            )

                        } catch (e: Exception) {

                            Log.e(TAG, "Encrypted model parsing failed", e)
                        }
                    }
                }
            })
    }
    fun getRoundStatus(
        onResult: (Int) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/round_status")
            .get()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Round status fetch failed", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string() ?: return
                        val json = JSONObject(body)
                        val round = json.getInt("current_round")
                        onResult(round)
                    }
                }
            })
    }
    fun getCurrentRound(
        onResult: (Int?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/round_status")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Round fetch failed", e)
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onResult(null)
                        return
                    }
                    val body = response.body?.string() ?: return
                    val json = JSONObject(body)
                    onResult(json.optInt("current_round"))
                }
            }
        })
    }


}
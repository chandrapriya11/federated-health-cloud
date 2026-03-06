package com.fedflaee.smarthealth.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESEncryptionManager {

    private const val TAG = "AES256Encryption"

    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12          // MUST match server
    private const val TAG_SIZE = 128        // 128-bit authentication tag

    private const val SERVER_KEY_FILE = "server_aes.key"

    // =====================================================
    // SAVE SERVER AES KEY
    // =====================================================

    fun saveServerKey(context: Context, key: ByteArray) {
        context.openFileOutput(SERVER_KEY_FILE, Context.MODE_PRIVATE)
            .use { it.write(key) }

        Log.d(TAG, "Server AES key saved locally")
    }

    // =====================================================
    // LOAD SERVER AES KEY
    // =====================================================

    private fun getServerKey(context: Context): ByteArray {
        return try {
            context.openFileInput(SERVER_KEY_FILE).readBytes()
        } catch (_: Exception) {
            Log.e(TAG, "Server AES key not found")
            throw Exception("Server AES key missing. Register device first.")
        }
    }

    // =====================================================
    // ENCRYPT WEIGHTS USING AES-GCM
    // =====================================================

    fun encryptPayload(
        context: Context,
        maskedWeights: FloatArray,
        round: Int
    ): File {

        val keyBytes = getServerKey(context)
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val prefs = context.getSharedPreferences(
            "secure_aggregation",
            Context.MODE_PRIVATE
        )

        val maskString = prefs.getString("mask", "") ?: ""
        val maskList = maskString.split(",")
            .filter { it.isNotBlank() }
            .map { it.toFloat() }

        val json = org.json.JSONObject().apply {
            put(
                "masked_weights",
                org.json.JSONArray(maskedWeights.toList())
            )
            put(
                "mask",
                org.json.JSONArray(maskList)
            )
        }

        val plainBytes = json.toString().toByteArray()

        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_MODE)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encryptedBytes = cipher.doFinal(plainBytes)

        val dir = File(
            context.getExternalFilesDir(null),
            "encrypted_models"
        )

        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "round_$round.bin")

        FileOutputStream(file).use { fos ->
            fos.write(iv)
            fos.write(encryptedBytes)
        }

        return file
    }
    fun encryptBytes(
        context: Context,
        plainBytes: ByteArray
    ): File {

        val keyBytes = getServerKey(context)
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val dir = File(
            context.getExternalFilesDir(null),
            "encrypted_models"
        )

        if (!dir.exists())
            dir.mkdirs()

        val file = File(dir, "payload.bin")

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            keySpec,
            gcmSpec
        )

        val encryptedBytes = cipher.doFinal(plainBytes)

        FileOutputStream(file).use {
            it.write(iv)
            it.write(encryptedBytes)
        }

        return file
    }
    fun decryptFromServer(
        context: Context,
        nonce: ByteArray,
        tag: ByteArray,
        ciphertext: ByteArray
    ): ByteArray {

        val key = getServerKey(context)

        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding")

        val spec =
            GCMParameterSpec(128, nonce)

        val secretKey =
            SecretKeySpec(key, "AES")

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            spec
        )

        cipher.updateAAD(ByteArray(0))

        return cipher.doFinal(ciphertext + tag)
    }
}

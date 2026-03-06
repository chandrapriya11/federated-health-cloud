package com.fedflaee.smarthealth.security
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenManager(context: Context) {

    private val alias = "JWT_KEY"
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(alias)) {

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(alias, null) as SecretKey
    }

    fun saveToken(token: String) {

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(token.toByteArray())

        val combined = iv + encrypted

        prefs.edit()
            .putString("jwt_token", Base64.encodeToString(combined, Base64.DEFAULT))
            .apply()
    }

    fun getToken(): String? {

        val stored = prefs.getString("jwt_token", null) ?: return null

        val combined = Base64.decode(stored, Base64.DEFAULT)

        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getKey(),
            GCMParameterSpec(128, iv)
        )

        val decrypted = cipher.doFinal(encrypted)

        return String(decrypted)
    }

}
package com.fedflaee.smarthealth.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_OAEP
import android.security.keystore.KeyProperties.KEY_ALGORITHM_RSA
import android.security.keystore.KeyProperties.DIGEST_SHA256
import android.security.keystore.KeyProperties.DIGEST_SHA512
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import java.security.spec.MGF1ParameterSpec

object RSAKeyManager {

    private const val ALIAS = "SMARTHEALTH_RSA_V3"

    fun generateKeyPairIfNeeded() {

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        if (ks.containsAlias(ALIAS)) return

        val generator =
            KeyPairGenerator.getInstance(
                KEY_ALGORITHM_RSA,
                "AndroidKeyStore"
            )

        val spec =
            KeyGenParameterSpec.Builder(
                ALIAS,
                PURPOSE_ENCRYPT or PURPOSE_DECRYPT
            )
                .setEncryptionPaddings(ENCRYPTION_PADDING_RSA_OAEP)
                .setDigests(DIGEST_SHA256, DIGEST_SHA512)
                .setKeySize(2048)
                .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKey(): String {

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        val cert =
            ks.getCertificate(ALIAS)

        val publicKey =
            cert.publicKey.encoded

        return Base64.encodeToString(
            publicKey,
            Base64.NO_WRAP
        )
    }

    fun decryptAESKey(encryptedKey: ByteArray): ByteArray {

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        val entry = ks.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val privateKey = entry.privateKey

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")

        val oaepParams = OAEPParameterSpec(
            "SHA-256",                       // main digest
            "MGF1",
            MGF1ParameterSpec.SHA1,          // MGF1 digest (MUST match server)
            PSource.PSpecified.DEFAULT
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            privateKey,
            oaepParams
        )

        return cipher.doFinal(encryptedKey)
    }
}
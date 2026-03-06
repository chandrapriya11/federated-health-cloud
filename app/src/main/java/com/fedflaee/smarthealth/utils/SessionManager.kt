package com.fedflaee.smarthealth.utils
import com.fedflaee.smarthealth.security.SecureTokenManager
import android.content.Context
import android.util.Log

object SessionManager {

    private const val PREFS = "device_prefs"
    private const val KEY_SESSION = "session_token"

    fun saveToken(context: Context, token: String) {

        val secureTokenManager = SecureTokenManager(context)
        secureTokenManager.saveToken(token.trim())

        Log.d("SESSION_MANAGER", "🔐 Session token saved securely")
    }

    fun getToken(context: Context): String? {

        val secureTokenManager = SecureTokenManager(context)
        val token = secureTokenManager.getToken()

        Log.d(
            "SESSION_MANAGER",
            if (token == null) "⚠️ No session token found"
            else "🔐 Secure session token loaded"
        )

        return token
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION)
            .apply()

        Log.w("SESSION_MANAGER", "🧹 Session token cleared")
    }
}

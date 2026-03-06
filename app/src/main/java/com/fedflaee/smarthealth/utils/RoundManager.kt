package com.fedflaee.smarthealth.utils

import android.content.Context

object RoundManager {

    private const val PREF_NAME = "FL_PREFS"

    private const val KEY_ROUND = "CURRENT_ROUND"

    private const val KEY_LAST_INDEX = "LAST_TRAINED_INDEX"


    // ============================
    // GET CURRENT ROUND
    // ============================

    fun getCurrentRound(
        context: Context
    ): Int {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getInt(KEY_ROUND, 0)
    }


    // ============================
    // INCREMENT ROUND
    // ============================

    fun incrementRound(
        context: Context
    ): Int {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        val next =
            getCurrentRound(context) + 1

        prefs.edit()
            .putInt(KEY_ROUND, next)
            .apply()

        return next
    }


    // ============================
    // GET LAST TRAINED INDEX
    // ============================

    fun getLastTrainedIndex(
        context: Context
    ): Int {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getInt(
            KEY_LAST_INDEX,
            0
        )
    }


    // ============================
    // SET LAST TRAINED INDEX  ← THIS FIXES YOUR ERROR
    // ============================

    fun setLastTrainedIndex(
        context: Context,
        index: Int
    ) {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putInt(KEY_LAST_INDEX, index)
            .apply()
    }
    fun hasSubmittedThisRound(context: Context, round: Int): Boolean {
        val prefs = context.getSharedPreferences("round_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("submitted_round", -1) == round
    }

    fun markSubmitted(context: Context, round: Int) {
        val prefs = context.getSharedPreferences("round_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("submitted_round", round).apply()
    }
}

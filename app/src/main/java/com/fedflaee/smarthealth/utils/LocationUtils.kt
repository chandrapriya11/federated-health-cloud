package com.fedflaee.smarthealth.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationUtils {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var initialized = false

    private var cachedLat: Double? = null
    private var cachedLon: Double? = null

    fun init(context: Context) {

        if (initialized) return

        fusedClient =
            LocationServices
                .getFusedLocationProviderClient(context)

        initialized = true
    }

    /**
     * Fetch fresh location every time
     * (Non-blocking, updates cache)
     */
    @SuppressLint("MissingPermission")
    private fun refreshLocation() {

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {

                cachedLat = location.latitude
                cachedLon = location.longitude

                Log.d(
                    "LOCATION",
                    "Updated → Lat=$cachedLat Lon=$cachedLon"
                )
            }

        }.addOnFailureListener { e ->

            Log.e("LOCATION", "Location fetch failed", e)
        }
    }

    /**
     * Used by HealthViewModel
     * Always attempts refresh before returning
     */
    fun getLastKnownLocation(
        context: Context
    ): Pair<Double, Double> {

        if (!initialized)
            init(context)

        refreshLocation()

        return if (
            cachedLat != null &&
            cachedLon != null
        ) {

            Pair(cachedLat!!, cachedLon!!)

        } else {

            // Safe fallback
            Pair(17.3850, 78.4867)
        }
    }

    /**
     * Async version (optional use)
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onResult: (Double, Double) -> Unit
    ) {

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {

                cachedLat = location.latitude
                cachedLon = location.longitude
            }

            onResult(
                cachedLat ?: 17.3850,
                cachedLon ?: 78.4867
            )
        }.addOnFailureListener {

            onResult(
                cachedLat ?: 17.3850,
                cachedLon ?: 78.4867
            )
        }
    }
}

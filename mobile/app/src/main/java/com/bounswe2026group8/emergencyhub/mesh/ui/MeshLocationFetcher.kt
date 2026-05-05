package com.bounswe2026group8.emergencyhub.mesh.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Best-effort FINE-accuracy location lookup for the offline forum.
 *
 * Three-phase lookup, falling through on null:
 *  1. Fused `lastLocation` — instant, returns whatever some app (Maps, weather, system)
 *     recently computed. Often null on a phone where no app has touched location.
 *  2. Raw `LocationManager.getLastKnownLocation` across every enabled provider
 *     (GPS, NETWORK, PASSIVE) — picks the freshest. Catches cases where Fused has
 *     nothing but a raw provider holds a stale fix from earlier.
 *  3. Active `getCurrentLocation(PRIORITY_HIGH_ACCURACY)` — engages GPS to compute
 *     a fresh fix. Takes a few seconds when the cache is cold, can return null if
 *     no satellites are visible (e.g., deep indoors).
 *
 * The caller MUST verify `ACCESS_FINE_LOCATION` is granted before calling this.
 */
internal object MeshLocationFetcher {

    @SuppressLint("MissingPermission")
    fun fetch(context: Context, onResult: (Location?) -> Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { cached ->
                if (cached != null) onResult(cached)
                else lookupRawProviders(context, client, onResult)
            }
            .addOnFailureListener { lookupRawProviders(context, client, onResult) }
    }

    @SuppressLint("MissingPermission")
    private fun lookupRawProviders(
        context: Context,
        client: FusedLocationProviderClient,
        onResult: (Location?) -> Unit
    ) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var best: Location? = null
        if (lm != null) {
            for (provider in lm.getProviders(true)) {
                val loc = try {
                    lm.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
                if (loc != null && (best == null || loc.time > best.time)) best = loc
            }
        }
        if (best != null) {
            onResult(best)
        } else {
            requestFresh(client, onResult)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFresh(
        client: FusedLocationProviderClient,
        onResult: (Location?) -> Unit
    ) {
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }
}

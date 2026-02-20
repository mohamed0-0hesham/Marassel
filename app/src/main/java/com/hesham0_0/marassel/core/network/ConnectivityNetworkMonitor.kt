package com.hesham0_0.marassel.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [NetworkMonitor] using [ConnectivityManager]
 * and [NetworkCallback] wrapped inside a [callbackFlow].
 *
 * Why callbackFlow?
 * [ConnectivityManager] uses a callback-based API. [callbackFlow] bridges
 * the callback world into coroutines by:
 * 1. Registering the callback when the first collector subscribes (cold start)
 * 2. Emitting values via [trySend] inside callbacks
 * 3. Unregistering the callback via [awaitClose] when all collectors cancel
 *
 * This ensures we never leak a registered [NetworkCallback].
 *
 * Network validation:
 * We only consider a network "online" if it has [NetworkCapabilities.NET_CAPABILITY_INTERNET]
 * AND [NetworkCapabilities.NET_CAPABILITY_VALIDATED]. VALIDATED means the system
 * has confirmed actual internet access (not just a local Wi-Fi connection with
 * no internet, or a captive portal).
 *
 * Multi-network awareness:
 * A device can have multiple active networks simultaneously (Wi-Fi + cellular).
 * We track all validated networks in a set and emit true if ANY are validated.
 * This prevents a false "offline" emission when switching from Wi-Fi to cellular.
 */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {

        // ── Setup ─────────────────────────────────────────────────────────

        val connectivityManager = context.getSystemService<ConnectivityManager>()

        // If ConnectivityManager is unavailable (should never happen on a real
        // device but possible in some test environments), emit offline and return.
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        // Track all currently validated networks.
        // Using a set handles multi-network scenarios:
        // e.g. Wi-Fi loses internet → cellular takes over → still online.
        val validatedNetworks = mutableSetOf<Network>()

        // ── Callback ──────────────────────────────────────────────────────

        val callback = object : NetworkCallback() {

            /**
             * Called when a network becomes available. Note: "available" does
             * NOT mean validated yet — we wait for [onCapabilitiesChanged]
             * to confirm actual internet access before emitting true.
             */
            override fun onAvailable(network: Network) {
                // Don't emit true here — capabilities might not be validated yet
            }

            /**
             * Called when capabilities change for an available network.
             * This is the correct place to check for VALIDATED internet.
             */
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                if (isValidated) {
                    validatedNetworks += network
                } else {
                    validatedNetworks -= network
                }

                trySend(validatedNetworks.isNotEmpty())
            }

            /**
             * Called when a network disconnects.
             * Remove it from the validated set and re-evaluate.
             */
            override fun onLost(network: Network) {
                validatedNetworks -= network
                trySend(validatedNetworks.isNotEmpty())
            }

            /**
             * Called when a network is unavailable after a request.
             * Treat as lost.
             */
            override fun onUnavailable() {
                trySend(validatedNetworks.isNotEmpty())
            }
        }

        // ── Register the callback ─────────────────────────────────────────

        // We request all networks with INTERNET capability.
        // The callback will filter down to VALIDATED ones.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // ── Emit initial state immediately ────────────────────────────────

        // Check current connectivity synchronously before any callbacks fire.
        // This ensures the first collector gets the current state right away
        // rather than waiting for the next network change.
        val currentlyOnline = connectivityManager.isCurrentlyOnline()
        trySend(currentlyOnline)

        // ── Cleanup ───────────────────────────────────────────────────────

        // awaitClose is called when the flow is canceled (all collectors gone).
        // This is CRITICAL — without it, we leak a registered NetworkCallback
        // which prevents the system from GC-ing it and wastes battery.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        // conflate() drops intermediate emissions if the collector is slow.
        // For network state, only the LATEST value matters — we don't need
        // to process every intermediate online/offline toggle.
        .conflate()
        // Suppress duplicate consecutive emissions.
        // e.g. Wi-Fi validated → cellular validated both emit true.
        // distinctUntilChanged ensures the UI only reacts to actual changes.
        .distinctUntilChanged()

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Synchronously checks the current network connectivity state.
     *
     * Used to emit an immediate value when a new collector subscribes,
     * so it doesn't have to wait for the next network change event.
     *
     * Uses the modern [ConnectivityManager.getNetworkCapabilities] API
     * (available from API 23, our minSdk is 24 so this is safe).
     */
    private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
        val activeNetwork = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
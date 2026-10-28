package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes real-time Android device connectivity via [ConnectivityManager] (`ACCESS_NETWORK_STATE`)
 * so the workspace can automatically surface Offline Field Mode and queue sync status.
 */
object NetworkConnectivityObserver {

  fun isCurrentlyConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  fun observeConnectivity(context: Context): Flow<Boolean> =
    callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
          trySend(false)
          close()
          return@callbackFlow
        }

        trySend(isCurrentlyConnected(context))

        val callback =
          object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
              trySend(true)
            }

            override fun onLost(network: Network) {
              trySend(isCurrentlyConnected(context))
            }

            override fun onCapabilitiesChanged(
              network: Network,
              networkCapabilities: NetworkCapabilities,
            ) {
              val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
              trySend(hasInternet)
            }
          }

        val request =
          NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }
          .onFailure { trySend(isCurrentlyConnected(context)) }

        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
      }
      .distinctUntilChanged()
}

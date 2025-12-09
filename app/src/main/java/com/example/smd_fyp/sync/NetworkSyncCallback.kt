package com.example.smd_fyp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Network callback to automatically sync data when connection is restored
 */
class NetworkSyncCallback(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : ConnectivityManager.NetworkCallback() {
    
    private var wasOffline = false
    
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        
        // Check if we were previously offline
        if (wasOffline) {
            android.util.Log.d("NetworkSyncCallback", "Connection restored, triggering sync...")
            
            // Wait a bit to ensure connection is stable
            lifecycleOwner.lifecycleScope.launch {
                delay(2000) // Wait 2 seconds for connection to stabilize
                
                if (SyncManager.isOnline(context)) {
                    android.util.Log.d("NetworkSyncCallback", "Connection stable, syncing all data...")
                    val result = withContext(Dispatchers.IO) {
                        SyncManager.syncAll(context)
                    }
                    
                    result.fold(
                        onSuccess = { syncResult ->
                            android.util.Log.d(
                                "NetworkSyncCallback",
                                "Auto-sync completed: ${syncResult.syncedBookings} bookings, " +
                                "${syncResult.syncedGrounds} grounds, ${syncResult.syncedUsers} users"
                            )
                        },
                        onFailure = { exception ->
                            android.util.Log.e("NetworkSyncCallback", "Auto-sync failed: ${exception.message}", exception)
                        }
                    )
                }
            }
        }
        
        wasOffline = false
    }
    
    override fun onLost(network: Network) {
        super.onLost(network)
        wasOffline = true
        android.util.Log.d("NetworkSyncCallback", "Connection lost")
    }
    
    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                          networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        if (hasInternet && wasOffline) {
            // Connection restored with internet
            onAvailable(network)
        } else if (!hasInternet) {
            wasOffline = true
        }
    }
    
    companion object {
        /**
         * Register network callback for auto-sync
         */
        fun register(context: Context, lifecycleOwner: LifecycleOwner) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            
            val callback = NetworkSyncCallback(context, lifecycleOwner)
            connectivityManager.registerNetworkCallback(networkRequest, callback)
            
            android.util.Log.d("NetworkSyncCallback", "Network callback registered for auto-sync")
        }
    }
}


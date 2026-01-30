package com.playermaxai.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log

class NetworkGuard(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var callback: ConnectivityManager.NetworkCallback? = null
    
    companion object {
        var isConnected = false
            private set
    }
    
    fun init(context: Context) {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = true
                Log.d("NetworkGuard", "Network available")
            }
            
            override fun onLost(network: Network) {
                isConnected = false
                Log.w("NetworkGuard", "Network lost")
            }
            
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, callback!!)
    }
    
    fun destroy() {
        callback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }
}

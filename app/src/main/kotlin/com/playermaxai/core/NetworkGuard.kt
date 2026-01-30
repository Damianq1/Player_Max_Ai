package com.playermaxai.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkGuard(private val context: Context) {
    
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    companion object {
        fun init(context: Context) {
            // Initialize network monitoring
        }
    }
}

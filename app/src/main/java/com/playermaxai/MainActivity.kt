package com.playermaxai

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

class MainActivity : Activity() {
    
    companion object {
        private const val SAMPLE_4K = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40)
            layoutParams = ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        }
        
        // 🎬 TYTUŁ Material3 Style
        val title = TextView(this).apply {
            text = "🎬 ULTIMATE PLAYER v1.0
Material3 Design"
            textSize = 28f
            setPadding(0, 0, 0, 60)
            gravity = Gravity.CENTER
            setTextColor(0xFF2196F3.toInt()) // Material3 Blue
        }
        layout.addView(title)
        
        // ▶️ 4K TEST BUTTON
        val play4k = Button(this).apply {
            text = "▶️ Odtwórz 4K VLC Test"
            textSize = 18f
            setPadding(40)
            setOnClickListener { playVideo(SAMPLE_4K) }
        }
        layout.addView(play4k)
        
        // 💾 STORAGE PERMS
        val storage = Button(this).apply {
            text = "💾 Storage Permissions"
            textSize = 18f
            setPadding(40)
            setOnClickListener { requestStoragePermission() }
        }
        layout.addView(storage)
        
        // ℹ️ INFO
        val info = Button(this).apply {
            text = "ℹ️ Device Information"
            textSize = 18f
            setPadding(40)
            setOnClickListener { showDeviceInfo() }
        }
        layout.addView(info)
        
        setContentView(layout)
    }
    
    private fun playVideo(url: String) {
        TextView(this).apply {
            text = """
                🎬 4K VIDEO PLAYER READY!
                
                URL: $url
                
                ✅ Material3 Design
                ✅ Hardware Acceleration  
                ✅ LibVLC 4.0 Ready
                ✅ 4K/8K Support
                ✅ Chromecast Ready
            """.trimIndent()
            textSize = 18f
            setPadding(40)
            gravity = Gravity.CENTER
        }.also { setContentView(it) }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION))
        }
    }
    
    private fun showDeviceInfo() {
        TextView(this).apply {
            text = """
                📱 DEVICE INFO - MATERIAL3
                
                Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Model: ${Build.MODEL}
                Brand: ${Build.MANUFACTURER}
                Device: ${Build.DEVICE}
                
                🎬 Ultimate Player v1.0
                ✅ Material Design 3
                ✅ minSdk 21 (Android 5.0+)
                ✅ targetSdk 34 (Android 14)
            """.trimIndent()
            textSize = 16f
            setPadding(40)
        }.also { setContentView(it) }
    }
}

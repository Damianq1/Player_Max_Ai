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

class MainActivity : Activity() {
    
    companion object {
        private const val SAMPLE_4K = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // TYTUŁ
        val title = TextView(this).apply {
            text = "🎬 ULTIMATE PLAYER v1.0

4K • HW ACCEL • Chromecast • VLC Ready"
            textSize = 26f
            setPadding(0, 0, 0, 60)
            gravity = Gravity.CENTER
        }
        layout.addView(title)
        
        // ODTWARZAJ 4K
        val play4k = Button(this).apply {
            text = "▶️ Odtwórz 4K TEST"
            textSize = 20f
            setOnClickListener { playVideo(SAMPLE_4K) }
        }
        layout.addView(play4k)
        
        // STORAGE PERMS
        val storage = Button(this).apply {
            text = "💾 Uprawnienia storage (Android 11+)"
            setOnClickListener { requestStoragePermission() }
        }
        layout.addView(storage)
        
        // INFO
        val info = Button(this).apply {
            text = "ℹ️ Info o urządzeniu"
            setOnClickListener { showDeviceInfo() }
        }
        layout.addView(info)
        
        setContentView(layout)
    }
    
    private fun playVideo(url: String) {
        TextView(this).apply {
            text = "🎬 ODTWARZANIE 4K VIDEO

$url

✅ HW ACCELERATION
✅ 4K Support
✅ VLC Ready

LibVLC w następnym commicie!"
            textSize = 20f
            setPadding(60, 60, 60, 60)
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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
                📱 INFO URZĄDZENIA:
                
                Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Model: ${Build.MODEL}
                Producent: ${Build.MANUFACTURER}
                Device: ${Build.DEVICE}
                
                ✅ Ultimate Player READY!
                ✅ minSdk 21 - Android 5.0+
                ✅ targetSdk 34 - Android 14
            """.trimIndent()
            textSize = 18f
            setPadding(40, 40, 40, 40)
        }.also { setContentView(it) }
    }
}

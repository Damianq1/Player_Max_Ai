package com.playermaxai

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.*
import android.content.res.Configuration
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.os.VibrationEffect.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.getSystemService
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewfindViewById<R.id.ViewBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.playermaxai.core.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.videolan.libvlc.*
import java.util.*
import kotlin.math.coerceIn

class MainActivity : AppCompatActivity(), PlaybackListener, MediaPlayer.EventListener, View.OnFocusChangeListener {

    // ViewBinding (szybsze UI)
    private lateinit var root_view: FrameLayout = ActivityMainBinding
    
    // Core Components (Filar I, IV)
    private lateinit var playerEngine: UltimatePlayerEngine
    private lateinit var m3uParser: M3UParserEngine
    private lateinit var networkGuard: NetworkGuard
    
    // State (Filar III, V)
    private var channels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    private var sliderValues = mutableMapOf<String, Int>()
    
    // System Services
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var prefs: SharedPreferences
    private lateinit var handler: Handler
    private lateinit var uiHandler: Handler
    
    // Guardians (Filar IV)
    private var wakeLock: PowerManager.WakeLock? = null
    private var connectivityReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var autoHideRunnable = Runnable { hideUI() }
    private var focusRestoreIndex = 0

    // Gestures + Animations (Filar V)
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // #13 StayAwake
        
        findViewById<R.id.= null
        setContentView(root_view)
        
        initAllSystems()
        setupUI()
        loadPreferences()
        handleIntent(intent) // #98 Direct URL Play
        NetworkGuard.init(this)
    }

    private fun initAllSystems() {
        // Core Engines
        playerEngine = UltimatePlayerEngine(this)
        m3uParser = M3UParserEngine()
        networkGuard = NetworkGuard(this)
        
        // Services
        audioManager = getSystemService()!!
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            getSystemService()!! else null
        prefs = getSharedPreferences("ultimate_player", Context.MODE_PRIVATE)
        handler = Handler(Looper.getMainLooper())
        uiHandler = Handler(Looper.getMainLooper())
        
        // WakeLock (#12)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UltimatePlayer:WakeLock")
        
        // Gesture Detector (#92)
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (velocityX > 2000) showMenu() else if (velocityX < -2000) hideMenu()
                return true
            }
            override fun onLongPress(e: MotionEvent) { showOSD() } // #93
        })
        
        // Connectivity Receiver (#62)
        connectivityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!isNetworkAvailable()) {
                    playerEngine.smartReconnect()
                    findViewById<R.id.statusText.text = getString(R.string.reconnecting)
                }
            }
        }
        
        // Battery Monitor (#72)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra("level", -1) ?: -1
                if (level < 20) Toast.makeText(this@MainActivity, R.string.low_battery, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        // Setup Screen (#24)
        findViewById<R.id.scanButton.setOnClickListener { scanM3U() }
        findViewById<R.id.urlInput.setText(prefs.getString("last_url", ""))
        
        // Sliders (#85-88)
        setupSliders()
        
        // Channel Navigation (#91)
        findViewById<R.id.prevChannelBtn.setOnClickListener { switchChannel(-1) }
        findViewById<R.id.nextChannelBtn.setOnClickListener { switchChannel(1) }
        findViewById<R.id.menuToggleBtn.setOnClickListener { toggleMenu() }
        
        // Focus Listeners (#32)
        listOf(findViewById<R.id.prevChannelBtn, findViewById<R.id.nextChannelBtn, findViewById<R.id.menuToggleBtn, 
               findViewById<R.id.scanButton).forEach { it.onFocusChangeListener = this }
        
        // Touch handling for gestures (#92)
        root_viewContainer.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun setupSliders() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        findViewById<R.id.volumeSeekbar.max = maxVolume
        findViewById<R.id.volumeSeekbar.progress = prefs.getInt("volume", maxVolume / 2)
        findViewById<R.id.volumeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                saveSliderValue("volume", progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { vibrate() }
        })
        
        // Video Filters (#85-87)
        listOf("brightness" to findViewById<R.id.brightnessSeekbar,
               "contrast" to findViewById<R.id.contrastSeekbar,
               "saturation" to findViewById<R.id.saturationSeekbar).forEach { (type, seekbar) ->
            seekbar.max = 200
            seekbar.progress = prefs.getInt("$type-slider", 100)
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    playerEngine.setVideoFilter(type, progress)
                    saveSliderValue("$type-slider", progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) { vibrate() }
            })
        }
    }

    // FILAR III: M3U Scanning (#41-60)
    private fun scanM3U() {
        val url = findViewById<R.id.urlInput.text.toString().trim()
        if (url.isEmpty()) return
        
        prefs.edit().putString("last_url", url).apply()
        findViewById<R.id.statusText.text = "Skanowanie..."
        hideKeyboard()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                channels = m3uParser.parseM3U(url)
                withContext(Dispatchers.Main) {
                    onScanComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<R.id.statusText.text = getString(R.string.scan_error, e.message)
                }
            }
        }
    }

    private fun onScanComplete() {
        findViewById<R.id.statusText.text = getString(R.string.channels_found, channels.size)
        Toast.makeText(this, "${channels.size} kanałów", Toast.LENGTH_SHORT).show() // #47
        
        buildChannelList() // #52 Dynamic Button Injector
        findViewById<R.id.setupScreen.isVisible = false
        showMenu()
        
        if (channels.isNotEmpty()) {
            currentChannelIndex = prefs.getInt("last_channel", 0).coerceIn(0, channels.lastIndex)
            playerEngine.playChannel(channels[currentChannelIndex]) // #58 Auto-start
        }
    }

    private fun buildChannelList() {
        findViewById<R.id.channelList.removeAllViews()
        channels.forEachIndexed { index, channel ->
            val button = MaterialButton(this).apply {
                text = channel.name
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 80.dpToPx()
                )
                setBackgroundResource(R.drawable.channel_button)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 16f
                setPadding(24.dpToPx(), 0, 24.dpToPx(), 0)
                tag = index
                setOnClickListener { 
                    currentChannelIndex = index
                    playerEngine.playChannel(channel)
                    showOSD(channel.name)
                    hideMenu()
                    vibrate()
                }
                setOnFocusChangeListener(this@MainActivity)
                setOnLongClickListener { 
                    showOSD(channel.name); true 
                }
            }
            findViewById<R.id.channelList.addView(button)
        }
        restoreFocus() // #95 Focus Restoration
    }

    // FILAR V: Sterowanie (#81-100)
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return when (event?.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { // #81
                if (findViewById<R.id.sideMenu.isVisible) hideMenu() else switchChannel(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { // #81
                switchChannel(1)
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> { // #84
                playerEngine.togglePlayPause()
                vibrate()
                true
            }
            KeyEvent.KEYCODE_MENU -> { // #82
                toggleMenu()
                true
            }
            KeyEvent.KEYCODE_BACK -> { // #83
                handleBackPress()
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }

    private fun switchChannel(delta: Int) {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + delta).coerceIn(0, channels.lastIndex)
        val channel = channels[currentChannelIndex]
        playerEngine.playChannel(channel)
        showOSD(channel.name)
        prefs.edit().putInt("last_channel", currentChannelIndex).apply() // #79
        vibrate()
    }

    private fun toggleMenu() {
        if (findViewById<R.id.sideMenu.isVisible) hideMenu() else showMenu()
    }

    private fun showMenu() {
        findViewById<R.id.sideMenu.isVisible = true
        findViewById<R.id.sideMenu.animate()
            .translationX(0f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(300)
            .start()
        uiHandler.removeCallbacks(autoHideRunnable)
        uiHandler.postDelayed(autoHideRunnable, 5000) // #94 UI Auto-Hide
    }

    private fun hideMenu() {
        findViewById<R.id.sideMenu.animate()
            .translationX((-700f).dpToPx())
            .setInterpolator(AccelerateInterpolator())
            .setDuration(250)
            .doOnEnd { findViewById<R.id.sideMenu.isVisible = false }
            .start()
    }

    private fun showOSD(channelName: String) {
        findViewById<R.id.osdChannelName.text = channelName
        findViewById<R.id.osdChannelName.animate().alpha(1f).setDuration(200).start()
        findViewById<R.id.osdControls.isVisible = true
        uiHandler.removeCallbacks(autoHideRunnable)
        uiHandler.postDelayed({ hideOSD() }, 5000)
    }

    private fun hideOSD() {
        findViewById<R.id.osdControls.animate().alpha(0f).setDuration(300).doOnEnd {
            findViewById<R.id.osdControls.isVisible = false
        }.start()
    }

    private fun hideUI() {
        if (findViewById<R.id.sideMenu.isVisible) hideMenu()
        if (findViewById<R.id.osdControls.isVisible) hideOSD()
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (hasFocus) {
            view?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(150)?.start()
            vibrate()
        } else {
            view?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
        }
    }

    private fun handleBackPress() {
        when {
            findViewById<R.id.sideMenu.isVisible -> hideMenu()
            findViewById<R.id.osdControls.isVisible -> hideOSD()
            else -> {
                // #40 Exit Confirmation
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.exit_confirm)
                    .setPositiveButton("Tak") { _, _ -> 
                        playerEngine.release()
                        finishAndRemoveTask()
                    }
                    .setNegativeButton("Nie", null)
                    .show()
            }
        }
    }

    private fun restoreFocus() {
        findViewById<R.id.channelList.getChildAt(focusRestoreIndex)?.requestFocus()
    }

    private fun saveSliderValue(key: String, value: Int) {
        sliderValues[key] = value
        prefs.edit().putInt(key, value).apply()
    }

    private fun loadPreferences() {
        findViewById<R.id.urlInput.setText(prefs.getString("last_url", ""))
        sliderValues["volume"] = prefs.getInt("volume", 50)
        // Load other sliders...
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService<ConnectivityManager>()!!
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hideKeyboard() {
        val imm = getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(findViewById<R.id.urlInput.windowToken, 0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.let { url ->
            findViewById<R.id.urlInput.setText(url)
            scanM3U()
        }
    }

    // PlaybackListener (#64)
    override fun onFreezeDetected() {
        runOnUiThread {
            playerEngine.playSmartFromFreeze()
            Toast.makeText(this, "Zamrożenie wykryte - wznowiono", Toast.LENGTH_SHORT).show()
        }
    }

    // MediaPlayer.EventListener (#20)
    override fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.EndReached, MediaPlayer.Event.EncounteredError -> {
                playerEngine.smartReconnect()
            }
            MediaPlayer.Event.Buffering -> {
                findViewById<R.id.statusText.text = getString(R.string.buffering)
            }
            MediaPlayer.Event.Playing -> {
                findViewById<R.id.statusText.text = "Odtwarzanie"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        wakeLock?.acquire(10*60*1000L) // 10 min
        registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        wakeLock?.release()
        unregisterReceiver(connectivityReceiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onDestroy() {
        playerEngine.release() // #99 App Close Cleanup
        super.onDestroy()
    }
}

// Extensions
private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

package com.playermaxai.core

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import org.videolan.libvlc.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class UltimatePlayerEngine(
    private val activity: AppCompatActivity
) : MediaPlayer.EventListener {
    
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentMedia: Media? = null
    
    // Guardians State
    private val listener: PlaybackListener = activity as PlaybackListener
    private val isReleased = AtomicBoolean(false)
    private var lastPosition = 0L
    private var freezeCounter = 0
    private var reconnectAttempts = 0
    private val maxReconnects = 3
    
    companion object {
        private val VLC_OPTS = arrayListOf(
            // FILAR I: Hardware Acceleration #6-7
            "--avcodec-hw=mediacodec,new",
            "--vout=android",
            "--aout=opensles",
            
            // #8 Network Caching Engine
            "--network-caching=4000",
            "--file-caching=3000",
            "--live-caching=1000",
            
            // #9 Clock Jitter + #10 TCP Force
            "--clock-jitter=0",
            "--network-tcp",
            "--http-reconnect",
            
            // #11 Cleartext + #17 Aspect Ratio
            "--no-one-instance",
            "--aspect-ratio=16:9",
            
            // #18 Audio Path + #19 Deinterlace
            "--audio-time-stretch",
            "--deinterlace-mode=yadif:2:4",
            
            // #74 Keep-Alive + #75 User-Agent
            "--http-user-agent=VLC/3.6.0 LibVLC/3.6.0",
            "--http-continuous",
            
            // #67 Latency + #76 Adaptive Bitrate
            "--live-flushing=0",
            "--hls-vod-mode=0"
        )
    }
    
    init {
        initLibVLC()
    }
    
    private fun initLibVLC() {
        libVLC = LibVLC(activity, VLC_OPTS)
        mediaPlayer = MediaPlayer(libVLC).apply {
            setEventListener(this@UltimatePlayerEngine)
        }
    }
    
    fun playChannel(channel: Channel) {
        if (isReleased.get()) return
        
        releaseCurrentMedia()
        
        val surfaceView = activity.findViewById<SurfaceView>(R.id.video_surface)
        mediaPlayer?.vlcVout?.setVideoView(surfaceView)
        mediaPlayer?.vlcVout?.attachViews()
        
        currentMedia = Media(libVLC, Uri.parse(channel.url)).apply {
            // Per-channel options
            addOption(":network-caching=5000")
            addOption(":http-referrer=")
            addOption(":no-video-title-show")
            if (channel.logoUrl != null) {
                addOption(":meta-title=${channel.name}")
            }
        }
        
        mediaPlayer?.media = currentMedia
        mediaPlayer?.play()
        
        Log.d("UltimatePlayer", "Playing: ${channel.name}")
        resetFreezeMonitor()
        reconnectAttempts = 0
    }
    
    fun setVideoFilter(type: String, value: Int) {
        // #5 Adjust Filter Loader
        val filterValue = ((value - 100) * 2.55).toInt() // 0-255 range
        val filter = when (type) {
            "brightness" -> "adjust{brightness=$filterValue}"
            "contrast" -> "adjust{contrast=$filterValue}"
            "saturation" -> "adjust{saturation=$filterValue}"
            else -> return
        }
        
        mediaPlayer?.let { mp ->
            mp.setEqualizer(null) // Reset first
            // VLC adjust filter (LibVLC 3.6 support)
            val options = arrayListOf(filter)
            mp.vlcVout?.setVideoFilterList(options.toTypedArray())
        }
    }
    
    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }
    
    fun smartReconnect() {
        if (reconnectAttempts >= maxReconnects || isReleased.get()) return
        
        reconnectAttempts++
        val delay = when (reconnectAttempts) {
            1 -> 5000L  // #65 Smart Reconnect Loop
            2 -> 10000L
            else -> 30000L
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            listener.onBufferingStateChanged(true)
            // Replay current channel (assumes activity has channels list)
            val activity = activity as? MainActivity
            activity?.channels?.getOrNull(activity.currentChannelIndex)?.let { channel ->
                playChannel(channel)
            }
        }, delay)
    }
    
    fun playSmartFromFreeze() {
        // #68 Player Recovery Logic
        mediaPlayer?.let { mp ->
            val currentPos = mp.time
            mp.time = max(0, currentPos - 2000) // Rewind 2s
            mp.play()
            Log.d("UltimatePlayer", "Smart recovery from freeze")
        }
    }
    
    override fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                listener.onBufferingStateChanged(false)
                resetFreezeMonitor()
            }
            MediaPlayer.Event.Buffering -> {
                listener.onBufferingStateChanged(true)
            }
            MediaPlayer.Event.EncounteredError, MediaPlayer.Event.EndReached -> {
                listener.onFreezeDetected()
                smartReconnect()
            }
            MediaPlayer.Event.Stopped -> {
                automaticVoutReset()
            }
        }
    }
    
    private fun resetFreezeMonitor() {
        lastPosition = 0L
        freezeCounter = 0
    }
    
    private fun automaticVoutReset() {
        // #67 Automatic Vout Reset
        mediaPlayer?.vlcVout?.detachViews()
        val surfaceView = activity.findViewById<SurfaceView>(R.id.video_surface)
        mediaPlayer?.vlcVout?.setVideoView(surfaceView)
        mediaPlayer?.vlcVout?.attachViews()
    }
    
    private fun releaseCurrentMedia() {
        currentMedia?.release()
        currentMedia = null
    }
    
    fun release() {
        if (isReleased.compareAndSet(false, true)) {
            mediaPlayer?.apply {
                vlcVout.detachViews()
                stop()
                release()
            }
            currentMedia?.release()
            libVLC?.release()
            Log.d("UltimatePlayer", "UltimatePlayerEngine released")
        }
    }
}

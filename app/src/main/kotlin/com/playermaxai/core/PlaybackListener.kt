package com.playermaxai.core

interface PlaybackListener {
    fun onFreezeDetected()
    fun onBufferingStateChanged(isBuffering: Boolean)
}

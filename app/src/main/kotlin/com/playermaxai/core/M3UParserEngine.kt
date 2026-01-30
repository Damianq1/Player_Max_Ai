package com.playermaxai.core

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern

class M3UParserEngine {
    
    fun parseM3U(urlString: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val channelId = AtomicInteger(1)
        
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 15000 // #56 Timeout Manager
                readTimeout = 15000
                setRequestProperty("User-Agent", "VLC/3.0 AppleWebKit/537.11") // #75
                setRequestProperty("Connection", "keep-alive") // #74
            }
            
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("M3UParser", "HTTP ${connection.responseCode}")
                return emptyList()
            }
            
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                parseLineByLine(reader, channels, channelId)
            }
        } catch (e: Exception) {
            Log.e("M3UParser", "Parse error: ${e.message}")
        }
        
        return channels
    }
    
    private fun parseLineByLine(
        reader: BufferedReader, 
        channels: MutableList<Channel>,
        channelId: AtomicInteger
    ) {
        var line: String?
        var currentName: String? = null
        var currentLogo: String? = null
        
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue
            
            when {
                // #43 EXTINF Tag Extractor
                trimmed.startsWith("#EXTINF:") -> {
                    currentName = extractChannelName(trimmed)
                    currentLogo = extractLogoUrl(trimmed)
                }
                // #45 URL Extractor + #59 URL Decoder
                trimmed.startsWith("http") -> {
                    if (currentName != null) {
                        val cleanUrl = URLDecoder.decode(trimmed, "UTF-8")
                        val cleanName = cleanChannelName(currentName) // #44
                        channels.add(Channel(channelId.getAndIncrement(), cleanName, cleanUrl, currentLogo))
                        currentName = null
                        currentLogo = null
                    }
                }
            }
        }
    }
    
    private fun extractChannelName(extinf: String): String {
        return Pattern.compile("#EXTINF:.*?,(.*)").matcher(extinf).also { it.find() }
            ?.group(1)?.trim() ?: "Unknown Channel"
    }
    
    private fun extractLogoUrl(extinf: String): String? {
        val logoMatch = Pattern.compile("tvg-logo="([^"]+)"").matcher(extinf)
        return if (logoMatch.find()) logoMatch.group(1) else null
    }
    
    private fun cleanChannelName(name: String): String {
        // #44 Channel Name Cleaner
        return name.replace(Regex("[\\(\\)\\[\\]\\-\\d]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(50) // Max 50 chars for UI
    }
}

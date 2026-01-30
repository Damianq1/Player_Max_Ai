package com.playermaxai.core

data class Channel(
    val id: Int,
    val name: String,
    val url: String,
    val logoUrl: String? = null
)

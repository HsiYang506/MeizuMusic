package com.jywkhyse.meizumusic.core.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: android.net.Uri?,
    val songCount: Int
)
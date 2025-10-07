package com.jywkhyse.meizumusic.core.model

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MediaMetadata as Media3MediaMetadata

@Parcelize
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: Uri,
    val bitrate: Int,
    val filePath: String
) : Parcelable {
    fun toMedia3MediaItem() =
        Media3MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                Media3MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(albumArtUri)
                    .setExtras(Bundle().apply {
                        putString("file_path", filePath)
                    })
                    .build()
            )
            .build()

}
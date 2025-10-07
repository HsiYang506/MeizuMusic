// 文件: /app/src/main/java/com/jywkhyse/meizumusic/core/model/PlaylistForUi.kt
package com.jywkhyse.meizumusic.core.model

import android.net.Uri

data class PlaylistForUi(
    val playlistId: Long,
    val name: String,
    val songCount: Int,
    val coverArtUri: Uri?
)
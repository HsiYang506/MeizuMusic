// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistEntity.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis()
)
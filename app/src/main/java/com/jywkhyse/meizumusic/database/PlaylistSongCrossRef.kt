// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistSongCrossRef.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Entity
import androidx.room.Index // <--- 导入这个

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songMediaId"],
    // --- 添加这行代码来创建索引 ---
    indices = [Index(value = ["songMediaId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songMediaId: Long
)
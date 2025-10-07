// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/SongUserData.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_user_data")
data class SongUserData(
    @PrimaryKey
    val mediaId: Long, // 对应 MediaStore.Audio.Media._ID，作为主键
    var isFavorite: Boolean = false,
    var playCount: Int = 0,
    var lastPlayedTimestamp: Long = 0, // 最后播放的时间戳
    var totalPlayDurationMillis: Long = 0 // <--- 新增字段：总播放时长（毫秒）
)
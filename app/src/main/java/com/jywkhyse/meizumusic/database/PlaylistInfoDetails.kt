// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistInfoDetails.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Embedded

/**
 * 一个从数据库查询出的、包含歌单详情的数据类。
 * 比 PlaylistInfo 多了一个 lastAddedSongMediaId 字段。
 */
data class PlaylistInfoDetails(
    @Embedded
    val playlist: PlaylistEntity,

    val songCount: Int,

    // 最近添加歌曲的 MediaStore ID
    val lastAddedSongMediaId: Long?
)
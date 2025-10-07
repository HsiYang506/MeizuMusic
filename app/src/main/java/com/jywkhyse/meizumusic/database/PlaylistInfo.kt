// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistInfo.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Embedded

/**
 * 一个用于UI层展示的数据类，包含了歌单实体信息和其实时计算出的歌曲数量。
 */
data class PlaylistInfo(
    @Embedded
    val playlist: PlaylistEntity,

    val songCount: Int
)
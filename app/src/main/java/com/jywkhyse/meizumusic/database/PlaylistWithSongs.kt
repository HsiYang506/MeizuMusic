// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistWithSongs.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        parentColumn = "playlistId",
        entity = SongUserData::class,       // 显式指定目标实体，更清晰
        entityColumn = "mediaId",           // ★★★ 关键修正点 ★★★：这里必须是 SongUserData 的主键/关联键
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",    // 中间表中，指向 PlaylistEntity 的外键
            entityColumn = "songMediaId"    // 中间表中，指向 SongUserData 的外键
        )
    )
    val songs: List<SongUserData>
)
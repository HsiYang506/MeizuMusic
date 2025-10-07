// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/AppDatabase.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongUserData::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
    ], version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songUserDataDao(): SongUserDataDao
    abstract fun playlistDao(): PlaylistDao // <--- 新增 PlaylistDao

}
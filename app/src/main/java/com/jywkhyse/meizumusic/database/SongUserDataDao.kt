// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/SongUserDataDao.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongUserDataDao {

    // 插入或更新一条数据
    @Upsert
    suspend fun upsert(songUserData: SongUserData)

    // 根据歌曲ID获取用户数据，使用Flow使其可被观察
    @Query("SELECT * FROM song_user_data WHERE mediaId = :mediaId")
    fun getSongUserData(mediaId: Long): Flow<SongUserData?>

    /**
     * 查询所有被标记为“喜欢”的歌曲。
     * 按 mediaId 降序排序，让最新喜欢的歌曲排在前面。
     */
    @Query("SELECT * FROM song_user_data WHERE isFavorite = 1 ORDER BY mediaId DESC")
    fun getFavoriteSongs(): Flow<List<SongUserData>>

    /**
     * 查询最近播放过的歌曲。
     * lastPlayedTimestamp > 0 确保只查询播放过的。
     * ORDER BY lastPlayedTimestamp DESC 确保按播放时间倒序排列（最近的在前）。
     * LIMIT 100 表示最多只保留最近的 100 条记录。
     */
    @Query("SELECT * FROM song_user_data WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 100")
    fun getRecentlyPlayedSongs(): Flow<List<SongUserData>>
}
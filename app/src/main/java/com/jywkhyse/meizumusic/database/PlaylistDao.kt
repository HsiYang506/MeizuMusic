// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/PlaylistDao.kt
package com.jywkhyse.meizumusic.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    // 插入一个新歌单
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    // 删除歌单
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun renamePlaylist(playlist: PlaylistEntity)

    // 或者使用 @Query 自定义更新特定字段
    @Query("UPDATE playlists SET name = :name WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)


    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // 向歌单中添加一首歌（即插入一条关联记录）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIntoPlaylist(crossRef: PlaylistSongCrossRef)

    // 从歌单中移除一首歌
    @Delete
    suspend fun deleteSongFromPlaylist(crossRef: PlaylistSongCrossRef)

    // 获取所有歌单（不包含歌曲详情）
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    // 使用 @Transaction 获取指定ID的、包含所有歌曲的歌单
    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs>

    /**
     * 新增：查询所有歌单及其包含的歌曲数量。
     * 这个查询使用了 LEFT JOIN 和 GROUP BY 来确保即使是歌曲数量为 0 的空歌单也能被正确查询出来。
     */
    @Query(
        """
        SELECT *, 
               (SELECT COUNT(songMediaId) 
                FROM playlist_song_cross_ref 
                WHERE playlist_song_cross_ref.playlistId = playlists.playlistId) AS songCount
        FROM playlists
        ORDER BY createdAt DESC
    """
    )
    fun getAllPlaylistInfo(): Flow<List<PlaylistInfo>>

    /**
     * REPLACED: 查询所有歌单及其歌曲数量，以及最后添加歌曲的 mediaId。
     * - (SELECT COUNT...) 用于计算歌曲总数。
     * - (SELECT songMediaId...ORDER BY rowid DESC LIMIT 1) 用于获取最后插入的 songMediaId。
     * `rowid` 是 SQLite 内部的行ID，可以作为插入顺序的可靠代理。
     */
    @Query(
        """
        SELECT 
            p.*,
            (SELECT COUNT(ps_inner.songMediaId) 
             FROM playlist_song_cross_ref AS ps_inner 
             WHERE ps_inner.playlistId = p.playlistId) AS songCount,
            (SELECT ps_outer.songMediaId 
             FROM playlist_song_cross_ref AS ps_outer 
             WHERE ps_outer.playlistId = p.playlistId 
             ORDER BY ps_outer.rowid DESC LIMIT 1) AS lastAddedSongMediaId
        FROM playlists AS p
        ORDER BY p.createdAt DESC
    """
    )
    fun getAllPlaylistInfoDetails(): Flow<List<PlaylistInfoDetails>>
}
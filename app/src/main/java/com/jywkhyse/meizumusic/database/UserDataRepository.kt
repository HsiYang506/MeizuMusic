// 文件: /app/src/main/java/com/jywkhyse/meizumusic/database/UserDataRepository.kt
package com.jywkhyse.meizumusic.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val dao: SongUserDataDao,
    private val playlistDao: PlaylistDao // <--- 注入 PlaylistDao
) {

    // 获取一首歌的用户数据 Flow
    fun getSongUserData(mediaId: Long) = dao.getSongUserData(mediaId)

    // 切换一首歌的“喜欢”状态
    suspend fun toggleFavorite(mediaId: Long) = withContext(Dispatchers.IO) {
        val currentData = dao.getSongUserData(mediaId).first() ?: SongUserData(mediaId)
        val newData = currentData.copy(isFavorite = !currentData.isFavorite)
        dao.upsert(newData)
    }

    // 记录一次播放
    suspend fun recordPlayback(mediaId: Long, durationMillis: Long) = withContext(Dispatchers.IO) {
        val currentData = dao.getSongUserData(mediaId).first() ?: SongUserData(mediaId)
        val newData = currentData.copy(
            playCount = currentData.playCount + 1,
            lastPlayedTimestamp = System.currentTimeMillis(),
            totalPlayDurationMillis = currentData.totalPlayDurationMillis + durationMillis
        )
        dao.upsert(newData)
    }

    fun getRecentlyPlayedSongs() = dao.getRecentlyPlayedSongs()
    fun getFavoriteSongs() = dao.getFavoriteSongs()

    fun getAllPlaylists() = playlistDao.getAllPlaylists() // <--- 获取所有播放列表

    fun getAllPlaylistInfo() = playlistDao.getAllPlaylistInfo()

    suspend fun createPlaylist(name: String, description: String? = null): Long =
        withContext(Dispatchers.IO) {
            val playlist = PlaylistEntity(name = name, description = description)
            playlistDao.insertPlaylist(playlist)
        }

    suspend fun deletePlaylist(playlistId: Long): Unit =
        withContext(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
        }

    suspend fun renamePlaylist(playlistId: Long, toName: String): Unit =
        withContext(Dispatchers.IO) {
            playlistDao.renamePlaylist(playlistId, toName)
        }


    suspend fun addSongToPlaylist(playlistId: Long, mediaId: Long) =
        withContext(Dispatchers.IO) {
            val crossRef = PlaylistSongCrossRef(playlistId = playlistId, songMediaId = mediaId)
            playlistDao.insertSongIntoPlaylist(crossRef)
        }

    suspend fun removeSongFromPlaylist(playlistId: Long, mediaId: Long) =
        withContext(Dispatchers.IO) {
            val crossRef = PlaylistSongCrossRef(playlistId = playlistId, songMediaId = mediaId)
            playlistDao.deleteSongFromPlaylist(crossRef)
        }

    fun getPlaylistWithSongs(playlistId: Long) = playlistDao.getPlaylistWithSongs(playlistId)


}
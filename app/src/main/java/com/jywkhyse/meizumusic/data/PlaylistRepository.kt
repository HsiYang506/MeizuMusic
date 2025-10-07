// 文件: /app/src/main/java/com/jywkhyse/meizumusic/data/PlaylistRepository.kt
package com.jywkhyse.meizumusic.data

import com.jywkhyse.meizumusic.core.model.PlaylistForUi
import com.jywkhyse.meizumusic.database.PlaylistDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val musicRepository: MusicRepository // 依赖 MusicRepository 来查询歌曲信息
) {

    fun getPlaylistsForUi(): Flow<List<PlaylistForUi>> {
        return playlistDao.getAllPlaylistInfoDetails()
            .map { detailsList ->
                // 1. 批量获取所有需要的歌曲信息
                val songIds = detailsList.mapNotNull { it.lastAddedSongMediaId }
                val songsMap = musicRepository.getSongsByIds(songIds).associateBy { it.id }

                // 2. 将数据库模型映射为 UI 模型
                detailsList.map { detail ->
                    val lastSong = songsMap[detail.lastAddedSongMediaId]
                    PlaylistForUi(
                        playlistId = detail.playlist.playlistId,
                        name = detail.playlist.name,
                        songCount = detail.songCount,
                        coverArtUri = lastSong?.albumArtUri
                    )
                }
            }
    }

    // ... (其他方法，如 createPlaylist, addSongToPlaylist 等)
}
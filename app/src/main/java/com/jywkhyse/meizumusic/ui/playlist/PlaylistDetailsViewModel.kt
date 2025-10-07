// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/playlist/PlaylistDetailsViewModel.kt
package com.jywkhyse.meizumusic.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import com.jywkhyse.meizumusic.database.PlaylistDao
import com.jywkhyse.meizumusic.database.PlaylistSongCrossRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle // 用于获取从 Intent 传入的参数
) : ViewModel() {

    // 从 Intent 中安全地获取 playlistId
    val playlistId: Long = savedStateHandle.get<Long>("playlist_id")!!

    private val _playlistName = MutableStateFlow("")
    val playlistName: StateFlow<String> = _playlistName

    // 使用 flatMapLatest 将数据库 Flow 转换为最终的歌曲列表 Flow
    val songsInPlaylist: StateFlow<List<MediaItem>> =
        playlistDao.getPlaylistWithSongs(playlistId)
            .flatMapLatest { playlistWithSongs ->
                _playlistName.value = playlistWithSongs.playlist.name
                val ids = playlistWithSongs.songs.map { it.mediaId }
                flow { emit(musicRepository.getSongsByIds(ids)) }
                    .map { mediaItemList ->
                        // 保持 Room 返回的顺序
                        val mediaItemMap = mediaItemList.associateBy { it.id }
                        playlistWithSongs.songs.mapNotNull { userData -> mediaItemMap[userData.mediaId] }
                    }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun removeSongFromPlaylist(song: MediaItem) {
        viewModelScope.launch {
            playlistDao.deleteSongFromPlaylist(
                PlaylistSongCrossRef(playlistId = playlistId, songMediaId = song.id)
            )
        }
    }

    fun renamePlaylist(playlistId: Long, toName: String) {
        viewModelScope.launch {
            playlistDao.renamePlaylist(playlistId, toName)
        }
    }
}
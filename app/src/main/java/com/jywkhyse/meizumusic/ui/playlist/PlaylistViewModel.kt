package com.jywkhyse.meizumusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.PlaylistForUi
import com.jywkhyse.meizumusic.data.PlaylistRepository
import com.jywkhyse.meizumusic.database.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val playlistsForUi: StateFlow<List<PlaylistForUi>> =
        playlistRepository.getPlaylistsForUi()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            userDataRepository.createPlaylist(name, description)
        }
    }

    fun addSongToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            userDataRepository.addSongToPlaylist(playlistId, mediaId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            userDataRepository.removeSongFromPlaylist(playlistId, mediaId)
        }
    }

    fun getPlaylistWithSongs(playlistId: Long) {
        viewModelScope.launch {
            userDataRepository.getPlaylistWithSongs(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            userDataRepository.deletePlaylist(playlistId)
        }
    }

    fun renamePlaylist(playlistId: Long, toName: String) {
        viewModelScope.launch {
            userDataRepository.renamePlaylist(playlistId, toName)
        }
    }


}
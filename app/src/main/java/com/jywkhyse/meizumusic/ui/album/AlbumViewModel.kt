package com.jywkhyse.meizumusic.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.Album
import com.jywkhyse.meizumusic.domain.GetMusicAlbumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getMusicAlbumUseCase: GetMusicAlbumUseCase
) : ViewModel() {

    private val _albums = MutableStateFlow<MutableList<Album>>(mutableListOf())
    val albums = _albums.asStateFlow()

    init {
        viewModelScope.launch {
            _albums.value = getMusicAlbumUseCase().toMutableList()
        }
    }
}
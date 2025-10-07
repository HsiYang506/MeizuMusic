package com.jywkhyse.meizumusic.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.Artist
import com.jywkhyse.meizumusic.domain.GetMusicArtistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val getMusicArtistUseCase: GetMusicArtistUseCase
) : ViewModel() {

    private val _artist = MutableStateFlow<MutableList<Artist>>(mutableListOf())
    val artists = _artist.asStateFlow()

    init {
        viewModelScope.launch {
            _artist.value = getMusicArtistUseCase().toMutableList()
        }
    }
}
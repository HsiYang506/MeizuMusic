package com.jywkhyse.meizumusic.ui.artist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.domain.GetMusicByArtistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val useCage: GetMusicByArtistUseCase
) : ViewModel() {

    private val _id = MutableStateFlow(-1L)


    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            _id
                .debounce(300) // 防抖：等待用户停止输入300毫秒后再搜索
                .filter { it != -1L } // 仅在查询非空时继续
                .distinctUntilChanged() // 仅在查询文本改变时继续
                .flatMapLatest { query ->
                    flow {
                        _isLoading.value = true
                        try {
                            val results = useCage(query)
                            emit(results)
                        } catch (e: Exception) {
                            // 处理错误
                            emit(emptyList())
                        } finally {
                            _isLoading.value = false
                        }
                    }
                }
                .collect { results ->
                    _mediaItems.value = results
                }
        }
    }

    fun onArtistIdChanged(albumId: Long) {
        _id.value = albumId
        Log.d("AlbumDetailsViewModel", "onArtistIdChanged: $albumId")
        if (_id.value == -1L) {
            _mediaItems.value = emptyList() // 如果清空输入，则立即清空结果
        }
    }
}
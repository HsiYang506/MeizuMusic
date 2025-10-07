// file: /app/src/main/java/com/jywkhyse/meizumusic/ui/MusicListViewModel.kt
package com.jywkhyse.meizumusic.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.SortOrder
import com.jywkhyse.meizumusic.domain.GetMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val getMusicUseCase: GetMusicUseCase
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    @OptIn(ExperimentalCoroutinesApi::class)
    val musicList: Flow<PagingData<MediaItem>> = _sortOrder.flatMapLatest { order ->
        getMusicUseCase(order)
    }.cachedIn(viewModelScope)

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }
}
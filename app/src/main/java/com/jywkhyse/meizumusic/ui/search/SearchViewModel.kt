// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchViewModel.kt
package com.jywkhyse.meizumusic.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
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
@OptIn(FlowPreview::class)
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 防抖：等待用户停止输入300毫秒后再搜索
                .filter { it.isNotBlank() } // 仅在查询非空时继续
                .distinctUntilChanged() // 仅在查询文本改变时继续
                .flatMapLatest { query ->
                    flow {
                        _isLoading.value = true
                        try {
                            val results = musicRepository.searchMusic(query)
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
                    _searchResults.value = results
                }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList() // 如果清空输入，则立即清空结果
        }
    }
}
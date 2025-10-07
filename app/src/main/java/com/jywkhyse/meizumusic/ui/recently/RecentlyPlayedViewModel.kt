package com.jywkhyse.meizumusic.ui.recently

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import com.jywkhyse.meizumusic.database.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
    musicRepository: MusicRepository
) : ViewModel() {

    val recentlyPlayedSongs: StateFlow<List<MediaItem>> =
        // 1. 从 Room 获取最近播放的 SongUserData 列表 (Flow)
        userDataRepository.getRecentlyPlayedSongs()
            .flatMapLatest { songUserDataList ->
                // 2. 提取所有 mediaId
                val ids = songUserDataList.map { it.mediaId }

                // 3. 使用这些 ID 去 MediaStore 批量查询歌曲详情
                flow { emit(musicRepository.getSongsByIds(ids)) }
                    .map { mediaItemList ->
                        // 4. (重要) 保持 Room 返回的顺序 (按播放时间排序)
                        val mediaItemMap = mediaItemList.associateBy { it.id }
                        songUserDataList.mapNotNull { userData -> mediaItemMap[userData.mediaId] }
                    }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
// file: /app/src/main/java/com/jywkhyse/meizumusic/domain/GetMusicUseCase.kt
package com.jywkhyse.meizumusic.domain

import androidx.paging.PagingData
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import com.jywkhyse.meizumusic.data.SortOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMusicUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(sortOrder: SortOrder): Flow<PagingData<MediaItem>> {
        return musicRepository.getMusic(sortOrder)
    }
}
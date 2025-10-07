// file: /app/src/main/java/com/jywkhyse/meizumusic/domain/GetMusicUseCase.kt
package com.jywkhyse.meizumusic.domain

import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import javax.inject.Inject

class GetMusicByAlbumUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(albumId: Long): List<MediaItem> {
        return musicRepository.getSongsByAlbum(albumId)
    }
}
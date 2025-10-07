// file: /app/src/main/java/com/jywkhyse/meizumusic/domain/GetMusicUseCase.kt
package com.jywkhyse.meizumusic.domain

import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.MusicRepository
import javax.inject.Inject

class GetMusicByFolderUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(folderPath: String): List<MediaItem> {
        return musicRepository.getSongsByFolder(folderPath)
    }
}
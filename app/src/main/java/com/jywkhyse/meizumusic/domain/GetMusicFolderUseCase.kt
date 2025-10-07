// file: /app/src/main/java/com/jywkhyse/meizumusic/domain/GetMusicUseCase.kt
package com.jywkhyse.meizumusic.domain

import com.jywkhyse.meizumusic.core.model.Folder
import com.jywkhyse.meizumusic.data.MusicRepository
import javax.inject.Inject

class GetMusicFolderUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): List<Folder> {
        return musicRepository.getFolders()
    }
}
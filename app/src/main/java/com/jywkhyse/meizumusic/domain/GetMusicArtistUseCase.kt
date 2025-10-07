// file: /app/src/main/java/com/jywkhyse/meizumusic/domain/GetMusicUseCase.kt
package com.jywkhyse.meizumusic.domain

import com.jywkhyse.meizumusic.core.model.Artist
import com.jywkhyse.meizumusic.data.MusicRepository
import javax.inject.Inject

class GetMusicArtistUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): List<Artist> {
        return musicRepository.getArtists()
    }
}
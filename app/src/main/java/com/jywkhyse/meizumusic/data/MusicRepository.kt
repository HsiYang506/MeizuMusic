package com.jywkhyse.meizumusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.jywkhyse.meizumusic.core.model.Album
import com.jywkhyse.meizumusic.core.model.Artist
import com.jywkhyse.meizumusic.core.model.Folder
import com.jywkhyse.meizumusic.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.common.MediaItem as Media3MediaItem // 使用别名


@Singleton
class MusicRepository @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val ALBUM_ART_URI: android.net.Uri = "content://media/external/audio/albumart".toUri()

        val MUSIC_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.DATA
        )

        val ALBUM_PROJECTION = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        private val ARTIST_PROJECTION = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        fun processMusicCursor(cursor: android.database.Cursor): MusicCursorData {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val bitrateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            return MusicCursorData(
                id = cursor.getLong(idColumn),
                title = cursor.getString(titleColumn) ?: "Unknown Title",
                artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                album = cursor.getString(albumColumn) ?: "Unknown Album",
                duration = cursor.getLong(durationColumn),
                albumId = cursor.getLong(albumIdColumn),
                artistId = cursor.getLong(artistIdColumn),
                bitrate = cursor.getInt(bitrateColumn),
                filePath = cursor.getString(dataColumn) ?: ""
            )
        }
    }

    data class MusicCursorData(
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long,
        val albumId: Long,
        val artistId: Long,
        val bitrate: Int,
        val filePath: String
    )

    data class AlbumCursorData(
        val id: Long,
        val name: String,
        val artist: String,
        val songCount: Int
    )


    private fun processAlbumCursor(cursor: android.database.Cursor): AlbumCursorData {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
        val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

        return AlbumCursorData(
            id = cursor.getLong(idColumn),
            name = cursor.getString(albumColumn) ?: "Unknown Album",
            artist = cursor.getString(artistColumn) ?: "Unknown Artist",
            songCount = cursor.getInt(songCountColumn)
        )
    }

    private suspend fun queryMusic(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String? = "${MediaStore.Audio.Media.TITLE} ASC"
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MUSIC_PROJECTION,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val data = processMusicCursor(cursor)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    data.id
                )
                val albumArtUri = ContentUris.withAppendedId(
                    ALBUM_ART_URI,
                    data.albumId
                )

                mediaItems.add(
                    MediaItem(
                        id = data.id,
                        uri = contentUri,
                        title = data.title,
                        artist = data.artist,
                        album = data.album,
                        duration = data.duration,
                        albumArtUri = albumArtUri,
                        bitrate = data.bitrate,
                        filePath = data.filePath
                    )
                )
            }
        }
        mediaItems
    }


    fun getMusic(sortOrder: SortOrder): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { MusicPagingSource(context, sortOrder) } // 不再传递 this
        ).flow
    }

    suspend fun getAllMusicAsMedia3Items(): List<Media3MediaItem> {
        val selection = "${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf("0")
        val mediaItems = queryMusic(selection, selectionArgs, null)
        return mediaItems.map { it.toMedia3MediaItem() } // 使用扩展函数转换
    }

    suspend fun searchMusic(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")

        queryMusic(selection, selectionArgs)
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            ALBUM_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val data = processAlbumCursor(cursor)
                val albumArtUri = ContentUris.withAppendedId(
                    ALBUM_ART_URI,
                    data.id
                )

                albums.add(
                    Album(
                        id = data.id,
                        name = data.name,
                        artist = data.artist,
                        albumArtUri = albumArtUri,
                        songCount = data.songCount
                    )
                )
            }
        }
        albums
    }

    suspend fun getSongsByAlbum(albumId: Long): List<MediaItem> = withContext(Dispatchers.IO) {
        val selection =
            "${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf(albumId.toString(), "0")
        queryMusic(selection, selectionArgs)
    }

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()
        context.contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            ARTIST_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val songCountColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val songCount = cursor.getInt(songCountColumn)
                artists.add(Artist(id = id, name = artist, songCount = songCount))
            }
        }
        artists
    }

    suspend fun getSongsByArtist(artistId: Long): List<MediaItem> = withContext(Dispatchers.IO) {
        val selection =
            "${MediaStore.Audio.Media.ARTIST_ID} = ? AND ${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf(artistId.toString(), "0")
        queryMusic(selection, selectionArgs)
    }

    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, Folder>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media.DURATION} > ?",
            arrayOf("0"),
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataColumn) ?: continue
                val folderPath = filePath.substringBeforeLast("/")
                val folderName = folderPath.substringAfterLast("/")
                val folder = folderMap[folderPath] ?: Folder(
                    path = folderPath,
                    name = folderName,
                    songCount = 0
                )
                folderMap[folderPath] = folder.copy(songCount = folder.songCount + 1)
            }
        }
        folderMap.values.sortedBy { it.name }
    }

    suspend fun getSongsByFolder(folderPath: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val selection =
                "${MediaStore.Audio.Media.DATA} LIKE ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ? AND ${MediaStore.Audio.Media.DURATION} > ?"
            val selectionArgs = arrayOf("$folderPath/%", "$folderPath/%/%", "0")
            queryMusic(selection, selectionArgs)
        }


    /**
     * 根据一组 mediaId，一次性从 MediaStore 查询所有对应的歌曲信息。
     */
    suspend fun getSongsByIds(ids: List<Long>): List<MediaItem> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        // 构建 "IN (?, ?, ?, ...)" 查询语句
        val selection = "${MediaStore.Audio.Media._ID} IN (${ids.joinToString(",") { "?" }})"
        val selectionArgs = ids.map { it.toString() }.toTypedArray()

        // 复用我们已有的通用查询方法
        return queryMusic(selection, selectionArgs, null)
    }
}
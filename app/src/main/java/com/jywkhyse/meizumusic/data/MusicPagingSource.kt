// 文件: /app/src/main/java/com/jywkhyse/meizumusic/data/MusicPagingSource.kt
package com.jywkhyse.meizumusic.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jywkhyse.meizumusic.core.model.MediaItem

class MusicPagingSource(
    private val context: Context, // 依赖 Context 而不是 Repository
    private val sortOrder: SortOrder
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize
        Log.d("MusicApp", "PagingSource: Loading page $pageNumber, size: $pageSize")

        return try {
            // --- 分页查询逻辑现在位于 PagingSource 内部 ---
            val mediaItems = queryMusicPaged(pageNumber, pageSize, sortOrder)
            Log.d("MusicApp", "PagingSource: Query successful, returned ${mediaItems.size} items.")

            val nextKey = if (mediaItems.size < pageSize) null else pageNumber + 1

            LoadResult.Page(
                data = mediaItems,
                prevKey = if (pageNumber == 0) null else pageNumber - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            Log.e("MusicApp", "PagingSource: Error loading data.", e)
            LoadResult.Error(e)
        }
    }

    // 将原 Repository 中的 queryMusicPaged 方法逻辑移到此处
    private fun queryMusicPaged(page: Int, pageSize: Int, sortOrder: SortOrder): List<MediaItem> {
        val sortOrderString = when (sortOrder) {
            SortOrder.DEFAULT -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            SortOrder.A_TO_Z -> "${MediaStore.Audio.Media.TITLE} ASC"
            SortOrder.Z_TO_A -> "${MediaStore.Audio.Media.TITLE} DESC"
        }
        val offset = page * pageSize
        val selection = "${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf("0")

        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val mediaItems = mutableListOf<MediaItem>()

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val queryArgs = Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    selectionArgs
                )
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(sortOrderString.split(" ")[0])
                )
                putInt(
                    android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    if (sortOrderString.endsWith("ASC")) android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            context.contentResolver.query(
                queryUri,
                MusicRepository.MUSIC_PROJECTION,
                queryArgs,
                null
            )
        } else {
            val fullSortOrder = "$sortOrderString LIMIT $pageSize OFFSET $offset"
            context.contentResolver.query(
                queryUri,
                MusicRepository.MUSIC_PROJECTION,
                selection,
                selectionArgs,
                fullSortOrder
            )
        }

        cursor?.use { c ->
            while (c.moveToNext()) {
                val data =
                    MusicRepository.processMusicCursor(c) // 假设 processMusicCursor 是 public 或 internal
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, data.id)
                val albumArtUri =
                    ContentUris.withAppendedId(MusicRepository.ALBUM_ART_URI, data.albumId)

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
        return mediaItems
    }
}
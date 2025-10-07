// 文件: /app/src/main/java/com/jywkhyse/meizumusic/player/SharedPlayerViewModel.kt
package com.jywkhyse.meizumusic.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.jywkhyse.meizumusic.core.model.PlaylistForUi
import com.jywkhyse.meizumusic.data.MusicRepository
import com.jywkhyse.meizumusic.data.PlaylistRepository
import com.jywkhyse.meizumusic.database.UserDataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataRepository: UserDataRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() { // 我们仍然继承 ViewModel 来免费获得 viewModelScope


    private var mediaControllerFuture: ListenableFuture<MediaController>
    val mediaController: MediaController?
        get() = if (mediaControllerFuture.isDone) mediaControllerFuture.get() else null

    // --- 新增：用于存储待执行指令的变量 ---
    private var pendingCommand: (() -> Unit)? = null

    // --- 暴露给 UI 的状态 ---
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    sealed class PlayMode
    object RepeatOff : PlayMode()
    object RepeatAll : PlayMode()
    object RepeatOne : PlayMode()
    object Shuffle : PlayMode()

    private val _playMode = MutableStateFlow<PlayMode>(RepeatAll) // 默认列表循环
    val playMode = _playMode.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    private var progressUpdateJob: Job? = null

    private val MIN_PLAY_DURATION_TO_COUNT = 15_000L // 至少播放15秒才算一次有效播放
    private var currentSongStartTime: Long = 0
    private var currentSongMediaId: Long? = null

    val playlistsForUi: StateFlow<List<PlaylistForUi>> =
        playlistRepository.getPlaylistsForUi()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


    init {
        // ViewModel 创建时就初始化连接
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            setupController()
            // 初始化时，根据播放器当前状态更新播放模式
            updatePlayModeState()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupController() {
        mediaController?.let { controller ->
            controller.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // 歌曲切换时，记录上一首歌的播放数据
                    recordPreviousSongPlayback()

                    // 记录新歌曲的开始时间和ID
                    _currentMediaItem.value = mediaItem
                    currentSongStartTime = System.currentTimeMillis()
                    currentSongMediaId = mediaItem?.mediaId?.toLongOrNull()

                    updateFavoriteStatus(mediaItem?.mediaId)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                    }
                }

                // 当播放器模式改变时（例如通过通知栏），也更新我们的状态
                override fun onRepeatModeChanged(repeatMode: Int) {
                    updatePlayModeState()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    updatePlayModeState()
                }
            })
            // 初始化状态
            _currentMediaItem.value = controller.currentMediaItem
            _isPlaying.value = controller.isPlaying
            _duration.value = controller.duration
            updateFavoriteStatus(controller.currentMediaItem?.mediaId)
            if (controller.isPlaying) startProgressUpdate()

            // --- 关键修复：检查并执行待处理的指令 ---
            pendingCommand?.let { command ->
                Log.d("SharedPlayerViewModel", "Executing pending command.")
                command.invoke()
                pendingCommand = null // 执行后清空
            }
        }
    }

    private fun recordPreviousSongPlayback() {
        val playedDuration = System.currentTimeMillis() - currentSongStartTime
        val mediaId = currentSongMediaId

        // 如果播放时长超过阈值，则记录
        if (mediaId != null && playedDuration >= MIN_PLAY_DURATION_TO_COUNT) {
            viewModelScope.launch {
                userDataRepository.recordPlayback(mediaId, playedDuration)
            }
        }
    }

    // 从播放器状态更新 ViewModel 的 StateFlow
    private fun updatePlayModeState() {
        mediaController?.let {
            if (it.shuffleModeEnabled) {
                _playMode.value = Shuffle
            } else {
                _playMode.value = when (it.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatOne
                    Player.REPEAT_MODE_ALL -> RepeatAll
                    else -> RepeatOff
                }
            }
        }
    }

    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                _playbackPosition.value = mediaController?.currentPosition ?: 0L
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
    }

    private fun updateFavoriteStatus(mediaId: String?) {
        viewModelScope.launch {
            val id = mediaId?.toLongOrNull()
            if (id != null) {
                val userData = userDataRepository.getSongUserData(id).first()
                _isFavorite.value = userData?.isFavorite ?: false
            } else {
                _isFavorite.value = false
            }
        }
    }

    // --- 暴露给 UI 的操作 ---
    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekToNext() = mediaController?.seekToNextMediaItem()
    fun seekToPrevious() = mediaController?.seekToPreviousMediaItem()
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _playbackPosition.value = position // 立即更新UI，避免延迟
    }

    /**
     * 切换到下一个播放模式
     * 顺序：列表循环 -> 全部随机 -> 单曲循环 -> 顺序播放
     */
    fun cycleNextPlayMode() {
        mediaController?.let {
            when (playMode.value) {
                is RepeatAll -> { // 当前是列表循环，下一个是随机
                    it.shuffleModeEnabled = true
                }

                is Shuffle -> { // 当前是随机，下一个是单曲循环
                    it.shuffleModeEnabled = false
                    it.repeatMode = Player.REPEAT_MODE_ONE
                }

                is RepeatOne -> { // 当前是单曲循环，下一个是顺序播放
                    it.repeatMode = Player.REPEAT_MODE_OFF
                }

                is RepeatOff -> { // 当前是顺序播放，下一个是列表循环
                    it.repeatMode = Player.REPEAT_MODE_ALL
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val mediaId = currentMediaItem.value?.mediaId?.toLongOrNull()
            if (mediaId != null) {
                userDataRepository.toggleFavorite(mediaId)
                updateFavoriteStatus(mediaId.toString())
            }
        }
    }

    fun playMediaItems(shuffledList: List<MediaItem>) {
        mediaController?.setMediaItems(shuffledList)
        mediaController?.prepare()
        mediaController?.play()
    }

    /**
     * 播放一个全新的列表，并从指定位置开始。
     * @param mediaItems 要播放的 Media3MediaItem 列表。
     * @param startPosition 开始播放的索引。
     */
    fun playNewList(mediaItems: List<MediaItem>, startPosition: Int = 0) {
        // --- 这里是关键修改 ---
        // 我们不再预先获取 controller 变量。
        // 相反，我们在 lambda 内部直接访问 this.mediaController 属性。
        // 这确保了 lambda 在执行时，访问的是最新的、非 null 的 controller 实例。
        val command: () -> Unit = {
            this.mediaController?.let { controller -> // 在执行时才获取 controller
                if (mediaItems.isNotEmpty()) {
                    controller.setMediaItems(mediaItems, startPosition, 0L)
                    controller.prepare()
                    controller.play()
                    Log.d(
                        "SharedPlayerViewModel",
                        "Playing new list with ${mediaItems.size} songs, starting at index $startPosition"
                    )
                }
            }
        }

        if (mediaController != null) {
            // 如果控制器已就绪，立即执行
            Log.d("SharedPlayerViewModel", "Controller is ready, executing command immediately.")
            command.invoke()
        } else {
            // 如果控制器未就绪，将指令存起来
            Log.d("SharedPlayerViewModel", "Controller not ready, queuing command.")
            pendingCommand = command
        }
    }

    /**
     * 播放单首歌曲。这会清空现有播放列表。
     * @param mediaItem 要播放的单首歌曲。
     */
    fun playSingleSong(mediaItem: MediaItem) {
        // 调用上面的方法，只是列表里只有一首歌
        playNewList(listOf(mediaItem), 0)
    }

    fun playNext(mediaItem: MediaItem) {
        // 在当前歌曲的后面添加一项
        mediaController?.addMediaItem(
            (mediaController?.currentMediaItemIndex ?: 0) + 1,
            mediaItem
        )
    }

    fun getFavoriteSongs() = userDataRepository.getFavoriteSongs()
    fun getRecentlyPlayedSongs() = userDataRepository.getRecentlyPlayedSongs()

    // 触发随机播放，返回一个被打乱的播放列表
    fun getShuffledPlaylist(): Flow<List<MediaItem>> = flow {
        val shuffledList = musicRepository.getAllMusicAsMedia3Items().shuffled()
        emit(shuffledList)
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            userDataRepository.createPlaylist(name, description)
        }
    }

    fun createPlaylistAndAddSong(newPlaylistName: String, id: Long) {
        viewModelScope.launch {
            val playlistId = userDataRepository.createPlaylist(newPlaylistName)
            addSongToPlaylist(playlistId, id)
        }
    }


    fun addSongToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            userDataRepository.addSongToPlaylist(playlistId, mediaId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            userDataRepository.removeSongFromPlaylist(playlistId, mediaId)
        }
    }


    override fun onCleared() {
        super.onCleared()
        recordPreviousSongPlayback()
        MediaController.releaseFuture(mediaControllerFuture)
        stopProgressUpdate()
    }

    // 时间格式化工具
    @SuppressLint("DefaultLocale")
    fun formatDuration(millis: Long): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }


}
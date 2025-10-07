// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/PlayerActivity.kt
package com.jywkhyse.meizumusic.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.databinding.ActivityPlayerBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- 在 setContentView 之前调用 ---
        // 1. 让应用布局延伸到系统栏（状态栏和导航栏）后面
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. 将状态栏和导航栏的背景设置为透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. 设置状态栏图标和文字为浅色（白色）
        val insetsController = WindowCompat.getInsetsController(window, binding.root)
        insetsController.isAppearanceLightStatusBars = false

        // --- 新增：处理 Insets ---
        // 这段代码的作用是获取系统栏的高度，并将其作为 padding 应用到你的内容布局上
        // 这样，你的背景依然是全屏的，但你的按钮和文字不会被状态栏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContent) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 应用内边距
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            // 返回消耗掉的 insets
            WindowInsetsCompat.CONSUMED
        }

        setupListeners()
        observeViewModel()

    }

    @SuppressLint("UseKtx")
    private fun setupListeners() {

        binding.run {
            musicPrevButton.setOnClickListener { sharedViewModel.seekToPrevious() }
            musicPlayPauseButton.setOnClickListener { sharedViewModel.playPause() }
            musicNextButton.setOnClickListener { sharedViewModel.seekToNext() }
            musicFavoriteButton.setOnClickListener { sharedViewModel.toggleFavorite() }
            musicPlaybackMode.setOnClickListener { sharedViewModel.cycleNextPlayMode() }
            musicLyricButton.setOnClickListener {
                lrcView.visibility = if (lrcView.isInvisible) View.VISIBLE else View.INVISIBLE
                musicAlbumArt.visibility =
                    if (musicAlbumArt.isInvisible) View.VISIBLE else View.INVISIBLE
                musicLyricButton.setColorFilter(getColor(if (lrcView.isVisible) R.color.md_white else R.color.md_white_70))
            }
            musicButtonBack.setOnClickListener { finish() }
            musicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        binding.musicPosition.text =
                            sharedViewModel.formatDuration(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        sharedViewModel.seekTo(it.progress.toLong())
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            sharedViewModel.currentMediaItem.collectLatest { mediaItem ->
                binding.musicTitle.text = mediaItem?.mediaMetadata?.title ?: "未知歌曲"
                binding.musicArtist.text = mediaItem?.mediaMetadata?.artist ?: "未知艺术家"
                binding.musicAlbumArt.load(mediaItem?.mediaMetadata?.artworkUri) {
                    error(R.drawable.ic_music_note)
                    placeholder(R.drawable.ic_music_note)
                    allowHardware(false) // <-- 添加这个选项
                }
                // --- 这是新逻辑的核心 ---
                updateBackgroundAndPalette(mediaItem?.mediaMetadata?.artworkUri)
                loadLyrics(mediaItem) // 加载歌词
            }
        }
        lifecycleScope.launch {
            sharedViewModel.isPlaying.collectLatest { isPlaying ->
                binding.musicPlayPauseButton.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                )
            }
        }
        lifecycleScope.launch {
            sharedViewModel.playMode.collectLatest { playMode ->
                val iconRes = when (playMode) {
                    is SharedPlayerViewModel.RepeatAll -> R.drawable.ic_list_loop
                    is SharedPlayerViewModel.Shuffle -> R.drawable.ic_shuffle
                    is SharedPlayerViewModel.RepeatOne -> R.drawable.ic_single_repeat
                    is SharedPlayerViewModel.RepeatOff -> R.drawable.ic_play_seq
                }
                binding.musicPlaybackMode.setImageResource(iconRes)
            }
        }
        lifecycleScope.launch {
            sharedViewModel.playbackPosition.collectLatest { position ->
                binding.musicPosition.text = sharedViewModel.formatDuration(position)
                binding.musicSeekBar.progress = position.toInt()
                binding.lrcView.updateTime(position) // 同步歌词
            }
        }
        lifecycleScope.launch {
            sharedViewModel.duration.collectLatest { duration ->
                binding.musicDuration.text = sharedViewModel.formatDuration(duration)
                binding.musicSeekBar.max = duration.toInt()
            }
        }
        lifecycleScope.launch {
            sharedViewModel.isFavorite.collect { isFavorite ->
                binding.musicFavoriteButton.setImageResource(
                    if (isFavorite) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )
            }
        }
    }

    private fun updateBackgroundAndPalette(artworkUri: Uri?) {
        if (artworkUri == null) {
            // 设置默认背景
            binding.blurBackground.setImageResource(R.drawable.ic_music_note)
            return
        }

        // 使用 Coil 加载图片为 Bitmap
        val request = ImageRequest.Builder(this)
            .data(artworkUri)
            .allowHardware(false) // Blurry 和 Palette 需要软件位图
            .target { drawable ->
                val bitmap =
                    (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return@target

                // 1. 生成高斯模糊背景
                Blurry.with(this@PlayerActivity)
                    .radius(25)
                    .sampling(2)
                    .from(bitmap)
                    .into(binding.blurBackground)

                // 2. 使用 Palette 提取颜色并生成遮罩
                Palette.from(bitmap).generate { palette ->
                    palette?.let {
                        val dominantColor = it.getMutedColor(Color.TRANSPARENT)
                        val vibrantColor = it.getDarkMutedColor(Color.TRANSPARENT)

                        val gradient = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(vibrantColor, dominantColor)
                        )
                        binding.paletteMask.background = gradient
                    }
                }

                // 将原图设置给专辑封面
                binding.musicAlbumArt.setImageBitmap(bitmap)
            }
            .build()
        imageLoader.enqueue(request)
    }

    private fun loadLyrics(mediaItem: androidx.media3.common.MediaItem?) {
        // 从 MediaItem 的 extras 中获取我们之前存入的文件路径
        val filePath = mediaItem?.mediaMetadata?.extras?.getString("file_path")

        if (filePath.isNullOrEmpty()) {
            binding.lrcView.loadLrc("[00:00.00]暂无歌词 (无法获取文件路径)")
            return
        }

        // 根据文件路径推断出 .lrc 文件路径
        // 例如, "/path/to/song.mp3" -> "/path/to/song.lrc"
        val lrcPath = filePath.substringBeforeLast(".") + ".lrc"
        val lrcFile = File(lrcPath)

        if (lrcFile.exists() && lrcFile.canRead()) {
            // 如果 lrc 文件存在且可读，就加载它
            Log.d("PlayerActivity", "找到并加载歌词: $lrcPath")
            binding.lrcView.loadLrc(lrcFile)
        } else {
            // 否则，显示“暂无歌词”
            Log.d("PlayerActivity", "未找到歌词文件: $lrcPath")
            binding.lrcView.loadLrc("[00:00.00]暂无歌词")
        }
    }
}
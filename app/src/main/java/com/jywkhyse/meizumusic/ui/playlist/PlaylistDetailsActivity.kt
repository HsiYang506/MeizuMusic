// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/playlist/PlaylistDetailsActivity.kt
package com.jywkhyse.meizumusic.ui.playlist

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.databinding.ActivityPlaylistDetailsBinding
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.GenericAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private val viewModel: PlaylistDetailsViewModel by viewModels()

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    private lateinit var songAdapter: GenericAdapter<MediaItem, ItemMusicBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBar()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        observeViewModel()

        binding.fabPlayAll.setOnClickListener {
            val currentList = songAdapter.items
            if (currentList.isNotEmpty()) {
                val media3Items = currentList.map { it.toMedia3MediaItem() }
                sharedViewModel.playNewList(media3Items, 0)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_playlist -> {
                showRenameDialog(viewModel.playlistId, viewModel.playlistName.value)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupStatusBar() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
    }


    private fun setupRecyclerView() {
        songAdapter = GenericAdapter(
            mutableListOf(),
            ItemMusicBinding::inflate,
        ) { holder, binding, pos, item ->
            binding.run {
                qualityTag.isVisible = false
                musicTitle.text = item.title
                musicArtist.text = "${item.artist} ${item.album}"
                albumArt.load(item.albumArtUri) {
                    error(R.drawable.ic_album)
                    placeholder(R.drawable.ic_album)
                }
                root.setOnClickListener {
                    val currentList = songAdapter.items
                    val media3Items = currentList.map { it.toMedia3MediaItem() }
                    sharedViewModel.playNewList(media3Items, pos)
                }
                optionsButton.setImageResource(R.drawable.ic_remove_circle)
                optionsButton.setColorFilter(R.color.md_red_500)
                optionsButton.setOnClickListener {
                    viewModel.removeSongFromPlaylist(item)
                }
            }


        }
        binding.recyclerViewSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(this@PlaylistDetailsActivity)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playlistName.collect { name ->
                binding.collapsingToolbar.title = name
            }
        }

        lifecycleScope.launch {
            viewModel.songsInPlaylist.collect { songs ->
                songAdapter.updateItems(songs)
                // 可以在这里用第一首歌的封面作为背景
                binding.headerImage.load(songs.firstOrNull()?.albumArtUri)
            }
        }
    }


    @SuppressLint("CheckResult")
    private fun showRenameDialog(playlistId: Long, playlistName: String) {
        MaterialDialog(this).show {
            title(text = "重命名歌单")
            input(prefill = playlistName, hint = "请输入新名称") { _, text ->
                // 调用 ViewModel 更新数据库
                viewModel.renamePlaylist(playlistId, text.toString())
            }
            positiveButton(text = "确定")
            negativeButton(text = "取消")
        }
    }
}
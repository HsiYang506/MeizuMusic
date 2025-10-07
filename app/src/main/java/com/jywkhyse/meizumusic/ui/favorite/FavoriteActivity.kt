// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchActivity.kt
package com.jywkhyse.meizumusic.ui.favorite


import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.databinding.ActivityListBinding
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.GenericAdapter
import com.jywkhyse.meizumusic.ui.common.SongOptionsMenuSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FavoriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var mediaItemAdapter: GenericAdapter<MediaItem, ItemMusicBinding>

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBar()
        setupActionBar()
        setupRecyclerView()
        observeViewModel()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        mediaItemAdapter =
            GenericAdapter(
                mutableListOf(),
                ItemMusicBinding::inflate
            ) { holder, binding, pos, item ->
                binding.run {
                    qualityTag.isVisible = false
                    musicTitle.text = item.title
                    musicArtist.text = item.artist
                    albumArt.load(item.albumArtUri) {
                        error(R.drawable.ic_album)
                        placeholder(R.drawable.ic_album)
                    }
                    root.setOnClickListener {
                        sharedViewModel.playSingleSong(item.toMedia3MediaItem())
                    }
                    setupOptionButton(optionsButton, pos, item)
                }
            }
        binding.recyclerView.apply {
            adapter = mediaItemAdapter
            layoutManager = LinearLayoutManager(this@FavoriteActivity)
        }
    }

    private fun setupOptionButton(
        imageButton: ImageButton,
        position: Int,
        song: MediaItem
    ) {
        imageButton.setOnClickListener {
            val position = position
            if (position != RecyclerView.NO_POSITION) {
                // ★★★ 只需一行代码，调用 BottomSheet ★★★
                SongOptionsMenuSheet.newInstance(song.id)
                    .show(supportFragmentManager, SongOptionsMenuSheet.TAG)
            }
        }
    }

    private fun setupStatusBar() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = intent.getStringExtra("name") ?: "我喜欢"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.recentlyPlayedSongs.collect { songs ->
                Log.d("RecentlyActivity", "observeViewModel: ,${songs.map { it.title }}")
                if (songs.isNotEmpty()) {
                    binding.emptyText.isVisible = false
                    mediaItemAdapter.updateItems(songs)
                } else {
                    binding.emptyText.isVisible = true
                    binding.emptyText.text = "暂无喜欢的歌曲"
                }
            }
        }
    }

}
// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchActivity.kt
package com.jywkhyse.meizumusic.ui.album


import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.databinding.ActivityAlbumDetailsBinding
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.GenericAdapter
import com.jywkhyse.meizumusic.ui.common.SongOptionsMenuSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private lateinit var mediaItemAdapter: GenericAdapter<MediaItem, ItemMusicBinding>

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
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
                    albumArt.setImageResource(com.jywkhyse.meizumusic.R.drawable.ic_album)
                    root.setOnClickListener {
                        val currentList = mediaItemAdapter.items
                        val media3Items = currentList.map { it.toMedia3MediaItem() }
                        sharedViewModel.playNewList(media3Items, pos)
                    }
                    setupOptionButton(optionsButton, pos, item)
                }
            }
        binding.recyclerView.apply {
            adapter = mediaItemAdapter
            layoutManager = LinearLayoutManager(this@AlbumDetailsActivity)
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
        supportActionBar?.title = intent.getStringExtra("name") ?: "专辑详情"
        binding.albumArt.load(intent.getStringExtra("albumArtUri")?.toUri()) {
            error(com.jywkhyse.meizumusic.R.drawable.ic_album)
            placeholder(com.jywkhyse.meizumusic.R.drawable.ic_album)
        }
        binding.textTitle.text = intent.getStringExtra("artist") ?: "未知艺术家"
        binding.textSummary.text = "${intent.getIntExtra("songCount", -1)}首"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.onAlbumIdChanged(intent.getLongExtra("albumId", -1))

        lifecycleScope.launch {
            viewModel.mediaItems.collect { results ->
                mediaItemAdapter.updateItems(results)
                binding.emptyText.text =
                    if (viewModel.isLoading.value || results.isNotEmpty()) "" else "无结果"
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                if (isLoading) {
                    binding.emptyText.isVisible = false
                } else {
                    binding.emptyText.isVisible = mediaItemAdapter.itemCount == 0
                }
            }
        }
    }

}
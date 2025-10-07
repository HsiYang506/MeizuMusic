// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchActivity.kt
package com.jywkhyse.meizumusic.ui.folder


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
class FolderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private val viewModel: FolderDetailsViewModel by viewModels()
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
                        val currentList = mediaItemAdapter.items
                        val media3Items = currentList.map { it.toMedia3MediaItem() }
                        sharedViewModel.playNewList(media3Items, pos)
                    }
                    setupOptionButton(optionsButton, pos, item)
                }
            }
        binding.recyclerView.apply {
            adapter = mediaItemAdapter
            layoutManager = LinearLayoutManager(this@FolderDetailsActivity)
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
        supportActionBar?.title = intent.getStringExtra("name") ?: "文件夹详情"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.onFolderChanged(intent.getStringExtra("path") ?: "")

        lifecycleScope.launch {
            viewModel.mediaItems.collect { results ->
                Log.d("AlbumDetailsActivity", "observeViewModel: ,${results.map { it.title }}")
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
// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchActivity.kt
package com.jywkhyse.meizumusic.ui.playlist


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.PlaylistForUi
import com.jywkhyse.meizumusic.databinding.ActivityListBinding
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.helper.DialogHelper
import com.jywkhyse.meizumusic.helper.PopMenuHelper
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.GenericAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var mediaItemAdapter: GenericAdapter<PlaylistForUi, ItemMusicBinding>

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    private lateinit var toolbar: Toolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.toolbar
        setupStatusBar()
        setupActionBar()
        setupRecyclerView()
        observeViewModel()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.playlist, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_playlist -> {
                showCreatePlaylistDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        mediaItemAdapter =
            GenericAdapter(
                mutableListOf(),
                ItemMusicBinding::inflate
            ) { holder, binding, pos, item ->
                binding.run {
                    qualityTag.isVisible = false
                    musicTitle.text = item.name
                    musicArtist.text = "${item.songCount} 首"
                    albumArt.load(item.coverArtUri) {
                        error(R.drawable.ic_album)
                        placeholder(R.drawable.ic_album)
                    }
                    root.setOnClickListener {
                        val intent = Intent(
                            this@PlaylistActivity,
                            PlaylistDetailsActivity::class.java
                        ).apply {
                            // ★★★ 传递 playlistId ★★★
                            putExtra("playlist_id", item.playlistId)
                            putExtra("anme", item.name)
                        }
                        startActivity(intent)
                    }
                    optionsButton.setOnClickListener {
                        PopMenuHelper.showOptionsMenu(
                            optionsButton,
                            PopMenuHelper.PlayList
                        ) { pos, txt ->
                            when (pos) {
                                0 -> showRenameDialog(item)
                                1 -> showDeleteConfirmDialog(item)
                            }
                        }
                    }
                }
            }
        binding.recyclerView.apply {
            adapter = mediaItemAdapter
            layoutManager = LinearLayoutManager(this@PlaylistActivity)
        }
    }

    private fun setupStatusBar() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
    }


    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra("name") ?: "我的歌单"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playlistsForUi.collect { forUis ->
                Timber.d("observeViewModel: ,${forUis.map { it.name }}")
                if (forUis.isNotEmpty()) {
                    binding.emptyText.isVisible = false
                    mediaItemAdapter.updateItems(forUis)
                } else {
                    binding.emptyText.isVisible = true
                    binding.emptyText.text = "暂无喜欢的歌曲"
                }
            }
        }
    }

    private fun showCreatePlaylistDialog() {
        DialogHelper.showCreatePlaylistDialog(this) {
            viewModel.createPlaylist(it)
            Toast.makeText(
                this@PlaylistActivity,
                "已创建歌单: $it",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("CheckResult")
    private fun showRenameDialog(playlist: PlaylistForUi) {
        MaterialDialog(this).show {
            title(text = "重命名歌单")
            input(prefill = playlist.name, hint = "请输入新名称") { _, text ->
                // 调用 ViewModel 更新数据库
                viewModel.renamePlaylist(playlist.playlistId, text.toString())
            }
            positiveButton(text = "确定")
            negativeButton(text = "取消")
        }
    }

    private fun showDeleteConfirmDialog(playlist: PlaylistForUi) {
        MaterialDialog(this).show {
            title(text = "删除歌单")
            message(text = "确定要删除歌单 '${playlist.name}' 吗？此操作无法撤销。")
            positiveButton(text = SpannableString("删除").apply {
                setSpan(
                    ForegroundColorSpan(Color.RED), // 红色
                    0, // 开始位置
                    "删除".length, // 结束位置（整个文本）
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }) {
                // 调用 ViewModel 更新数据库
                viewModel.deletePlaylist(playlist.playlistId)
            }
            negativeButton(text = "取消")
        }
    }

}
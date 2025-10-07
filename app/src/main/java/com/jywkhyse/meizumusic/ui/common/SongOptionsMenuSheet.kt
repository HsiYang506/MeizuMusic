// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/common/SongOptionsMenuSheet.kt
package com.jywkhyse.meizumusic.ui.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.core.model.PlaylistForUi
import com.jywkhyse.meizumusic.data.MusicRepository
import com.jywkhyse.meizumusic.databinding.SheetSongOptionsBinding
import com.jywkhyse.meizumusic.helper.DialogHelper.showCreatePlaylistDialog
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.playlist.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SongOptionsMenuSheet : BottomSheetDialogFragment() {

    private var _binding: SheetSongOptionsBinding? = null
    private val binding get() = _binding!!

    // 直接注入所有需要的 ViewModel
    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    @Inject
    lateinit var musicRepository: MusicRepository // 用于根据ID获取歌曲详情

    private val playlistsViewModel: PlaylistViewModel by viewModels()

    private var song: MediaItem? = null

    private var currentPlaylists: List<PlaylistForUi> = emptyList() // 创建一个属性来持有最新的列表


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val songId = requireArguments().getLong(ARG_SONG_ID)

        // 根据 ID 获取完整的歌曲信息
        viewLifecycleOwner.lifecycleScope.launch {
            song = musicRepository.getSongsByIds(listOf(songId)).firstOrNull()
            song?.let { setupUI(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // 使用 collect 来异步监听数据流
            sharedViewModel.playlistsForUi.collect { playlists ->
                // 每当数据库中的歌单数据发生变化，这里的代码就会被执行
                // 第一次会收到 initialValue (空列表)，紧接着在数据库查询完成后，会收到真实的列表
                Timber.d("Playlist has been updated. New list size: ${playlists.size}")

                // 将最新的列表保存在我们的属性中
                currentPlaylists = playlists
            }
        }
    }

    private fun setupUI(song: MediaItem) {
        // 设置头部信息
        binding.albumArt.load(song.albumArtUri)
        binding.songTitle.text = song.title

        // 设置菜单项文本和图标
        binding.optionAddToPlaylist.textOption.apply {
            text = "添加到歌单..."
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_playlist_add, 0, 0, 0)
            setOnClickListener { showAddToPlaylistDialog(song) }
        }
        binding.optionPlayNext.textOption.apply {
            text = "下一首播放"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_next_play, 0, 0, 0)
            setOnClickListener {
                sharedViewModel.playNext(song.toMedia3MediaItem())
                Toast.makeText(context, "'${song.title}' 将在下一首播放", Toast.LENGTH_SHORT).show()
                dismiss() // 关闭 BottomSheet
            }
        }
        binding.optionShare.textOption.apply {
            text = "分享"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_share, 0, 0, 0)
            setOnClickListener {
                dismiss() // 关闭 BottomSheet
            }
        }
        binding.optionInfo.textOption.apply {
            text = "歌曲信息"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_info, 0, 0, 0)
            setOnClickListener {
                dismiss() // 关闭 BottomSheet
            }
        }
        binding.optionDelete.textOption.apply {
            text = "永久删除"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete_forever, 0, 0, 0)
            setOnClickListener {
                dismiss() // 关闭 BottomSheet
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun showAddToPlaylistDialog(song: MediaItem) {
        val playlistNames = currentPlaylists.map { it.name }

        MaterialDialog(requireContext()).show {
            title(text = "添加到歌单")
            listItems(items = playlistNames) { _, index, _ ->
                val selectedPlaylist = currentPlaylists[index]
                playlistsViewModel.addSongToPlaylist(selectedPlaylist.playlistId, song.id)
                Toast.makeText(
                    requireContext(),
                    "已将 '${song.title}' 添加到 '${selectedPlaylist.name}'",
                    Toast.LENGTH_SHORT
                ).show()
                this@SongOptionsMenuSheet.dismiss() // 关闭 BottomSheet
                dismiss()
            }
            neutralButton(text = "新建歌单") {
                showCreatePlaylistDialog(requireContext()) { newPlaylistName ->
                    this@SongOptionsMenuSheet.dismiss() // 关闭 BottomSheet
                    dismiss()
                    // 在创建成功后，再次调用此方法，并传入新创建的歌单ID
                    sharedViewModel.createPlaylistAndAddSong(newPlaylistName, song.id)
                    Toast.makeText(
                        requireContext(),
                        "已创建 '$newPlaylistName' 并添加歌曲",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            negativeButton(text = "取消")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SongOptionsMenuSheet"
        private const val ARG_SONG_ID = "song_id"

        // 使用此方法创建实例，以安全地传递参数
        fun newInstance(songId: Long): SongOptionsMenuSheet {
            return SongOptionsMenuSheet().apply {
                arguments = bundleOf(ARG_SONG_ID to songId)
            }
        }
    }
}
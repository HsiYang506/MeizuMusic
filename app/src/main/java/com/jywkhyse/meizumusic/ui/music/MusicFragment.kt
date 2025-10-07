// file: /app/src/main/java/com/jywkhyse/meizumusic/ui/MusicListFragment.kt
package com.jywkhyse.meizumusic.ui.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.data.SortOrder
import com.jywkhyse.meizumusic.databinding.FragmentMusicListBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import com.jywkhyse.meizumusic.ui.PlayerActivity
import com.jywkhyse.meizumusic.ui.favorite.FavoriteActivity
import com.jywkhyse.meizumusic.ui.playlist.PlaylistActivity
import com.jywkhyse.meizumusic.ui.recently.RecentlyActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class MusicFragment : Fragment() {

    private var _binding: FragmentMusicListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by viewModels()
    private lateinit var musicAdapter: MusicPagingAdapter

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel

    private var isGridView = false // 追踪当前视图模式


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Timber.d("权限已授予，开始加载音乐。")
                loadMusic()
            } else {
                Timber.w("用户拒绝了权限请求。")
                Toast.makeText(requireContext(), "需要权限才能读取音乐文件。", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        checkPermissionsAndLoadMusic()
        setupLoadStateListener() // 新增：设置状态监听器
        observeSharedViewModel() // 监听共享 ViewModel
        setupMiniPlayerControls() // 设置迷你播放器按钮点击事件
        binding.sortButton.setOnClickListener {
            // 这里可以弹出一个对话框让用户选择，为简化先实现循环切换
            val nextOrder = when (viewModel.sortOrder.value) {
                SortOrder.DEFAULT -> SortOrder.A_TO_Z
                SortOrder.A_TO_Z -> SortOrder.Z_TO_A
                SortOrder.Z_TO_A -> SortOrder.DEFAULT
            }
            viewModel.setSortOrder(nextOrder)
            binding.sortButton.setImageResource(
                when (nextOrder) {
                    SortOrder.DEFAULT -> R.drawable.ic_sort
                    SortOrder.A_TO_Z -> R.drawable.ic_sort_az
                    SortOrder.Z_TO_A -> R.drawable.ic_sort_za
                }
            )
//            Toast.makeText(requireContext(), "排序方式: ${nextOrder.name}", Toast.LENGTH_SHORT).show()
        }
        binding.favoriteContainer?.setOnClickListener {
            startActivity(Intent(requireContext(), FavoriteActivity::class.java))
        }
        binding.recentlyContainer?.setOnClickListener {
            startActivity(Intent(requireContext(), RecentlyActivity::class.java))
        }
        binding.playlistContainer?.setOnClickListener {
            startActivity(Intent(requireContext(), PlaylistActivity::class.java))
        }
        binding.shufflePlayButton.setOnClickListener {
            playShuffledList()
        }
        binding.viewModeButton.setOnClickListener {
            updateRecyclerViewLayoutManager()
        }
        binding.miniPlayerContainer.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)

            // Temporarily set the layer type to SOFTWARE for the transition

            val p1 = Pair(binding.miniPlayerContainer as View, "mini_player")
            val p2 = Pair(binding.miniAlbumArt as View, "album_art")

            val options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), p1, p2)
            startActivity(intent, options.toBundle())
        }
        setupObserver()
    }

    private fun setupObserver() {
        lifecycleScope.launch {
            sharedViewModel.getFavoriteSongs().collectLatest {
                _binding?.tvFavoriteCount?.text = "${it.size}首"
            }
        }
        lifecycleScope.launch {
            sharedViewModel.getRecentlyPlayedSongs().collectLatest {
                _binding?.tvRecentPlayCount?.text = "${it.size}首"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeSharedViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.currentMediaItem.collectLatest { mediaItem ->
                if (mediaItem == null) {
                    binding.miniPlayerContainer.isVisible = false
                } else {
                    binding.miniPlayerContainer.isVisible = true
                    binding.miniMusicTitle.text = mediaItem.mediaMetadata.title
                    binding.miniMusicArtist.text = mediaItem.mediaMetadata.artist
                    binding.miniAlbumArt.load(mediaItem.mediaMetadata.artworkUri) {
                        error(R.drawable.ic_music_note)
                        allowHardware(false) // <-- 添加这个选项
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.isPlaying.collectLatest { isPlaying ->
                binding.miniPlayPauseButton.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.isFavorite.collectLatest {
                binding.miniFavoriteButton.setImageResource(
                    if (it) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                )
                if (it) binding.miniFavoriteButton.setColorFilter(
                    requireContext().getColor(
                        R.color.md_red_500
                    )
                ) else binding.miniFavoriteButton.clearColorFilter()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.playbackPosition.collectLatest {
                binding.miniProgress.progress = it.toInt()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.duration.collectLatest {
                binding.miniProgress.max = it.toInt()
            }
        }
    }

    private fun updateRecyclerViewLayoutManager() {
        isGridView = !isGridView
        if (isGridView) {
            // 切换到网格布局
            binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2) // 3列网格
            binding.viewModeButton.setImageResource(R.drawable.ic_list) // 假设你有 ic_list.xml
            musicAdapter.setViewType(MusicPagingAdapter.VIEW_TYPE_GRID)
        } else {
            // 切换回列表布局
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.viewModeButton.setImageResource(R.drawable.ic_grid_view)
            musicAdapter.setViewType(MusicPagingAdapter.VIEW_TYPE_LIST)
        }
        // 通知 Adapter 重绘所有可见项，以应用新的 ViewHolder
        musicAdapter.notifyItemRangeChanged(0, musicAdapter.itemCount)
    }

    private fun playShuffledList() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.getShuffledPlaylist().collect { shuffledList ->
                if (shuffledList.isNotEmpty()) {
                    sharedViewModel.playMediaItems(shuffledList)
                    Toast.makeText(requireContext(), "已开始随机播放", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "没有找到可播放的歌曲", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun setupMiniPlayerControls() {
        binding.miniPlayPauseButton.setOnClickListener { sharedViewModel.playPause() }
        binding.miniNextButton.setOnClickListener { sharedViewModel.seekToNext() }
        binding.miniFavoriteButton.setOnClickListener { sharedViewModel.toggleFavorite() }
    }

    private fun playSong(position: Int, item: MediaItem) {
        val controller = sharedViewModel.mediaController ?: return


        // 1. 从 Adapter 获取当前已加载的整个列表
        val currentList = musicAdapter.snapshot().items
        if (currentList.isEmpty()) {
            Timber.w("Adapter list is empty, cannot play.")
            return
        }

        // 2. 将我们的数据模型 List 转换为 Media3 的 MediaItem List
        val media3Items = currentList.map(MediaItem::toMedia3MediaItem)

        // 3. 将整个列表和起始位置发送给播放器
        //    setMediaItems 会清空旧列表，并用新列表替换
        //    第二个参数 position 告诉播放器从哪首歌开始播放
        controller.setMediaItems(media3Items, position, 0L)
        controller.prepare()
        controller.play()

        Timber.d("Playing playlist with ${media3Items.size} songs, starting at index $position (${item.title})")
    }


    private fun setupLoadStateListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            musicAdapter.loadStateFlow.collectLatest { loadStates ->
                Timber.d("setupLoadStateListener: $loadStates")
                // Mediatod.REFRESH 是初次加载或下拉刷新的状态
                val refreshState = loadStates.refresh

                // 根据状态控制 ProgressBar 的可见性
                binding.progressBar.visibility =
                    if (refreshState is LoadState.Loading) View.VISIBLE else View.GONE

                // 根据状态控制 SwipeRefreshLayout 的刷新动画
                binding.swipeRefreshLayout.isRefreshing = refreshState is LoadState.Loading

                // 初次加载或刷新成功后
                if (refreshState is LoadState.NotLoading) {
                    // 并且 item 数量为 0，说明是空数据
                    if (musicAdapter.itemCount == 0) {
                        binding.emptyText.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyText.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }

                // 处理错误状态
                if (refreshState is LoadState.Error) {
                    // 可以显示错误信息
                    binding.emptyText.text = "加载失败: ${refreshState.error.localizedMessage}"
                    binding.emptyText.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    Timber.e(refreshState.error, "Paging LoadState Error")
                }
            }
        }
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicPagingAdapter(childFragmentManager) { position, mediaItem ->
            // 点击列表项时的操作
            playSong(position, mediaItem) // 调用新的 playSong 方法
        }
        // 初始布局为列表
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = musicAdapter
        binding.swipeRefreshLayout.setOnRefreshListener {
            musicAdapter.refresh()
            binding.swipeRefreshLayout.isRefreshing = false // La UI de carga de Paging se encargará
        }
    }

    private fun checkPermissionsAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.d("权限已经被授予，直接加载音乐。")
                loadMusic()
            }

            else -> {
                Timber.d("没有权限，正在请求权限...")
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadMusic() {
        Timber.d("loadMusic 方法被调用，开始收集 ViewModel 的数据流。")
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.musicList.collectLatest { pagingData ->
                Timber.d("接收到新的 PagingData，提交给 Adapter。")
                musicAdapter.submitData(pagingData)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
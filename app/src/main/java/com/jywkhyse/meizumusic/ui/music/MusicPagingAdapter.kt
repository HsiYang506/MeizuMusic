// file: /app/src/main/java/com/jywkhyse/meizumusic/ui/MusicPagingAdapter.kt
package com.jywkhyse.meizumusic.ui.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.databinding.ItemMusicGridBinding
import com.jywkhyse.meizumusic.ui.common.SongOptionsMenuSheet

class MusicPagingAdapter(
    private val fragmentManager: FragmentManager, // 传入 FragmentManager
    private val onItemClick: (Int, MediaItem) -> Unit
) : PagingDataAdapter<MediaItem, RecyclerView.ViewHolder>(MUSIC_COMPARATOR) {

    // 当前视图类型，默认为列表
    private var currentViewType = VIEW_TYPE_LIST

    // 供外部调用的方法，用于切换视图类型
    fun setViewType(viewType: Int) {
        currentViewType = viewType
    }

    // 根据位置返回当前应有的视图类型
    override fun getItemViewType(position: Int): Int {
        return currentViewType
    }


    // 根据 viewType 创建不同的 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GRID -> {
                val binding =
                    ItemMusicGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MusicGridViewHolder(binding)
            }

            else -> { // 默认为 List
                val binding =
                    ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MusicListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let {
            when (holder) {
                is MusicListViewHolder -> holder.bind(it)
                is MusicGridViewHolder -> holder.bind(it)
            }
        }
    }

    // --- 列表视图的 ViewHolder ---
    inner class MusicListViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { item ->
                        onItemClick(position, item) // 传递 position 和 item
                    }
                }
            }
        }

        fun bind(item: MediaItem) {
            binding.musicTitle.text = item.title
            binding.musicArtist.text = item.artist
            binding.albumArt.load(item.albumArtUri) {
                error(R.drawable.ic_music_note)
                placeholder(R.drawable.ic_music_note)
            }
            setupOptionButton(binding.optionsButton, bindingAdapterPosition, item)
            // 在 onBindViewHolder 中
            val bitrate = item.bitrate
            when {
                bitrate >= 320000 -> { // 大于等于 320kbps 定义为 SQ
                    binding.qualityTag.visibility = View.VISIBLE
                    binding.qualityTag.text = "SQ"
                    binding.qualityTag.setTextColor(itemView.context.getColor(R.color.md_red_500))
                    binding.qualityTag.setBackgroundColor(itemView.context.getColor(R.color.md_red_50))
                }

                bitrate >= 192000 -> { // 大于等于 192kbps 定义为 HQ
                    binding.qualityTag.visibility = View.VISIBLE
                    binding.qualityTag.text = "HQ"
                    binding.qualityTag.setTextColor(itemView.context.getColor(R.color.md_blue_500))
                    binding.qualityTag.setBackgroundColor(itemView.context.getColor(R.color.md_blue_50))
                }

                else -> {
                    binding.qualityTag.visibility = View.GONE
                }
            }
        }
    }

    // --- 网格视图的 ViewHolder ---
    inner class MusicGridViewHolder(private val binding: ItemMusicGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { item ->
                        onItemClick(position, item) // 传递 position 和 item
                    }
                }
            }
        }

        fun bind(item: MediaItem) {
            binding.musicTitle.text = item.title
            binding.musicArtist.text = item.artist
            binding.albumArt.load(item.albumArtUri) {
                error(R.drawable.ic_music_note)
                placeholder(R.drawable.ic_music_note)
            }
            setupOptionButton(binding.optionsButton, bindingAdapterPosition, item)
            // 在 onBindViewHolder 中
            val bitrate = item.bitrate
            when {
                bitrate >= 320000 -> { // 大于等于 320kbps 定义为 SQ
                    binding.qualityTag.visibility = View.VISIBLE
                    binding.qualityTag.text = "SQ"
                    binding.qualityTag.setTextColor(itemView.context.getColor(R.color.md_red_500))
                    binding.qualityTag.setBackgroundColor(itemView.context.getColor(R.color.md_red_50))
                }

                bitrate >= 192000 -> { // 大于等于 192kbps 定义为 HQ
                    binding.qualityTag.visibility = View.VISIBLE
                    binding.qualityTag.text = "HQ"
                    binding.qualityTag.setTextColor(itemView.context.getColor(R.color.md_blue_500))
                    binding.qualityTag.setBackgroundColor(itemView.context.getColor(R.color.md_blue_50))
                }

                else -> {
                    binding.qualityTag.visibility = View.GONE
                }
            }
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
                    .show(fragmentManager, SongOptionsMenuSheet.TAG)
            }
        }
    }


    companion object {
        private val MUSIC_COMPARATOR = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem == newItem
        }
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1

    }
}
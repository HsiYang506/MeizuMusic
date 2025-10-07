package com.jywkhyse.meizumusic.ui.album

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.Album
import com.jywkhyse.meizumusic.databinding.ItemMusicGridBinding

@SuppressLint("NotifyDataSetChanged")
class AlbumAdapter(private val onItemClick: (position: Int, item: Album) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataList = emptyList<Album>()

    fun submitList(results: List<Album>) {
        dataList = results
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: ItemMusicGridBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(holder: RecyclerView.ViewHolder, item: Album) {
            with(holder.itemView) {
                // Set title and artist with highlighting if keyword is present
                binding.musicTitle.text = item.name
                binding.musicArtist.text = "${item.songCount}首"

                // Load album art
                binding.albumArt.load(item.albumArtUri) {
                    error(R.drawable.ic_music_note)
                    placeholder(R.drawable.ic_music_note)
                }

                // Hide quality tag
                binding.qualityTag.isVisible = false
                binding.optionsButton.isVisible = false

                // Set click listener
                setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(position, item)
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val binding =
            ItemMusicGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        (holder as? ViewHolder)?.run {
            this.bind(holder, dataList[holder.bindingAdapterPosition])
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


}

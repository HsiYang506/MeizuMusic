package com.jywkhyse.meizumusic.ui.artist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.Artist
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding

@SuppressLint("NotifyDataSetChanged")
class ArtistAdapter(
    private val onItemClick: (position: Int, item: Artist) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataList = emptyList<Artist>()

    fun submitList(results: List<Artist>) {
        dataList = results
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(holder: RecyclerView.ViewHolder, item: Artist) {
            with(holder.itemView) {
                // Set title and artist with highlighting if keyword is present
                binding.musicTitle.text = item.name
                binding.musicArtist.text = "${item.songCount}首"

                binding.albumArt.isInvisible = true
                binding.iconView.isVisible = true
                binding.iconView.setImageResource(R.drawable.ic_artist)

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
            ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

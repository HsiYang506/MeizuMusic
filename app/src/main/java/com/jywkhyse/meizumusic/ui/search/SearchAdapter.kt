package com.jywkhyse.meizumusic.ui.search

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.core.model.MediaItem
import com.jywkhyse.meizumusic.databinding.ItemMusicBinding
import com.jywkhyse.meizumusic.ui.common.SongOptionsMenuSheet
import java.util.regex.Pattern

@SuppressLint("NotifyDataSetChanged")
class SearchAdapter(
    private val fragmentManager: FragmentManager, // 传入 FragmentManager
    private val onItemClick: (position: Int, item: MediaItem) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataList = emptyList<MediaItem>()
    private var keyword = ""

    fun submitList(results: List<MediaItem>) {
        dataList = results
        notifyDataSetChanged()
    }

    fun submitList(keyword: String, results2: List<MediaItem>) {
        this.keyword = keyword
        dataList = results2
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(holder: RecyclerView.ViewHolder, item: MediaItem, keyword: String = "") {
            with(holder.itemView) {
                // Set title and artist with highlighting if keyword is present
                binding.musicTitle.text = if (keyword.isNotBlank()) {
                    highlightText(item.title, keyword)
                } else {
                    item.title
                }

                binding.musicArtist.text = if (keyword.isNotBlank()) {
                    highlightText("${item.artist}\t${item.album}", keyword)
                } else {
                    item.artist
                }

                // Load album art
                binding.albumArt.load(item.albumArtUri) {
                    error(R.drawable.ic_music_note)
                    placeholder(R.drawable.ic_music_note)
                }

                // Hide quality tag
                binding.qualityTag.isVisible = false

                // Set click listener
                setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(position, item)
                    }
                }

                setupOptionButton(binding.optionsButton, bindingAdapterPosition, item)
            }
        }

        /**
         * Highlights all occurrences of the keyword in the input text using SpannableString.
         * @param text The text to highlight.
         * @param keyword The keyword to search for and highlight.
         * @return A SpannableString with highlighted keyword(s).
         */
        private fun highlightText(text: String, keyword: String): SpannableString {
            val spannable = SpannableString(text)
            if (keyword.isBlank()) return spannable

            // Find all matches of the keyword (case-insensitive)
            val regex = Regex(Pattern.quote(keyword), RegexOption.IGNORE_CASE)
            regex.findAll(text).forEach { matchResult ->
                spannable.setSpan(
                    ForegroundColorSpan("#FF4081".toColorInt()), // Highlight color
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
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
            this.bind(holder, dataList[holder.bindingAdapterPosition], keyword)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


}

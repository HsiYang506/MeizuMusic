package com.jywkhyse.meizumusic.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A generic RecyclerView adapter using ViewBinding.
 *
 * @param T The type of data items in the list.
 * @param VB The specific ViewBinding type for the item layout.
 * @param items The list of data items to display.
 * @param inflate A function to inflate the ViewBinding for the item view.
 * @param bind A callback function to bind the data to the ViewBinding. It receives the binding, position, and item data.
 */
class GenericAdapter<T, VB : ViewBinding>(
    val items: MutableList<T>,
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    private val bind: (RecyclerView.ViewHolder, VB, Int, T) -> Unit
) : RecyclerView.Adapter<GenericAdapter<T, VB>.ViewHolder>() {

    /**
     * ViewHolder class that holds the ViewBinding.
     */
    inner class ViewHolder(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return // Handle potential out-of-bounds safely
        bind(holder, holder.binding, holder.bindingAdapterPosition, item)
    }

    override fun getItemCount(): Int = items.size

    // Optional: Method to update data and notify changes (for mutable lists or dynamic updates)
    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<T>) {
        // For simplicity, just notify full data set change. For efficiency, consider DiffUtil in production.
        items.apply {
            clear()
            addAll(newItems)
        }
        notifyDataSetChanged()
    }

}
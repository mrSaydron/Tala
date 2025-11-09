package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.dictionary.Dictionary

class CollectionWordsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items: MutableList<Item> = mutableListOf()

    fun submitItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is Item.Header -> VIEW_TYPE_HEADER
        is Item.Word -> VIEW_TYPE_WORD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_collection_group_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_collection_word, parent, false)
                WordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderViewHolder).bind(item)
            is Item.Word -> (holder as WordViewHolder).bind(item.dictionary)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.collectionGroupTitleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.collectionGroupSubtitleTextView)

        fun bind(item: Item.Header) {
            titleTextView.text = item.title
            subtitleTextView.isVisible = !item.subtitle.isNullOrBlank()
            if (!item.subtitle.isNullOrBlank()) {
                subtitleTextView.text = item.subtitle
            }
        }
    }

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordTextView: TextView = itemView.findViewById(R.id.collectionWordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.collectionWordTranslationTextView)

        fun bind(dictionary: Dictionary) {
            wordTextView.text = dictionary.word
            translationTextView.text = dictionary.translation
        }
    }

    sealed class Item {
        data class Header(val title: String, val subtitle: String?) : Item()
        data class Word(val dictionary: Dictionary) : Item()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_WORD = 1
    }
}


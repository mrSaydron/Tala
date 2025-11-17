package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tala.R
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryWithDependentCount

class DictionaryAdapter(
    private val onItemClick: (Dictionary) -> Unit,
    private val onAddToCollectionClick: ((Dictionary) -> Unit)? = null
) : RecyclerView.Adapter<DictionaryAdapter.DictionaryViewHolder>() {

    private val items: MutableList<DictionaryWithDependentCount> = mutableListOf()

    fun submitList(entries: List<DictionaryWithDependentCount>) {
        items.clear()
        items.addAll(entries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dictionary_entry, parent, false)
        return DictionaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DictionaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DictionaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dictionaryImageView: ImageView = itemView.findViewById(R.id.dictionaryImageView)
        private val wordTextView: TextView = itemView.findViewById(R.id.wordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.translationTextView)
        private val dependentCountTextView: TextView = itemView.findViewById(R.id.dependentCountTextView)
        private val addToCollectionButton: ImageButton = itemView.findViewById(R.id.addToCollectionButton)

        fun bind(item: DictionaryWithDependentCount) {
            val entry = item.dictionary
            wordTextView.text = entry.word
            translationTextView.text = entry.translation

            val totalCount = item.dependentCount + 1
            dependentCountTextView.text = itemView.context.getString(
                R.string.dictionary_dependent_count_template,
                totalCount
            )

            val imagePath = entry.imagePath
            if (!imagePath.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_image)
                    .centerCrop()
                    .into(dictionaryImageView)
            } else {
                dictionaryImageView.setImageResource(R.drawable.ic_image)
            }

            itemView.setOnClickListener {
                onItemClick(entry)
            }

            val addClick = onAddToCollectionClick
            if (addClick != null) {
                addToCollectionButton.isVisible = true
                addToCollectionButton.setOnClickListener {
                    addClick(entry)
                }
            } else {
                addToCollectionButton.isVisible = false
                addToCollectionButton.setOnClickListener(null)
            }
        }
    }
}

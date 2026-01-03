package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.wordCollection.WordCollection

class CollectionListAdapter(
    private val onItemClick: (WordCollection) -> Unit
) : RecyclerView.Adapter<CollectionListAdapter.CollectionViewHolder>() {

    private val items: MutableList<WordCollection> = mutableListOf()

    fun submitList(collections: List<WordCollection>) {
        items.clear()
        items.addAll(collections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_list_entry, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.collectionNameTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.collectionDescriptionTextView)

        fun bind(item: WordCollection) {
            nameTextView.text = item.name

            val description = item.description
            descriptionTextView.isVisible = !description.isNullOrBlank()
            if (!description.isNullOrBlank()) {
                descriptionTextView.text = description
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}


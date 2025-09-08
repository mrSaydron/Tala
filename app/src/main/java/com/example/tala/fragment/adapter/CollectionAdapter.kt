package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.card.CardViewModel

class CollectionAdapter(
    private var collections: List<CardCollection>,
    private val onCollectionClick: (CardCollection) -> Unit,
    private val cardViewModelProvider: () -> CardViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder>() {

    inner class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        private val newTextView: TextView = itemView.findViewById(R.id.newCountTextView)
        private val resetTextView: TextView = itemView.findViewById(R.id.resetCountTextView)
        private val inProgressTextView: TextView = itemView.findViewById(R.id.inProgressCountTextView)

        fun bind(collection: CardCollection) {
            nameTextView.text = collection.name

            val vm = cardViewModelProvider()
            vm.getNewCardsCountByCategory(collection.id).observe(lifecycleOwner) { count ->
                newTextView.text = "$count"
            }
            vm.getResetCardsCountByCategory(collection.id).observe(lifecycleOwner) { count ->
                resetTextView.text = "$count"
            }
            vm.getInProgressCardCountByCategory(collection.id).observe(lifecycleOwner) { count ->
                inProgressTextView.text = "$count"
            }

            itemView.setOnClickListener { onCollectionClick(collection) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(collections[position])
    }

    override fun getItemCount(): Int = collections.size

    fun updateData(newCollections: List<CardCollection>) {
        collections = newCollections
        notifyDataSetChanged()
    }
}



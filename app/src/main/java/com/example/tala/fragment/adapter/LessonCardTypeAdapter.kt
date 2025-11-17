package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.model.enums.CardTypeEnum

class LessonCardTypeAdapter(
    private val onRemoveClick: ((CardTypeEnum) -> Unit)? = null
) : RecyclerView.Adapter<LessonCardTypeAdapter.ViewHolder>() {

    private val items: MutableList<CardTypeEnum> = mutableListOf()

    fun setItems(newItems: List<CardTypeEnum>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson_card_type, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.cardTypeTitleTextView)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.cardTypeRemoveButton)

        fun bind(item: CardTypeEnum) {
            titleTextView.text = item.titleRu
            val removeHandler = onRemoveClick
            removeButton.isVisible = removeHandler != null
            removeButton.setOnClickListener(
                removeHandler?.let { handler -> View.OnClickListener { handler(item) } }
            )
        }
    }
}


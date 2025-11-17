package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.model.enums.CardTypeEnum
import com.google.android.material.switchmaterial.SwitchMaterial

class CollectionCardTypeSelectionAdapter(
    private val onToggle: (CardTypeEnum, Boolean) -> Unit
) : RecyclerView.Adapter<CollectionCardTypeSelectionAdapter.ViewHolder>() {

    private val items: MutableList<CardTypeEnum> = mutableListOf()
    private val selected: MutableSet<CardTypeEnum> = mutableSetOf()

    fun submit(items: List<CardTypeEnum>, selected: Set<CardTypeEnum>) {
        this.items.clear()
        this.items.addAll(items)
        updateSelection(selected)
    }

    fun updateSelection(selection: Set<CardTypeEnum>) {
        selected.clear()
        selected.addAll(selection)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card_type_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.cardTypeOptionTitleTextView)
        private val toggleSwitch: SwitchMaterial = itemView.findViewById(R.id.cardTypeOptionSwitch)

        fun bind(item: CardTypeEnum) {
            titleTextView.text = item.titleRu
            toggleSwitch.setOnCheckedChangeListener(null)
            toggleSwitch.isChecked = selected.contains(item)
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }
        }
    }
}



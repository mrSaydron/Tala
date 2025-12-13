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
import com.example.tala.fragment.adapter.DictionaryAdapter.ListItem.GroupHeader
import com.example.tala.fragment.adapter.DictionaryAdapter.ListItem.GroupWord

class DictionaryAdapter(
    private val onItemClick: (Dictionary) -> Unit,
    private val onAddToCollectionClick: ((Dictionary) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class Group(
        val base: Dictionary,
        val words: List<Dictionary>
    )

    private val groups: MutableList<Group> = mutableListOf()
    private val items: MutableList<ListItem> = mutableListOf()
    private val expandedGroupIds: MutableSet<Int> = mutableSetOf()

    fun submitGroups(newGroups: List<Group>) {
        val validExpandedIds = expandedGroupIds.filter { id ->
            newGroups.any { it.base.id == id }
        }
        expandedGroupIds.clear()
        expandedGroupIds.addAll(validExpandedIds)
        groups.clear()
        groups.addAll(newGroups)
        rebuildItems()
    }

    private fun rebuildItems() {
        items.clear()
        groups.forEach { group ->
            val groupId = group.base.id
            val isExpanded = expandedGroupIds.contains(groupId)
            items += GroupHeader(group, isExpanded)
            if (isExpanded) {
                group.words
                    .sortedWith(
                        compareBy<Dictionary> { if (it.id == group.base.id) 0 else 1 }
                            .thenBy { it.word.lowercase() }
                            .thenBy { it.translation.lowercase() }
                    )
                    .forEach { dictionary ->
                        items += GroupWord(dictionary)
                    }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is GroupHeader -> VIEW_TYPE_GROUP_HEADER
        is GroupWord -> VIEW_TYPE_GROUP_WORD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GROUP_HEADER -> {
                val view = inflater.inflate(R.layout.item_dictionary_entry, parent, false)
                GroupViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_dictionary_group_word, parent, false)
                GroupWordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GroupHeader -> (holder as GroupViewHolder).bind(item)
            is GroupWord -> (holder as GroupWordViewHolder).bind(item.dictionary)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleGroup(groupId: Int) {
        if (expandedGroupIds.contains(groupId)) {
            expandedGroupIds.remove(groupId)
        } else {
            expandedGroupIds.add(groupId)
        }
        rebuildItems()
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dictionaryImageView: ImageView = itemView.findViewById(R.id.dictionaryImageView)
        private val wordTextView: TextView = itemView.findViewById(R.id.wordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.translationTextView)
        private val dependentCountTextView: TextView = itemView.findViewById(R.id.dependentCountTextView)
        private val addToCollectionButton: ImageButton = itemView.findViewById(R.id.addToCollectionButton)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)

        fun bind(item: GroupHeader) {
            val group = item.group
            val entry = group.base

            wordTextView.text = entry.word
            translationTextView.text = entry.translation

            val totalWords = group.words.size
            dependentCountTextView.isVisible = totalWords > 1
            if (totalWords > 1) {
                dependentCountTextView.text = itemView.context.getString(
                    R.string.dictionary_group_word_count_template,
                    totalWords
                )
            }

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

            val addClick = onAddToCollectionClick
            updateHeaderContentVisibility(item.isExpanded)
            setupAddToCollectionButton(addClick, entry, item.isExpanded)
            setupExpandControls(item.isExpanded, group.base.id, totalWords > 1)
            itemView.setOnClickListener {
                toggleGroup(group.base.id)
            }
        }

        private fun updateHeaderContentVisibility(isExpanded: Boolean) {
            if (isExpanded) {
                dictionaryImageView.isVisible = false
                wordTextView.isVisible = false
                translationTextView.isVisible = false
            } else {
                dictionaryImageView.isVisible = true
                wordTextView.isVisible = true
                translationTextView.isVisible = true
            }
        }

        private fun setupAddToCollectionButton(
            addClick: ((Dictionary) -> Unit)?,
            entry: Dictionary,
            isExpanded: Boolean
        ) {
            if (addClick != null && !isExpanded) {
                addToCollectionButton.isVisible = true
                addToCollectionButton.setOnClickListener {
                    addClick(entry)
                }
            } else {
                addToCollectionButton.isVisible = false
                addToCollectionButton.setOnClickListener(null)
            }
        }

        private fun setupExpandControls(
            isExpanded: Boolean,
            groupId: Int,
            canExpand: Boolean
        ) {
            expandButton.isVisible = canExpand
            expandButton.rotation = if (isExpanded) 180f else 0f
            if (canExpand) {
                expandButton.setOnClickListener {
                    toggleGroup(groupId)
                }
                expandButton.contentDescription = itemView.context.getString(
                    if (isExpanded) {
                        R.string.dictionary_collapse_group_button
                    } else {
                        R.string.dictionary_expand_group_button
                    }
                )
            } else {
                expandButton.setOnClickListener(null)
            }
        }
    }

    inner class GroupWordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val wordImageView: ImageView = itemView.findViewById(R.id.dictionaryGroupWordImageView)
        private val wordTextView: TextView = itemView.findViewById(R.id.dictionaryGroupWordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.dictionaryGroupWordTranslationTextView)
        private val addButton: ImageButton = itemView.findViewById(R.id.dictionaryGroupWordAddButton)

        fun bind(dictionary: Dictionary) {
            wordTextView.text = dictionary.word
            translationTextView.text = dictionary.translation

            val imagePath = dictionary.imagePath
            if (!imagePath.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_image)
                    .centerCrop()
                    .into(wordImageView)
            } else {
                wordImageView.setImageResource(R.drawable.ic_image)
            }

            itemView.setOnClickListener {
                onItemClick(dictionary)
            }

            val addClick = onAddToCollectionClick
            if (addClick != null) {
                addButton.isVisible = true
                addButton.setOnClickListener {
                    addClick(dictionary)
                }
            } else {
                addButton.isVisible = false
                addButton.setOnClickListener(null)
            }
        }
    }

    sealed class ListItem {
        data class GroupHeader(val group: Group, val isExpanded: Boolean) : ListItem()
        data class GroupWord(val dictionary: Dictionary) : ListItem()
    }

    companion object {
        private const val VIEW_TYPE_GROUP_HEADER = 0
        private const val VIEW_TYPE_GROUP_WORD = 1
    }
}

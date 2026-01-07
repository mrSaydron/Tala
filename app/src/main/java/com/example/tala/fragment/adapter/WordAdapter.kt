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
import com.example.tala.entity.word.Word
import com.example.tala.fragment.adapter.WordAdapter.ListItem.GroupHeader
import com.example.tala.fragment.adapter.WordAdapter.ListItem.GroupWord

class WordAdapter(
    private val onItemClick: (Word) -> Unit,
    private val onAddToCollectionClick: ((Word) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class Group(
        val base: Word,
        val words: List<Word>
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
                        compareBy<Word> { if (it.id == group.base.id) 0 else 1 }
                            .thenBy { it.word.lowercase() }
                            .thenBy { it.translation.lowercase() }
                    )
                    .forEach { word ->
                        items += GroupWord(word)
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
                val view = inflater.inflate(R.layout.item_word_entry, parent, false)
                GroupViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_word_group_word, parent, false)
                GroupWordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GroupHeader -> (holder as GroupViewHolder).bind(item)
            is GroupWord -> (holder as GroupWordViewHolder).bind(item.word)
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

        private val wordImageView: ImageView = itemView.findViewById(R.id.wordImageView)
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
                    R.string.word_group_word_count_template,
                    totalWords
                )
            }

            val imagePath = entry.imagePath
            if (!imagePath.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_image)
                    .centerCrop()
                    .into(wordImageView)
            } else {
                wordImageView.setImageResource(R.drawable.ic_image)
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
                wordImageView.isVisible = false
                wordTextView.isVisible = false
                translationTextView.isVisible = false
            } else {
                wordImageView.isVisible = true
                wordTextView.isVisible = true
                translationTextView.isVisible = true
            }
        }

        private fun setupAddToCollectionButton(
            addClick: ((Word) -> Unit)?,
            entry: Word,
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
                        R.string.word_collapse_group_button
                    } else {
                        R.string.word_expand_group_button
                    }
                )
            } else {
                expandButton.setOnClickListener(null)
            }
        }
    }

    inner class GroupWordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val wordImageView: ImageView = itemView.findViewById(R.id.wordGroupWordImageView)
        private val wordTextView: TextView = itemView.findViewById(R.id.wordGroupWordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.wordGroupWordTranslationTextView)
        private val addButton: ImageButton = itemView.findViewById(R.id.wordGroupWordAddButton)

        fun bind(word: Word) {
            wordTextView.text = word.word
            translationTextView.text = word.translation

            val imagePath = word.imagePath
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
                onItemClick(word)
            }

            val addClick = onAddToCollectionClick
            if (addClick != null) {
                addButton.isVisible = true
                addButton.setOnClickListener {
                    addClick(word)
                }
            } else {
                addButton.isVisible = false
                addButton.setOnClickListener(null)
            }
        }
    }

    sealed class ListItem {
        data class GroupHeader(val group: Group, val isExpanded: Boolean) : ListItem()
        data class GroupWord(val word: Word) : ListItem()
    }

    companion object {
        private const val VIEW_TYPE_GROUP_HEADER = 0
        private const val VIEW_TYPE_GROUP_WORD = 1
    }
}

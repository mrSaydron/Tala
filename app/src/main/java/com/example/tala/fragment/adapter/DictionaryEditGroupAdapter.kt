package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.databinding.ItemDictionaryGroupBinding
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryLevel
import com.example.tala.entity.dictionary.PartOfSpeech

class DictionaryEditGroupAdapter(
    private val partOfSpeechItems: List<PartOfSpeech>,
    private val levelItems: List<DictionaryLevel?>,
    private val onGroupsChanged: () -> Unit,
) : RecyclerView.Adapter<DictionaryEditGroupAdapter.GroupViewHolder>() {

    private val groups = mutableListOf<DictionaryEditGroup>()
    private val attachedHolders = mutableSetOf<GroupViewHolder>()
    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDictionaryGroupBinding.inflate(inflater, parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position], position)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_EXPANSION)) {
            holder.updateExpansion(groups[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = groups.size

    override fun onViewAttachedToWindow(holder: GroupViewHolder) {
        super.onViewAttachedToWindow(holder)
        attachedHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: GroupViewHolder) {
        super.onViewDetachedFromWindow(holder)
        attachedHolders.remove(holder)
    }

    fun submitGroups(items: List<DictionaryEditGroup>) {
        groups.clear()
        groups.addAll(items)
        notifyDataSetChanged()
        onGroupsChanged()
    }

    fun addGroup(group: DictionaryEditGroup) {
        groups.add(group)
        notifyItemInserted(groups.lastIndex)
        onGroupsChanged()
    }

    fun removeGroup(position: Int) {
        if (position !in groups.indices) return
        groups.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, groups.size - position)
        onGroupsChanged()
    }

    fun getGroups(): List<DictionaryEditGroup> = groups

    fun validateAndBuildGroups(): List<DictionaryGroupPayload>? {
        var isValid = true
        attachedHolders.forEach { holder ->
            isValid = holder.validate() && isValid
        }
        if (!isValid) return null

        return groups.map { group ->
            val dictionaries = group.items.mapNotNull { it.toDictionaryOrNull() }
            val baseId = group.baseWordId ?: dictionaries.firstOrNull()?.baseWordId
            val normalized = if (baseId != null) {
                dictionaries.map { it.copy(baseWordId = baseId) }
            } else {
                dictionaries.mapIndexed { index, dictionary ->
                    if (index == 0) dictionary.copy(baseWordId = null) else dictionary.copy(baseWordId = dictionary.baseWordId)
                }
            }
            DictionaryGroupPayload(
                baseWordId = baseId,
                dictionaries = normalized
            )
        }
    }

    private fun toggleGroup(position: Int) {
        val group = groups.getOrNull(position) ?: return
        group.isExpanded = !group.isExpanded
        notifyItemChanged(position, PAYLOAD_EXPANSION)
        onGroupsChanged()
    }

    inner class GroupViewHolder(
        private val binding: ItemDictionaryGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val entryAdapter: DictionaryEditAdapter

        init {
            entryAdapter = DictionaryEditAdapter(
                partOfSpeechItems = partOfSpeechItems,
                levelItems = levelItems,
                onRemoveItem = { position -> handleEntryRemoved(position) }
            )

            binding.groupEntriesRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.groupEntriesRecyclerView.adapter = entryAdapter
            binding.groupEntriesRecyclerView.setRecycledViewPool(viewPool)
            binding.groupEntriesRecyclerView.itemAnimator = null

            binding.addEntryButton.setOnClickListener {
                val groupIndex = adapterPosition
                if (groupIndex == RecyclerView.NO_POSITION) return@setOnClickListener
                val group = groups.getOrNull(groupIndex) ?: return@setOnClickListener
                val newItem = DictionaryEditItem(baseWordId = group.baseWordId, level = group.level)
                group.items.add(newItem)
                entryAdapter.submitItems(group.items.toList())
                group.isExpanded = true
                updateExpansion(group)
                updateRemovalState(group)
            }

            binding.removeGroupButton.setOnClickListener {
                val groupIndex = adapterPosition
                if (groupIndex == RecyclerView.NO_POSITION) return@setOnClickListener
                removeGroup(groupIndex)
            }

            binding.expandCollapseButton.setOnClickListener {
                val groupIndex = adapterPosition
                if (groupIndex == RecyclerView.NO_POSITION) return@setOnClickListener
                toggleGroup(groupIndex)
            }
        }

        private fun handleEntryRemoved(position: Int) {
            val groupIndex = adapterPosition
            if (groupIndex == RecyclerView.NO_POSITION) return
            val group = groups.getOrNull(groupIndex) ?: return
            if (position !in group.items.indices) return
            group.items.removeAt(position)
            entryAdapter.submitItems(group.items.toList())
            updateRemovalState(group)
        }

        fun bind(group: DictionaryEditGroup, position: Int) {
            binding.groupTitleTextView.text = binding.root.context.getString(
                R.string.dictionary_group_title,
                position + 1
            )
            binding.removeGroupButton.isEnabled = groups.size > 1
            entryAdapter.submitItems(group.items.toList())
            entryAdapter.setRemovalEnabled(group.items.size > 1)
            updateExpansion(group)
        }

        fun validate(): Boolean {
            return entryAdapter.validateAndBuildDictionaries() != null
        }

        private fun updateRemovalState(group: DictionaryEditGroup) {
            entryAdapter.setRemovalEnabled(group.items.size > 1)
            binding.removeGroupButton.isEnabled = groups.size > 1
            onGroupsChanged()
        }

        fun updateExpansion(group: DictionaryEditGroup) {
            val expanded = group.isExpanded
            binding.groupEntriesRecyclerView.isVisible = expanded
            binding.addEntryButton.isVisible = expanded
            binding.expandCollapseButton.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
            binding.expandCollapseButton.contentDescription = binding.root.context.getString(
                if (expanded) R.string.dictionary_collapse_group_button else R.string.dictionary_expand_group_button
            )
        }
    }

    companion object {
        private const val PAYLOAD_EXPANSION = "payload_expansion"
    }
}

data class DictionaryGroupPayload(
    val baseWordId: Int?,
    val dictionaries: List<Dictionary>,
)


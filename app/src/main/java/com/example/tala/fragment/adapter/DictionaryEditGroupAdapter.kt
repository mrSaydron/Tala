package com.example.tala.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tala.R
import com.example.tala.databinding.ItemDictionaryEditBinding
import com.example.tala.databinding.ItemDictionaryGroupAddBinding
import com.example.tala.databinding.ItemDictionaryGroupHeaderBinding
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryLevel
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.entity.dictionary.TagType
import com.google.android.material.chip.Chip

class DictionaryEditorAdapter(
    private val partOfSpeechItems: List<PartOfSpeech>,
    private val levelItems: List<DictionaryLevel?>,
    private val listener: Listener,
) : ListAdapter<DictionaryEditorAdapter.Item, RecyclerView.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onToggleGroup(groupIndex: Int)
        fun onRemoveGroup(groupIndex: Int)
        fun onAddWord(groupIndex: Int)
        fun onRemoveWord(groupIndex: Int, itemIndex: Int)
        fun onSelectImage(groupIndex: Int, itemIndex: Int)
        fun onRemoveImage(groupIndex: Int, itemIndex: Int)
    }

    private val attachedWordHolders = mutableSetOf<WordViewHolder>()
    private val groups: MutableList<DictionaryEditGroup> = mutableListOf()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).stableId

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Item.GroupHeader -> VIEW_TYPE_HEADER
        is Item.WordEntry -> VIEW_TYPE_WORD
        is Item.AddWord -> VIEW_TYPE_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDictionaryGroupHeaderBinding.inflate(inflater, parent, false)
                GroupHeaderViewHolder(binding)
            }
            VIEW_TYPE_WORD -> {
                val binding = ItemDictionaryEditBinding.inflate(inflater, parent, false)
                WordViewHolder(binding)
            }
            else -> {
                val binding = ItemDictionaryGroupAddBinding.inflate(inflater, parent, false)
                AddWordViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.GroupHeader -> (holder as GroupHeaderViewHolder).bind(item)
            is Item.WordEntry -> (holder as WordViewHolder).bind(item)
            is Item.AddWord -> (holder as AddWordViewHolder).bind(item)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is WordViewHolder) {
            attachedWordHolders.add(holder)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is WordViewHolder) {
            attachedWordHolders.remove(holder)
        }
    }

    fun submitGroups(newGroups: List<DictionaryEditGroup>) {
        groups.clear()
        groups.addAll(newGroups)
        submitList(buildItems())
    }

    fun validateAndBuildGroups(): List<DictionaryGroupPayload>? {
        var isValid = true
        attachedWordHolders.forEach { holder ->
            isValid = holder.validate() && isValid
        }
        if (!isValid) return null

        return groups.map { group ->
            val dictionaries = group.items.mapNotNull { it.toDictionaryOrNull() }
            val baseId = group.baseWordId ?: dictionaries.firstOrNull()?.baseWordId
            val normalized = if (baseId != null) {
                dictionaries.mapIndexed { index, dictionary ->
                    if (index == 0) {
                        dictionary.copy(baseWordId = baseId)
                    } else {
                        dictionary.copy(baseWordId = baseId)
                    }
                }
            } else {
                dictionaries.mapIndexed { index, dictionary ->
                    if (index == 0) {
                        dictionary.copy(baseWordId = null)
                    } else {
                        dictionary.copy(baseWordId = dictionary.baseWordId)
                    }
                }
            }
            DictionaryGroupPayload(
                baseWordId = normalized.firstOrNull()?.baseWordId,
                dictionaries = normalized
            )
        }
    }

    fun refresh() {
        submitList(buildItems())
    }

    private fun buildItems(): List<Item> {
        val items = mutableListOf<Item>()
        groups.forEachIndexed { index, group ->
            val canRemoveGroup = groups.size > 1
            items += Item.GroupHeader(
                groupKey = group.key,
                groupIndex = index,
                group = group,
                canRemove = canRemoveGroup
            )
            if (group.isExpanded) {
                val totalItems = group.items.size
                group.items.forEachIndexed { itemIndex, editItem ->
                    items += Item.WordEntry(
                        itemKey = editItem.key,
                        groupIndex = index,
                        itemIndex = itemIndex,
                        item = editItem,
                        totalItemsInGroup = totalItems
                    )
                }
                items += Item.AddWord(
                    groupKey = group.key,
                    groupIndex = index
                )
            }
        }
        return items
    }

    inner class GroupHeaderViewHolder(
        private val binding: ItemDictionaryGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item.GroupHeader) {
            val context = binding.root.context
            binding.groupTitleTextView.text = context.getString(
                R.string.dictionary_group_title,
                item.groupIndex + 1
            )
            val firstItem = item.group.items.firstOrNull()
            binding.groupSubtitleTextView.apply {
                isVisible = !firstItem?.word.isNullOrBlank() || !firstItem?.translation.isNullOrBlank()
                text = listOfNotNull(
                    firstItem?.word?.takeIf { it.isNotBlank() },
                    firstItem?.translation?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            }
            binding.groupCountTextView.text = context.resources.getQuantityString(
                R.plurals.dictionary_group_word_count,
                item.group.items.size,
                item.group.items.size
            )
            binding.levelChip.apply {
                val levelTitle = item.group.level?.name
                    ?: firstItem?.level?.name
                isVisible = !levelTitle.isNullOrBlank()
                if (!levelTitle.isNullOrBlank()) {
                    text = levelTitle
                }
            }
            binding.expandButton.rotation = if (item.group.isExpanded) 180f else 0f
            binding.removeGroupButton.isEnabled = item.canRemove
            binding.removeGroupButton.alpha = if (item.canRemove) 1f else 0.4f

            binding.root.setOnClickListener {
                listener.onToggleGroup(item.groupIndex)
            }
            binding.expandButton.setOnClickListener {
                listener.onToggleGroup(item.groupIndex)
            }
            binding.removeGroupButton.setOnClickListener {
                listener.onRemoveGroup(item.groupIndex)
            }
        }
    }

    inner class WordViewHolder(
        private val binding: ItemDictionaryEditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: DictionaryEditItem? = null
        private var currentGroupIndex: Int = RecyclerView.NO_POSITION
        private var currentItemIndex: Int = RecyclerView.NO_POSITION
        private var isBinding = false
        private var partAdapter: ArrayAdapter<String>? = null
        private var levelAdapter: ArrayAdapter<String>? = null
        private val tagItems = TagType.values().toList()
        private var availableTags: List<TagType> = emptyList()

        init {
            setupTextWatchers()
            setupSpinners()
            setupTagPicker()
            binding.removeEntryButton.setOnClickListener {
                if (currentGroupIndex != RecyclerView.NO_POSITION && currentItemIndex != RecyclerView.NO_POSITION) {
                    listener.onRemoveWord(currentGroupIndex, currentItemIndex)
                }
            }
            binding.selectImageButton.setOnClickListener {
                if (currentGroupIndex != RecyclerView.NO_POSITION && currentItemIndex != RecyclerView.NO_POSITION) {
                    listener.onSelectImage(currentGroupIndex, currentItemIndex)
                }
            }
            binding.removeImageButton.setOnClickListener {
                if (currentGroupIndex != RecyclerView.NO_POSITION && currentItemIndex != RecyclerView.NO_POSITION) {
                    listener.onRemoveImage(currentGroupIndex, currentItemIndex)
                }
            }
        }

        fun bind(item: Item.WordEntry) {
            currentItem = item.item
            currentGroupIndex = item.groupIndex
            currentItemIndex = item.itemIndex
            isBinding = true

            binding.removeEntryButton.isVisible = item.totalItemsInGroup > 1
            clearErrors()
            ensureAdapters()

            val editItem = item.item
            with(binding) {
                if (wordEditText.text?.toString() != editItem.word) wordEditText.setText(editItem.word)
                if (translationEditText.text?.toString() != editItem.translation) translationEditText.setText(editItem.translation)
                if (ipaEditText.text?.toString() != editItem.ipa) ipaEditText.setText(editItem.ipa)
                if (hintEditText.text?.toString() != editItem.hint) hintEditText.setText(editItem.hint)
                if (frequencyEditText.text?.toString() != editItem.frequencyText) frequencyEditText.setText(editItem.frequencyText)
            }

            val context = binding.root.context
            val partIndex = partOfSpeechItems.indexOf(editItem.partOfSpeech).takeIf { it >= 0 } ?: 0
            binding.partOfSpeechSpinner.setSelection(partIndex, false)

            val levelIndex = levelItems.indexOf(editItem.level).takeIf { it >= 0 } ?: 0
            binding.levelSpinner.setSelection(levelIndex, false)

            val imagePath = editItem.imagePath
            if (!imagePath.isNullOrBlank()) {
                Glide.with(binding.root)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_image)
                    .centerCrop()
                    .into(binding.imagePreview)
                binding.removeImageButton.isVisible = true
            } else {
                binding.imagePreview.setImageResource(R.drawable.ic_image)
                binding.removeImageButton.isVisible = false
            }

            refreshTagChips()
            updateTagPickerOptions()
            isBinding = false
        }

        fun validate(): Boolean {
            val item = currentItem ?: return true
            var valid = true
            val context = binding.root.context

            if (item.word.isBlank()) {
                binding.wordInputLayout.error = context.getString(R.string.dictionary_error_required)
                valid = false
            } else {
                binding.wordInputLayout.error = null
            }

            if (item.translation.isBlank()) {
                binding.translationInputLayout.error = context.getString(R.string.dictionary_error_required)
                valid = false
            } else {
                binding.translationInputLayout.error = null
            }

            val freqText = item.frequencyText.trim()
            if (freqText.isNotEmpty()) {
                val normalized = freqText.replace(',', '.')
                val parsed = normalized.toDoubleOrNull()
                if (parsed == null) {
                    binding.frequencyInputLayout.error = context.getString(R.string.dictionary_error_frequency_format)
                    valid = false
                } else {
                    binding.frequencyInputLayout.error = null
                    item.frequencyText = normalized
                }
            } else {
                binding.frequencyInputLayout.error = null
            }

            return valid
        }

        private fun clearErrors() {
            binding.wordInputLayout.error = null
            binding.translationInputLayout.error = null
            binding.frequencyInputLayout.error = null
        }

        private fun setupTextWatchers() {
            binding.wordEditText.doAfterTextChanged {
                if (isBinding) return@doAfterTextChanged
                currentItem?.word = it?.toString()?.trim().orEmpty()
                binding.wordInputLayout.error = null
            }
            binding.translationEditText.doAfterTextChanged {
                if (isBinding) return@doAfterTextChanged
                currentItem?.translation = it?.toString()?.trim().orEmpty()
                binding.translationInputLayout.error = null
            }
            binding.ipaEditText.doAfterTextChanged {
                if (isBinding) return@doAfterTextChanged
                currentItem?.ipa = it?.toString().orEmpty()
            }
            binding.hintEditText.doAfterTextChanged {
                if (isBinding) return@doAfterTextChanged
                currentItem?.hint = it?.toString().orEmpty()
            }
            binding.frequencyEditText.doAfterTextChanged {
                if (isBinding) return@doAfterTextChanged
                currentItem?.frequencyText = it?.toString()?.trim().orEmpty()
                binding.frequencyInputLayout.error = null
            }
        }

        private fun setupSpinners() {
            binding.partOfSpeechSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (isBinding) return
                    currentItem?.partOfSpeech = partOfSpeechItems.getOrNull(position) ?: PartOfSpeech.NOUN
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

            binding.levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (isBinding) return
                    currentItem?.level = levelItems.getOrNull(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }

        private fun ensureAdapters() {
            val context = binding.root.context
            if (partAdapter == null) {
                partAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    partOfSpeechItems.map { it.localizedName(context) }
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                binding.partOfSpeechSpinner.adapter = partAdapter
            }
            if (levelAdapter == null) {
                levelAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    levelItems.map { it?.name ?: context.getString(R.string.dictionary_level_none) }
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                binding.levelSpinner.adapter = levelAdapter
            }
        }

        private fun setupTagPicker() {
            val context = binding.root.context
            val autoComplete = binding.tagPickerAutoComplete
            val adapter = ArrayAdapter<String>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                mutableListOf()
            )
            autoComplete.setAdapter(adapter)
            autoComplete.setOnItemClickListener { _, _, position, _ ->
                val tag = availableTags.getOrNull(position) ?: return@setOnItemClickListener
                val item = currentItem ?: return@setOnItemClickListener
                if (item.tags.add(tag)) {
                    refreshTagChips()
                    updateTagPickerOptions()
                }
                autoComplete.setText("", false)
            }
            autoComplete.setOnClickListener {
                if (availableTags.isNotEmpty()) {
                    autoComplete.showDropDown()
                }
            }
        }

        private fun refreshTagChips() {
            val item = currentItem ?: return
            val context = binding.root.context
            val chipGroup = binding.tagsChipGroup
            chipGroup.removeAllViews()
            val sortedTags = item.tags.sortedBy { context.getString(it.localizedNameRes()) }
            sortedTags.forEach { tag ->
                val chip = Chip(context).apply {
                    text = context.getString(tag.localizedNameRes())
                    isCheckable = false
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        currentItem?.tags?.remove(tag)
                        refreshTagChips()
                        updateTagPickerOptions()
                    }
                }
                chipGroup.addView(chip)
            }
            chipGroup.isVisible = sortedTags.isNotEmpty()
        }

        private fun updateTagPickerOptions() {
            val item = currentItem ?: return
            val context = binding.root.context
            val autoComplete = binding.tagPickerAutoComplete
            val adapter = (autoComplete.adapter as? ArrayAdapter<String>) ?: return
            availableTags = tagItems.filterNot { item.tags.contains(it) }
            adapter.clear()
            adapter.addAll(availableTags.map { context.getString(it.localizedNameRes()) })
            adapter.notifyDataSetChanged()

            val hasOptions = availableTags.isNotEmpty()
            binding.tagPickerLayout.isEnabled = hasOptions
            binding.tagPickerLayout.isEndIconVisible = hasOptions
            if (!hasOptions) {
                autoComplete.setText("", false)
            }
        }
    }

    inner class AddWordViewHolder(
        private val binding: ItemDictionaryGroupAddBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item.AddWord) {
            binding.addWordButton.setOnClickListener {
                listener.onAddWord(item.groupIndex)
            }
        }
    }

    sealed class Item(val id: String) {
        val stableId: Long get() = id.hashCode().toLong()

        data class GroupHeader(
            private val groupKey: String,
            val groupIndex: Int,
            val group: DictionaryEditGroup,
            val canRemove: Boolean,
        ) : Item("header_$groupKey")

        data class WordEntry(
            private val itemKey: String,
            val groupIndex: Int,
            val itemIndex: Int,
            val item: DictionaryEditItem,
            val totalItemsInGroup: Int,
        ) : Item("word_$itemKey")

        data class AddWord(
            private val groupKey: String,
            val groupIndex: Int,
        ) : Item("add_$groupKey")
    }

    data class DictionaryGroupPayload(
        val baseWordId: Int?,
        val dictionaries: List<Dictionary>,
    )

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_WORD = 1
        private const val VIEW_TYPE_ADD = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = when {
                oldItem is Item.GroupHeader && newItem is Item.GroupHeader -> {
                    oldItem.group.isExpanded == newItem.group.isExpanded &&
                        oldItem.group.items.size == newItem.group.items.size &&
                        oldItem.group.level == newItem.group.level &&
                        oldItem.canRemove == newItem.canRemove
                }

                oldItem is Item.WordEntry && newItem is Item.WordEntry -> {
                    oldItem.item == newItem.item &&
                        oldItem.totalItemsInGroup == newItem.totalItemsInGroup
                }

                oldItem is Item.AddWord && newItem is Item.AddWord -> true
                else -> false
            }
        }
    }
}

private fun PartOfSpeech.localizedName(context: Context): String = when (this) {
    PartOfSpeech.NOUN -> context.getString(R.string.dictionary_part_of_speech_noun)
    PartOfSpeech.VERB -> context.getString(R.string.dictionary_part_of_speech_verb)
    PartOfSpeech.ADJECTIVE -> context.getString(R.string.dictionary_part_of_speech_adjective)
    PartOfSpeech.ADVERB -> context.getString(R.string.dictionary_part_of_speech_adverb)
    PartOfSpeech.PRONOUN -> context.getString(R.string.dictionary_part_of_speech_pronoun)
    PartOfSpeech.PREPOSITION -> context.getString(R.string.dictionary_part_of_speech_preposition)
    PartOfSpeech.CONJUNCTION -> context.getString(R.string.dictionary_part_of_speech_conjunction)
    PartOfSpeech.INTERJECTION -> context.getString(R.string.dictionary_part_of_speech_interjection)
    PartOfSpeech.DETERMINER -> context.getString(R.string.dictionary_part_of_speech_determiner)
    PartOfSpeech.ARTICLE -> context.getString(R.string.dictionary_part_of_speech_article)
    PartOfSpeech.NUMERAL -> context.getString(R.string.dictionary_part_of_speech_numeral)
    PartOfSpeech.PARTICLE -> context.getString(R.string.dictionary_part_of_speech_particle)
    PartOfSpeech.AUXILIARY_VERB -> context.getString(R.string.dictionary_part_of_speech_auxiliary_verb)
    PartOfSpeech.MODAL_VERB -> context.getString(R.string.dictionary_part_of_speech_modal_verb)
    PartOfSpeech.PHRASAL_VERB -> context.getString(R.string.dictionary_part_of_speech_phrasal_verb)
    PartOfSpeech.GERUND -> context.getString(R.string.dictionary_part_of_speech_gerund)
    PartOfSpeech.PROPER_NOUN -> context.getString(R.string.dictionary_part_of_speech_proper_noun)
    PartOfSpeech.IDIOM -> context.getString(R.string.dictionary_part_of_speech_idiom)
    PartOfSpeech.UNKNOWN -> context.getString(R.string.dictionary_part_of_speech_noun)
}


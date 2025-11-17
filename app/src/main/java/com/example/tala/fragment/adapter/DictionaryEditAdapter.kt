package com.example.tala.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tala.R
import com.example.tala.databinding.ItemDictionaryEditBinding
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryLevel
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.entity.dictionary.TagType
import com.google.android.material.chip.Chip

class DictionaryEditAdapter(
    private val partOfSpeechItems: List<PartOfSpeech>,
    private val levelItems: List<DictionaryLevel?>,
    private val onRemoveItem: (Int) -> Unit,
    private val onSelectImage: (position: Int) -> Unit,
    private val onRemoveImage: (position: Int) -> Unit,
) : RecyclerView.Adapter<DictionaryEditAdapter.DictionaryEditViewHolder>() {

    private val items = mutableListOf<DictionaryEditItem>()
    private val attachedHolders = mutableSetOf<DictionaryEditViewHolder>()
    private var removalEnabled: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryEditViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDictionaryEditBinding.inflate(inflater, parent, false)
        return DictionaryEditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DictionaryEditViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun onViewAttachedToWindow(holder: DictionaryEditViewHolder) {
        super.onViewAttachedToWindow(holder)
        attachedHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: DictionaryEditViewHolder) {
        super.onViewDetachedFromWindow(holder)
        attachedHolders.remove(holder)
    }

    fun submitItems(newItems: List<DictionaryEditItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: DictionaryEditItem) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getItems(): List<DictionaryEditItem> = items

    fun setRemovalEnabled(enabled: Boolean) {
        removalEnabled = enabled
        notifyDataSetChanged()
    }

    fun validateAndBuildDictionaries(): List<Dictionary>? {
        var isValid = true
        attachedHolders.forEach { holder ->
            isValid = holder.validate() && isValid
        }
        if (!isValid) return null
        return items.mapNotNull { it.toDictionaryOrNull() }
    }

    inner class DictionaryEditViewHolder(
        private val binding: ItemDictionaryEditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: DictionaryEditItem? = null
        private var isBinding: Boolean = false

        private val tagItems = TagType.values().toList()
        private var availableTags: List<TagType> = emptyList()

        init {
            setupTextWatchers()
            setupSpinners()
            setupTagPicker()
            binding.removeEntryButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveItem(position)
                }
            }
        }

        fun bind(item: DictionaryEditItem) {
            currentItem = item
            isBinding = true

            binding.removeEntryButton.isVisible = removalEnabled && items.size > 1

            with(binding) {
                if (wordEditText.text?.toString() != item.word) {
                    wordEditText.setText(item.word)
                }
                if (translationEditText.text?.toString() != item.translation) {
                    translationEditText.setText(item.translation)
                }
                if (ipaEditText.text?.toString() != item.ipa) {
                    ipaEditText.setText(item.ipa)
                }
                if (hintEditText.text?.toString() != item.hint) {
                    hintEditText.setText(item.hint)
                }
                if (frequencyEditText.text?.toString() != item.frequencyText) {
                    frequencyEditText.setText(item.frequencyText)
                }
            }

            val context = binding.root.context

            val localizedParts = partOfSpeechItems.map { it.localizedName(context) }
            binding.partOfSpeechSpinner.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                localizedParts
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            binding.levelSpinner.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                levelItems.map { it?.name ?: context.getString(R.string.dictionary_level_none) }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            val partIndex = partOfSpeechItems.indexOf(item.partOfSpeech).takeIf { it >= 0 } ?: 0
            binding.partOfSpeechSpinner.setSelection(partIndex, false)

            val levelIndex = levelItems.indexOf(item.level).takeIf { it >= 0 } ?: 0
            binding.levelSpinner.setSelection(levelIndex, false)

            val imagePath = item.imagePath
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

            binding.selectImageButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSelectImage(position)
                }
            }
            binding.removeImageButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveImage(position)
                }
            }

            refreshTagChips()
            updateTagPickerOptions()

            clearErrors()
            isBinding = false
        }

        fun validate(): Boolean {
            val item = currentItem ?: return true
            var valid = true

            if (item.word.isBlank()) {
                binding.wordInputLayout.error = binding.root.context.getString(R.string.dictionary_error_required)
                valid = false
            } else {
                binding.wordInputLayout.error = null
            }

            if (item.translation.isBlank()) {
                binding.translationInputLayout.error = binding.root.context.getString(R.string.dictionary_error_required)
                valid = false
            } else {
                binding.translationInputLayout.error = null
            }

            val freqText = item.frequencyText.trim()
            if (freqText.isNotEmpty()) {
                val normalized = freqText.replace(',', '.')
                val parsed = normalized.toDoubleOrNull()
                if (parsed == null) {
                    binding.frequencyInputLayout.error = binding.root.context.getString(R.string.dictionary_error_frequency_format)
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
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    if (isBinding) return
                    currentItem?.partOfSpeech = partOfSpeechItems.getOrNull(position) ?: PartOfSpeech.NOUN
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            binding.levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    if (isBinding) return
                    currentItem?.level = levelItems.getOrNull(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            val adapter = (autoComplete.adapter as? ArrayAdapter<String>)
                ?: return
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
}

// todo перенести в enum
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


data class DictionaryEditItem(
    var id: Int = 0,
    var baseWordId: Int? = null,
    var word: String = "",
    var translation: String = "",
    var partOfSpeech: PartOfSpeech = PartOfSpeech.NOUN,
    var ipa: String = "",
    var hint: String = "",
    var imagePath: String = "",
    var frequencyText: String = "",
    var level: DictionaryLevel? = null,
    val tags: MutableSet<TagType> = mutableSetOf(),
) {
    fun toDictionaryOrNull(): Dictionary? {
        return Dictionary(
            id = id,
            word = word.trim(),
            translation = translation.trim(),
            partOfSpeech = partOfSpeech,
            ipa = ipa.trim().takeIf { it.isNotEmpty() },
            hint = hint.trim().takeIf { it.isNotEmpty() },
            imagePath = imagePath.trim().takeIf { it.isNotEmpty() },
            baseWordId = baseWordId,
            frequency = frequencyText.trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull(),
            level = level,
            tags = tags.toSet(),
        )
    }

    companion object {
        fun fromDictionary(dictionary: Dictionary): DictionaryEditItem = DictionaryEditItem(
            id = dictionary.id,
            baseWordId = dictionary.baseWordId,
            word = dictionary.word,
            translation = dictionary.translation,
            partOfSpeech = dictionary.partOfSpeech,
            ipa = dictionary.ipa.orEmpty(),
            hint = dictionary.hint.orEmpty(),
            imagePath = dictionary.imagePath.orEmpty(),
            frequencyText = dictionary.frequency?.toString().orEmpty(),
            level = dictionary.level,
            tags = dictionary.tags.toMutableSet(),
        )
    }
}

package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.entity.dictionary.TagType

class DictionaryAdapter(
    private val onItemClick: (Dictionary) -> Unit,
    private val onAddToCollectionClick: ((Dictionary) -> Unit)? = null
) : RecyclerView.Adapter<DictionaryAdapter.DictionaryViewHolder>() {

    private val items: MutableList<Dictionary> = mutableListOf()

    fun submitList(entries: List<Dictionary>) {
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

        private val wordTextView: TextView = itemView.findViewById(R.id.wordTextView)
        private val translationTextView: TextView = itemView.findViewById(R.id.translationTextView)
        private val partOfSpeechTextView: TextView = itemView.findViewById(R.id.partOfSpeechTextView)
        private val ipaTextView: TextView = itemView.findViewById(R.id.ipaTextView)
        private val levelTextView: TextView = itemView.findViewById(R.id.levelTextView)
        private val frequencyTextView: TextView = itemView.findViewById(R.id.frequencyTextView)
        private val tagsTextView: TextView = itemView.findViewById(R.id.tagsTextView)
        private val baseWordTextView: TextView = itemView.findViewById(R.id.baseWordTextView)
        private val hintTextView: TextView = itemView.findViewById(R.id.hintTextView)
        private val addToCollectionButton: ImageButton = itemView.findViewById(R.id.addToCollectionButton)

        fun bind(entry: Dictionary) {
            wordTextView.text = entry.word
            translationTextView.text = entry.translation
            val localizedPart = entry.partOfSpeech.localizedName(itemView.context)
            partOfSpeechTextView.text = itemView.context.getString(
                R.string.dictionary_part_of_speech_template,
                localizedPart
            )

            val ipa = entry.ipa
            ipaTextView.isVisible = !ipa.isNullOrBlank()
            if (!ipa.isNullOrBlank()) {
                ipaTextView.text = itemView.context.getString(
                    R.string.dictionary_ipa_template,
                    ipa
                )
            }

            val level = entry.level?.name
            levelTextView.isVisible = !level.isNullOrBlank()
            if (!level.isNullOrBlank()) {
                levelTextView.text = itemView.context.getString(
                    R.string.dictionary_level_template,
                    level
                )
            }

            val frequency = entry.frequency
            frequencyTextView.isVisible = frequency != null
            if (frequency != null) {
                frequencyTextView.text = itemView.context.getString(
                    R.string.dictionary_frequency_template,
                    frequency
                )
            }

            val tagsText = entry.tags
                .map { tag -> itemView.context.getString(tag.localizedNameRes()) }
                .sorted()
                .joinToString(", ")
            tagsTextView.isVisible = tagsText.isNotBlank()
            if (tagsText.isNotBlank()) {
                tagsTextView.text = itemView.context.getString(
                    R.string.dictionary_tags_template,
                    tagsText
                )
            }

            val baseWordId = entry.baseWordId
            baseWordTextView.isVisible = baseWordId != null
            if (baseWordId != null) {
                baseWordTextView.text = itemView.context.getString(
                    R.string.dictionary_base_word_template,
                    baseWordId
                )
            }

            val hint = entry.hint
            hintTextView.isVisible = !hint.isNullOrBlank()
            if (!hint.isNullOrBlank()) {
                hintTextView.text = itemView.context.getString(
                    R.string.dictionary_hint_template,
                    hint
                )
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

private fun PartOfSpeech.localizedName(context: android.content.Context): String = when (this) {
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

private fun TagType.localizedNameRes(): Int = when (this) {
    TagType.IS_IDIOM -> R.string.dictionary_tag_is_idiom
    TagType.IS_PHRASAL -> R.string.dictionary_tag_is_phrasal
    TagType.IS_COLLOCATION -> R.string.dictionary_tag_is_collocation
    TagType.CASE_SENSITIVE -> R.string.dictionary_tag_case_sensitive
    TagType.IS_FIXED_EXPRESSION -> R.string.dictionary_tag_is_fixed_expression
    TagType.PLURAL -> R.string.dictionary_tag_plural
    TagType.PAST_SIMPLE -> R.string.dictionary_tag_past_simple
    TagType.PAST_PARTICIPLE -> R.string.dictionary_tag_past_participle
    TagType.COMPARATIVE -> R.string.dictionary_tag_comparative
    TagType.SUPERLATIVE -> R.string.dictionary_tag_superlative
}


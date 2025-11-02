package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.dictionary.Dictionary

class DictionaryAdapter : RecyclerView.Adapter<DictionaryAdapter.DictionaryViewHolder>() {

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

        fun bind(entry: Dictionary) {
            wordTextView.text = entry.word
            translationTextView.text = entry.translation
            partOfSpeechTextView.text = itemView.context.getString(
                R.string.dictionary_part_of_speech_template,
                entry.partOfSpeech.value
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

            val tagsText = entry.tags.map { it.value }.sorted().joinToString(", ")
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
        }
    }
}


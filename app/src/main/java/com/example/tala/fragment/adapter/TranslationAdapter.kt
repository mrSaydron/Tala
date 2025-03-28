package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R

class TranslationAdapter(
    private val translations: List<String>,
    private val onTranslationClick: (String) -> Unit
) : RecyclerView.Adapter<TranslationAdapter.TranslationViewHolder>() {

    inner class TranslationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val translationTextView: TextView = itemView as TextView

        fun bind(translation: String) {
            translationTextView.text = translation
            itemView.setOnClickListener {
                onTranslationClick(translation)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_translation, parent, false)
        return TranslationViewHolder(view)
    }

    override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
        holder.bind(translations[position])
    }

    override fun getItemCount(): Int = translations.size
}
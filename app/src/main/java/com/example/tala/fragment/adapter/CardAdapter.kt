package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tala.R
import com.example.tala.entity.card.Card
import com.example.tala.model.dto.CardListDto

class CardAdapter(
    private val cards: List<CardListDto>,
    private val onEditClick: (CardListDto) -> Unit,
    private val onDeleteClick: (CardListDto) -> Unit,
    private val categoryIdToName: Map<Int, String>
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val englishWordTextView: TextView = itemView.findViewById(R.id.englishWordTextView)
        private val russianWordTextView: TextView = itemView.findViewById(R.id.russianWordTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val wordImageView: ImageView = itemView.findViewById(R.id.wordImageView)
        private val editButton: Button = itemView.findViewById(R.id.editButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(card: CardListDto) {
            englishWordTextView.text = card.english
            russianWordTextView.text = card.russian
            categoryTextView.text = "Категория: ${categoryIdToName[card.categoryId] ?: "—"}"
            card.imagePath?.let { path ->
                Glide.with(itemView.context)
                    .load(path)
                    .into(wordImageView)
            }
            // Обработка нажатия на кнопку "Редактировать"
            editButton.setOnClickListener {
                onEditClick(card)
            }

            // Обработка нажатия на кнопку "Удалить"
            deleteButton.setOnClickListener {
                onDeleteClick(card)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size
}
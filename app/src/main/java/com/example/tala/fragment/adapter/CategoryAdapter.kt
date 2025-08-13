package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.category.Category
import com.example.tala.entity.card.CardViewModel

class CategoryAdapter(
    private var categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit,
    private val cardViewModelProvider: () -> CardViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        private val newTextView: TextView = itemView.findViewById(R.id.newCountTextView)
        private val resetTextView: TextView = itemView.findViewById(R.id.resetCountTextView)
        private val inProgressTextView: TextView = itemView.findViewById(R.id.inProgressCountTextView)

        fun bind(category: Category) {
            nameTextView.text = category.name

            // Подписки на статистику по категории
            val vm = cardViewModelProvider()
            vm.getNewCardsCountByCategory(category.id).observe(lifecycleOwner) { count ->
                newTextView.text = "$count"
            }
            vm.getResetCardsCountByCategory(category.id).observe(lifecycleOwner) { count ->
                resetTextView.text = "$count"
            }
            vm.getInProgressCardCountByCategory(category.id).observe(lifecycleOwner) { count ->
                inProgressTextView.text = "$count"
            }

            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}


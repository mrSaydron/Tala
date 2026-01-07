package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.lesson.Lesson
import com.example.tala.fragment.LessonListFragment.LessonWithStats
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import android.widget.TextView

class LessonListAdapter(
    private val onItemClick: (Lesson) -> Unit,
    private val onEditClick: (Lesson) -> Unit
) : RecyclerView.Adapter<LessonListAdapter.LessonViewHolder>() {

    private val items: MutableList<LessonWithStats> = mutableListOf()

    fun submitList(lessons: List<LessonWithStats>) {
        items.clear()
        items.addAll(lessons)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson_entry, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.lessonNameTextView)
        private val newChip: Chip = itemView.findViewById(R.id.lessonNewChip)
        private val returnedChip: Chip = itemView.findViewById(R.id.lessonReturnedChip)
        private val learningChip: Chip = itemView.findViewById(R.id.lessonLearningChip)
        private val editButton: MaterialButton = itemView.findViewById(R.id.lessonEditButton)

        fun bind(item: LessonWithStats) {
            nameTextView.text = item.lesson.name
            newChip.text = itemView.context.getString(R.string.lesson_status_new_chip, item.newCount)
            returnedChip.text = itemView.context.getString(R.string.lesson_status_returned_chip, item.returnedCount)
            learningChip.text = itemView.context.getString(R.string.lesson_status_learning_chip, item.learningCount)
            itemView.setOnClickListener { onItemClick(item.lesson) }
            editButton.setOnClickListener { onEditClick(item.lesson) }
        }
    }
}


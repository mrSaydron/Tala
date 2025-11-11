package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.entity.lesson.Lesson

class LessonListAdapter : RecyclerView.Adapter<LessonListAdapter.LessonViewHolder>() {

    private val items: MutableList<Lesson> = mutableListOf()

    fun submitList(lessons: List<Lesson>) {
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

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.lessonNameTextView)

        fun bind(item: Lesson) {
            nameTextView.text = item.name
        }
    }
}


package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.MainActivity
import com.example.tala.R
import com.example.tala.databinding.FragmentLessonListBinding
import com.example.tala.entity.lesson.Lesson
import com.example.tala.entity.lesson.LessonViewModel
import com.example.tala.fragment.adapter.LessonListAdapter
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.LessonCardService
import kotlinx.coroutines.launch

class LessonListFragment : Fragment() {

    private var _binding: FragmentLessonListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lessonViewModel: LessonViewModel
    private lateinit var adapter: LessonListAdapter

    private val lessonCardService: LessonCardService
        get() = MainActivity.lessonCardService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lessonViewModel = ViewModelProvider(requireActivity())[LessonViewModel::class.java]

        adapter = LessonListAdapter(
            onItemClick = { lesson ->
                openLesson(lesson.id)
            },
            onEditClick = { lesson ->
                openEditLesson(lesson.id)
            }
        )
        binding.lessonRecyclerView.adapter = adapter
        binding.lessonRecyclerView.itemAnimator = null

        binding.lessonAddFab.setOnClickListener {
            val fragment = LessonAddFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.wordNavButton.setOnClickListener {
            openWord()
        }

        binding.collectionNavButton.setOnClickListener {
            openCollections()
        }

        binding.settingsNavButton.setOnClickListener {
            openSettings()
        }

        val initialPaddingLeft = binding.root.paddingLeft
        val initialPaddingTop = binding.root.paddingTop
        val initialPaddingRight = binding.root.paddingRight
        val initialPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft,
                initialPaddingTop + systemBars.top,
                initialPaddingRight,
                initialPaddingBottom + systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
        loadLessons()
    }

    private fun loadLessons() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            binding.lessonListEmptyState.isVisible = false

            runCatching {
                lessonViewModel.getAll()
            }.onSuccess { lessons ->
                val sorted = lessons.sortedBy { it.name.lowercase() }
                
                // Загружаем статистику для каждого урока
                val lessonsWithStats = sorted.map { lesson ->
                    val stats = runCatching {
                        lessonCardService.countCardsByStatus(lesson.id)
                    }.getOrElse { emptyMap() }
                    
                    LessonWithStats(
                        lesson = lesson,
                        newCount = stats[StatusEnum.NEW] ?: 0,
                        returnedCount = stats[StatusEnum.PROGRESS_RESET] ?: 0,
                        learningCount = stats[StatusEnum.IN_PROGRESS] ?: 0
                    )
                }
                
                adapter.submitList(lessonsWithStats)
                binding.lessonRecyclerView.isVisible = sorted.isNotEmpty()
                binding.lessonListEmptyState.isVisible = sorted.isEmpty()
            }.onFailure {
                adapter.submitList(emptyList())
                binding.lessonRecyclerView.isVisible = false
                binding.lessonListEmptyState.isVisible = true
                binding.lessonListEmptyState.text = getString(R.string.lesson_list_error_state)
            }

            setLoading(false)
        }
    }

    data class LessonWithStats(
        val lesson: Lesson,
        val newCount: Int,
        val returnedCount: Int,
        val learningCount: Int
    )

    private fun setLoading(isLoading: Boolean) {
        binding.lessonListProgressBar.isVisible = isLoading
    }

    private fun openLesson(lessonId: Int) {
        val fragment = LessonFragment.newInstance(lessonId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openWord() {
        val fragment = WordListFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openCollections() {
        val fragment = CollectionListFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openSettings() {
        val fragment = SettingsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openEditLesson(lessonId: Int) {
        val fragment = LessonAddFragment()
        // TODO: Передать lessonId для редактирования, когда LessonAddFragment будет поддерживать редактирование
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


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
import com.example.tala.R
import com.example.tala.databinding.FragmentLessonListBinding
import com.example.tala.entity.lesson.LessonViewModel
import com.example.tala.fragment.adapter.LessonListAdapter
import com.example.tala.fragment.CollectionListFragment
import com.example.tala.fragment.DictionaryListFragment
import com.example.tala.fragment.SettingsFragment
import kotlinx.coroutines.launch

class LessonListFragment : Fragment() {

    private var _binding: FragmentLessonListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lessonViewModel: LessonViewModel
    private lateinit var adapter: LessonListAdapter

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

        adapter = LessonListAdapter { lesson ->
            openLesson(lesson.id)
        }
        binding.lessonRecyclerView.adapter = adapter
        binding.lessonRecyclerView.itemAnimator = null

        binding.lessonAddButton.setOnClickListener {
            val fragment = LessonAddFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.dictionaryNavButton.setOnClickListener {
            openDictionary()
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
                adapter.submitList(sorted)
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

    private fun openDictionary() {
        val fragment = DictionaryListFragment.newInstance()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


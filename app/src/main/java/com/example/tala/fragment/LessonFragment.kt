package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tala.R
import com.example.tala.MainActivity
import com.example.tala.databinding.FragmentLessonBinding
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.service.lessonCard.LessonCardService
import kotlinx.coroutines.launch

class LessonFragment : Fragment() {

    private var _binding: FragmentLessonBinding? = null
    private val binding get() = _binding!!

    private val lessonId: Int by lazy {
        requireArguments().getInt(ARG_LESSON_ID)
    }

    private val lessonCardService: LessonCardService
        get() = MainActivity.lessonCardService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lessonToolbar.setNavigationIcon(R.drawable.ic_expand_more)
        binding.lessonToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
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

        loadLessonCards()
    }

    private fun loadLessonCards() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            binding.lessonEmptyStateTextView.isVisible = false

            val cards = runCatching {
                lessonCardService.getCards(lessonId)
            }.getOrElse { emptyList() }

            setLoading(false)

            if (cards.isEmpty()) {
                binding.lessonEmptyStateTextView.isVisible = true
                return@launch
            }

            displayCard(cards.first())
        }
    }

    private fun displayCard(card: LessonCardDto) {
        val fragment = when (card) {
            is TranslateLessonCardDto -> TranslateCardTypeFragment.newInstance(card)
            else -> null
        }

        if (fragment == null) {
            binding.lessonEmptyStateTextView.isVisible = true
            binding.lessonEmptyStateTextView.text = getString(R.string.lesson_fragment_unsupported_type)
            return
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.reviewContentContainer, fragment)
            .commit()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.lessonProgressBar.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LESSON_ID = "lesson_id"

        fun newInstance(lessonId: Int): LessonFragment {
            val fragment = LessonFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_LESSON_ID, lessonId)
            }
            return fragment
        }
    }
}


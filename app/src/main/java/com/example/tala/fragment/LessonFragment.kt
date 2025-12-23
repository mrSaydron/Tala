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
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.EnterWordLessonCardDto
import com.example.tala.model.enums.StatusEnum
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
        childFragmentManager.setFragmentResultListener(
            TranslateCardTypeFragment.RESULT_REVIEW_COMPLETED,
            viewLifecycleOwner
        ) { _, _ ->
            loadLessonCards()
        }
    }

    private fun loadLessonCards() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            binding.lessonEmptyStateTextView.isVisible = false

            val cards = runCatching { lessonCardService.getCards(lessonId) }
                .getOrElse { emptyList() }

            setLoading(false)

            val nextCard = selectNextCard(cards)

            if (nextCard == null) {
                binding.lessonEmptyStateTextView.isVisible = true
                binding.reviewContentContainer.isVisible = false
                return@launch
            }

            displayCard(nextCard)
        }
    }

    private fun selectNextCard(cards: List<LessonCardDto>): LessonCardDto? {
        if (cards.isEmpty()) return null
        val now = System.currentTimeMillis()
        val dueCards = cards.filter { isCardDue(it, now) }
        if (dueCards.isEmpty()) return null

        return dueCards.minByOrNull { resolveNextReviewTimestamp(it) }
    }

    private fun displayCard(card: LessonCardDto) {
        binding.lessonEmptyStateTextView.isVisible = false
        val fragment = when (card) {
            is TranslateLessonCardDto -> TranslateCardTypeFragment.newInstance(card)
            is ReverseTranslateLessonCardDto -> ReverseTranslateCardTypeFragment.newInstance(card)
            is EnterWordLessonCardDto -> EnterWordCardTypeFragment.newInstance(card)
            else -> null
        }

        if (fragment == null) {
            binding.lessonEmptyStateTextView.isVisible = true
            binding.lessonEmptyStateTextView.text = getString(R.string.lesson_fragment_unsupported_type)
            binding.reviewContentContainer.isVisible = false
            return
        }

        binding.reviewContentContainer.isVisible = true
        childFragmentManager.beginTransaction()
            .replace(R.id.reviewContentContainer, fragment)
            .commit()
    }

    private fun isCardDue(card: LessonCardDto, now: Long): Boolean {
        val (status, nextReviewDate) = card.statusAndNextReview() ?: return true
        if (status == StatusEnum.NEW || status == StatusEnum.PROGRESS_RESET) return true
        return nextReviewDate?.let { it <= now } ?: true
    }

    private fun resolveNextReviewTimestamp(card: LessonCardDto): Long {
        val nextReviewDate = card.statusAndNextReview()?.second ?: return Long.MIN_VALUE
        return nextReviewDate ?: Long.MIN_VALUE
    }

    private fun LessonCardDto.statusAndNextReview(): Pair<StatusEnum, Long?>? {
        if (this is TranslateLessonCardDto) {
            return status to nextReviewDate
        }
        if (this is ReverseTranslateLessonCardDto) {
            return status to nextReviewDate
        }
        if (this is EnterWordLessonCardDto) {
            return status to nextReviewDate
        }
        return null
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


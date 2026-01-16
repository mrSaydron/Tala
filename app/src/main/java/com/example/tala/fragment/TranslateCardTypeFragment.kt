package com.example.tala.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.R
import com.example.tala.databinding.FragmentTranslateCardTypeBinding
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.enums.StatusEnum
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TranslateCardTypeFragment : Fragment() {

    private var _binding: FragmentTranslateCardTypeBinding? = null
    private val binding get() = _binding!!

    private val dto: TranslateLessonCardDto by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelable<TranslateLessonCardDto>(ARG_CARD_DTO)
            ?: throw IllegalStateException("TranslateLessonCardDto is required")
    }

    private val info: WordCardInfo
        get() = dto.cardInfo

    private val statsNewCount: Int by lazy {
        requireArguments().getInt(ARG_STATS_NEW_COUNT, 0)
    }
    private val statsReturnedCount: Int by lazy {
        requireArguments().getInt(ARG_STATS_RETURNED_COUNT, 0)
    }
    private val statsInProgressCount: Int by lazy {
        requireArguments().getInt(ARG_STATS_IN_PROGRESS_COUNT, 0)
    }

    private var isSubmitting: Boolean = false
    private var answerRevealed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslateCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestionSide()
        setupListeners()
        speakWord()
    }

    private fun setupQuestionSide() {
        binding.translateCardWordTextView.text = info.english ?: dto.word
        binding.translateCardTranslationTextView.visibility = View.GONE
        binding.translateCardAnswerButtonsGroup.visibility = View.GONE
        binding.translateCardShowAnswerButton.visibility = View.VISIBLE
        binding.translateCardLoadingIndicator.visibility = View.GONE

        setupHint()
        loadImage(blurred = true)
        updateStats()

        binding.translateCardHardButton.text = getString(
            R.string.translate_card_answer_hard
        )
        binding.translateCardMediumButton.text = getString(
            R.string.translate_card_answer_medium
        )
        binding.translateCardEasyButton.text = getString(
            R.string.translate_card_answer_easy
        )

        binding.translateCardHardButton.contentDescription =
            getString(R.string.translate_card_answer_hard_description)
        binding.translateCardMediumButton.contentDescription =
            getString(R.string.translate_card_answer_medium_description)
        binding.translateCardEasyButton.contentDescription =
            getString(R.string.translate_card_answer_easy_description)

        answerRevealed = false
        setAnswerButtonsEnabled(true)
    }

    private fun setupListeners() {
        binding.translateCardShowAnswerButton.setOnClickListener {
            revealAnswer()
        }

        binding.translateCardPlayButton.setOnClickListener {
            speakWord()
        }

        binding.translateCardHardButton.setOnClickListener {
            submitAnswer(QUALITY_HARD)
        }
        binding.translateCardMediumButton.setOnClickListener {
            submitAnswer(QUALITY_MEDIUM)
        }
        binding.translateCardEasyButton.setOnClickListener {
            submitAnswer(QUALITY_EASY)
        }
    }

    private fun revealAnswer() {
        if (answerRevealed) return
        answerRevealed = true
        binding.translateCardShowAnswerButton.visibility = View.GONE
        binding.translateCardTranslationTextView.visibility = View.VISIBLE
        binding.translateCardTranslationTextView.text = info.russian ?: dto.translation
        binding.translateCardAnswerButtonsGroup.visibility = View.VISIBLE
        loadImage(blurred = false)
        updateAnswerButtonsText()
    }

    private fun setupHint() {
        val hint = info.hint ?: dto.hint
        if (hint.isNullOrBlank()) {
            binding.translateCardHintTextView.visibility = View.GONE
        } else {
            binding.translateCardHintTextView.visibility = View.VISIBLE
            binding.translateCardHintTextView.text = hint
        }
    }

    private fun updateStats() {
        binding.translateCardNewCount.text = statsNewCount.toString()
        binding.translateCardReturnedCount.text = statsReturnedCount.toString()
        binding.translateCardInProgressCount.text = statsInProgressCount.toString()

        clearStatsHighlight()
        when (dto.status) {
            StatusEnum.NEW -> highlightStat(binding.translateCardNewCount, R.color.status_new_bg)
            StatusEnum.PROGRESS_RESET -> highlightStat(binding.translateCardReturnedCount, R.color.status_reset_bg)
            StatusEnum.IN_PROGRESS -> highlightStat(binding.translateCardInProgressCount, R.color.status_in_progress_bg)
            else -> Unit
        }
    }

    private fun clearStatsHighlight() {
        ViewCompat.setBackgroundTintList(binding.translateCardNewCount, null)
        ViewCompat.setBackgroundTintList(binding.translateCardReturnedCount, null)
        ViewCompat.setBackgroundTintList(binding.translateCardInProgressCount, null)
    }

    private fun highlightStat(target: View, colorRes: Int) {
        val color = ContextCompat.getColor(requireContext(), colorRes)
        ViewCompat.setBackgroundTintList(target, ColorStateList.valueOf(color))
    }

    private fun loadImage(blurred: Boolean) {
        val path = info.imagePath ?: dto.imagePath
        if (path.isNullOrBlank()) {
            binding.translateCardImageContainer.visibility = View.GONE
            binding.translateCardImageView.visibility = View.GONE
            binding.translateCardImageView.setImageDrawable(null)
            return
        }

        binding.translateCardImageContainer.visibility = View.VISIBLE
        binding.translateCardImageView.visibility = View.VISIBLE
        val request = Glide.with(this)
            .load(path)
            .centerCrop()

        if (blurred) {
            request.transform(BlurTransformation(25, 6))
        }

        request.into(binding.translateCardImageView)
    }

    private fun speakWord() {
        val text = info.english ?: dto.word
        if (text.isNullOrBlank()) return
        MainActivity.textToSpeechHelper.speak(text)
    }

    private fun updateAnswerButtonsText() {
        binding.translateCardHardButton.text =
            "${getString(R.string.translate_card_answer_hard)}\n${HARD_INTERVAL_HINT}"
        binding.translateCardMediumButton.text =
            "${getString(R.string.translate_card_answer_medium)}\n${formatMediumInterval()}"
        binding.translateCardEasyButton.text =
            "${getString(R.string.translate_card_answer_easy)}\n${formatEasyInterval()}"
    }

    private fun formatMediumInterval(): String {
        return if (dto.status == StatusEnum.NEW || dto.status == StatusEnum.PROGRESS_RESET) {
            "1 дн."
        } else {
            val intervalDays = (dto.intervalMinutes / MINUTES_IN_DAY.toDouble() * dto.ef)
                .coerceAtLeast(1.0)
                .roundToInt()
            "$intervalDays дн."
        }
    }

    private fun formatEasyInterval(): String {
        return if (dto.status == StatusEnum.NEW || dto.status == StatusEnum.PROGRESS_RESET) {
            "2 дн."
        } else {
            val intervalDays = (dto.intervalMinutes * dto.ef * 1.5 / MINUTES_IN_DAY)
                .coerceAtLeast(1.0)
                .roundToInt()
            "$intervalDays дн."
        }
    }

    private fun submitAnswer(quality: Int) {
        if (!answerRevealed || isSubmitting) return
        isSubmitting = true
        setAnswerButtonsEnabled(false)
        binding.translateCardLoadingIndicator.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                MainActivity.lessonCardService.answerResult(dto, null, quality)
            }

            binding.translateCardLoadingIndicator.visibility = View.GONE

            result.onFailure {
                Toast.makeText(requireContext(), R.string.translate_card_result_error, Toast.LENGTH_SHORT).show()
                isSubmitting = false
                setAnswerButtonsEnabled(true)
                return@launch
            }

            parentFragmentManager.setFragmentResult(
                RESULT_REVIEW_COMPLETED,
                bundleOf(
                    RESULT_ARG_PROGRESS_ID to dto.progressId,
                    RESULT_ARG_QUALITY to quality
                )
            )
        }
    }

    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        binding.translateCardHardButton.isEnabled = enabled
        binding.translateCardMediumButton.isEnabled = enabled
        binding.translateCardEasyButton.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CARD_DTO = "translate_card_dto"
        const val RESULT_REVIEW_COMPLETED = "translate_card_result"
        const val RESULT_ARG_PROGRESS_ID = "progress_id"
        const val RESULT_ARG_QUALITY = "quality"
        private const val ARG_STATS_NEW_COUNT = "stats_new_count"
        private const val ARG_STATS_RETURNED_COUNT = "stats_returned_count"
        private const val ARG_STATS_IN_PROGRESS_COUNT = "stats_in_progress_count"

        private const val QUALITY_HARD = 0
        private const val QUALITY_MEDIUM = 3
        private const val QUALITY_EASY = 5
        private const val HARD_INTERVAL_HINT = "<10 мин."
        private const val MINUTES_IN_DAY = 1440.0

        fun newInstance(
            dto: TranslateLessonCardDto,
            newCount: Int,
            returnedCount: Int,
            inProgressCount: Int
        ): TranslateCardTypeFragment {
            return TranslateCardTypeFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CARD_DTO, dto)
                    putInt(ARG_STATS_NEW_COUNT, newCount)
                    putInt(ARG_STATS_RETURNED_COUNT, returnedCount)
                    putInt(ARG_STATS_IN_PROGRESS_COUNT, inProgressCount)
                }
            }
        }
    }
}

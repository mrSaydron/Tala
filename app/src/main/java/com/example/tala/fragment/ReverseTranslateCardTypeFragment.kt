package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.R
import com.example.tala.databinding.FragmentReverseTranslateCardTypeBinding
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import com.example.tala.model.enums.StatusEnum
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ReverseTranslateCardTypeFragment : Fragment() {

    private var _binding: FragmentReverseTranslateCardTypeBinding? = null
    private val binding get() = _binding!!

    private val dto: ReverseTranslateLessonCardDto by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelable<ReverseTranslateLessonCardDto>(ARG_CARD_DTO)
            ?: throw IllegalStateException("ReverseTranslateLessonCardDto is required")
    }

    private val info: WordCardInfo
        get() = dto.cardInfo

    private var isSubmitting: Boolean = false
    private var answerRevealed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReverseTranslateCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestionSide()
        setupListeners()
    }

    private fun setupQuestionSide() {
        binding.reverseTranslateCardQuestionTextView.text = info.russian ?: dto.translation
        binding.reverseTranslateCardHintTextView.visibility = View.GONE
        binding.reverseTranslateCardAnswerContainer.visibility = View.GONE
        binding.reverseTranslateCardPlayButton.visibility = View.GONE
        binding.reverseTranslateCardAnswerButtonsGroup.visibility = View.GONE
        binding.reverseTranslateCardShowAnswerButton.visibility = View.VISIBLE
        binding.reverseTranslateCardLoadingIndicator.visibility = View.GONE

        setupHint()
        loadImage()
        setupButtonContentDescriptions()

        answerRevealed = false
        setAnswerButtonsEnabled(true)
    }

    private fun setupHint() {
        val hint = info.hint ?: dto.hint
        if (hint.isNullOrBlank()) {
            binding.reverseTranslateCardHintTextView.visibility = View.GONE
        } else {
            binding.reverseTranslateCardHintTextView.visibility = View.VISIBLE
            binding.reverseTranslateCardHintTextView.text = hint
        }
    }

    private fun loadImage() {
        val path = info.imagePath ?: dto.imagePath
        if (path.isNullOrBlank()) {
            binding.reverseTranslateCardImageView.visibility = View.GONE
            binding.reverseTranslateCardImageView.setImageDrawable(null)
            return
        }

        binding.reverseTranslateCardImageView.visibility = View.VISIBLE
        Glide.with(this)
            .load(path)
            .centerCrop()
            .into(binding.reverseTranslateCardImageView)
    }

    private fun setupListeners() {
        binding.reverseTranslateCardShowAnswerButton.setOnClickListener {
            revealAnswer()
        }
        binding.reverseTranslateCardPlayButton.setOnClickListener {
            speakWord()
        }
        binding.reverseTranslateCardHardButton.setOnClickListener {
            submitAnswer(QUALITY_HARD)
        }
        binding.reverseTranslateCardMediumButton.setOnClickListener {
            submitAnswer(QUALITY_MEDIUM)
        }
        binding.reverseTranslateCardEasyButton.setOnClickListener {
            submitAnswer(QUALITY_EASY)
        }
    }

    private fun revealAnswer() {
        if (answerRevealed) return
        answerRevealed = true

        binding.reverseTranslateCardShowAnswerButton.visibility = View.GONE
        binding.reverseTranslateCardAnswerContainer.visibility = View.VISIBLE
        binding.reverseTranslateCardAnswerTextView.text = info.english ?: dto.word
        binding.reverseTranslateCardPlayButton.visibility = View.VISIBLE
        binding.reverseTranslateCardAnswerButtonsGroup.visibility = View.VISIBLE

        updateAnswerButtonsText()
        speakWord()
    }

    private fun setupButtonContentDescriptions() {
        binding.reverseTranslateCardHardButton.contentDescription =
            getString(R.string.translate_card_answer_hard_description)
        binding.reverseTranslateCardMediumButton.contentDescription =
            getString(R.string.translate_card_answer_medium_description)
        binding.reverseTranslateCardEasyButton.contentDescription =
            getString(R.string.translate_card_answer_easy_description)
    }

    private fun updateAnswerButtonsText() {
        binding.reverseTranslateCardHardButton.text =
            "${getString(R.string.translate_card_answer_hard)}\n${HARD_INTERVAL_HINT}"
        binding.reverseTranslateCardMediumButton.text =
            "${getString(R.string.translate_card_answer_medium)}\n${formatMediumInterval()}"
        binding.reverseTranslateCardEasyButton.text =
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

    private fun speakWord() {
        val text = info.english ?: dto.word
        if (text.isNullOrBlank()) return
        MainActivity.textToSpeechHelper.speak(text)
    }

    private fun submitAnswer(quality: Int) {
        if (!answerRevealed || isSubmitting) return
        isSubmitting = true
        setAnswerButtonsEnabled(false)
        binding.reverseTranslateCardLoadingIndicator.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                MainActivity.lessonCardService.answerResult(dto, null, quality)
            }

            binding.reverseTranslateCardLoadingIndicator.visibility = View.GONE

            result.onFailure {
                Toast.makeText(requireContext(), R.string.translate_card_result_error, Toast.LENGTH_SHORT).show()
                isSubmitting = false
                setAnswerButtonsEnabled(true)
                return@launch
            }

            parentFragmentManager.setFragmentResult(
                TranslateCardTypeFragment.RESULT_REVIEW_COMPLETED,
                bundleOf(
                    TranslateCardTypeFragment.RESULT_ARG_PROGRESS_ID to dto.progressId,
                    TranslateCardTypeFragment.RESULT_ARG_QUALITY to quality
                )
            )
        }
    }

    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        binding.reverseTranslateCardHardButton.isEnabled = enabled
        binding.reverseTranslateCardMediumButton.isEnabled = enabled
        binding.reverseTranslateCardEasyButton.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_DTO = "reverse_translate_card_dto"

        private const val QUALITY_HARD = 0
        private const val QUALITY_MEDIUM = 3
        private const val QUALITY_EASY = 5
        private const val HARD_INTERVAL_HINT = "<10 мин."
        private const val MINUTES_IN_DAY = 1440.0

        fun newInstance(dto: ReverseTranslateLessonCardDto): ReverseTranslateCardTypeFragment {
            return ReverseTranslateCardTypeFragment().apply {
                arguments = bundleOf(ARG_CARD_DTO to dto)
            }
        }
    }
}


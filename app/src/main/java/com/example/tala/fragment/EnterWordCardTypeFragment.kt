package com.example.tala.fragment

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.R
import com.example.tala.databinding.FragmentEnterWordCardTypeBinding
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.dto.lessonCard.EnterWordLessonCardDto
import com.example.tala.model.enums.StatusEnum
import com.example.tala.util.TextDiffHighlighter
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class EnterWordCardTypeFragment : Fragment() {

    private var _binding: FragmentEnterWordCardTypeBinding? = null
    private val binding get() = _binding!!

    private val dto: EnterWordLessonCardDto by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelable<EnterWordLessonCardDto>(ARG_CARD_DTO)
            ?: throw IllegalStateException("EnterWordLessonCardDto is required")
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
        _binding = FragmentEnterWordCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestionSide()
        setupListeners()
        binding.enterWordCardAnswerInput.requestFocus()
    }

    private fun setupQuestionSide() {
        binding.enterWordCardQuestionTextView.text = info.russian ?: dto.translation
        binding.enterWordCardHintTextView.visibility = View.GONE
        binding.enterWordCardAnswerContainer.visibility = View.GONE
        binding.enterWordCardDiffTextView.visibility = View.GONE
        binding.enterWordCardAnswerButtonsGroup.visibility = View.GONE
        binding.enterWordCardPlayButton.visibility = View.GONE
        binding.enterWordCardShowAnswerButton.visibility = View.VISIBLE
        binding.enterWordCardLoadingIndicator.visibility = View.GONE
        binding.enterWordCardAnswerInput.setText("")

        setupHint()
        loadImage()
        setAnswerButtonsEnabled(true)
        setupButtonContentDescriptions()
        answerRevealed = false
    }

    private fun setupHint() {
        val hint = info.hint ?: dto.hint
        if (hint.isNullOrBlank()) {
            binding.enterWordCardHintTextView.visibility = View.GONE
        } else {
            binding.enterWordCardHintTextView.visibility = View.VISIBLE
            binding.enterWordCardHintTextView.text = hint
        }
    }

    private fun loadImage() {
        val path = info.imagePath ?: dto.imagePath
        if (path.isNullOrBlank()) {
            binding.enterWordCardImageView.visibility = View.GONE
            binding.enterWordCardImageView.setImageDrawable(null)
            return
        }

        binding.enterWordCardImageView.visibility = View.VISIBLE
        Glide.with(this)
            .load(path)
            .centerCrop()
            .into(binding.enterWordCardImageView)
    }

    private fun setupListeners() {
        binding.enterWordCardShowAnswerButton.setOnClickListener {
            revealAnswer()
        }

        binding.enterWordCardPlayButton.setOnClickListener {
            speakWord()
        }

        binding.enterWordCardAnswerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                revealAnswer()
                true
            } else {
                false
            }
        }

        binding.enterWordCardAnswerInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                revealAnswer()
                true
            } else {
                false
            }
        }

        binding.enterWordCardHardButton.setOnClickListener {
            submitAnswer(QUALITY_HARD)
        }
        binding.enterWordCardMediumButton.setOnClickListener {
            submitAnswer(QUALITY_MEDIUM)
        }
        binding.enterWordCardEasyButton.setOnClickListener {
            submitAnswer(QUALITY_EASY)
        }
    }

    private fun revealAnswer() {
        if (answerRevealed) return
        answerRevealed = true

        binding.enterWordCardShowAnswerButton.visibility = View.GONE
        binding.enterWordCardAnswerContainer.visibility = View.VISIBLE
        binding.enterWordCardAnswerTextView.text = info.english ?: dto.word
        binding.enterWordCardPlayButton.visibility = View.VISIBLE
        binding.enterWordCardAnswerButtonsGroup.visibility = View.VISIBLE

        val userAnswer = binding.enterWordCardAnswerInput.text?.toString().orEmpty()
        val diffSpannable = TextDiffHighlighter.buildColoredAnswer(userAnswer, info.english ?: dto.word)
        binding.enterWordCardDiffTextView.visibility = View.VISIBLE
        binding.enterWordCardDiffTextView.text = diffSpannable

        updateAnswerButtonsText()
        speakWord()
    }

    private fun setupButtonContentDescriptions() {
        binding.enterWordCardHardButton.contentDescription =
            getString(R.string.translate_card_answer_hard_description)
        binding.enterWordCardMediumButton.contentDescription =
            getString(R.string.translate_card_answer_medium_description)
        binding.enterWordCardEasyButton.contentDescription =
            getString(R.string.translate_card_answer_easy_description)
    }

    private fun updateAnswerButtonsText() {
        binding.enterWordCardHardButton.text =
            "${getString(R.string.translate_card_answer_hard)}\n${HARD_INTERVAL_HINT}"
        binding.enterWordCardMediumButton.text =
            "${getString(R.string.translate_card_answer_medium)}\n${formatMediumInterval()}"
        binding.enterWordCardEasyButton.text =
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
        binding.enterWordCardLoadingIndicator.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                MainActivity.lessonCardService.answerResult(dto.progressId, quality)
            }.getOrNull()

            binding.enterWordCardLoadingIndicator.visibility = View.GONE

            if (result == null) {
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
        binding.enterWordCardHardButton.isEnabled = enabled
        binding.enterWordCardMediumButton.isEnabled = enabled
        binding.enterWordCardEasyButton.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_DTO = "enter_word_card_dto"

        private const val QUALITY_HARD = 0
        private const val QUALITY_MEDIUM = 3
        private const val QUALITY_EASY = 5
        private const val HARD_INTERVAL_HINT = "<10 мин."
        private const val MINUTES_IN_DAY = 1440.0

        fun newInstance(dto: EnterWordLessonCardDto): EnterWordCardTypeFragment {
            return EnterWordCardTypeFragment().apply {
                arguments = bundleOf(ARG_CARD_DTO to dto)
            }
        }
    }
}


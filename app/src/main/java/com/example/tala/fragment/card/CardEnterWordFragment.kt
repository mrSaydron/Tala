package com.example.tala.fragment.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.databinding.FragmentCardEnterWordBinding
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.util.TextDiffHighlighter

class CardEnterWordFragment(private val getInfo: () -> WordCardInfo) : CardReviewBase() {

    private lateinit var binding: FragmentCardEnterWordBinding
    private var info: WordCardInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardEnterWordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        info = getInfo()
        bind()

        binding.playButton.setOnClickListener {
            info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }
        }

        // Нажатие на кнопку Done на клавиатуре имитирует "Показать перевод"
        binding.translationInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                (parentFragment as? com.example.tala.fragment.ReviewFragment)?.showTranslation()
                true
            } else false
        }
    }

    override fun roll() {
        binding.wordTextView.visibility = View.VISIBLE
        binding.playButton.visibility = View.VISIBLE

        // Спрятать поле ввода и показать ответ пользователя с подсветкой
        val userInput = binding.translationInput.text?.toString() ?: ""
        val correctAnswer = binding.wordTextView.text?.toString() ?: ""

        val coloredAnswer = TextDiffHighlighter.buildColoredAnswer(userInput, correctAnswer)

        binding.translationInput.visibility = View.GONE
        binding.userAnswerTextView.visibility = View.VISIBLE
        binding.userAnswerTextView.setTextColor(Color.BLACK)
        binding.userAnswerTextView.text = coloredAnswer

        info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }
    }

    

    override fun bind() {
        info?.let { data ->
            binding.wordTextView.text = data.english
            binding.translationTextView.text = data.russian
            if (!data.hint.isNullOrEmpty()) {
                binding.hintTextView.text = data.hint
                binding.hintTextView.visibility = View.VISIBLE
            } else {
                binding.hintTextView.visibility = View.GONE
            }
            data.imagePath?.let { path ->
                Glide.with(this)
                    .load(path)
                    .into(binding.wordImageView)
            }
        }
        binding.wordTextView.visibility = View.GONE
        binding.playButton.visibility = View.GONE
        binding.translationInput.visibility = View.VISIBLE
        binding.translationInput.setText("")
        binding.userAnswerTextView.visibility = View.GONE
    }

}
package com.example.tala.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tala.ReviewSettings
import com.example.tala.databinding.FragmentReviewBinding
import com.example.tala.entity.learningMode.LearningMode
import com.example.tala.entity.learningMode.LearningModeViewModel
import com.example.tala.entity.card.CardViewModel
import com.example.tala.entity.card.Card
import com.example.tala.integration.mistral.MistralRequest
import com.example.tala.integration.mistral.MistralRequestMessage
import com.example.tala.integration.mistral.SentenceResponse
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.service.ApiClient
import com.example.tala.service.TextToSpeechHelper
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ReviewFragment : Fragment() {

    private lateinit var viewModel: CardViewModel
    private lateinit var learningModeViewModel: LearningModeViewModel
    private lateinit var binding: FragmentReviewBinding
    private lateinit var textToSpeechHelper: TextToSpeechHelper

    private var isTranslationShown = false
    private var currentCard: Card? = null
    private lateinit var reviewSettings: ReviewSettings
    private var sentenceResponse: SentenceResponse? = null

    private var selectedCategoryId: Int = 0 // ID выбранной категории

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCategoryId = arguments?.getInt("categoryId") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация ViewModel
        viewModel = ViewModelProvider(this)[CardViewModel::class.java]
        learningModeViewModel = ViewModelProvider(this)[LearningModeViewModel::class.java]

        // Загружаем следующее слово для повторения
        lifecycleScope.launch {
            loadNextWord()
        }

        // Обработка нажатия на кнопку "Показать перевод"
        binding.showTranslationButton.setOnClickListener {
            if (!isTranslationShown) {
                showTranslation()
            }
        }

        // Инициализация настроек
        reviewSettings = ReviewSettings(requireContext())

        // Обработка нажатия на кнопки оценки
        binding.easyButton.setOnClickListener { resultEasy() }
        binding.mediumButton.setOnClickListener { resultMedium() }
        binding.hardButton.setOnClickListener { resultHard() }

        textToSpeechHelper = TextToSpeechHelper(requireContext()) { isInitialized ->
            if (!isInitialized) {
                Toast.makeText(requireContext(), "Озвучка не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show()
            }
        }

        binding.speakButton.setOnClickListener {
            val word = currentCard?.english ?: return@setOnClickListener
            textToSpeechHelper.speak(word)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeechHelper.shutdown() // Освобождаем ресурсы TTS
    }

    // Загружает следующее слово для повторения
    private suspend fun loadNextWord() {
        val endDayTime = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        viewModel.getNextWordToReview(endDayTime)?.let { word ->
            Log.i("ReviewFragment", "loadNextWord: $word")
            currentCard = word
            updateUIForLearningMode()
            isTranslationShown = false

            word.imagePath?.let { path ->
                Glide.with(this)
                    .load(path)
                    .into(binding.wordImageView)
                binding.wordImageView.visibility = View.VISIBLE
            } ?: run {
                binding.wordImageView.visibility = View.GONE
            }

            // todo убрать в метод updateUIForLearningMode
            if (currentCard!!.cardType == CardTypeEnum.TRANSLATE) {
                currentCard?.english?.let { textToSpeechHelper.speak(it) }
                binding.wordImageView.visibility = View.GONE
            }

        } ?: {
            // Если слов для повторения нет
            Log.i("ReviewFragment", "loadNextWord: not words")
            binding.wordTextView.text = "Слова для повторения закончились!"
            binding.showTranslationButton.visibility = View.GONE
            binding.progressTextView.visibility = View.GONE
            binding.wordImageView.visibility = View.GONE
            binding.speakButton.visibility = View.GONE
        }
        setupProgress()
    }

    private fun updateUIForLearningMode() {
        binding.reviewTextView.visibility = View.GONE

        when (currentCard!!.cardType) {
            CardTypeEnum.TRANSLATE -> {
                binding.wordTextView.text = currentCard?.english
                binding.answerTextView.visibility = View.GONE
                binding.reviewTextView.visibility = View.GONE
                binding.showTranslationButton.visibility = View.VISIBLE
                binding.translationInput.visibility = View.GONE
            }
            CardTypeEnum.REVERSE_TRANSLATE -> {
                binding.wordTextView.text = currentCard?.russian
                binding.answerTextView.visibility = View.GONE
                binding.reviewTextView.visibility = View.GONE
                binding.showTranslationButton.visibility = View.VISIBLE
                binding.translationInput.visibility = View.GONE
            }
            CardTypeEnum.ENTER_WORD -> {
                binding.wordTextView.text = currentCard?.russian
                binding.answerTextView.visibility = View.GONE
                binding.reviewTextView.visibility = View.GONE
                binding.showTranslationButton.visibility = View.VISIBLE
                binding.translationInput.visibility = View.VISIBLE
                binding.translationInput.setText("")
            }
            CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> {
                currentCard?.let {
                    binding.wordTextView.text = ""
                    binding.answerTextView.visibility = View.GONE
                    binding.reviewTextView.visibility = View.GONE
                    binding.showTranslationButton.visibility = View.GONE
                    binding.translationInput.visibility = View.GONE
                    lifecycleScope.launch {
                        sentenceResponse = getSentence(it.english, it.russian)

                        binding.wordTextView.text = sentenceResponse?.rus
                        binding.showTranslationButton.visibility = View.VISIBLE
                        binding.translationInput.visibility = View.VISIBLE
                        binding.translationInput.setText("")
                    }
                }
            }
            else -> {
                Log.i("ReviewFragment", "showTranslation: not mode")
            }
        }
    }

    // Показывает перевод слова
    private fun showTranslation() {
        currentCard?.let {
            hideKeyboard()
            isTranslationShown = true
            binding.answerTextView.visibility = View.VISIBLE
            binding.showTranslationButton.visibility = View.GONE
            binding.wordImageView.visibility = View.VISIBLE

            binding.hardButton.text = "Сложно ${viewModel.getHardInterval(currentCard!!)}"
            binding.mediumButton.text = "Средне ${viewModel.getMediumInterval(currentCard!!)}"
            binding.easyButton.text = "Легко ${viewModel.getEasyInterval(currentCard!!)}"
            (binding.easyButton.parent as View).visibility = View.VISIBLE

            when (currentCard!!.cardType) {
                CardTypeEnum.TRANSLATE -> {
                    binding.wordTextView.text = currentCard?.english
                    binding.answerTextView.text = currentCard?.russian
                    binding.translationInput.visibility = View.GONE
                }
                CardTypeEnum.REVERSE_TRANSLATE -> {
                    binding.wordTextView.text = currentCard?.english
                    binding.answerTextView.text = currentCard?.russian
                    binding.translationInput.visibility = View.GONE

                    currentCard?.english?.let { textToSpeechHelper.speak(it) }
                }
                CardTypeEnum.ENTER_WORD -> {
                    binding.wordTextView.text = currentCard?.english
                    binding.answerTextView.text = currentCard?.russian
                    binding.translationInput.visibility = View.VISIBLE

                    currentCard?.english?.let { textToSpeechHelper.speak(it) }
                }
                CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> {
                    binding.wordTextView.text = sentenceResponse?.rus
                    binding.answerTextView.text = sentenceResponse?.eng
                    binding.translationInput.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        sentenceResponse?.let {
                            val review = getReview(it, binding.translationInput.text.toString())
                            binding.reviewTextView.visibility = View.VISIBLE
                            binding.reviewTextView.text = review

                            sentenceResponse?.eng?.let { sentence -> textToSpeechHelper.speak(sentence) }
                        }
                    }
                }
                else -> {
                    Log.i("ReviewFragment", "showTranslation: not mode")
                }
            }
        }
    }

    private fun resultHard() {
        currentCard?.let {
            viewModel.resultHard(it)
            lifecycleScope.launch {
                loadNextWord() // Загружаем следующее слово
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun resultMedium() {
        currentCard?.let {
            viewModel.resultMedium(it)
            lifecycleScope.launch {
                loadNextWord() // Загружаем следующее слово
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun resultEasy() {
        currentCard?.let {
            viewModel.resultEasy(it)
            lifecycleScope.launch {
                loadNextWord() // Загружаем следующее слово
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun setupProgress() {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        viewModel.getWordsToReviewCount(endDate).observe(viewLifecycleOwner) { count ->
            binding.progressTextView.text = "Слов для повторения: $count"
        }
    }

    private suspend fun getSentence(eng: String, rus: String): SentenceResponse {
        try {
            val englishLevel = reviewSettings.englishLevel
            val response = ApiClient.mistralApi.generateText(
                request = MistralRequest(
                    model = "mistral-small-latest",
                    messages = listOf(
                        MistralRequestMessage(
                            role = "user",
                            content = "Составь предложение на английском и переведи его на русский со словом $eng ($rus), уровень знания английского $englishLevel. Ответь в формате json в виде {\"eng\": \"\", \"rus\": \"\"}"
                        )
                    )
                )
            )
            var content = response.choices.first().message.content
            content = content.substring(7, content.length - 3)
            return Gson().fromJson(content, SentenceResponse::class.java)
        } catch (e: Exception) {
            Log.e("ReviewFragment", "getSentence: ${e.message}")
            throw e
        }
    }

    private suspend fun getReview(sentenceResponse: SentenceResponse, translation: String): String {
        try {
            val englishLevel = reviewSettings.englishLevel
            val response = ApiClient.mistralApi.generateText(
                request = MistralRequest(
                    model = "mistral-small-latest",
                    messages = listOf(
                        MistralRequestMessage(
                            role = "user",
                            content = """Есть предложение на русском "${sentenceResponse.rus}". Пользователь перевел его на английский как: "$translation". Уровень знания английского $englishLevel. Оцени перевод пользователя. Найди ошибки, если они есть"""
                        )
                    )
                )
            )
            return response.choices.first().message.content
        } catch (e: Exception) {
            Log.e("ReviewFragment", "getSentence: ${e.message}")
            throw e
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    companion object {
        fun newInstance(categoryId: Int): ReviewFragment {
            val fragment = ReviewFragment()
            val args = Bundle()
            args.putInt("categoryId", categoryId)
            fragment.arguments = args
            return fragment
        }
    }
}
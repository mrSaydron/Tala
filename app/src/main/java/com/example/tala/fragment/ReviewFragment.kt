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
import com.example.tala.R
import com.example.tala.ReviewSettings
import com.example.tala.databinding.FragmentReviewBinding
import com.example.tala.entity.card.CardViewModel
import com.example.tala.entity.card.Card
import com.example.tala.fragment.card.CardEnterWordFragment
import com.example.tala.fragment.card.CardReverseTranslateFragment
import com.example.tala.fragment.card.CardReviewBase
import com.example.tala.fragment.card.CardTranslateFragment
import com.example.tala.model.dto.CardListDto
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
    private lateinit var binding: FragmentReviewBinding
    private lateinit var textToSpeechHelper: TextToSpeechHelper

    private var isTranslationShown = false
    private var currentCard: Card? = null
    private var currentCardFragment: CardReviewBase? = null
    private lateinit var reviewSettings: ReviewSettings

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

        // Переход к редактированию через AddWordFragment
        binding.editButton.setOnClickListener {
            currentCard?.let { card ->
                val dto = CardListDto(
                    commonId = card.commonId,
                    english = card.english,
                    russian = card.russian,
                    categoryId = card.categoryId,
                    imagePath = card.imagePath,
                    info = card.info,
                )
                val editFragment = AddWordFragment.newInstance(dto)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, editFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Инициализация настроек
        reviewSettings = ReviewSettings(requireContext())

        // Обработка нажатия на кнопки оценки
        binding.easyButton.setOnClickListener { resultEasy() }
        binding.mediumButton.setOnClickListener { resultMedium() }
        binding.hardButton.setOnClickListener { resultHard() }

        // Подсказки для статистики
        binding.newChip.setOnClickListener {
            Toast.makeText(requireContext(), "новых слов", Toast.LENGTH_SHORT).show()
        }
        binding.resetChip.setOnClickListener {
            Toast.makeText(requireContext(), "возвращенных", Toast.LENGTH_SHORT).show()
        }
        binding.inProgressChip.setOnClickListener {
            Toast.makeText(requireContext(), "в обучении", Toast.LENGTH_SHORT).show()
        }

        textToSpeechHelper = TextToSpeechHelper(requireContext()) { isInitialized ->
            if (!isInitialized) {
                Toast.makeText(requireContext(), "Озвучка не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeechHelper.shutdown() // Освобождаем ресурсы TTS
    }

    // Загружает следующее слово для повторения
    private suspend fun loadNextWord() {
        Log.i(TAG, "loadNextWord")

        isTranslationShown = false
        val endDayTime = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val card = viewModel.getNextWordToReview(selectedCategoryId, endDayTime)

        Log.i(TAG, "loadNextWord: card - $card")
        if (card != null) {
            binding.reviewContentContainer.visibility = View.VISIBLE
            binding.showTranslationButton.visibility = View.VISIBLE
            binding.editButton.visibility = View.VISIBLE
            (binding.newChip.parent as View).visibility = View.VISIBLE

            currentCard = card
            currentCardFragment = when (card.cardType) {
                CardTypeEnum.TRANSLATE -> CardTranslateFragment { card }
                CardTypeEnum.REVERSE_TRANSLATE -> CardReverseTranslateFragment { card }
                CardTypeEnum.ENTER_WORD -> CardEnterWordFragment { card }
                CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> TODO()
                CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE -> TODO()
            }

            Log.i(TAG, "loadNextWord: currentCardFragment - $currentCardFragment")
            currentCardFragment?.let {
                childFragmentManager.beginTransaction()
                    .replace(R.id.reviewContentContainer, it)
                    .commit()
            }

            binding.showTranslationButton.visibility = View.VISIBLE
            (binding.easyButton.parent as View).visibility = View.GONE
        } else {
            Log.i(TAG, "loadNextWord: there is no next word")
            binding.messageTextView.visibility = View.VISIBLE

            binding.reviewContentContainer.visibility = View.GONE
            binding.showTranslationButton.visibility = View.GONE
            binding.editButton.visibility = View.GONE
            (binding.easyButton.parent as View).visibility = View.GONE
            (binding.newChip.parent as View).visibility = View.GONE
        }
        setupProgress()
    }

    override fun onResume() {
        super.onResume()
        // После возврата из AddWordFragment перезагружаем текущее слово/интерфейс
        lifecycleScope.launch {
            loadNextWord()
        }
    }

    // Показывает перевод слова
    fun showTranslation() {
        currentCardFragment?.roll()
        currentCard?.let {
            hideKeyboard()
            isTranslationShown = true
            binding.showTranslationButton.visibility = View.GONE

            binding.hardButton.text = "Сложно\n${viewModel.getHardInterval(currentCard!!)}"
            binding.mediumButton.text = "Средне\n${viewModel.getMediumInterval(currentCard!!)}"
            binding.easyButton.text = "Легко\n${viewModel.getEasyInterval(currentCard!!)}"
            (binding.easyButton.parent as View).visibility = View.VISIBLE
        }
    }

    private fun resultHard() {
        currentCard?.let { card ->
            lifecycleScope.launch {
                // Дожидаемся записи изменений, чтобы избежать повторного показа карточки из-за гонки
                viewModel.resultHardSuspend(card)
                loadNextWord()
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun resultMedium() {
        currentCard?.let { card ->
            lifecycleScope.launch {
                viewModel.resultMediumSuspend(card)
                loadNextWord()
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun resultEasy() {
        currentCard?.let { card ->
            lifecycleScope.launch {
                viewModel.resultEasySuspend(card)
                loadNextWord()
                (binding.easyButton.parent as View).visibility = View.GONE
            }
        }
    }

    private fun setupProgress() {
        viewModel.getNewCardsCountByCategory(selectedCategoryId).observe(viewLifecycleOwner) { count ->
            binding.newChip.text = "$count"
        }
        viewModel.getResetCardsCountByCategory(selectedCategoryId).observe(viewLifecycleOwner) { count ->
            binding.resetChip.text = "$count"
        }
        viewModel.getInProgressCardCountByCategory(selectedCategoryId).observe(viewLifecycleOwner) { count ->
            binding.inProgressChip.text = "$count"
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
        private const val TAG = "ReviewFragment"

        fun newInstance(categoryId: Int): ReviewFragment {
            val fragment = ReviewFragment()
            val args = Bundle()
            args.putInt("categoryId", categoryId)
            fragment.arguments = args
            return fragment
        }
    }
}
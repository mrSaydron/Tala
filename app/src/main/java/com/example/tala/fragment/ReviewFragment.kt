package com.example.tala.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tala.R
import com.example.tala.ReviewSettings
import com.example.tala.databinding.FragmentReviewBinding
import com.example.tala.entity.card.CardViewModel
import com.example.tala.fragment.card.CardEnterWordFragment
import com.example.tala.fragment.card.CardReverseTranslateFragment
import com.example.tala.fragment.card.CardReviewBase
import com.example.tala.fragment.card.CardTranslateFragment
import com.example.tala.integration.mistral.MistralRequest
import com.example.tala.integration.mistral.MistralRequestMessage
import com.example.tala.integration.mistral.SentenceResponse
import com.example.tala.model.dto.CardDto
import com.example.tala.model.dto.copy
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ReviewFragment : Fragment() {

    private enum class StudyMode {
        SCHEDULED, FREE_RANDOM, FREE_HARD, FREE_SOON, NONE
    }

    private lateinit var viewModel: CardViewModel
    private lateinit var binding: FragmentReviewBinding

    private var isTranslationShown = false
    private var currentDto: CardDto? = null
    private var currentCardFragment: CardReviewBase? = null
    private lateinit var reviewSettings: ReviewSettings

    private var selectedCollectionId: Int = 0 // ID выбранной коллекции
    private val queue = ArrayDeque<CardDto>()
    private var mode: StudyMode = StudyMode.NONE

    private var systemBarsTopInset: Int = 0

    // Defaults to restore chip backgrounds when status changes
    private var defaultNewChipBg: ColorStateList? = null
    private var defaultResetChipBg: ColorStateList? = null
    private var defaultInProgressChipBg: ColorStateList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCollectionId = arguments?.getInt("collectionId") ?: 0
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

        // Загружаем очередь планового обучения
        lifecycleScope.launch { loadScheduledQueue() }

        // Обработка нажатия на кнопку "Показать перевод"
        binding.showTranslationButton.setOnClickListener {
            if (!isTranslationShown) {
                showTranslation()
            }
        }

        // Переход к редактированию через AddWordFragment
        binding.editButton.setOnClickListener {
            currentDto?.commonId?.let { commonId ->
                val editFragment = AddWordFragment.newInstance(commonId)
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

        // Save default backgrounds to restore later
        defaultNewChipBg = binding.newChip.chipBackgroundColor
        defaultResetChipBg = binding.resetChip.chipBackgroundColor
        defaultInProgressChipBg = binding.inProgressChip.chipBackgroundColor

        // Кнопки свободного обучения
        binding.freeRandomButton.setOnClickListener {
            promptCount { count ->
                lifecycleScope.launch { startFreeMode(StudyMode.FREE_RANDOM, count) }
            }
        }
        binding.freeHardButton.setOnClickListener {
            promptCount { count ->
                lifecycleScope.launch { startFreeMode(StudyMode.FREE_HARD, count) }
            }
        }
        binding.freeSoonButton.setOnClickListener {
            promptCount { count ->
                lifecycleScope.launch { startFreeMode(StudyMode.FREE_SOON, count) }
            }
        }

        // Обработка системных отступов: когда нет изображения, контент должен быть ниже статус-бара
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollArea) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            systemBarsTopInset = top
            applyTopInsetPadding()
            insets
        }
    }

    // Запуск свободного режима с заданным количеством карточек
    private suspend fun startFreeMode(targetMode: StudyMode, count: Int) {
        Log.i(TAG, "startFreeMode: $targetMode, count=$count")
        isTranslationShown = false
        queue.clear()

        val list = when (targetMode) {
            StudyMode.FREE_RANDOM -> viewModel.getRandomCardsForFreeStudy(selectedCollectionId, count)
            StudyMode.FREE_HARD -> viewModel.getHardCardsForFreeStudy(selectedCollectionId, count)
            StudyMode.FREE_SOON -> viewModel.getSoonCardsForFreeStudy(selectedCollectionId, count)
            else -> emptyList()
        }

        if (list.isEmpty()) {
            Toast.makeText(requireContext(), "Нет карточек для выбранного режима", Toast.LENGTH_SHORT).show()
            return
        }

        mode = targetMode
        queue.addAll(list)

        binding.reviewContentContainer.visibility = View.VISIBLE
        binding.showTranslationButton.visibility = View.VISIBLE
        binding.editButton.visibility = View.VISIBLE
        binding.freeStudyGroup.visibility = View.GONE
        (binding.newChip.parent as View).visibility = View.VISIBLE

        showNextFromQueue()
        setupProgress()
    }

    private fun promptCount(onChosen: (Int) -> Unit) {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Количество (например, 10)"
        input.setText("10")

        // Контейнер с горизонтальными отступами
        val container = FrameLayout(requireContext())
        val padding = (20 * resources.displayMetrics.density).toInt()
        container.setPadding(padding, 0, padding, 0)
        container.addView(
            input,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сколько карточек взять?")
            .setView(container)
            .setPositiveButton("OK") { dialog, _ ->
                val num = input.text?.toString()?.toIntOrNull() ?: 10
                dialog.dismiss()
                onChosen(num.coerceIn(1, 200))
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Загружает очередь карточек для планового обучения
    private suspend fun loadScheduledQueue() {
        Log.i(TAG, "loadScheduledQueue")
        isTranslationShown = false
        queue.clear()

        val endDayTime = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val list = viewModel.getDueCardsListByCollection(selectedCollectionId, endDayTime)

        if (list.isNotEmpty()) {
            mode = StudyMode.SCHEDULED
            queue.addAll(list)
            (binding.newChip.parent as View).visibility = View.VISIBLE
            binding.editButton.visibility = View.VISIBLE
            binding.reviewContentContainer.visibility = View.VISIBLE
            binding.showTranslationButton.visibility = View.VISIBLE
            binding.freeStudyGroup.visibility = View.GONE
            showNextFromQueue()
        } else {
            mode = StudyMode.NONE
            binding.freeStudyGroup.visibility = View.VISIBLE
            binding.reviewContentContainer.visibility = View.GONE
            binding.showTranslationButton.visibility = View.GONE
            binding.editButton.visibility = View.GONE
            (binding.easyButton.parent as View).visibility = View.GONE
            (binding.newChip.parent as View).visibility = View.GONE
        }
        setupProgress()
        applyTopInsetPadding()
    }

    private fun showNextFromQueue() {
        currentDto = queue.firstOrNull()
        if (currentDto == null) {
            mode = StudyMode.NONE
            binding.reviewContentContainer.visibility = View.GONE
            binding.showTranslationButton.visibility = View.GONE
            binding.editButton.visibility = View.GONE
            (binding.easyButton.parent as View).visibility = View.GONE
            (binding.newChip.parent as View).visibility = View.GONE

            binding.freeStudyGroup.visibility = View.VISIBLE
            highlightStatusChips(null)
            setupProgress()
            applyTopInsetPadding()
            return
        }

        val dto = currentDto!!
        currentCardFragment = when (dto.cardType) {
            CardTypeEnum.TRANSLATE -> CardTranslateFragment.newInstance(dto.info as WordCardInfo)
            CardTypeEnum.REVERSE_TRANSLATE -> CardReverseTranslateFragment.newInstance(dto.info as WordCardInfo)
            CardTypeEnum.ENTER_WORD -> CardEnterWordFragment.newInstance(dto.info as WordCardInfo)
            CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> TODO()
            CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE -> TODO()
        }

        currentCardFragment?.let {
            childFragmentManager.beginTransaction()
                .replace(R.id.reviewContentContainer, it)
                .commit()
        }

        binding.showTranslationButton.visibility = View.VISIBLE
        (binding.easyButton.parent as View).visibility = View.GONE
        isTranslationShown = false
        highlightStatusChips(dto.status)
        setupProgress()
        applyTopInsetPadding()
    }

    private fun applyTopInsetPadding() {
        val reviewVisible = binding.reviewContentContainer.visibility == View.VISIBLE
        val topPadding = if (reviewVisible) 0 else systemBarsTopInset
        binding.scrollArea.setPadding(
            binding.scrollArea.paddingLeft,
            topPadding,
            binding.scrollArea.paddingRight,
            binding.scrollArea.paddingBottom
        )
    }

    // Показывает перевод слова
    fun showTranslation() {
        currentCardFragment?.roll()
        currentDto?.let { dto ->
            hideKeyboard()
            isTranslationShown = true
            binding.showTranslationButton.visibility = View.GONE

            if (mode == StudyMode.SCHEDULED) {
                binding.hardButton.text = "Сложно\n${viewModel.getHardInterval(dto)}"
                binding.mediumButton.text = "Средне\n${viewModel.getMediumInterval(dto)}"
                binding.easyButton.text = "Легко\n${viewModel.getEasyInterval(dto)}"
            } else {
                binding.hardButton.text = "Сложно"
                binding.mediumButton.text = "Средне"
                binding.easyButton.text = "Легко"
            }
            (binding.easyButton.parent as View).visibility = View.VISIBLE
        }
    }

    private fun resultHard() {
        val dto = currentDto ?: return
        lifecycleScope.launch {
            if (mode == StudyMode.SCHEDULED) {
                viewModel.resultHardSuspend(dto)
            }
            queue.removeFirstOrNull()
            queue.addLast(dto.copy(status = StatusEnum.PROGRESS_RESET))
            (binding.easyButton.parent as View).visibility = View.GONE
            showNextFromQueue()
        }
    }

    private fun resultMedium() {
        val dto = currentDto ?: return
        lifecycleScope.launch {
            if (mode == StudyMode.SCHEDULED) {
                viewModel.resultMediumSuspend(dto)
            }
            queue.removeFirstOrNull()
            (binding.easyButton.parent as View).visibility = View.GONE
            showNextFromQueue()
        }
    }

    private fun resultEasy() {
        val dto = currentDto ?: return
        lifecycleScope.launch {
            if (mode == StudyMode.SCHEDULED) {
                viewModel.resultEasySuspend(dto)
            }
            queue.removeFirstOrNull()
            (binding.easyButton.parent as View).visibility = View.GONE
            showNextFromQueue()
        }
    }

    private fun setupProgress() {
        val newCount = queue.count { it.status == StatusEnum.NEW }
        val resetCount = queue.count { it.status == StatusEnum.PROGRESS_RESET }
        val inProgressCount = queue.count { it.status == StatusEnum.IN_PROGRESS }

        binding.newChip.text = "$newCount"
        binding.resetChip.text = "$resetCount"
        binding.inProgressChip.text = "$inProgressCount"
    }

    private fun highlightStatusChips(status: StatusEnum?) {
        // Restore defaults first
        defaultNewChipBg?.let { binding.newChip.chipBackgroundColor = it }
        defaultResetChipBg?.let { binding.resetChip.chipBackgroundColor = it }
        defaultInProgressChipBg?.let { binding.inProgressChip.chipBackgroundColor = it }

        when (status) {
            StatusEnum.NEW -> binding.newChip.setChipBackgroundColorResource(R.color.status_new_bg)
            StatusEnum.PROGRESS_RESET -> binding.resetChip.setChipBackgroundColorResource(R.color.status_reset_bg)
            StatusEnum.IN_PROGRESS -> binding.inProgressChip.setChipBackgroundColorResource(R.color.status_in_progress_bg)
            else -> { /* no-op */ }
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

        fun newInstance(collectionId: Int): ReviewFragment {
            val fragment = ReviewFragment()
            val args = Bundle()
            args.putInt("collectionId", collectionId)
            fragment.arguments = args
            return fragment
        }
    }
}
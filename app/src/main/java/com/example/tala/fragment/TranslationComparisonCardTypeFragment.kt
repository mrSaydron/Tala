package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.MainActivity
import com.example.tala.R
import com.example.tala.databinding.FragmentTranslationComparisonCardTypeBinding
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.service.lessonCard.model.CardAnswer
import kotlinx.coroutines.launch

class TranslationComparisonCardTypeFragment : Fragment() {

    private var _binding: FragmentTranslationComparisonCardTypeBinding? = null
    private val binding get() = _binding!!

    private val dto: TranslationComparisonLessonCardDto by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelable<TranslationComparisonLessonCardDto>(ARG_CARD_DTO)
            ?: throw IllegalStateException("TranslationComparisonLessonCardDto is required")
    }

    private lateinit var wordAdapter: ComparisonWordAdapter
    private lateinit var optionAdapter: ComparisonTranslationAdapter
    private var optionItemTouchHelper: ItemTouchHelper? = null
    private var wordItemTouchHelper: ItemTouchHelper? = null

    private lateinit var rowHeights: MutableList<Int>

    private var isSubmitting = false
    private var answerRevealed = false
    private var currentAnswer: CardAnswer.Comparison? = null
    private var wasAnswerFullyCorrect = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationComparisonCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rowHeights = MutableList(dto.items.size) { 0 }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLists()
        setupButtons()
    }

    private fun setupLists() {
        val context = requireContext()
        wordAdapter = ComparisonWordAdapter(
            items = dto.items.map { it.copy() }.toMutableList(),
            rowHeights = rowHeights,
            onHeightMeasured = ::onRowHeightMeasured
        )
        binding.translationComparisonWordsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wordAdapter
            isNestedScrollingEnabled = false
        }

        val shuffledOptions = dto.items.map {
            ComparisonTranslationAdapter.OptionItem(
                dictionaryId = it.dictionaryId,
                translation = it.translation
            )
        }.shuffled()

        optionAdapter = ComparisonTranslationAdapter(
            items = shuffledOptions.toMutableList(),
            rowHeights = rowHeights,
            onHeightMeasured = ::onRowHeightMeasured
        )
        binding.translationComparisonOptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = optionAdapter
            isNestedScrollingEnabled = false
        }

        val wordDragCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                wordAdapter.swapItems(from, to)
                optionAdapter.notifyDataSetChanged()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // no-op
            }

            override fun isLongPressDragEnabled(): Boolean = true
        }
        wordItemTouchHelper = ItemTouchHelper(wordDragCallback).also {
            it.attachToRecyclerView(binding.translationComparisonWordsRecyclerView)
        }

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                optionAdapter.swapItems(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // no-op
            }

            override fun isLongPressDragEnabled(): Boolean = true
        }
        optionItemTouchHelper = ItemTouchHelper(callback).also {
            it.attachToRecyclerView(binding.translationComparisonOptionsRecyclerView)
        }
    }

    private fun onRowHeightMeasured(index: Int, measuredHeight: Int) {
        val currentBinding = _binding ?: return
        if (index !in rowHeights.indices) return
        val current = rowHeights[index]
        val newHeight = maxOf(current, measuredHeight)
        if (newHeight == current) return

        rowHeights[index] = newHeight
        currentBinding.translationComparisonWordsRecyclerView.applyHeightToVisibleItem(index, newHeight)
        currentBinding.translationComparisonOptionsRecyclerView.applyHeightToVisibleItem(index, newHeight)
    }

    private fun setupButtons() {
        binding.translationComparisonShowAnswerButton.setOnClickListener {
            revealAnswer()
        }
        binding.translationComparisonNextButton.setOnClickListener {
            submitAnswer()
        }
    }

    private fun revealAnswer() {
        if (answerRevealed) return
        val currentOptions = optionAdapter.currentOrder()
        val currentWords = wordAdapter.currentOrder()
        if (currentOptions.size != currentWords.size) return

        val matches = currentWords.mapIndexed { index, item ->
            val selected = currentOptions.getOrNull(index)
            CardAnswer.Comparison.Match(
                progressId = item.progressId,
                selectedDictionaryId = selected?.dictionaryId
            )
        }
        currentAnswer = CardAnswer.Comparison(matches)

        val correctness = matches.mapIndexed { index, match ->
            match.selectedDictionaryId == currentWords[index].dictionaryId
        }
        wasAnswerFullyCorrect = correctness.all { it }

        wordAdapter.showResult(correctness)
        optionAdapter.showResult(
            correctTranslations = currentWords.map { it.translation },
            userTranslations = currentOptions.map { it.translation },
            correctness = correctness,
            correctDictionaryIds = currentWords.map { it.dictionaryId }
        )

        answerRevealed = true
        binding.translationComparisonShowAnswerButton.visibility = View.GONE
        binding.translationComparisonNextButton.visibility = View.VISIBLE
        binding.translationComparisonNextButton.isEnabled = true
        optionItemTouchHelper?.attachToRecyclerView(null)
        wordItemTouchHelper?.attachToRecyclerView(null)
    }

    private fun submitAnswer() {
        if (!answerRevealed) {
            Toast.makeText(requireContext(), R.string.translation_comparison_instruction, Toast.LENGTH_SHORT).show()
            return
        }
        if (isSubmitting) return
        val answer = currentAnswer ?: return
        isSubmitting = true
        binding.translationComparisonNextButton.isEnabled = false
        binding.translationComparisonLoadingIndicator.visibility = View.VISIBLE
        val quality = if (wasAnswerFullyCorrect) QUALITY_SUCCESS else QUALITY_FAILURE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                MainActivity.lessonCardService.answerResult(dto, answer, quality)
            }

            binding.translationComparisonLoadingIndicator.visibility = View.GONE

            result.onFailure {
                Toast.makeText(requireContext(), R.string.translate_card_result_error, Toast.LENGTH_SHORT).show()
                isSubmitting = false
                binding.translationComparisonNextButton.isEnabled = true
                return@launch
            }

            parentFragmentManager.setFragmentResult(
                TranslateCardTypeFragment.RESULT_REVIEW_COMPLETED,
                bundleOf(
                    TranslateCardTypeFragment.RESULT_ARG_PROGRESS_ID to dto.items.firstOrNull()?.progressId,
                    TranslateCardTypeFragment.RESULT_ARG_QUALITY to quality
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ComparisonWordAdapter(
        private val items: MutableList<TranslationComparisonLessonCardDto.Item>,
        private val rowHeights: List<Int>,
        private val onHeightMeasured: (Int, Int) -> Unit
    ) : RecyclerView.Adapter<ComparisonWordAdapter.WordViewHolder>() {

        private val highlights: MutableList<HighlightState> =
            MutableList(items.size) { HighlightState.NEUTRAL }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_translation_comparison_word, parent, false)
            return WordViewHolder(view)
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            val item = items[position]
            val desiredHeight = rowHeights.getOrElse(position) { 0 }
            holder.bind(item.word, highlights[position], desiredHeight)
            holder.itemView.post {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val height = holder.itemView.height
                    if (height > 0) {
                        onHeightMeasured(adapterPosition, height)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun swapItems(from: Int, to: Int) {
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return
            if (from == to) return
            val movedItem = items.removeAt(from)
            items.add(to, movedItem)
            val movedHighlight = highlights.removeAt(from)
            highlights.add(to, movedHighlight)
            notifyItemMoved(from, to)
        }

        fun currentOrder(): List<TranslationComparisonLessonCardDto.Item> = items.toList()

        fun showResult(correctness: List<Boolean>) {
            correctness.forEachIndexed { index, isCorrect ->
                highlights[index] = if (isCorrect) HighlightState.CORRECT else HighlightState.INCORRECT
            }
            notifyDataSetChanged()
        }

        class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView = itemView.findViewById<android.widget.TextView>(R.id.comparisonWordTextView)
            fun bind(text: String, highlight: HighlightState, desiredHeight: Int) {
                textView.text = text
                val colorRes = when (highlight) {
                    HighlightState.NEUTRAL -> R.color.comparison_bg_neutral
                    HighlightState.CORRECT -> R.color.comparison_bg_correct
                    HighlightState.INCORRECT -> R.color.comparison_bg_incorrect
                }
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
                itemView.applyRowHeight(desiredHeight)
            }
        }
    }

    private class ComparisonTranslationAdapter(
        private val items: MutableList<OptionItem>,
        private val rowHeights: List<Int>,
        private val onHeightMeasured: (Int, Int) -> Unit
    ) : RecyclerView.Adapter<ComparisonTranslationAdapter.TranslationViewHolder>() {

        private var state: State = State.PLAY

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_translation_comparison_option, parent, false)
            return TranslationViewHolder(view)
        }

        override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
            val item = items[position]
            val desiredHeight = rowHeights.getOrElse(position) { 0 }
            holder.bind(item, state, desiredHeight)
            holder.itemView.post {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val measured = holder.itemView.height
                    if (measured > 0) {
                        onHeightMeasured(adapterPosition, measured)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun swapItems(from: Int, to: Int) {
            if (state == State.RESULT) return
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return
            if (from == to) return
            val moved = items.removeAt(from)
            items.add(to, moved)
            notifyItemMoved(from, to)
        }

        fun currentOrder(): List<OptionItem> = items.toList()

        fun showResult(
            correctTranslations: List<String>,
            userTranslations: List<String>,
            correctness: List<Boolean>,
            correctDictionaryIds: List<Int?>
        ) {
            state = State.RESULT
            val newItems = correctTranslations.mapIndexed { index, translation ->
                val userAnswer = userTranslations.getOrNull(index)
                OptionItem(
                    dictionaryId = correctDictionaryIds.getOrNull(index),
                    translation = translation,
                    userTranslation = userAnswer,
                    isCorrect = correctness.getOrElse(index) { false }
                )
            }
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        class TranslationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val translationTextView =
                itemView.findViewById<android.widget.TextView>(R.id.comparisonOptionTranslationTextView)
            private val userAnswerTextView =
                itemView.findViewById<android.widget.TextView>(R.id.comparisonOptionUserAnswerTextView)

            fun bind(item: OptionItem, state: State, desiredHeight: Int) {
                translationTextView.text = item.translation
                val context = itemView.context
                val colorRes = when {
                    state == State.PLAY -> R.color.comparison_bg_neutral
                    item.isCorrect -> R.color.comparison_bg_correct
                    else -> R.color.comparison_bg_incorrect
                }
                itemView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
                itemView.applyRowHeight(desiredHeight)

                if (state == State.RESULT && !item.isCorrect && !item.userTranslation.isNullOrEmpty()) {
                    userAnswerTextView.visibility = View.VISIBLE
                    userAnswerTextView.text =
                        context.getString(R.string.translation_comparison_user_answer, item.userTranslation)
                } else {
                    userAnswerTextView.visibility = View.GONE
                }
            }
        }

        data class OptionItem(
            val dictionaryId: Int?,
            val translation: String,
            val userTranslation: String? = null,
            val isCorrect: Boolean = false
        )

        private enum class State { PLAY, RESULT }
    }

    private enum class HighlightState { NEUTRAL, CORRECT, INCORRECT }

    companion object {
        private const val ARG_CARD_DTO = "translation_comparison_card_dto"

        private const val QUALITY_FAILURE = 0
        private const val QUALITY_SUCCESS = 5

        fun newInstance(dto: TranslationComparisonLessonCardDto): TranslationComparisonCardTypeFragment {
            return TranslationComparisonCardTypeFragment().apply {
                arguments = bundleOf(ARG_CARD_DTO to dto)
            }
        }
    }
}

private fun View.applyRowHeight(height: Int) {
    val targetHeight = if (height > 0) height else ViewGroup.LayoutParams.WRAP_CONTENT
    val currentLayoutParams = layoutParams
    if (currentLayoutParams == null) {
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            targetHeight
        )
    } else if (currentLayoutParams.height != targetHeight) {
        currentLayoutParams.height = targetHeight
        layoutParams = currentLayoutParams
    }
}

private fun RecyclerView.applyHeightToVisibleItem(position: Int, height: Int) {
    val holder = findViewHolderForAdapterPosition(position) ?: return
    holder.itemView.applyRowHeight(height)
}


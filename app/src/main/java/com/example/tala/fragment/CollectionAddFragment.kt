package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.R
import com.example.tala.databinding.FragmentCollectionAddBinding
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordViewModel
import com.example.tala.entity.wordCollection.WordCollection
import com.example.tala.entity.wordCollection.WordCollectionViewModel
import com.example.tala.entity.lessoncardtype.LessonCardType
import com.example.tala.entity.lessoncardtype.LessonCardTypeViewModel
import com.example.tala.fragment.adapter.DictionaryAdapter
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.fragment.model.CardTypeConditionArgs
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionAddFragment : Fragment() {

    private var _binding: FragmentCollectionAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var wordCollectionViewModel: WordCollectionViewModel
    private lateinit var wordViewModel: WordViewModel
    private lateinit var lessonCardTypeViewModel: LessonCardTypeViewModel
    private val wordsAdapter = DictionaryAdapter(
        onItemClick = { _ -> },
        onAddToCollectionClick = null
    )
    private val selectedWords: MutableList<Word> = mutableListOf()
    private val selectedCardTypes: MutableList<CardTypeConditionArgs> = mutableListOf()

    private var collectionId: Int? = null
    private var isCollectionLoaded: Boolean = false
    private var nameDraft: String = ""
    private var descriptionDraft: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID)?.takeIf { it > 0 }

        parentFragmentManager.setFragmentResultListener(SELECT_WORD_REQUEST_KEY, this) { _, bundle ->
            val wordId = bundle.getInt(DictionaryListFragment.RESULT_SELECTED_DICTIONARY_ID, -1)
            if (wordId > 0) {
                onDictionaryChosen(wordId)
            }
        }

        parentFragmentManager.setFragmentResultListener(CollectionCardTypeFragment.RESULT_KEY_CARD_TYPES, this) { _, bundle ->
            val detailed = bundle.getParcelableArrayList<CardTypeConditionArgs>(
                CollectionCardTypeFragment.RESULT_SELECTED_CARD_TYPES_DETAIL
            )
            if (detailed != null) {
                selectedCardTypes.clear()
                selectedCardTypes.addAll(detailed)
            } else {
                val selected = bundle.getStringArrayList(CollectionCardTypeFragment.RESULT_SELECTED_CARD_TYPES)
                    ?.mapNotNull { value ->
                        runCatching { CardTypeEnum.valueOf(value) }.getOrNull()
                    }
                    ?.map { CardTypeConditionArgs(cardType = it, enabled = true) }
                    ?: emptyList()
                selectedCardTypes.clear()
                selectedCardTypes.addAll(selected)
            }
            applyDefaultCardTypesIfEmpty()
            updateCardTypeSummary()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordCollectionViewModel =
            ViewModelProvider(requireActivity())[WordCollectionViewModel::class.java]
        wordViewModel = ViewModelProvider(requireActivity())[WordViewModel::class.java]
        lessonCardTypeViewModel =
            ViewModelProvider(requireActivity())[LessonCardTypeViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupCardTypeCard()
        applyWindowInsets()
        setupInputListeners()

        if (collectionId != null) {
            val targetId = collectionId!!
            if (isCollectionLoaded) {
                applyDraftsToInputs()
                updateWordsList()
                updateCardTypeSummary()
            } else {
                loadCollection(targetId)
            }
        } else {
            applyDefaultCardTypesIfEmpty()
            applyDraftsToInputs()
            updateCardTypeSummary()
            updateWordsList()
        }

    }

    private fun setupToolbar() {
        binding.collectionToolbar.setNavigationIcon(R.drawable.ic_expand_more)
        binding.collectionToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.collectionWordsRecyclerView.adapter = wordsAdapter
    }

    private fun setupButtons() {
        binding.collectionAddWordsButton.setOnClickListener {
            openDictionarySelection()
        }
        binding.collectionSaveButton.setOnClickListener {
            saveCollection()
        }
    }

    private fun setupCardTypeCard() {
        binding.collectionCardTypeCard.setOnClickListener {
            openCardTypeSelection()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.collectionToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun openCardTypeSelection() {
        applyDefaultCardTypesIfEmpty()
        val fragment = CollectionCardTypeFragment.newInstance(selectedCardTypes)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun applyDefaultCardTypesIfEmpty() {
        val available = CardTypeEnum.values().filter { it.use }
        if (selectedCardTypes.isEmpty()) {
            selectedCardTypes.addAll(
                available.map { cardType ->
                    CardTypeConditionArgs(cardType = cardType, enabled = true)
                }
            )
        } else {
            val existingTypes = selectedCardTypes.map { it.cardType }.toSet()
            val missing = available.filterNot { existingTypes.contains(it) }
            selectedCardTypes.addAll(missing.map { CardTypeConditionArgs(cardType = it, enabled = false) })
        }
    }

    private fun updateCardTypeSummary() {
        val binding = _binding ?: return
        val activeTypes = selectedCardTypes.filter { it.enabled }.map { it.cardType }
        val summary = if (activeTypes.isEmpty()) {
            getString(R.string.collection_card_type_empty)
        } else {
            activeTypes
                .sortedBy { it.ordinal }
                .joinToString(separator = ", ") { it.titleRu }
        }
        binding.collectionCardTypeValueTextView.text = summary
    }

    private fun openDictionarySelection() {
        val fragment = DictionaryListFragment.newInstance(
            selectionMode = true,
            selectionResultKey = SELECT_WORD_REQUEST_KEY
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onDictionaryChosen(wordId: Int) {
        if (selectedWords.any { it.id == wordId }) {
            Toast.makeText(
                requireContext(),
                R.string.collection_list_word_already_added,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val dictionary = runCatching { wordViewModel.getById(wordId) }
                .getOrNull()
            if (dictionary != null) {
                selectedWords.add(dictionary)
                updateWordsList()
            }
            setLoading(false)
        }
    }

    private fun loadCollection(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val collectionWithEntries = runCatching {
                wordCollectionViewModel.getByIdWithEntries(id)
            }.getOrNull()

            if (collectionWithEntries != null) {
                nameDraft = collectionWithEntries.collection.name
                descriptionDraft = collectionWithEntries.collection.description.orEmpty()
                applyDraftsToInputs()

                val wordIds = collectionWithEntries.entries.map { it.wordId }
                val dictionaries = if (wordIds.isEmpty()) {
                    emptyList()
                } else {
                    wordViewModel.getByIds(wordIds)
                }
                selectedWords.clear()
                selectedWords.addAll(dictionaries.sortedBy { it.word.lowercase() })

                val cardTypes = lessonCardTypeViewModel.getByCollectionId(id)
                val byType = cardTypes.associateBy { it.cardType }
                val available = CardTypeEnum.values().filter { it.use }
                selectedCardTypes.clear()
                selectedCardTypes.addAll(
                    available.map { cardType ->
                        val stored = byType[cardType]
                        CardTypeConditionArgs(
                            cardType = cardType,
                            enabled = stored != null,
                            conditionOnType = stored?.conditionOnCardType,
                            conditionOnValue = stored?.conditionOnValue,
                            conditionOffType = stored?.conditionOffCardType,
                            conditionOffValue = stored?.conditionOffValue
                        )
                    }
                )
            }

            isCollectionLoaded = true
            updateWordsList()
            applyDefaultCardTypesIfEmpty()
            updateCardTypeSummary()
            setLoading(false)
        }
    }

    private fun updateWordsList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val groups = buildDisplayGroups()
            wordsAdapter.submitGroups(groups)
            binding.collectionEmptyStateTextView.isVisible = groups.isEmpty()
        }
    }

    private suspend fun buildDisplayGroups(): List<DictionaryAdapter.Group> = withContext(Dispatchers.Default) {
        if (selectedWords.isEmpty()) {
            return@withContext emptyList()
        }

        selectedWords
            .groupBy { it.baseWordId ?: it.id }
            .map { (_, dictionaries) ->
                val sortedById = dictionaries.sortedWith(
                    compareBy<Word> { it.id }
                        .thenBy { it.word.lowercase() }
                        .thenBy { it.translation.lowercase() }
                )
                val primary = sortedById.first()
                val words = sortedById.sortedWith(
                    compareBy<Word> { if (it.id == primary.id) 0 else 1 }
                        .thenBy { it.word.lowercase() }
                        .thenBy { it.translation.lowercase() }
                )
                DictionaryAdapter.Group(
                    base = primary,
                    words = words
                )
            }
            .sortedWith(
                compareBy(
                    { it.base.word.lowercase() },
                    { it.base.translation.lowercase() }
                )
            )
    }

    private fun saveCollection() {
        val name = binding.collectionNameEditText.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.collectionNameLayout.error = getString(R.string.collection_list_name_required)
            return
        } else {
            binding.collectionNameLayout.error = null
        }

        val description = binding.collectionDescriptionEditText.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }

        val activeCardTypes = selectedCardTypes
            .filter { it.enabled }
            .map { it.withDefaults() }
        if (activeCardTypes.isEmpty()) {
            Toast.makeText(requireContext(), R.string.collection_card_type_error_required, Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val saved = runCatching {
                val targetCollectionId = collectionId?.let {
                    wordCollectionViewModel.updateSync(
                        WordCollection(id = it, name = name, description = description)
                    )
                    it
                } ?: run {
                    val newId = wordCollectionViewModel.insertSync(
                        WordCollection(name = name, description = description)
                    ).toInt()
                    collectionId = newId
                    newId
                }
                wordCollectionViewModel.replaceCollectionWordsSync(
                    targetCollectionId,
                    selectedWords.map { it.id }.distinct()
                )
                lessonCardTypeViewModel.replaceForCollection(
                    targetCollectionId,
                    activeCardTypes
                        .sortedBy { it.cardType.ordinal }
                        .map { cardType ->
                            LessonCardType(
                                collectionId = targetCollectionId,
                                cardType = cardType.cardType,
                                conditionOnCardType = cardType.conditionOnType,
                                conditionOnValue = cardType.conditionOnValue,
                                conditionOffCardType = cardType.conditionOffType,
                                conditionOffValue = cardType.conditionOffValue
                            )
                        }
                )
            }.isSuccess

            setLoading(false)

            if (saved) {
                Toast.makeText(requireContext(), R.string.collection_list_save_success, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), R.string.collection_list_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.collectionProgressBar.isVisible = isLoading
        binding.collectionSaveButton.isEnabled = !isLoading
        binding.collectionAddWordsButton.isEnabled = !isLoading
        binding.collectionNameLayout.isEnabled = !isLoading
        binding.collectionDescriptionLayout.isEnabled = !isLoading
        binding.collectionCardTypeCard.isEnabled = !isLoading
    }

    private fun setupInputListeners() {
        binding.collectionNameEditText.doAfterTextChanged {
            nameDraft = it?.toString().orEmpty()
        }
        binding.collectionDescriptionEditText.doAfterTextChanged {
            descriptionDraft = it?.toString().orEmpty()
        }
    }

    private fun CardTypeConditionArgs.withDefaults(): CardTypeConditionArgs {
        val onValue = if (conditionOnType != null) conditionOnValue ?: DEFAULT_CONDITION_THRESHOLD else null
        val offValue = if (conditionOffType != null) conditionOffValue ?: DEFAULT_CONDITION_THRESHOLD else null
        return copy(conditionOnValue = onValue, conditionOffValue = offValue)
    }

    private fun applyDraftsToInputs() {
        if (binding.collectionNameEditText.text?.toString() != nameDraft) {
            binding.collectionNameEditText.setText(nameDraft)
        }
        if (binding.collectionDescriptionEditText.text?.toString() != descriptionDraft) {
            binding.collectionDescriptionEditText.setText(descriptionDraft)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        private const val SELECT_WORD_REQUEST_KEY = "collection_select_word"
        private const val DEFAULT_CONDITION_THRESHOLD = 10

        fun newInstance(collectionId: Int? = null): CollectionAddFragment {
            val fragment = CollectionAddFragment()
            fragment.arguments = Bundle().apply {
                collectionId?.let { putInt(ARG_COLLECTION_ID, it) }
            }
            return fragment
        }
    }
}


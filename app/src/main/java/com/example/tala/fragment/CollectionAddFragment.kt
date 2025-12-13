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
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryViewModel
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionViewModel
import com.example.tala.entity.lessoncardtype.LessonCardType
import com.example.tala.entity.lessoncardtype.LessonCardTypeViewModel
import com.example.tala.fragment.adapter.DictionaryAdapter
import com.example.tala.model.enums.CardTypeEnum
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionAddFragment : Fragment() {

    private var _binding: FragmentCollectionAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryCollectionViewModel: DictionaryCollectionViewModel
    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var lessonCardTypeViewModel: LessonCardTypeViewModel
    private val wordsAdapter = DictionaryAdapter(
        onItemClick = { _ -> },
        onAddToCollectionClick = null
    )
    private val selectedDictionaries: MutableList<Dictionary> = mutableListOf()
    private val selectedCardTypes: MutableList<CardTypeEnum> = mutableListOf()

    private var collectionId: Int? = null
    private var isCollectionLoaded: Boolean = false
    private var nameDraft: String = ""
    private var descriptionDraft: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID)?.takeIf { it > 0 }

        parentFragmentManager.setFragmentResultListener(SELECT_WORD_REQUEST_KEY, this) { _, bundle ->
            val dictionaryId = bundle.getInt(DictionaryListFragment.RESULT_SELECTED_DICTIONARY_ID, -1)
            if (dictionaryId > 0) {
                onDictionaryChosen(dictionaryId)
            }
        }

        parentFragmentManager.setFragmentResultListener(CollectionCardTypeFragment.RESULT_KEY_CARD_TYPES, this) { _, bundle ->
            val selected = bundle.getStringArrayList(CollectionCardTypeFragment.RESULT_SELECTED_CARD_TYPES)
                ?.mapNotNull { runCatching { CardTypeEnum.valueOf(it) }.getOrNull() }
                ?: emptyList()
            selectedCardTypes.clear()
            selectedCardTypes.addAll(selected)
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

        dictionaryCollectionViewModel =
            ViewModelProvider(requireActivity())[DictionaryCollectionViewModel::class.java]
        dictionaryViewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
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
        if (selectedCardTypes.isEmpty()) {
            selectedCardTypes.addAll(CardTypeEnum.values().filter { it.use })
        }
    }

    private fun updateCardTypeSummary() {
        val binding = _binding ?: return
        val summary = if (selectedCardTypes.isEmpty()) {
            getString(R.string.collection_card_type_empty)
        } else {
            selectedCardTypes
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

    private fun onDictionaryChosen(dictionaryId: Int) {
        if (selectedDictionaries.any { it.id == dictionaryId }) {
            Toast.makeText(
                requireContext(),
                R.string.collection_list_word_already_added,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val dictionary = runCatching { dictionaryViewModel.getById(dictionaryId) }
                .getOrNull()
            if (dictionary != null) {
                selectedDictionaries.add(dictionary)
                updateWordsList()
            }
            setLoading(false)
        }
    }

    private fun loadCollection(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val collectionWithEntries = runCatching {
                dictionaryCollectionViewModel.getByIdWithEntries(id)
            }.getOrNull()

            if (collectionWithEntries != null) {
                nameDraft = collectionWithEntries.collection.name
                descriptionDraft = collectionWithEntries.collection.description.orEmpty()
                applyDraftsToInputs()

                val dictionaryIds = collectionWithEntries.entries.map { it.dictionaryId }
                val dictionaries = if (dictionaryIds.isEmpty()) {
                    emptyList()
                } else {
                    dictionaryViewModel.getByIds(dictionaryIds)
                }
                selectedDictionaries.clear()
                selectedDictionaries.addAll(dictionaries.sortedBy { it.word.lowercase() })

                val cardTypes = lessonCardTypeViewModel.getByCollectionId(id)
                selectedCardTypes.clear()
                selectedCardTypes.addAll(cardTypes.map { it.cardType })
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
        if (selectedDictionaries.isEmpty()) {
            return@withContext emptyList()
        }

        selectedDictionaries
            .groupBy { it.baseWordId ?: it.id }
            .map { (_, dictionaries) ->
                val sortedById = dictionaries.sortedWith(
                    compareBy<Dictionary> { it.id }
                        .thenBy { it.word.lowercase() }
                        .thenBy { it.translation.lowercase() }
                )
                val primary = sortedById.first()
                val words = sortedById.sortedWith(
                    compareBy<Dictionary> { if (it.id == primary.id) 0 else 1 }
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

        if (selectedCardTypes.isEmpty()) {
            Toast.makeText(requireContext(), R.string.collection_card_type_error_required, Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val saved = runCatching {
                val targetCollectionId = collectionId?.let {
                    dictionaryCollectionViewModel.updateSync(
                        DictionaryCollection(id = it, name = name, description = description)
                    )
                    it
                } ?: run {
                    val newId = dictionaryCollectionViewModel.insertSync(
                        DictionaryCollection(name = name, description = description)
                    ).toInt()
                    collectionId = newId
                    newId
                }
                dictionaryCollectionViewModel.replaceCollectionDictionariesSync(
                    targetCollectionId,
                    selectedDictionaries.map { it.id }.distinct()
                )
                lessonCardTypeViewModel.replaceForCollection(
                    targetCollectionId,
                    selectedCardTypes
                        .sortedBy { it.ordinal }
                        .map { cardType ->
                        LessonCardType(collectionId = targetCollectionId, cardType = cardType)
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

        fun newInstance(collectionId: Int? = null): CollectionAddFragment {
            val fragment = CollectionAddFragment()
            fragment.arguments = Bundle().apply {
                collectionId?.let { putInt(ARG_COLLECTION_ID, it) }
            }
            return fragment
        }
    }
}


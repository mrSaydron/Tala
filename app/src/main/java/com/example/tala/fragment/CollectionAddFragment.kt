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
import com.example.tala.fragment.adapter.CollectionWordsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionAddFragment : Fragment() {

    private var _binding: FragmentCollectionAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryCollectionViewModel: DictionaryCollectionViewModel
    private lateinit var dictionaryViewModel: DictionaryViewModel
    private val wordsAdapter = CollectionWordsAdapter()
    private val selectedDictionaries: MutableList<Dictionary> = mutableListOf()

    private var collectionId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID)?.takeIf { it > 0 }

        parentFragmentManager.setFragmentResultListener(SELECT_WORD_REQUEST_KEY, this) { _, bundle ->
            val dictionaryId = bundle.getInt(DictionaryListFragment.RESULT_SELECTED_DICTIONARY_ID, -1)
            if (dictionaryId > 0) {
                onDictionaryChosen(dictionaryId)
            }
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

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        applyWindowInsets()

        collectionId?.let { loadCollection(it) } ?: updateWordsList()
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

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.collectionToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            WindowInsetsCompat.CONSUMED
        }
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
                binding.collectionNameEditText.setText(collectionWithEntries.collection.name)
                binding.collectionDescriptionEditText.setText(
                    collectionWithEntries.collection.description.orEmpty()
                )

                val dictionaryIds = collectionWithEntries.entries.map { it.dictionaryId }
                val dictionaries = if (dictionaryIds.isEmpty()) {
                    emptyList()
                } else {
                    dictionaryViewModel.getByIds(dictionaryIds)
                }
                selectedDictionaries.clear()
                selectedDictionaries.addAll(dictionaries.sortedBy { it.word.lowercase() })
            }

            updateWordsList()
            setLoading(false)
        }
    }

    private fun updateWordsList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = buildListItems()
            wordsAdapter.submitItems(items)
            binding.collectionEmptyStateTextView.isVisible = selectedDictionaries.isEmpty()
        }
    }

    private suspend fun buildListItems(): List<CollectionWordsAdapter.Item> = withContext(Dispatchers.Default) {
        val items = mutableListOf<CollectionWordsAdapter.Item>()
        if (selectedDictionaries.isEmpty()) {
            return@withContext items
        }

        val entriesById = selectedDictionaries.associateBy { it.id }
        val groups = selectedDictionaries.groupBy { it.baseWordId ?: it.id }
        val missingBaseIds = groups.keys.filter { key -> entriesById[key] == null }
        val baseEntries = if (missingBaseIds.isNotEmpty()) {
            dictionaryViewModel.getByIds(missingBaseIds)
        } else {
            emptyList()
        }.associateBy { it.id }

        val sortedGroups = groups.entries.sortedBy { entry ->
            val base = entriesById[entry.key] ?: baseEntries[entry.key] ?: entry.value.first()
            base.word.lowercase()
        }

        sortedGroups.forEach { (baseId, dictionaries) ->
            val baseEntry = entriesById[baseId] ?: baseEntries[baseId] ?: dictionaries.first()
            items += CollectionWordsAdapter.Item.Header(
                title = baseEntry.word,
                subtitle = baseEntry.translation.takeIf { it.isNotBlank() }
            )
            dictionaries
                .sortedWith(compareBy({ it.word.lowercase() }, { it.translation.lowercase() }))
                .forEach { dictionary ->
                    items += CollectionWordsAdapter.Item.Word(dictionary)
                }
        }
        items
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


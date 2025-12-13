package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.R
import com.example.tala.databinding.FragmentDictionaryListBinding
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryViewModel
import com.example.tala.fragment.adapter.DictionaryAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryListFragment : Fragment() {

    private var _binding: FragmentDictionaryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var dictionaryAdapter: DictionaryAdapter
    private var selectionMode: Boolean = false
    private var selectionResultKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectionMode = arguments?.getBoolean(ARG_SELECTION_MODE, false) ?: false
        selectionResultKey = arguments?.getString(ARG_SELECTION_RESULT_KEY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dictionaryViewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]

        dictionaryAdapter = DictionaryAdapter(
            onItemClick = { entry ->
                openDictionaryEntry(entry.id)
            },
            onAddToCollectionClick = if (selectionMode && !selectionResultKey.isNullOrEmpty()) {
                { entry -> onAddDictionaryToCollection(entry.id) }
            } else {
                null
            }
        )

        binding.dictionaryRecyclerView.adapter = dictionaryAdapter
        binding.dictionaryRecyclerView.itemAnimator = null

        binding.dictionaryAddButton.setOnClickListener {
            openDictionaryEntry(null)
        }

        if (selectionMode) {
            binding.dictionaryTitleTextView.setText(R.string.dictionary_list_title_selection)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, topInset, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        loadDictionaryEntries()
    }

    private fun loadDictionaryEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.dictionaryProgressBar.isVisible = true
            binding.dictionaryEmptyStateTextView.isVisible = false

            runCatching {
                val entries = dictionaryViewModel.getAll()
                buildGroupedEntries(entries)
            }.onSuccess { groups ->
                dictionaryAdapter.submitGroups(groups)
                binding.dictionaryRecyclerView.isVisible = groups.isNotEmpty()
                binding.dictionaryEmptyStateTextView.isVisible = groups.isEmpty()
                if (groups.isEmpty()) {
                    binding.dictionaryEmptyStateTextView.text = getString(R.string.dictionary_empty_state)
                } 
            }.onFailure {
                dictionaryAdapter.submitGroups(emptyList())
                binding.dictionaryRecyclerView.isVisible = false
                binding.dictionaryEmptyStateTextView.isVisible = true
                binding.dictionaryEmptyStateTextView.text = getString(R.string.dictionary_error_state)
            }

            binding.dictionaryProgressBar.isVisible = false
        }
    }

    private fun openDictionaryEntry(entryId: Int?) {
        val fragment = DictionaryAddFragment.newInstance(entryId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SELECTION_MODE = "dictionary_selection_mode"
        private const val ARG_SELECTION_RESULT_KEY = "dictionary_selection_result_key"
        const val RESULT_SELECTED_DICTIONARY_ID = "dictionary_selected_id"

        fun newInstance(
            selectionMode: Boolean = false,
            selectionResultKey: String? = null
        ): DictionaryListFragment {
            val fragment = DictionaryListFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(ARG_SELECTION_MODE, selectionMode)
                selectionResultKey?.let { putString(ARG_SELECTION_RESULT_KEY, it) }
            }
            return fragment
        }
    }

    private fun onAddDictionaryToCollection(dictionaryId: Int) {
        val resultKey = selectionResultKey
        if (!resultKey.isNullOrEmpty()) {
            parentFragmentManager.setFragmentResult(
                resultKey,
                bundleOf(RESULT_SELECTED_DICTIONARY_ID to dictionaryId)
            )
            parentFragmentManager.popBackStack()
        }
    }

    private suspend fun buildGroupedEntries(
        entries: List<Dictionary>
    ): List<DictionaryAdapter.Group> = withContext(Dispatchers.Default) {
        if (entries.isEmpty()) {
            return@withContext emptyList()
        }

        val entriesById = entries.associateBy { it.id }
        val groupedEntries = entries.groupBy { dictionary ->
            dictionary.baseWordId ?: dictionary.id
        }

        val missingBaseIds = groupedEntries.keys.filter { baseId ->
            entriesById[baseId] == null
        }

        val missingBaseEntries = if (missingBaseIds.isNotEmpty()) {
            dictionaryViewModel.getByIds(missingBaseIds)
        } else {
            emptyList()
        }.associateBy { it.id }

        val baseEntries = entriesById + missingBaseEntries

        groupedEntries.entries
            .map { (baseId, groupEntries) ->
                val baseEntry = baseEntries[baseId] ?: groupEntries.first()
                val allWords = (groupEntries + baseEntry).distinctBy { it.id }

                val sortedWords = allWords.sortedWith(
                    compareBy<Dictionary> { if (it.id == baseEntry.id) 0 else 1 }
                        .thenBy { it.word.lowercase() }
                        .thenBy { it.translation.lowercase() }
                )

                DictionaryAdapter.Group(
                    base = baseEntry,
                    words = sortedWords
                )
            }
            .sortedWith(
                compareBy(
                    { it.base.word.lowercase() },
                    { it.base.translation.lowercase() }
                )
            )
    }
}


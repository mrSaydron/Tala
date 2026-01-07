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
import com.example.tala.databinding.FragmentWordListBinding
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordViewModel
import com.example.tala.fragment.adapter.WordAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordListFragment : Fragment() {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private lateinit var wordViewModel: WordViewModel
    private lateinit var wordAdapter: WordAdapter
    private var selectionMode: Boolean = false
    private var selectionResultKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectionMode = arguments?.getBoolean(ARG_SELECTION_MODE, false) ?: false
        selectionResultKey = arguments?.getString(ARG_SELECTION_RESULT_KEY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordViewModel = ViewModelProvider(requireActivity())[WordViewModel::class.java]

        wordAdapter = WordAdapter(
            onItemClick = { entry ->
                openWordEntry(entry.id)
            },
            onAddToCollectionClick = if (selectionMode && !selectionResultKey.isNullOrEmpty()) {
                { entry -> onAddWordToCollection(entry.id) }
            } else {
                null
            }
        )

        binding.wordRecyclerView.adapter = wordAdapter
        binding.wordRecyclerView.itemAnimator = null

        binding.wordAddButton.setOnClickListener {
            openWordEntry(null)
        }

        if (selectionMode) {
            binding.wordTitleTextView.setText(R.string.word_list_title_selection)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, topInset, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        loadWordEntries()
    }

    private fun loadWordEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.wordProgressBar.isVisible = true
            binding.wordEmptyStateTextView.isVisible = false

            runCatching {
                val entries = wordViewModel.getAll()
                buildGroupedEntries(entries)
            }.onSuccess { groups ->
                wordAdapter.submitGroups(groups)
                binding.wordRecyclerView.isVisible = groups.isNotEmpty()
                binding.wordEmptyStateTextView.isVisible = groups.isEmpty()
                if (groups.isEmpty()) {
                    binding.wordEmptyStateTextView.text = getString(R.string.word_empty_state)
                } 
            }.onFailure {
                wordAdapter.submitGroups(emptyList())
                binding.wordRecyclerView.isVisible = false
                binding.wordEmptyStateTextView.isVisible = true
                binding.wordEmptyStateTextView.text = getString(R.string.word_error_state)
            }

            binding.wordProgressBar.isVisible = false
        }
    }

    private fun openWordEntry(entryId: Int?) {
        val fragment = WordAddFragment.newInstance(entryId)
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
        private const val ARG_SELECTION_MODE = "word_selection_mode"
        private const val ARG_SELECTION_RESULT_KEY = "word_selection_result_key"
        const val RESULT_SELECTED_WORD_ID = "word_selected_id"

        fun newInstance(
            selectionMode: Boolean = false,
            selectionResultKey: String? = null
        ): WordListFragment {
            val fragment = WordListFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(ARG_SELECTION_MODE, selectionMode)
                selectionResultKey?.let { putString(ARG_SELECTION_RESULT_KEY, it) }
            }
            return fragment
        }
    }

    private fun onAddWordToCollection(wordId: Int) {
        val resultKey = selectionResultKey
        if (!resultKey.isNullOrEmpty()) {
            parentFragmentManager.setFragmentResult(
                resultKey,
                bundleOf(RESULT_SELECTED_WORD_ID to wordId)
            )
            parentFragmentManager.popBackStack()
        }
    }

    private suspend fun buildGroupedEntries(
        entries: List<Word>
    ): List<WordAdapter.Group> = withContext(Dispatchers.Default) {
        if (entries.isEmpty()) {
            return@withContext emptyList()
        }

        val entriesById = entries.associateBy { it.id }
        val groupedEntries = entries.groupBy { word ->
            word.baseWordId ?: word.id
        }

        val missingBaseIds = groupedEntries.keys.filter { baseId ->
            entriesById[baseId] == null
        }

        val missingBaseEntries = if (missingBaseIds.isNotEmpty()) {
            wordViewModel.getByIds(missingBaseIds)
        } else {
            emptyList()
        }.associateBy { it.id }

        val baseEntries = entriesById + missingBaseEntries

        groupedEntries.entries
            .map { (baseId, groupEntries) ->
                val baseEntry = baseEntries[baseId] ?: groupEntries.first()
                val allWords = (groupEntries + baseEntry).distinctBy { it.id }

                val sortedWords = allWords.sortedWith(
                    compareBy<Word> { if (it.id == baseEntry.id) 0 else 1 }
                        .thenBy { it.word.lowercase() }
                        .thenBy { it.translation.lowercase() }
                )

                WordAdapter.Group(
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


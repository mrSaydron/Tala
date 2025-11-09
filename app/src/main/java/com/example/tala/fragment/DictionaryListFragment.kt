package com.example.tala.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.R
import com.example.tala.databinding.FragmentDictionaryListBinding
import com.example.tala.entity.dictionary.DictionaryViewModel
import com.example.tala.fragment.adapter.DictionaryAdapter
import kotlinx.coroutines.launch

class DictionaryListFragment : Fragment() {

    private var _binding: FragmentDictionaryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var dictionaryAdapter: DictionaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dictionaryViewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]

        dictionaryAdapter = DictionaryAdapter { entry ->
            openDictionaryEntry(entry.id)
        }

        binding.dictionaryRecyclerView.adapter = dictionaryAdapter
        binding.dictionaryRecyclerView.itemAnimator = null

        binding.dictionaryAddButton.setOnClickListener {
            openDictionaryEntry(null)
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
                dictionaryViewModel.getAll()
            }.onSuccess { entries ->
                val baseEntries = entries.filter { entry ->
                    entry.baseWordId == null || entry.baseWordId == entry.id
                }
                val sortedEntries = baseEntries.sortedWith(compareBy({ it.word.lowercase() }, { it.translation.lowercase() }))
                dictionaryAdapter.submitList(sortedEntries)
                binding.dictionaryRecyclerView.isVisible = sortedEntries.isNotEmpty()
                binding.dictionaryEmptyStateTextView.isVisible = sortedEntries.isEmpty()
                if (sortedEntries.isEmpty()) {
                    binding.dictionaryEmptyStateTextView.text = getString(R.string.dictionary_empty_state)
                }
            }.onFailure { error ->
                Log.e(TAG, "loadDictionaryEntries", error)
                dictionaryAdapter.submitList(emptyList())
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
        private const val TAG = "DictionaryListFragment"
    }
}


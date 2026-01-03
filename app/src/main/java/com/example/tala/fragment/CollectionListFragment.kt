package com.example.tala.fragment

import android.os.Bundle
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
import com.example.tala.databinding.FragmentCollectionListBinding
import com.example.tala.entity.wordCollection.WordCollectionViewModel
import com.example.tala.fragment.adapter.CollectionListAdapter
import kotlinx.coroutines.launch

class CollectionListFragment : Fragment() {

    private var _binding: FragmentCollectionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var collectionViewModel: WordCollectionViewModel
    private lateinit var collectionAdapter: CollectionListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionViewModel = ViewModelProvider(requireActivity())[WordCollectionViewModel::class.java]

        collectionAdapter = CollectionListAdapter { collection ->
            openCollection(collection.id)
        }

        binding.collectionRecyclerView.adapter = collectionAdapter
        binding.collectionRecyclerView.itemAnimator = null

        binding.collectionAddButton.setOnClickListener {
            openCollection(null)
        }

        val initialPaddingLeft = binding.root.paddingLeft
        val initialPaddingTop = binding.root.paddingTop
        val initialPaddingRight = binding.root.paddingRight
        val initialPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft,
                initialPaddingTop + systemBars.top,
                initialPaddingRight,
                initialPaddingBottom + systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
        loadCollections()
    }

    private fun loadCollections() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            binding.collectionListEmptyState.isVisible = false

            runCatching {
                collectionViewModel.getAll()
            }.onSuccess { collections ->
                val sorted = collections.sortedBy { it.name.lowercase() }
                collectionAdapter.submitList(sorted)
                binding.collectionRecyclerView.isVisible = sorted.isNotEmpty()
                binding.collectionListEmptyState.isVisible = sorted.isEmpty()
            }.onFailure {
                collectionAdapter.submitList(emptyList())
                binding.collectionRecyclerView.isVisible = false
                binding.collectionListEmptyState.isVisible = true
                binding.collectionListEmptyState.text = getString(R.string.collection_overview_error_state)
            }

            setLoading(false)
        }
    }

    private fun openCollection(collectionId: Int?) {
        val fragment = CollectionAddFragment.newInstance(collectionId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.collectionListProgressBar.isVisible = isLoading
        binding.collectionAddButton.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


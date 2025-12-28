package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.tala.R
import com.example.tala.databinding.FragmentCollectionCardTypeBinding
import com.example.tala.fragment.adapter.CollectionCardTypeSelectionAdapter
import com.example.tala.fragment.model.CardTypeConditionArgs
import com.example.tala.model.enums.CardTypeEnum

class CollectionCardTypeFragment : Fragment() {

    private var _binding: FragmentCollectionCardTypeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CollectionCardTypeSelectionAdapter
    private val state: MutableMap<CardTypeEnum, CardTypeConditionArgs> = mutableMapOf()
    private val availableCardTypes: List<CardTypeEnum> =
        CardTypeEnum.values().filter { it.use }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = arguments?.getParcelableArrayList<CardTypeConditionArgs>(ARG_SELECTED_CARD_TYPES).orEmpty()
        val initialByType = initial.associateBy { it.cardType }

        availableCardTypes.forEach { type ->
            val existing = initialByType[type]
            state[type] = existing ?: CardTypeConditionArgs(
                cardType = type,
                enabled = false
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecycler()
        setupButtons()
        applyWindowInsets()
        updateEmptyState()
    }

    private fun setupToolbar() {
        binding.collectionCardTypeToolbar.apply {
            title = getString(R.string.collection_card_type_title)
            setNavigationIcon(R.drawable.ic_expand_more)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupRecycler() {
        adapter = CollectionCardTypeSelectionAdapter(
            availableCardTypes = availableCardTypes,
            activeTypesProvider = { activeTypes() },
            onToggle = { type, isChecked ->
                val current = state[type] ?: CardTypeConditionArgs(cardType = type)
                state[type] = current.copy(enabled = isChecked)
                refreshList()
            },
            onUpdate = { updated ->
                val current = state[updated.cardType] ?: updated
                // preserve enabled flag from current state
                state[updated.cardType] = updated.copy(enabled = current.enabled)
            }
        )
        binding.collectionCardTypeRecyclerView.adapter = adapter
        refreshList()
    }

    private fun setupButtons() {
        binding.collectionCardTypeSaveButton.setOnClickListener {
            handleSave()
        }
    }

    private fun handleSave() {
        val selected = state.values.filter { it.enabled }
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.collection_card_type_error_required, Toast.LENGTH_SHORT).show()
            return
        }

        parentFragmentManager.setFragmentResult(
            RESULT_KEY_CARD_TYPES,
            bundleOf(
                RESULT_SELECTED_CARD_TYPES to ArrayList(
                    selected.sortedBy { it.cardType.ordinal }.map { it.cardType.name }
                ),
                RESULT_SELECTED_CARD_TYPES_DETAIL to ArrayList(selected.sortedBy { it.cardType.ordinal })
            )
        )
        parentFragmentManager.popBackStack()
    }

    private fun applyWindowInsets() {
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

    private fun updateEmptyState() {
        val isEmpty = availableCardTypes.isEmpty()
        binding.collectionCardTypeRecyclerView.isVisible = !isEmpty
        binding.collectionCardTypeEmptyStateTextView.isVisible = isEmpty
    }

    private fun refreshList() {
        adapter.submit(state.values.toList())
    }

    private fun activeTypes(): List<CardTypeEnum> =
        state.values.filter { it.enabled }.map { it.cardType }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SELECTED_CARD_TYPES = "arg_selected_card_types"

        const val RESULT_KEY_CARD_TYPES = "collection_card_types_result"
        const val RESULT_SELECTED_CARD_TYPES = "collection_card_types_selected"
        const val RESULT_SELECTED_CARD_TYPES_DETAIL = "collection_card_types_selected_detail"

        fun newInstance(selectedCardTypes: List<CardTypeConditionArgs>): CollectionCardTypeFragment {
            return CollectionCardTypeFragment().apply {
                arguments = bundleOf(
                    ARG_SELECTED_CARD_TYPES to ArrayList(selectedCardTypes)
                )
            }
        }
    }
}



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
import com.example.tala.databinding.FragmentLessonAddBinding
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionViewModel
import com.example.tala.entity.lesson.Lesson
import com.example.tala.entity.lesson.LessonViewModel
import com.example.tala.entity.lessoncardtype.LessonCardTypeViewModel
import com.example.tala.fragment.adapter.LessonCardTypeAdapter
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonAddFragment : Fragment() {

    private var _binding: FragmentLessonAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var lessonViewModel: LessonViewModel
    private lateinit var lessonCardTypeViewModel: LessonCardTypeViewModel
    private lateinit var dictionaryCollectionViewModel: DictionaryCollectionViewModel

    private lateinit var adapter: LessonCardTypeAdapter
    private var collectionCardTypes: List<CardTypeEnum> = emptyList()

    private var collections: List<DictionaryCollection> = emptyList()
    private var selectedCollection: DictionaryCollection? = null
        set(value) {
            field = value
            updateCollectionState()
            if (value == null) {
                collectionCardTypes = emptyList()
                updateCardTypeState()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lessonViewModel = ViewModelProvider(requireActivity())[LessonViewModel::class.java]
        lessonCardTypeViewModel = ViewModelProvider(requireActivity())[LessonCardTypeViewModel::class.java]
        dictionaryCollectionViewModel =
            ViewModelProvider(requireActivity())[DictionaryCollectionViewModel::class.java]

        setupToolbar()
        setupRecycler()
        setupButtons()
        applyWindowInsets()

        loadCollections()
        updateCardTypeState()
        updateCollectionState()
    }

    private fun setupToolbar() {
        binding.lessonAddToolbar.setNavigationIcon(R.drawable.ic_expand_more)
        binding.lessonAddToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecycler() {
        adapter = LessonCardTypeAdapter()
        binding.lessonCardTypesRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.lessonSaveButton.setOnClickListener {
            saveLesson()
        }
        binding.lessonCollectionCard.setOnClickListener {
            showCollectionSelectionDialog()
        }
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

    private fun loadCollections() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val loaded = runCatching {
                dictionaryCollectionViewModel.getAll()
            }.getOrElse {
                emptyList()
            }
            collections = loaded
            setLoading(false)
            updateCollectionState()
            updateCardTypeState()
        }
    }

    private fun loadCollectionCardTypes(collectionId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val types = runCatching {
                lessonCardTypeViewModel.getByCollectionId(collectionId)
            }.getOrElse { emptyList() }
            collectionCardTypes = types.map { it.cardType }.sortedBy { it.ordinal }
            updateCardTypeState()
        }
    }

    private fun showCollectionSelectionDialog() {
        if (collections.isEmpty()) {
            Toast.makeText(requireContext(), R.string.lesson_add_no_collections, Toast.LENGTH_SHORT).show()
            return
        }
        val names = collections.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.lesson_add_select_collection_title)
            .setItems(names) { dialog, which ->
                val collection = collections[which]
                selectedCollection = collection
                collectionCardTypes = emptyList()
                updateCardTypeState()
                loadCollectionCardTypes(collection.id)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateCardTypeState() {
        val binding = _binding ?: return
        adapter.setItems(collectionCardTypes)
        binding.lessonCardTypeEmptyState.isVisible = collectionCardTypes.isEmpty()
    }

    private fun updateCollectionState() {
        val binding = _binding ?: return
        val name = selectedCollection?.name
        binding.lessonCollectionValueTextView.text = name ?: getString(R.string.lesson_add_collection_placeholder)
    }

    private fun saveLesson() {
        val name = binding.lessonNameEditText.text?.toString()?.trim().orEmpty()
        val fullNameRaw = binding.lessonFullNameEditText.text?.toString()?.trim()
        val fullName = if (fullNameRaw.isNullOrBlank()) name else fullNameRaw

        var isValid = true

        if (name.isBlank()) {
            binding.lessonNameLayout.error = getString(R.string.lesson_add_error_name_required)
            isValid = false
        } else {
            binding.lessonNameLayout.error = null
        }

        if (selectedCollection == null) {
            Toast.makeText(requireContext(), R.string.lesson_add_error_collection_required, Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (collectionCardTypes.isEmpty()) {
            Toast.makeText(requireContext(), R.string.lesson_add_error_card_types_required, Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val collection = selectedCollection
        if (!isValid || collection == null) {
            return
        }

        val cardService = MainActivity.lessonCardService

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val success = runCatching {
                withContext(Dispatchers.IO) {
                    val lessonId = lessonViewModel.insertSync(
                        Lesson(
                            name = name,
                            fullName = fullName,
                            collectionId = collection.id
                        )
                    ).toInt()
                    cardService.createProgress(lessonId)
                }
            }.isSuccess
            setLoading(false)
            if (success) {
                Toast.makeText(requireContext(), R.string.lesson_add_success, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), R.string.lesson_add_error_save, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.lessonAddProgressBar.isVisible = isLoading
        binding.lessonSaveButton.isEnabled = !isLoading
        binding.lessonNameLayout.isEnabled = !isLoading
        binding.lessonFullNameLayout.isEnabled = !isLoading
        binding.lessonCollectionCard.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


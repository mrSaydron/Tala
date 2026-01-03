package com.example.tala.fragment

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tala.R
import com.example.tala.databinding.FragmentWordAddBinding
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.DictionaryLevel
import com.example.tala.entity.word.WordViewModel
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.fragment.adapter.DictionaryEditGroup
import com.example.tala.fragment.adapter.DictionaryEditItem
import com.example.tala.fragment.adapter.DictionaryEditorAdapter
import com.example.tala.fragment.adapter.DictionaryEditorAdapter.DictionaryGroupPayload
import com.example.tala.fragment.dialog.ImagePickerDialog
import com.example.tala.integration.picture.ImageRepository
import com.example.tala.service.DictionarySearchProvider
import com.example.tala.service.GeminiDictionarySearchProvider
import com.example.tala.service.YandexDictionarySearchProvider
import com.example.tala.util.ImageStorage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

private data class ImageTarget(val groupIndex: Int, val itemIndex: Int)

class DictionaryAddFragment : Fragment() {

    private var _binding: FragmentWordAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryViewModel: WordViewModel
    private lateinit var groupAdapter: DictionaryEditorAdapter

    private val groups: MutableList<DictionaryEditGroup> = mutableListOf()

    private val dictionarySearchProvider: DictionarySearchProvider =
        GeminiDictionarySearchProvider(fallbackProvider = YandexDictionarySearchProvider())
    private val partOfSpeechItems = PartOfSpeech.values().toList()
    private val levelItems = listOf<DictionaryLevel?>(null) + DictionaryLevel.values().toList()

    private var entryId: Int? = null
    private var existingEntry: Word? = null
    private val imageRepo by lazy { ImageRepository() }
    private var pendingPickerTarget: ImageTarget? = null
    private var pendingCropTarget: ImageTarget? = null

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                pendingCropTarget = null
                return@registerForActivityResult
            }
            val target = pendingCropTarget ?: return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val outputUri = UCrop.getOutput(data) ?: return@registerForActivityResult
            applyImage(target, outputUri)
            pendingCropTarget = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryId = arguments?.getInt(ARG_ENTRY_ID)?.takeIf { it != 0 }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dictionaryViewModel = ViewModelProvider(requireActivity())[WordViewModel::class.java]

        setupToolbar()
        setupRecycler()
        setupButtons()
        setupImagePickerListener()

        if (entryId != null) {
            binding.wordSearchContainer.isVisible = false
            binding.addGroupButton.isVisible = false
            binding.saveWordButton.text = getString(R.string.dictionary_update_button)
            loadEntry(entryId!!)
        } else {
            binding.deleteWordButton.isVisible = false
            addGroup()
        }

        updateEmptyState()
    }

    private fun setupToolbar() {
        binding.wordToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecycler() {
        groupAdapter = DictionaryEditorAdapter(
            partOfSpeechItems = partOfSpeechItems,
            levelItems = levelItems,
            listener = object : DictionaryEditorAdapter.Listener {
                override fun onToggleGroup(groupIndex: Int) {
                    val group = groups.getOrNull(groupIndex) ?: return
                    group.isExpanded = !group.isExpanded
                    refreshGroups()
                }

                override fun onRemoveGroup(groupIndex: Int) {
                    if (groupIndex !in groups.indices) return
                    groups.removeAt(groupIndex)
                    refreshGroups()
                }

                override fun onAddWord(groupIndex: Int) {
                    val group = groups.getOrNull(groupIndex) ?: return
                    group.items.add(
                        DictionaryEditItem(
                            baseWordId = group.baseWordId,
                            level = group.level
                        )
                    )
                    refreshGroups()
                    scrollToGroup(groupIndex)
                }

                override fun onRemoveWord(groupIndex: Int, itemIndex: Int) {
                    val group = groups.getOrNull(groupIndex) ?: return
                    if (group.items.size == 1 || itemIndex !in group.items.indices) return
                    group.items.removeAt(itemIndex)
                    refreshGroups()
                }

                override fun onSelectImage(groupIndex: Int, itemIndex: Int) {
                    openImagePickerForItem(groupIndex, itemIndex)
                }

                override fun onRemoveImage(groupIndex: Int, itemIndex: Int) {
                    updateItemImage(groupIndex, itemIndex, null)
                }
            }
        )
        binding.wordGroupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.wordGroupsRecyclerView.adapter = groupAdapter
        binding.wordGroupsRecyclerView.itemAnimator = null
    }

    private fun setupButtons() {
        binding.wordSearchButton.setOnClickListener { handleSearch() }
        binding.addGroupButton.setOnClickListener { addGroup() }
        binding.saveWordButton.setOnClickListener { handleSave() }
        binding.deleteWordButton.setOnClickListener { handleDelete() }
    }

    private fun loadEntry(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val group = dictionaryViewModel.getGroupByEntryId(id)
            if (group.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.dictionary_error_not_found), Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            populateEntry(group)
        }
    }

    private fun populateEntry(entries: List<Word>) {
        if (entries.isEmpty()) return
        existingEntry = entries.first()
        binding.deleteWordButton.isVisible = true

        groups.clear()
        val first = entries.first()
        groups += DictionaryEditGroup(
            baseWordId = first.baseWordId ?: first.id,
            level = first.level,
            items = entries.map { DictionaryEditItem.fromWord(it) }.toMutableList(),
            isExpanded = true
        )
        refreshGroups()
    }

    private fun handleSave() {
        val groupPayloads = groupAdapter.validateAndBuildGroups() ?: return

        if (groupPayloads.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.dictionary_empty_results), Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            try {
                if (existingEntry == null) {
                    saveNewGroups(groupPayloads)
                    Toast.makeText(requireContext(), getString(R.string.dictionary_save_success), Toast.LENGTH_SHORT).show()
                } else {
                    updateExistingGroups(groupPayloads)
                    Toast.makeText(requireContext(), getString(R.string.dictionary_update_success), Toast.LENGTH_SHORT).show()
                }
                parentFragmentManager.popBackStack()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun saveNewGroups(groups: List<DictionaryGroupPayload>) {
        groups.forEach { payload ->
            if (payload.dictionaries.isEmpty()) return@forEach

            val normalized = payload.dictionaries.mapIndexed { index, dictionary ->
                if (index == 0) {
                    dictionary.copy(id = 0, baseWordId = null)
                } else {
                    dictionary.copy(id = 0, baseWordId = null)
                }
            }

            val baseEntry = normalized.first()
            val insertedId = dictionaryViewModel.insertSync(baseEntry)
            val baseId = insertedId.toInt()

            dictionaryViewModel.updateSync(baseEntry.copy(id = baseId, baseWordId = baseId))

            normalized.drop(1).forEach { entry ->
                dictionaryViewModel.insertSync(entry.copy(id = 0, baseWordId = baseId))
            }
        }
    }

    private suspend fun updateExistingGroups(groups: List<DictionaryGroupPayload>) {
        val baseId = existingEntry?.baseWordId ?: existingEntry?.id ?: return

        val (existingGroup, newGroups) = groups.partition { payload ->
            payload.baseWordId == baseId || payload.dictionaries.any { it.id == existingEntry?.id }
        }

        existingGroup.firstOrNull()?.let { payload ->
            payload.dictionaries.forEach { entry ->
                val normalized = entry.copy(baseWordId = baseId)
                when {
                    normalized.id == existingEntry?.id -> dictionaryViewModel.updateSync(normalized)
                    normalized.id == 0 -> dictionaryViewModel.insertSync(normalized.copy(baseWordId = baseId))
                    else -> dictionaryViewModel.updateSync(normalized)
                }
            }

            val payloadIds = payload.dictionaries.mapNotNull { it.id.takeIf { id -> id != 0 } }.toSet()
            val currentEntries = dictionaryViewModel.getGroupByBaseId(baseId)
            currentEntries
                .filter { it.id !in payloadIds }
                .forEach { dictionaryViewModel.delete(it) }
        }

        val newPayloads = newGroups.filter { it.dictionaries.isNotEmpty() }
        if (newPayloads.isNotEmpty()) {
            saveNewGroups(newPayloads)
        }
    }

    private fun setupImagePickerListener() {
        parentFragmentManager.setFragmentResultListener(
            ImagePickerDialog.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val target = pendingPickerTarget ?: return@setFragmentResultListener
            val selection = bundle.getString(ImagePickerDialog.KEY_URL_OR_URI).orEmpty()
            if (selection.isBlank()) {
                pendingPickerTarget = null
                return@setFragmentResultListener
            }
            handleSelectedImage(target, selection)
        }
    }

    private fun openImagePickerForItem(groupIndex: Int, itemIndex: Int) {
        pendingPickerTarget = ImageTarget(groupIndex, itemIndex)
        val item = groups
            .getOrNull(groupIndex)
            ?.items
            ?.getOrNull(itemIndex)
        val query = item?.word?.takeIf { it.isNotBlank() }
            ?: item?.translation?.takeIf { it.isNotBlank() }
            ?: ""
        ImagePickerDialog.newInstance(query)
            .show(parentFragmentManager, "DictionaryImagePickerDialog")
    }

    private fun removeImageForItem(groupIndex: Int, itemIndex: Int) {
        updateItemImage(groupIndex, itemIndex, null)
    }

    private fun handleSelectedImage(target: ImageTarget, urlOrUri: String) {
        if (urlOrUri.startsWith("http", ignoreCase = true)) {
            downloadImage(urlOrUri) { file ->
                if (!isAdded) return@downloadImage
                val localFile = file?.let { ImageStorage.copyFileToInternal(requireContext(), it) } ?: file
                val localUri = localFile?.let { Uri.fromFile(it) }
                if (localUri != null) {
                    setImageWithOptionalCrop(target, localUri)
                } else {
                    Toast.makeText(requireContext(), R.string.dictionary_error_state, Toast.LENGTH_SHORT).show()
                    if (pendingPickerTarget == target) {
                        pendingPickerTarget = null
                    }
                }
            }
        } else {
            val pickedUri = Uri.parse(urlOrUri)
            val localFile = ImageStorage.copyUriToInternal(requireContext(), pickedUri)
            val localUri = localFile?.let { Uri.fromFile(it) } ?: pickedUri
            setImageWithOptionalCrop(target, localUri)
        }
    }

    private fun setImageWithOptionalCrop(target: ImageTarget, uri: Uri) {
        if (!isAdded) return
        if (ImageStorage.shouldCrop(requireContext(), uri)) {
            startCropping(target, uri)
        } else {
            applyImage(target, uri)
            if (pendingPickerTarget == target) {
                pendingPickerTarget = null
            }
        }
    }

    private fun startCropping(target: ImageTarget, uri: Uri) {
        if (!isAdded) return
        pendingCropTarget = target
        val destinationUri = Uri.fromFile(
            File(
                ImageStorage.getAppImagesDir(requireContext()),
                "cropped_${System.currentTimeMillis()}.jpg"
            )
        )
        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1280, 1280)
        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun applyImage(target: ImageTarget, uri: Uri) {
        if (!isAdded) return
        updateItemImage(target.groupIndex, target.itemIndex, uri.toString())
        if (pendingPickerTarget == target) {
            pendingPickerTarget = null
        }
    }

    private fun downloadImage(imageUrl: String, callback: (File?) -> Unit) {
        imageRepo.downloadToFile(requireContext(), imageUrl, callback)
    }

    private fun handleDelete() {
        val entry = existingEntry ?: return
        dictionaryViewModel.delete(entry)
        Toast.makeText(requireContext(), getString(R.string.dictionary_delete_success), Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun handleSearch() {
        val query = binding.wordSearchEditText.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            binding.wordSearchInputLayout.error = getString(R.string.dictionary_error_required)
            return
        }
        binding.wordSearchInputLayout.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            val result = runCatching { dictionarySearchProvider.search(query) }
            showLoading(false)

            result.onSuccess { grouped ->
                val hasEntries = grouped.any { it.isNotEmpty() }
                if (!hasEntries) {
                    groups.clear()
                    refreshGroups()
                    updateEmptyState(getString(R.string.dictionary_empty_results))
                } else {
                    existingEntry = null
                    binding.deleteWordButton.isVisible = false
                    groups.clear()
                    grouped.filter { it.isNotEmpty() }.forEachIndexed { index, dictionaries ->
                        val baseEntry = dictionaries.first()
                        val items = dictionaries.map { dictionary ->
                            DictionaryEditItem.fromWord(dictionary.copy(id = 0))
                        }.toMutableList()
                        groups += DictionaryEditGroup(
                            baseWordId = baseEntry.baseWordId,
                            level = baseEntry.level,
                            items = items,
                            isExpanded = index == 0
                        )
                    }
                    if (groups.isEmpty()) {
                        updateEmptyState(getString(R.string.dictionary_empty_results))
                    } else {
                        refreshGroups()
                        binding.wordGroupsRecyclerView.scrollToPosition(0)
                    }
                }
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.dictionary_error_state), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addGroup() {
        val shouldExpand = groups.isEmpty()
        val group = DictionaryEditGroup(
            items = mutableListOf(DictionaryEditItem()),
            isExpanded = shouldExpand
        )
        groups += group
        refreshGroups()
        scrollToGroup(groups.lastIndex)
    }

    private fun showLoading(show: Boolean) {
        binding.wordProgressBar.isVisible = show
        binding.wordSearchButton.isEnabled = !show
        binding.wordSearchInputLayout.isEnabled = !show
        binding.saveWordButton.isEnabled = !show
        binding.deleteWordButton.isEnabled = !show
        binding.addGroupButton.isEnabled = !show

        if (show) {
            binding.wordGroupsRecyclerView.isVisible = false
            binding.wordEmptyStateTextView.isVisible = false
        } else {
            updateEmptyState()
        }
    }

    private fun updateEmptyState(message: String? = null) {
        val isLoading = binding.wordProgressBar.isVisible
        val isEmpty = !isLoading && groups.isEmpty()
        binding.wordGroupsRecyclerView.isVisible = !isLoading && groups.isNotEmpty()
        binding.wordEmptyStateTextView.isVisible = isEmpty
        if (isEmpty) {
            binding.wordEmptyStateTextView.text = message ?: getString(R.string.dictionary_empty_results)
        }
        if (entryId == null) {
            binding.addGroupButton.isVisible = !isLoading
        }
    }

    private fun refreshGroups() {
        groupAdapter.submitGroups(groups)
        updateEmptyState()
    }

    private fun scrollToGroup(groupIndex: Int) {
        if (groupIndex < 0) return
        binding.wordGroupsRecyclerView.post {
            val lastPosition = groupAdapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.wordGroupsRecyclerView.smoothScrollToPosition(lastPosition)
            }
        }
    }

    private fun updateItemImage(groupIndex: Int, itemIndex: Int, imagePath: String?) {
        val group = groups.getOrNull(groupIndex) ?: return
        if (itemIndex !in group.items.indices) return
        val normalized = imagePath.orEmpty()
        val item = group.items[itemIndex]
        item.imagePath = normalized

        val baseId = group.baseWordId
        val isBaseItem = when {
            itemIndex == 0 -> true
            baseId != null -> item.id == baseId || item.baseWordId == null
            else -> false
        }

        if (isBaseItem && normalized.isNotBlank()) {
            group.items.forEachIndexed { index, entry ->
                if (index != itemIndex && entry.imagePath.isBlank()) {
                    entry.imagePath = normalized
                }
            }
        }

        refreshGroups()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ENTRY_ID = "arg_dictionary_entry_id"

        fun newInstance(entryId: Int? = null): DictionaryAddFragment {
            return DictionaryAddFragment().apply {
                arguments = Bundle().apply {
                    entryId?.let { putInt(ARG_ENTRY_ID, it) }
                }
            }
        }
    }
}

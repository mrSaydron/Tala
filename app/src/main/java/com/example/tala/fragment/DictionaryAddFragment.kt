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
import com.example.tala.databinding.FragmentDictionaryAddBinding
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryLevel
import com.example.tala.entity.dictionary.DictionaryViewModel
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.fragment.adapter.DictionaryEditGroup
import com.example.tala.fragment.adapter.DictionaryEditGroupAdapter
import com.example.tala.fragment.adapter.DictionaryEditItem
import com.example.tala.fragment.adapter.DictionaryGroupPayload
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

    private var _binding: FragmentDictionaryAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var groupAdapter: DictionaryEditGroupAdapter

    private val dictionarySearchProvider: DictionarySearchProvider =
        GeminiDictionarySearchProvider(fallbackProvider = YandexDictionarySearchProvider())
    private val partOfSpeechItems = PartOfSpeech.values().toList()
    private val levelItems = listOf<DictionaryLevel?>(null) + DictionaryLevel.values().toList()

    private var entryId: Int? = null
    private var existingEntry: Dictionary? = null
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
        _binding = FragmentDictionaryAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dictionaryViewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]

        setupRecycler()
        setupButtons()
        setupImagePickerListener()

        if (entryId != null) {
            binding.dictionarySearchContainer.isVisible = false
            binding.addGroupButton.isVisible = false
            binding.saveDictionaryButton.text = getString(R.string.dictionary_update_button)
            loadEntry(entryId!!)
        } else {
            binding.deleteDictionaryButton.isVisible = false
            addGroup()
        }

        updateEmptyState()
    }

    private fun setupRecycler() {
        groupAdapter = DictionaryEditGroupAdapter(
            partOfSpeechItems = partOfSpeechItems,
            levelItems = levelItems,
            onGroupsChanged = { updateEmptyState() },
            onSelectImage = { groupIndex, itemIndex -> openImagePickerForItem(groupIndex, itemIndex) },
            onRemoveImage = { groupIndex, itemIndex -> removeImageForItem(groupIndex, itemIndex) }
        )
        binding.dictionaryGroupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.dictionaryGroupsRecyclerView.adapter = groupAdapter
    }

    private fun setupButtons() {
        binding.dictionarySearchButton.setOnClickListener { handleSearch() }
        binding.addGroupButton.setOnClickListener { addGroup() }
        binding.saveDictionaryButton.setOnClickListener { handleSave() }
        binding.deleteDictionaryButton.setOnClickListener { handleDelete() }
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

    private fun populateEntry(entries: List<Dictionary>) {
        if (entries.isEmpty()) return
        existingEntry = entries.first()
        binding.deleteDictionaryButton.isVisible = true

        val group = DictionaryEditGroup(
            baseWordId = entries.first().baseWordId ?: entries.first().id,
            level = entries.first().level,
            items = entries.map { DictionaryEditItem.fromDictionary(it) }.toMutableList(),
            isExpanded = true
        )
        groupAdapter.submitGroups(listOf(group))
        updateEmptyState()
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
        val item = groupAdapter.getGroups()
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
        groupAdapter.updateItemImage(groupIndex, itemIndex, null)
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
        groupAdapter.updateItemImage(target.groupIndex, target.itemIndex, uri.toString())
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
        val query = binding.dictionarySearchEditText.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            binding.dictionarySearchInputLayout.error = getString(R.string.dictionary_error_required)
            return
        }
        binding.dictionarySearchInputLayout.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            val result = runCatching { dictionarySearchProvider.search(query) }
            showLoading(false)

            result.onSuccess { grouped ->
                val hasEntries = grouped.any { it.isNotEmpty() }
                if (!hasEntries) {
                    groupAdapter.submitGroups(emptyList())
                    updateEmptyState(getString(R.string.dictionary_empty_results))
                } else {
                    existingEntry = null
                    binding.deleteDictionaryButton.isVisible = false
                    val groups = grouped.mapIndexed { index, dictionaries ->
                        val baseEntry = dictionaries.firstOrNull()
                        val items = dictionaries.map { dictionary ->
                            DictionaryEditItem.fromDictionary(
                                dictionary.copy(id = 0)
                            )
                        }.toMutableList()
                        DictionaryEditGroup(
                            baseWordId = baseEntry?.baseWordId,
                            level = baseEntry?.level,
                            items = items,
                            isExpanded = index == 0
                        )
                    }
                    groupAdapter.submitGroups(groups)
                    updateEmptyState()
                }
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.dictionary_error_state), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addGroup() {
        val shouldExpand = groupAdapter.itemCount == 0
        val group = DictionaryEditGroup(
            items = mutableListOf(DictionaryEditItem()),
            isExpanded = shouldExpand
        )
        groupAdapter.addGroup(group)
        updateEmptyState()
        binding.dictionaryGroupsRecyclerView.post {
            binding.dictionaryGroupsRecyclerView.smoothScrollToPosition(groupAdapter.itemCount - 1)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.dictionaryProgressBar.isVisible = show
        if (show) {
            binding.dictionaryGroupsRecyclerView.isVisible = false
            binding.dictionaryEmptyStateTextView.isVisible = false
        } else {
            updateEmptyState()
        }
    }

    private fun updateEmptyState(message: String? = null) {
        val isEmpty = groupAdapter.itemCount == 0 && !binding.dictionaryProgressBar.isVisible
        binding.dictionaryGroupsRecyclerView.isVisible = !isEmpty
        binding.dictionaryEmptyStateTextView.isVisible = isEmpty
        if (isEmpty) {
            binding.dictionaryEmptyStateTextView.text = message ?: getString(R.string.dictionary_empty_results)
        }
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

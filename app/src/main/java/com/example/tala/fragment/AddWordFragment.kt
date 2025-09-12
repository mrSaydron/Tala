package com.example.tala.fragment

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.tala.R
import com.example.tala.databinding.FragmentAddWordBinding
import com.example.tala.entity.card.CardViewModel
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.collection.CollectionViewModel
import com.example.tala.fragment.dialog.AddCollectionDialog
import com.example.tala.fragment.dialog.RenameCollectionDialog
import com.example.tala.fragment.dialog.ImagePickerDialog
import com.example.tala.integration.translation.TranslationRepository
import com.example.tala.integration.picture.ImageRepository
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.enums.CardTypeEnum
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.dto.info.CardInfo
import java.io.File
import kotlin.math.max
import androidx.core.widget.addTextChangedListener
import com.example.tala.util.ImageStorage

class AddWordFragment : Fragment() {

    private lateinit var binding: FragmentAddWordBinding

    private lateinit var cardViewModel: CardViewModel
    private lateinit var collectionViewModel: CollectionViewModel

    private var currentCard: CardListDto? = null
    private lateinit var collectionAdapter: ArrayAdapter<String>
    private val collections = mutableListOf<CardCollection>()
    private var imagePath: String? = null
    private val selectedTypes = mutableSetOf<CardTypeEnum>()

    private val translationRepo by lazy { TranslationRepository() }
    private val imageRepo by lazy { ImageRepository() }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val croppedUri = UCrop.getOutput(data!!)
            croppedUri?.let {
                binding.wordImageView.setImageURI(it)
                imagePath = it.toString()
            }
        }
    }

    // Синхронизация выбора коллекции
    private var desiredCollectionId: Int? = null
    private var collectionsLoaded: Boolean = false
    private var initialSelectionApplied: Boolean = false
    private var pendingSelectCollectionName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddWordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardViewModel = ViewModelProvider(this)[CardViewModel::class.java]
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]

        binding.deleteWordButton.visibility = View.GONE

        lifecycleScope.launch {
            val argCommonId: String? = arguments?.getString(ARG_COMMON_ID)
            if (!argCommonId.isNullOrEmpty()) {
                currentCard = cardViewModel.getCardListByCommonId(argCommonId)
            }
            bindCurrentCard()
            // Запомним желаемую коллекцию после загрузки карточки
            desiredCollectionId = currentCard?.collectionId
            applySelections()
        }

        // Инициализация Spinner
        collectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        collectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.collectionSpinner.adapter = collectionAdapter

        // Переводные иконки: состояние доступности
        setupTranslationButtonsState()
        setupTranslationInputsListeners()

        // Загрузка коллекций
        loadCollections()

        setupDeleteWordButton()
        setupSaveButton()
        setupAddCollectionButton()
        setupCollectionSpinnerListener()
        setupAutoFillOnFocusLoss(binding.englishWordInput, binding.russianWordInput, "en", "ru")
        setupAutoFillOnFocusLoss(binding.russianWordInput, binding.englishWordInput, "ru", "en")
        setupImagePicker()
        setupCardTypesButton()
        setupTranslationButtons()
        setValueAndVisibility()
    }

    private fun setupTranslationInputsListeners() {
        binding.englishWordInput.addTextChangedListener { text ->
            binding.chooseRussianTranslationButton.isEnabled = !text.isNullOrBlank()
        }
        binding.russianWordInput.addTextChangedListener { text ->
            binding.chooseEnglishTranslationButton.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupDeleteWordButton() {
        binding.deleteWordButton.setOnClickListener {
            val commonId = currentCard?.commonId
            if (!commonId.isNullOrEmpty()) {
                lifecycleScope.launch {
                    try {
                        cardViewModel.deleteSync(commonId)
                    } finally {
                        Toast.makeText(requireContext(), "Слово удалено", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            try {
                val englishWord = binding.englishWordInput.text.toString()
                val russianWord = binding.russianWordInput.text.toString()
                val selectedCollection =
                    if (collections.size > 0) collections[max(
                        binding.collectionSpinner.selectedItemPosition,
                        0
                    )]
                    else null
                if (englishWord.isEmpty() || russianWord.isEmpty()) {
                    Toast.makeText(requireContext(), "Заполните оба поля!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (selectedTypes.isEmpty()) {
                    Toast.makeText(requireContext(), "Выберите хотя бы один тип карточек", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val hint = binding.hintInput.text.toString().trim().ifEmpty { null }
                val baseInfo = WordCardInfo(
                    english = englishWord,
                    russian = russianWord,
                    imagePath = imagePath,
                    hint = hint,
                )
                val cards: Map<CardTypeEnum, CardInfo> = selectedTypes.associateWith { baseInfo }
                val cardDto = if (currentCard == null) {
                    CardListDto(
                        collectionId = selectedCollection?.id ?: 0,
                        cards = cards,
                    )
                } else {
                    CardListDto(
                        commonId = currentCard!!.commonId,
                        collectionId = selectedCollection?.id ?: 0,
                        cards = cards,
                    )
                }
                cardViewModel.saveCard(cardDto)
                if (currentCard != null) parentFragmentManager.popBackStack()
                resetForm()
                Toast.makeText(requireContext(), "Слово сохранено!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                println("---> ${e.message}")
            }
        }
    }

    private fun setupAddCollectionButton() {
        binding.addCollectionButton.setOnClickListener {
            val dialog = AddCollectionDialog { collectionName ->
                lifecycleScope.launch {
                    val exists = collectionViewModel.existsCollectionByName(collectionName)
                    if (exists) {
                        Toast.makeText(requireContext(), "Коллекция с таким именем уже существует", Toast.LENGTH_SHORT).show()
                    } else {
                        val collection = CardCollection(name = collectionName)
                        collectionViewModel.insertCollection(collection)
                        Toast.makeText(requireContext(), "Коллекция добавлена!", Toast.LENGTH_SHORT).show()
                        pendingSelectCollectionName = collectionName
                        applySelections()
                    }
                }
            }
            dialog.show(parentFragmentManager, "AddCollectionDialog")
        }
    }

    private fun setupCollectionSpinnerListener() {
        binding.collectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCollection = collections[position]
                showCollectionButtons(selectedCollection)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.deleteCollectionButton.visibility = View.GONE
                binding.renameCollectionButton.visibility = View.GONE
            }
        }
    }

    private fun setupImagePicker() {
        binding.wordImageView.setOnClickListener {
            val dialog = ImagePickerDialog(
                initialQuery = binding.englishWordInput.text.toString(),
                onImageSelected = { imageUrl ->
                    handleSelectedImage(imageUrl)
                }
            )
            dialog.show(parentFragmentManager, "ImagePickerDialog")
        }
    }

    private fun setupCardTypesButton() {
        binding.cardTypesSettingsButton.setOnClickListener {
            showCardTypesDialog()
        }
    }

    private fun setupTranslationButtons() {
        binding.chooseRussianTranslationButton.setOnClickListener {
            val englishWord = binding.englishWordInput.text.toString()
            chooseTranslation(
                word = englishWord,
                from = "en",
                to = "ru",
                title = "Русский перевод",
            ) { chosen -> binding.russianWordInput.setText(chosen) }
        }

        binding.chooseEnglishTranslationButton.setOnClickListener {
            val russianWord = binding.russianWordInput.text.toString()
            chooseTranslation(
                word = russianWord,
                from = "ru",
                to = "en",
                title = "Английский перевод",
            ) { chosen -> binding.englishWordInput.setText(chosen) }
        }
    }

    private fun showReviewStats(commonId: String?) {
        commonId?.let {
            lifecycleScope.launch {
                val entries = CardTypeEnum.entries
                    .filter { it.use }
                    .mapNotNull { type ->
                        val card = cardViewModel.getCardByTypeAndCommonId(type, commonId)
                        card?.let { c ->
                            val next = java.time.Instant.ofEpochSecond(c.nextReviewDate)
                            val localNext = java.time.LocalDateTime.ofInstant(next, java.time.ZoneId.systemDefault())
                            "${type.name}: ${localNext.toLocalDate()}"
                        }
                    }
                val text = entries.joinToString("\n")
                if (text.isNotEmpty()) {
                    binding.reviewStatsLabel.visibility = View.VISIBLE
                    binding.reviewStatsText.visibility = View.VISIBLE
                    binding.reviewStatsText.text = text
                } else {
                    binding.reviewStatsLabel.visibility = View.GONE
                    binding.reviewStatsText.visibility = View.GONE
                }
            }
        }
    }

    private fun loadCollections() {
        collectionViewModel.getAllCollections().observe(viewLifecycleOwner) { collectionList ->
            collections.clear()
            collections.addAll(collectionList)
            collectionAdapter.clear()
            collectionAdapter.addAll(collections.map { it.name })
            collectionAdapter.notifyDataSetChanged()

            // Флаг, что коллекции загружены, пытаемся применить нужный выбор
            collectionsLoaded = true
            applySelections()
        }
    }

    private fun applySelections() {
        if (!collectionsLoaded) return
        // 1) Если есть отложенный выбор по имени (новая коллекция) — применяем его в приоритете
        pendingSelectCollectionName?.let { name ->
            val idxByName = collections.indexOfFirst { it.name == name }
            if (idxByName >= 0) {
                binding.collectionSpinner.setSelection(idxByName, false)
                pendingSelectCollectionName = null
                return
            }
        }
        // 2) Иначе единовременно применяем исходную коллекцию слова по id
        if (!initialSelectionApplied) {
            val targetId = desiredCollectionId ?: return
            val idx = collections.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                binding.collectionSpinner.setSelection(idx, false)
                initialSelectionApplied = true
            }
        }
    }

    private fun showCollectionButtons(collection: CardCollection) {
        val isDefault = collection.name == DEFAULT_COLLECTION_NAME
        binding.deleteCollectionButton.visibility = if (isDefault) View.GONE else View.VISIBLE
        binding.renameCollectionButton.visibility = if (isDefault) View.GONE else View.VISIBLE
        if (!isDefault) {
            binding.deleteCollectionButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить коллекцию «${collection.name}»?")
                    .setMessage("При удалении коллекции «${collection.name}» будут удалены все слова из этой коллекции. Продолжить?")
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        lifecycleScope.launch {
                            cardViewModel.deleteCardsByCollection(collection.id)
                            collectionViewModel.deleteCollection(collection)
                            Toast.makeText(requireContext(), "Коллекция и связанные слова удалены", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            binding.renameCollectionButton.setOnClickListener {
                val dialog = RenameCollectionDialog(initialName = collection.name) { newName ->
                    val trimmed = newName.trim()
                    lifecycleScope.launch {
                        when {
                            trimmed.isEmpty() -> {
                                Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                            }
                            trimmed == collection.name -> {
                                // без изменений
                            }
                            collectionViewModel.existsCollectionByName(trimmed) -> {
                                Toast.makeText(requireContext(), "Коллекция с таким именем уже существует", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                collectionViewModel.renameCollection(collection.id, trimmed)
                                pendingSelectCollectionName = trimmed
                                applySelections()
                                Toast.makeText(requireContext(), "Коллекция переименована", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialog.show(parentFragmentManager, "RenameCollectionDialog")
            }
        }
    }

    // onActivityResult удалён, используем Activity Result API

    private fun chooseTranslation(word: String, from: String, to: String, title: String, onChosen: (String) -> Unit) {
        lifecycleScope.launch {
            val items = translationRepo.getTranslations(word, from, to)
            if (items.isNotEmpty()) {
                val arr = items.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(title)
                    .setItems(arr) { _, which -> onChosen(arr[which]) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Нет доступных переводов", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchImages(query: String): List<String> = imageRepo.searchImages(query)

    private fun startCropping(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(ImageStorage.getAppImagesDir(requireContext()), "cropped_${System.currentTimeMillis()}.jpg"))
        val uCrop = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1280, 1280)
        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun handleSelectedImage(urlOrUri: String) {
        if (urlOrUri.startsWith("http", true)) {
            loadImageFromUrl(urlOrUri) { file ->
                file?.let {
                    val localFile = ImageStorage.copyFileToInternal(requireContext(), it)
                    val localUri = Uri.fromFile(localFile ?: it)
                    setImageWithOptionalCrop(localUri)
                }
            }
        } else {
            val contentUri = Uri.parse(urlOrUri)
            val localFile = ImageStorage.copyUriToInternal(requireContext(), contentUri)
            val chosenUri = if (localFile != null) Uri.fromFile(localFile) else contentUri
            setImageWithOptionalCrop(chosenUri)
        }
    }

    private fun setImageWithOptionalCrop(uri: Uri) {
        if (ImageStorage.shouldCrop(requireContext(), uri)) {
            startCropping(uri)
        } else {
            binding.wordImageView.setImageURI(uri)
            imagePath = uri.toString()
        }
    }

    private fun setupAutoFillOnFocusLoss(sourceField: EditText, targetField: EditText, from: String, to: String) {
        sourceField.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val source = sourceField.text.toString()
                val target = targetField.text.toString()
                if (source.isNotEmpty() && target.isEmpty()) {
                    lifecycleScope.launch {
                        translationRepo.getTranslations(source, from, to).firstOrNull()?.let {
                            targetField.setText(it)
                        }
                        val images = fetchImages(source)
                        images.firstOrNull()?.let { url ->
                            loadImageFromUrl(url) { file ->
                                file?.let {
                                    val local = ImageStorage.copyFileToInternal(requireContext(), it)
                                    val localUri = Uri.fromFile(local ?: it)
                                    binding.wordImageView.setImageURI(localUri)
                                    imagePath = localUri.toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showCardTypesDialog() {
        val availableTypes = CardTypeEnum.entries.filter { it.use }
        val titles = availableTypes.map { it.titleRu }.toTypedArray()
        val checked = availableTypes.map { selectedTypes.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Типы карточек")
            .setMultiChoiceItems(titles, checked) { _, which, isChecked ->
                val type = availableTypes[which]
                if (isChecked) selectedTypes.add(type) else selectedTypes.remove(type)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setValueAndVisibility()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Удалены локальные копирующие методы: используем ImageStorage

    private fun loadImageFromUrl(imageUrl: String, callback: (File?) -> Unit) =
        imageRepo.downloadToFile(requireContext(), imageUrl, callback)

    private fun setupTranslationButtonsState() {
        val english = binding.englishWordInput.text?.toString()?.trim().orEmpty()
        val russian = binding.russianWordInput.text?.toString()?.trim().orEmpty()
        binding.chooseRussianTranslationButton.isEnabled = english.isNotEmpty()
        binding.chooseEnglishTranslationButton.isEnabled = russian.isNotEmpty()
    }

    private fun setValueAndVisibility() {
        setValueAndVisibilityForEnglish()
        setValueAndVisibilityForRussian()
        setValueAndVisibilityForHint()
        setValueAndVisibilityForImage()
    }

    private fun setValueAndVisibilityForEnglish() {
        val shouldShow = shouldShowEnglishSection()
        setSectionVisibility(R.id.sectionEnglish, shouldShow)
    }

    private fun setValueAndVisibilityForRussian() {
        val shouldShow = shouldShowRussianSection()
        setSectionVisibility(R.id.sectionRussian, shouldShow)
    }

    private fun setValueAndVisibilityForHint() {
        val shouldShow = shouldShowHintSection()
        setSectionVisibility(R.id.sectionHint, shouldShow)
    }

    private fun setValueAndVisibilityForImage() {
        val shouldShow = shouldShowImageSection()
        setSectionVisibility(R.id.sectionImage, shouldShow)
    }

    private fun shouldShowEnglishSection(): Boolean = true
    private fun shouldShowRussianSection(): Boolean = true
    private fun shouldShowHintSection(): Boolean = true
    private fun shouldShowImageSection(): Boolean = true

    private fun setSectionVisibility(viewId: Int, visible: Boolean) {
        val v = binding.root.findViewById<View>(viewId)
        v?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun bindCurrentCard() {
        if (currentCard != null) {
            showReviewStats(currentCard?.commonId)
            setValueAndVisibility()
            setValues()

            // Инициализируем выбранные типы карточек
            if (currentCard?.cards?.isNotEmpty() == true) {
                selectedTypes.clear()
                selectedTypes.addAll(currentCard!!.cards.keys)
            }

            // Настройка кнопки удаления
            binding.deleteWordButton.visibility = View.VISIBLE
        } else {
            if (selectedTypes.isEmpty()) {
                selectedTypes.addAll(CardTypeEnum.entries.filter { type -> type.use })
            }
        }
    }

    private fun setValues() {
        currentWordInfo()?.let { info ->
            binding.englishWordInput.setText(info.english)
            binding.russianWordInput.setText(info.russian)
            binding.hintInput.setText(info.hint)
            Glide.with(this)
                .load(info.imagePath)
                .into(binding.wordImageView)
            imagePath = info.imagePath
        }
    }

    private fun currentWordInfo(): WordCardInfo? =
        currentCard?.cards?.values?.filterIsInstance<WordCardInfo>()?.firstOrNull()

    companion object {
        private const val ARG_COMMON_ID = "common_id"
        private const val DEFAULT_COLLECTION_NAME = "Default"

        fun newInstance(commonId: String?): AddWordFragment {
            val fragment = AddWordFragment()
            val args = Bundle()
            if (!commonId.isNullOrEmpty()) args.putString(ARG_COMMON_ID, commonId)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(card: CardListDto?): AddWordFragment {
            val fragment = AddWordFragment()
            fragment.currentCard = card
            return fragment
        }
    }

    private fun resetForm() {
        binding.englishWordInput.text?.clear()
        binding.russianWordInput.text?.clear()
        binding.hintInput.text?.clear()
        binding.wordImageView.setImageDrawable(null)
        imagePath = null
        setupTranslationButtonsState()
    }
}
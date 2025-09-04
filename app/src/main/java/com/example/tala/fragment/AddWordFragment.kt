package com.example.tala.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tala.R
import com.example.tala.databinding.FragmentAddWordBinding
import com.example.tala.entity.card.CardViewModel
import com.example.tala.entity.category.Category
import com.example.tala.entity.category.CategoryViewModel
import com.example.tala.fragment.dialog.AddCategoryDialog
import com.example.tala.fragment.dialog.ImagePickerDialog
import com.example.tala.integration.dictionary.YandexDictionaryApi.Companion.YANDEX_API_KEY
import com.example.tala.integration.picture.UnsplashApi.Companion.USPLASH_API_KEY
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.service.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.dto.info.CardInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.webkit.MimeTypeMap
import kotlin.math.max
import androidx.core.widget.addTextChangedListener

class AddWordFragment : Fragment() {

    private lateinit var binding: FragmentAddWordBinding

    private lateinit var cardViewModel: CardViewModel
    private lateinit var categoryViewModel: CategoryViewModel

    private var currentCard: CardListDto? = null
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val categories = mutableListOf<Category>()
    private var imagePath: String? = null
    private val selectedTypes = mutableSetOf<CardTypeEnum>()

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
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        // Инициализируем выбранные типы карточек
        if (currentCard?.types?.isNotEmpty() == true) {
            selectedTypes.clear()
            selectedTypes.addAll(currentCard!!.types)
        } else if (selectedTypes.isEmpty()) {
            selectedTypes.addAll(CardTypeEnum.entries.filter { it.use })
        }

        // Если пришел commonId в аргументах — загрузим карточку
        val argCommonId = arguments?.getString(ARG_COMMON_ID)
        if (!argCommonId.isNullOrEmpty()) {
            lifecycleScope.launch {
                currentCard = cardViewModel.getCardListByCommonId(argCommonId)
                bindCurrentCard()
            }
        } else currentCard?.let {
            binding.englishWordInput.setText(it.english)
            binding.russianWordInput.setText(it.russian)
            binding.hintInput.setText(it.info?.let { info ->
                try { org.json.JSONObject(info).optString("hint", "") } catch (_: Exception) { "" }
            })

            Glide.with(this)
                .load(it.imagePath)
                .into(binding.wordImageView)

            imagePath = it.imagePath

            // Загрузка статистики повторений по типам
            lifecycleScope.launch {
                showReviewStats(it.commonId)
            }
        }

        // Инициализация Spinner
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // Переводные иконки: состояние доступности
        setupTranslationButtonsState()
        binding.englishWordInput.addTextChangedListener { text ->
            binding.chooseRussianTranslationButton.isEnabled = !text.isNullOrBlank()
        }
        binding.russianWordInput.addTextChangedListener { text ->
            binding.chooseEnglishTranslationButton.isEnabled = !text.isNullOrBlank()
        }

        // Загрузка категорий
        loadCategories()

        // Кнопка удаления (только для существующих слов)
        if (currentCard != null) {
            binding.deleteWordButton.visibility = View.VISIBLE
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
        } else {
            binding.deleteWordButton.visibility = View.GONE
        }

        binding.saveButton.setOnClickListener {
            try {
                val englishWord = binding.englishWordInput.text.toString()
                val russianWord = binding.russianWordInput.text.toString()
                val selectedCategory =
                    if (categories.size > 0) categories[max(
                        binding.categorySpinner.selectedItemPosition,
                        0
                    )]
                    else null
                Log.i("AddWordFragment", "onViewCreated: selectCategory")

                if (englishWord.isNotEmpty() && russianWord.isNotEmpty()) {
                    if (selectedTypes.isEmpty()) {
                        Toast.makeText(requireContext(), "Выберите хотя бы один тип карточек", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (currentCard == null) {
                        val hint = binding.hintInput.text.toString().trim().ifEmpty { null }
                        val baseInfo = WordCardInfo(
                            english = englishWord,
                            russian = russianWord,
                            imagePath = imagePath,
                            hint = hint,
                        )
                        val cards: Map<CardTypeEnum, CardInfo> = selectedTypes.associateWith { baseInfo }
                        val cardDto = CardListDto(
                            english = englishWord,
                            russian = russianWord,
                            categoryId = selectedCategory?.id ?: 0,
                            imagePath = imagePath,
                            info = null,
                            types = emptySet(),
                            cards = cards,
                        )
                        cardViewModel.insert(cardDto)
                    } else {
                        val hint = binding.hintInput.text.toString().trim().ifEmpty { null }
                        val baseInfo = WordCardInfo(
                            english = englishWord,
                            russian = russianWord,
                            imagePath = imagePath,
                            hint = hint,
                        )
                        val cards: Map<CardTypeEnum, CardInfo> = selectedTypes.associateWith { baseInfo }
                        val cardDto = CardListDto(
                            commonId = currentCard!!.commonId,
                            english = englishWord,
                            russian = russianWord,
                            categoryId = selectedCategory?.id ?: 0,
                            imagePath = imagePath,
                            info = null,
                            types = emptySet(),
                            cards = cards,
                        )
                        cardViewModel.update(cardDto)
                        parentFragmentManager.popBackStack()
                    }
                    binding.englishWordInput.text?.clear()
                    binding.russianWordInput.text?.clear()
                    binding.hintInput.text?.clear()
                    binding.wordImageView.setImageDrawable(null)
                    imagePath = null
                    setupTranslationButtonsState()
                    Toast.makeText(requireContext(), "Слово сохранено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Заполните оба поля!", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                println("---> ${e.message}")
            }
        }

        binding.addCategoryButton.setOnClickListener {
            val dialog = AddCategoryDialog { categoryName ->
                val category = Category(name = categoryName)
                categoryViewModel.insertCategory(category)
                Toast.makeText(requireContext(), "Категория добавлена!", Toast.LENGTH_SHORT).show()
            }
            dialog.show(parentFragmentManager, "AddCategoryDialog")
        }

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                showCategoryButtons(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.deleteCategoryButton.visibility = View.GONE
            }
        }

        setupEnglishWordFocus()
        setupRussianWordFocus()

        // Обработка нажатия на изображение
        binding.wordImageView.setOnClickListener {
            val dialog = ImagePickerDialog(
                initialQuery = binding.englishWordInput.text.toString(),
                onImageSelected = { imageUrl ->
                    if (imageUrl.startsWith("http", true)) {
                        loadImageFromUrl(imageUrl) { file ->
                            file?.let {
                                val localFile = copyFileToInternalImages(it)
                                val localUri = Uri.fromFile(localFile ?: it)
                                if (shouldCrop(localUri)) {
                                    startCropping(localUri)
                                } else {
                                    binding.wordImageView.setImageURI(localUri)
                                    imagePath = localUri.toString()
                                }
                            }
                        }
                    } else {
                        val contentUri = Uri.parse(imageUrl)
                        val localFile = copyImageToInternalStorage(contentUri)
                        if (localFile != null) {
                            val localUri = Uri.fromFile(localFile)
                            if (shouldCrop(localUri)) {
                                startCropping(localUri)
                            } else {
                                binding.wordImageView.setImageURI(localUri)
                                imagePath = localUri.toString() // Сохраняем путь к локальной копии
                            }
                        } else {
                            // fallback: если копирование не удалось — пробуем кадрировать напрямую или ставим как есть
                            if (shouldCrop(contentUri)) {
                                startCropping(contentUri)
                            } else {
                                binding.wordImageView.setImageURI(contentUri)
                                imagePath = contentUri.toString()
                            }
                        }
                    }
                }
            )
            dialog.show(parentFragmentManager, "ImagePickerDialog")
        }

        // Кнопка настроек типов карточек
        binding.cardTypesSettingsButton.setOnClickListener {
            showCardTypesDialog()
        }

        // Кнопка для выбора перевода
        binding.chooseRussianTranslationButton.setOnClickListener {
            lifecycleScope.launch {
                val englishWord = binding.englishWordInput.text.toString()
                val translations = fetchTranslationEnRu(englishWord)
                if (translations.isNotEmpty()) {
                    val items = translations.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Русский перевод")
                        .setItems(items) { _, which ->
                            binding.russianWordInput.setText(items[which])
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Нет доступных переводов", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        binding.chooseEnglishTranslationButton.setOnClickListener {
            lifecycleScope.launch {
                val russianWord = binding.russianWordInput.text.toString()
                val translations = fetchTranslationRuEn(russianWord)
                if (translations.isNotEmpty()) {
                    val items = translations.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Английский перевод")
                        .setItems(items) { _, which ->
                            binding.englishWordInput.setText(items[which])
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Нет доступных переводов", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private suspend fun showReviewStats(commonId: String?) {
        if (commonId.isNullOrEmpty()) return
        val sb = StringBuilder()
        CardTypeEnum.entries.filter { it.use }.forEach { type ->
            val card = cardViewModel.getCardByTypeAndCommonId(type, commonId)
            if (card != null) {
                val next = java.time.Instant.ofEpochSecond(card.nextReviewDate)
                val localNext = java.time.LocalDateTime.ofInstant(next, java.time.ZoneId.systemDefault())
                val formatted = localNext.toLocalDate().toString()
                sb.append("${type.name}: ")
                    .append(formatted)
                    .append('\n')
            }
        }

        val text = sb.toString().trim()
        if (text.isNotEmpty()) {
            binding.reviewStatsLabel.visibility = View.VISIBLE
            binding.reviewStatsText.visibility = View.VISIBLE
            binding.reviewStatsText.text = text
        } else {
            binding.reviewStatsLabel.visibility = View.GONE
            binding.reviewStatsText.visibility = View.GONE
        }
    }

    private fun loadCategories() {
        categoryViewModel.getAllCategories().observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            categories.addAll(categoryList)
            categoryAdapter.clear()
            categoryAdapter.addAll(categories.map { it.name })
            categoryAdapter.notifyDataSetChanged()
        }
    }

    private fun showCategoryButtons(category: Category) {
        binding.deleteCategoryButton.visibility = View.VISIBLE
        binding.deleteCategoryButton.setOnClickListener {
            categoryViewModel.deleteCategory(category)
            Toast.makeText(requireContext(), "Категория удалена!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val croppedUri = UCrop.getOutput(data!!)
            croppedUri?.let {
                binding.wordImageView.setImageURI(it)
                imagePath = it.toString() // Сохраняем путь к кадрированному изображению
            }
        }
    }

    private suspend fun fetchTranslationEnRu(word: String): List<String> {
        return try {
            val response = ApiClient.yandexDictionaryApi.getTranslation(
                text = word,
                lang = "en-ru",
                apiKey = YANDEX_API_KEY
            )
            response.def.flatMap { definition ->
                definition.tr.map { it.text }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTranslationRuEn(word: String): List<String> {
        return try {
            val response = ApiClient.yandexDictionaryApi.getTranslation(
                text = word,
                lang = "ru-en",
                apiKey = YANDEX_API_KEY
            )
            response.def.flatMap { definition ->
                definition.tr.map { it.text }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchImages(query: String): List<String> {
        return try {
            val response = ApiClient.unsplashApi.searchImages(
                query = query,
                apiKey = USPLASH_API_KEY
            )
            response.results.map { it.urls.regular }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun startCropping(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(getAppImagesDir(), "cropped_${System.currentTimeMillis()}.jpg"))
        UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1280, 1280)
            .start(requireContext(), this)
    }

    private fun shouldCrop(uri: Uri): Boolean {
        val (width, height) = getImageDimensions(uri) ?: return false
        val maxSide = max(width, height)
        return maxSide > 2048
    }

    private fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        return try {
            val inputStream = when (uri.scheme?.lowercase()) {
                "file" -> FileInputStream(File(uri.path ?: return null))
                else -> requireContext().contentResolver.openInputStream(uri)
            } ?: return null
            inputStream.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                val width = options.outWidth
                val height = options.outHeight
                if (width > 0 && height > 0) Pair(width, height) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setupEnglishWordFocus() {
        binding.englishWordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val englishWord = binding.englishWordInput.text.toString()
                val russianWord = binding.russianWordInput.text.toString()
                if (englishWord.isNotEmpty() && russianWord.isEmpty()) {
                    lifecycleScope.launch {
                        val images = fetchImages(englishWord)
                        if (images.isNotEmpty()) {
                            val imageUrl = images.first()
                            loadImageFromUrl(imageUrl) { file ->
                                file?.let {
                                    val localFile = copyFileToInternalImages(it)
                                    val localUri = Uri.fromFile(localFile ?: it)
                                    binding.wordImageView.setImageURI(localUri)
                                    imagePath = localUri.toString()
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Изображение не найдено", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (englishWord.isNotEmpty() && russianWord.isEmpty()) {
                    lifecycleScope.launch {
                        val translations = fetchTranslationEnRu(englishWord)
                        if (translations.isNotEmpty()) {
                            binding.russianWordInput.setText(translations.first()) // Выбираем первый перевод
                        } else {
                            Toast.makeText(requireContext(), "Перевод не найден", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupRussianWordFocus() {
        binding.russianWordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val englishWord = binding.englishWordInput.text.toString()
                val russianWord = binding.russianWordInput.text.toString()
                if (russianWord.isNotEmpty() && englishWord.isEmpty()) {
                    lifecycleScope.launch {
                        val images = fetchImages(russianWord)
                        if (images.isNotEmpty()) {
                            val imageUrl = images.first()
                            loadImageFromUrl(imageUrl) { file ->
                                file?.let {
                                    val localFile = copyFileToInternalImages(it)
                                    val localUri = Uri.fromFile(localFile ?: it)
                                    binding.wordImageView.setImageURI(localUri)
                                    imagePath = localUri.toString()
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Изображение не найдено", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (russianWord.isNotEmpty() && englishWord.isEmpty()) {
                    lifecycleScope.launch {
                        val translations = fetchTranslationRuEn(russianWord)
                        if (translations.isNotEmpty()) {
                            binding.englishWordInput.setText(translations.first()) // Выбираем первый перевод
                        } else {
                            Toast.makeText(requireContext(), "Перевод не найден", Toast.LENGTH_SHORT).show()
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
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadImage(imageUrl: String) {
        Glide.with(requireContext())
            .load(imageUrl)
            .override(800, 600)
            .into(binding.wordImageView)
    }

    private fun copyImageToInternalStorage(uri: Uri): File? {
        return try {
            val resolver = requireContext().contentResolver
            val mimeType = resolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            val imagesDir = getAppImagesDir()

            val fileName = "img_${System.currentTimeMillis()}.$extension"
            val outFile = File(imagesDir, fileName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppImagesDir(): File {
        val imagesDir = File(requireContext().filesDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        return imagesDir
    }

    private fun copyFileToInternalImages(sourceFile: File, suggestedExtension: String? = null): File? {
        return try {
            val extension = suggestedExtension ?: sourceFile.extension.ifBlank { "jpg" }
            val destFile = File(getAppImagesDir(), "img_${System.currentTimeMillis()}.$extension")
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (_: Exception) {
            null
        }
    }

    private fun loadImageFromUrl(imageUrl: String, callback: (File?) -> Unit) {
        Glide.with(requireContext())
            .asFile()
            .load(imageUrl)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    callback(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    callback(null)
                }
            })
    }

    private fun setupTranslationButtonsState() {
        val english = binding.englishWordInput.text?.toString()?.trim().orEmpty()
        val russian = binding.russianWordInput.text?.toString()?.trim().orEmpty()
        binding.chooseRussianTranslationButton.isEnabled = english.isNotEmpty()
        binding.chooseEnglishTranslationButton.isEnabled = russian.isNotEmpty()
    }

    private fun bindCurrentCard() {
        currentCard?.let {
            binding.englishWordInput.setText(it.english)
            binding.russianWordInput.setText(it.russian)
            binding.hintInput.setText(it.info?.let { info ->
                try { org.json.JSONObject(info).optString("hint", "") } catch (_: Exception) { "" }
            })

            Glide.with(this)
                .load(it.imagePath)
                .into(binding.wordImageView)

            imagePath = it.imagePath

            lifecycleScope.launch {
                showReviewStats(it.commonId)
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 100

        private const val ARG_COMMON_ID = "common_id"

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
}
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
import com.example.tala.fragment.dialog.TranslationPickerDialog
import com.example.tala.integration.dictionary.YandexDictionaryApi.Companion.YANDEX_API_KEY
import com.example.tala.integration.picture.UnsplashApi.Companion.USPLASH_API_KEY
import com.example.tala.model.dto.CardListDto
import com.example.tala.service.ApiClient
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class AddWordFragment : Fragment() {

    private lateinit var binding: FragmentAddWordBinding

    private lateinit var cardViewModel: CardViewModel
    private lateinit var categoryViewModel: CategoryViewModel

    private var currentCard: CardListDto? = null
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val categories = mutableListOf<Category>()
    private var imagePath: String? = null

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

        currentCard?.let {
            binding.englishWordInput.setText(it.english)
            binding.russianWordInput.setText(it.russian)

            Glide.with(this)
                .load(it.imagePath)
                .into(binding.wordImageView)
//            binding.wordImageView.setImageURI(Uri.parse(it.imagePath))

            imagePath = it.imagePath
        }

        // Инициализация Spinner
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // Загрузка категорий
        loadCategories()

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

                    if (currentCard == null) {
                        val cardDto = CardListDto(
                            english = englishWord,
                            russian = russianWord,
                            categoryId = selectedCategory?.id ?: 0,
                            imagePath = imagePath,
                        )
                        cardViewModel.insert(cardDto)
                    } else {
                        val cardDto = CardListDto(
                            commonId = currentCard!!.commonId,
                            english = englishWord,
                            russian = russianWord,
                            categoryId = currentCard!!.categoryId,
                            imagePath = imagePath,
                        )
                        cardViewModel.update(cardDto)
                        parentFragmentManager.popBackStack()
                    }
                    binding.englishWordInput.text.clear()
                    binding.russianWordInput.text.clear()
                    binding.wordImageView.setImageDrawable(null)
                    imagePath = null
                    Toast.makeText(requireContext(), "Слово сохранено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Заполните оба поля!", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                println("---> ${e.message}")
            }
        }

        binding.reviewButton.setOnClickListener {
            val reviewFragment = ReviewFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, reviewFragment)
                .addToBackStack(null)
                .commit()
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

        // Обработка нажатия на кнопку "Добавить изображение"
//        binding.addImageButton.setOnClickListener {
//            openImagePicker()
//        }

//        setupTranslationListener()
        setupEnglishWordFocus()
        setupRussianWordFocus()

//        cropImageButton.setOnClickListener {
//            val imageUri = // Получи URI изображения (например, из Glide)
//                startCropping(imageUri)
//        }

        // Обработка нажатия на изображение
        binding.wordImageView.setOnClickListener {
            val dialog = ImagePickerDialog(
                initialQuery = binding.englishWordInput.text.toString(),
                onImageSelected = { imageUrl ->
                    if (imageUrl.startsWith("http", true)) {
                        loadImageFromUrl(imageUrl) { file ->
                            file?.let {
                                val fileUri = Uri.fromFile(file)
                                binding.wordImageView.setImageURI(fileUri)
                                imagePath = fileUri.toString() // Сохраняем путь к локальной копии
                            }
                        }
                    } else {
                        val fileUri = Uri.parse(imageUrl)
                        binding.wordImageView.setImageURI(fileUri)
                        imagePath = fileUri.toString() // Сохраняем путь к локальной копии
                    }
                }
            )
            dialog.show(parentFragmentManager, "ImagePickerDialog")
        }

        // Кнопка для выбора перевода
        binding.chooseRussianTranslationButton.setOnClickListener {
            lifecycleScope.launch {
                val englishWord = binding.englishWordInput.text.toString()
                val translations = fetchTranslationEnRu(englishWord)
                if (translations.isNotEmpty()) {
                    val dialog = TranslationPickerDialog(
                        translations = translations,
                        onTranslationSelected = { translation ->
                            binding.russianWordInput.setText(translation)
                        }
                    )
                    dialog.show(parentFragmentManager, "TranslationPickerDialog")
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
                    val dialog = TranslationPickerDialog(
                        translations = translations,
                        onTranslationSelected = { translation ->
                            binding.englishWordInput.setText(translation)
                        }
                    )
                    dialog.show(parentFragmentManager, "TranslationPickerDialog")
                } else {
                    Toast.makeText(requireContext(), "Нет доступных переводов", Toast.LENGTH_SHORT)
                        .show()
                }
            }
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

//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
//            val imageUri = data.data
//            imageUri?.let {
//                wordImageView.setImageURI(it)
//                imagePath = it.toString() // Сохраняем путь к изображению
//            }
//        }
//    }

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
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
        UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f) // Квадратное изображение
            .start(requireContext(), this)
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
                            val imageUrl = images.first() // Выбираем первое изображение
                            loadImageFromUrl(imageUrl) { file ->
                                file?.let {
                                    val fileUri = Uri.fromFile(file)
                                    binding.wordImageView.setImageURI(fileUri)
                                    imagePath = fileUri.toString() // Сохраняем путь к локальной копии
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
                            val imageUrl = images.first() // Выбираем первое изображение
                            loadImageFromUrl(imageUrl) { file ->
                                file?.let {
                                    val fileUri = Uri.fromFile(file)
                                    binding.wordImageView.setImageURI(fileUri)
                                    imagePath = fileUri.toString() // Сохраняем путь к локальной копии
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

    private fun loadImage(imageUrl: String) {
        Glide.with(requireContext())
            .load(imageUrl)
            .override(800, 600)
            .into(binding.wordImageView)
    }

    private fun copyImageToInternalStorage(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
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

    companion object {
        private const val REQUEST_IMAGE_PICK = 100

        private const val ARG_WORD = "word"

        fun newInstance(card: CardListDto?): AddWordFragment {
            val fragment = AddWordFragment()
            fragment.currentCard = card
            return fragment
        }
    }

}
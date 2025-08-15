package com.example.tala.fragment.dialog

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.tala.databinding.DialogImagePickerBinding
import com.example.tala.fragment.adapter.ImageAdapter
import com.example.tala.integration.picture.UnsplashApi.Companion.USPLASH_API_KEY
import com.example.tala.service.ApiClient
import kotlinx.coroutines.launch

class ImagePickerDialog(
    private val initialQuery: String,
    private val onImageSelected: (String) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogImagePickerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogImagePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация поля ввода
        binding.searchQueryInput.setText(initialQuery)

        // Обработка нажатия на кнопку поиска
        binding.searchQueryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Выполняем поиск при открытии диалога
        performSearch()

        binding.addImageButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun performSearch() {
        val query = binding.searchQueryInput.text.toString()
        if (query.isNotEmpty()) {
            searchImages(query)
        }
    }

    private fun searchImages(query: String) {
        lifecycleScope.launch {
            try {
                val images = fetchImages(query)
                val adapter = ImageAdapter(images) { imageUrl ->
                    onImageSelected(imageUrl)
                    dismiss()
                }
                binding.imageRecyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка при загрузке изображений", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchImages(query: String): List<String> {
        val response = ApiClient.unsplashApi.searchImages(
            query = query,
            apiKey = USPLASH_API_KEY
        )
        return response.results.map { it.urls.regular }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                onImageSelected(it.toString())
                dismiss()
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
    }
}
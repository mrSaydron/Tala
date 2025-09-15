package com.example.tala.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.tala.databinding.DialogImagePickerBinding
import com.example.tala.fragment.adapter.ImageAdapter
import com.example.tala.integration.picture.UnsplashApi.Companion.USPLASH_API_KEY
import com.example.tala.service.ApiClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ImagePickerDialog : BottomSheetDialogFragment() {

    private lateinit var binding: DialogImagePickerBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(KEY_URL_OR_URI to it.toString())
            )
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogImagePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialQuery = requireArguments().getString(ARG_INITIAL_QUERY).orEmpty()

        binding.searchQueryInput.setText(initialQuery)
        binding.searchInputLayout.isEndIconVisible = !binding.searchQueryInput.text.isNullOrBlank()
        binding.searchQueryInput.addTextChangedListener { text ->
            binding.searchInputLayout.isEndIconVisible = !text.isNullOrBlank()
        }

        binding.searchQueryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.searchInputLayout.setEndIconOnClickListener { performSearch() }

        performSearch()

        binding.addImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
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
                    parentFragmentManager.setFragmentResult(
                        RESULT_KEY,
                        bundleOf(KEY_URL_OR_URI to imageUrl)
                    )
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

    companion object {
        private const val ARG_INITIAL_QUERY = "initial_query"

        const val RESULT_KEY = "image_picker_result"
        const val KEY_URL_OR_URI = "url_or_uri"

        fun newInstance(initialQuery: String) = ImagePickerDialog().apply {
            arguments = bundleOf(ARG_INITIAL_QUERY to initialQuery)
        }
    }
}
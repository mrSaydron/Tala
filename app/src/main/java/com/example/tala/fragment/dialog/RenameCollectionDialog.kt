package com.example.tala.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import com.example.tala.R
import com.example.tala.ui.dialog.BaseMaterialDialogFragment

class RenameCollectionDialog : BaseMaterialDialogFragment() {

    override fun provideTitle() = "Переименовать коллекцию"

    override fun createContentView(): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_collection, null)
        val saveCollectionButton: Button = view.findViewById(R.id.saveCollectionButton)
        val collectionNameInput: EditText = view.findViewById(R.id.collectionNameInput)

        val initialName = requireArguments().getString(ARG_INITIAL_NAME).orEmpty()
        collectionNameInput.setText(initialName)
        collectionNameInput.setSelection(collectionNameInput.text.length)

        saveCollectionButton.setOnClickListener {
            val newName = collectionNameInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                val collectionId = requireArguments().getInt(ARG_COLLECTION_ID)
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(
                        KEY_COLLECTION_ID to collectionId,
                        KEY_NAME to newName
                    )
                )
                dismiss()
            } else {
                collectionNameInput.error = "Введите название"
            }
        }
        return view
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        private const val ARG_INITIAL_NAME = "initial_name"

        const val RESULT_KEY = "rename_collection_result"
        const val KEY_COLLECTION_ID = "collection_id"
        const val KEY_NAME = "name"

        fun newInstance(collectionId: Int, initialName: String) = RenameCollectionDialog().apply {
            arguments = bundleOf(
                ARG_COLLECTION_ID to collectionId,
                ARG_INITIAL_NAME to initialName
            )
        }
    }
}


package com.example.tala.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import com.example.tala.R
import com.example.tala.ui.dialog.BaseMaterialDialogFragment

class AddCollectionDialog : BaseMaterialDialogFragment() {

    override fun provideTitle() = "Добавить коллекцию"

    override fun createContentView(): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_collection, null)
        val saveCollectionButton: Button = view.findViewById(R.id.saveCollectionButton)
        val collectionNameInput: EditText = view.findViewById(R.id.collectionNameInput)

        saveCollectionButton.setOnClickListener {
            val collectionName = collectionNameInput.text.toString().trim()
            if (collectionName.isNotEmpty()) {
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(KEY_NAME to collectionName)
                )
                dismiss()
            } else {
                collectionNameInput.error = "Введите название"
            }
        }

        return view
    }

    companion object {
        const val RESULT_KEY = "add_collection_result"
        const val KEY_NAME = "name"

        fun newInstance() = AddCollectionDialog()
    }
}
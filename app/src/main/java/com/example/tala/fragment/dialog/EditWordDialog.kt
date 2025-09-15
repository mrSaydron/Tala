package com.example.tala.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import com.example.tala.R
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.ui.dialog.BaseMaterialDialogFragment

class EditWordDialog : BaseMaterialDialogFragment() {

    override fun provideTitle() = "Редактировать слово"

    override fun createContentView(): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_word, null)
        val editEnglishWord: EditText = view.findViewById(R.id.editEnglishWord)
        val editRussianWord: EditText = view.findViewById(R.id.editRussianWord)
        val editHint: EditText = view.findViewById(R.id.editHint)
        val saveEditButton: Button = view.findViewById(R.id.saveEditButton)

        val commonId = requireArguments().getString(ARG_COMMON_ID).orEmpty()
        val infoJson = requireArguments().getString(ARG_INFO_JSON).orEmpty()

        val parsedInfo = WordCardInfo.fromJson(infoJson)
        parsedInfo.english?.let { editEnglishWord.setText(it) }
        parsedInfo.russian?.let { editRussianWord.setText(it) }
        parsedInfo.hint?.let { if (it.isNotEmpty()) editHint.setText(it) }

        saveEditButton.setOnClickListener {
            val englishWord = editEnglishWord.text.toString().trim()
            val russianWord = editRussianWord.text.toString().trim()
            if (englishWord.isNotEmpty() && russianWord.isNotEmpty()) {
                val hint = editHint.text.toString().trim().ifEmpty { null }
                val updatedInfoJson = parsedInfo.copy(
                    english = englishWord,
                    russian = russianWord,
                    hint = hint
                ).toJsonOrNull()
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(
                        KEY_COMMON_ID to commonId,
                        KEY_INFO_JSON to updatedInfoJson
                    )
                )
                dismiss()
            } else {
                editEnglishWord.error = if (englishWord.isEmpty()) "Обязательное поле" else null
                editRussianWord.error = if (russianWord.isEmpty()) "Обязательное поле" else null
            }
        }

        return view
    }

    companion object {
        private const val ARG_COMMON_ID = "common_id"
        private const val ARG_INFO_JSON = "info_json"

        const val RESULT_KEY = "edit_word_result"
        const val KEY_COMMON_ID = "common_id"
        const val KEY_INFO_JSON = "info_json"

        fun newInstance(commonId: String, infoJson: String) = EditWordDialog().apply {
            arguments = bundleOf(
                ARG_COMMON_ID to commonId,
                ARG_INFO_JSON to infoJson
            )
        }
    }
}
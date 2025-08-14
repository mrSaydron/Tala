package com.example.tala.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.tala.R
import com.example.tala.entity.card.Card

class EditWordDialog(private val card: Card, private val onSave: (Card) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_edit_word, null)
        val editEnglishWord: EditText = view.findViewById(R.id.editEnglishWord)
        val editRussianWord: EditText = view.findViewById(R.id.editRussianWord)
        val editHint: EditText = view.findViewById(R.id.editHint)
        val saveEditButton: Button = view.findViewById(R.id.saveEditButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Редактировать слово")
            .create()

        // Заполняем поля текущими значениями
        editEnglishWord.setText(card.english)
        editRussianWord.setText(card.russian)
        // info.hint в json-строке info (если есть)
        card.info?.let { infoJson ->
            try {
                val hint = org.json.JSONObject(infoJson).optString("hint", "")
                if (hint.isNotEmpty()) editHint.setText(hint)
            } catch (_: Exception) { }
        }

        // Обработка нажатия на кнопку "Сохранить"
        saveEditButton.setOnClickListener {
            val englishWord = editEnglishWord.text.toString()
            val russianWord = editRussianWord.text.toString()

            if (englishWord.isNotEmpty() && russianWord.isNotEmpty()) {
                val hint = editHint.text.toString().trim()
                val newInfo = if (hint.isNotEmpty()) {
                    try {
                        val base = if (card.info.isNullOrEmpty()) org.json.JSONObject() else org.json.JSONObject(card.info)
                        base.put("hint", hint)
                        base.toString()
                    } catch (_: Exception) {
                        org.json.JSONObject().put("hint", hint).toString()
                    }
                } else {
                    // если подсказка очищена, пробуем удалить её из info
                    try {
                        if (!card.info.isNullOrEmpty()) {
                            val base = org.json.JSONObject(card.info)
                            base.remove("hint")
                            if (base.length() == 0) null else base.toString()
                        } else null
                    } catch (_: Exception) { card.info }
                }

                val updatedWord = card.copy(english = englishWord, russian = russianWord, info = newInfo)
                onSave(updatedWord)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Заполните оба поля!", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}
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
        val saveEditButton: Button = view.findViewById(R.id.saveEditButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Редактировать слово")
            .create()

        // Заполняем поля текущими значениями
        editEnglishWord.setText(card.english)
        editRussianWord.setText(card.russian)

        // Обработка нажатия на кнопку "Сохранить"
        saveEditButton.setOnClickListener {
            val englishWord = editEnglishWord.text.toString()
            val russianWord = editRussianWord.text.toString()
            val categoryId = 

            if (englishWord.isNotEmpty() && russianWord.isNotEmpty()) {
                val updatedWord = card.copy(english = englishWord, russian = russianWord)
                onSave(updatedWord)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Заполните оба поля!", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}
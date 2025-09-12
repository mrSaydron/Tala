package com.example.tala.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.tala.R

class RenameCollectionDialog(
    private val initialName: String,
    private val onSave: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_add_collection, null)

        val saveCollectionButton: Button = view.findViewById(R.id.saveCollectionButton)
        val collectionNameInput: EditText = view.findViewById(R.id.collectionNameInput)

        collectionNameInput.setText(initialName)
        collectionNameInput.setSelection(collectionNameInput.text.length)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Переименовать коллекцию")
            .create()

        saveCollectionButton.setOnClickListener {
            val newName = collectionNameInput.text.toString()
            if (newName.isNotEmpty()) {
                onSave(newName)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Введите название коллекции!", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}



package com.example.tala.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.tala.R

class AddCollectionDialog(private val onSave: (String) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_add_collection, null)

        val saveCollectionButton: Button = view.findViewById(R.id.saveCollectionButton)
        val collectionNameInput: EditText = view.findViewById(R.id.collectionNameInput)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Добавить коллекцию")
            .create()

        saveCollectionButton.setOnClickListener {
            val collectionName = collectionNameInput.text.toString()

            if (collectionName.isNotEmpty()) {
                onSave(collectionName)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Введите название коллекции!", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}
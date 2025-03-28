package com.example.tala.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.fragment.adapter.TranslationAdapter

class TranslationPickerDialog(
    private val translations: List<String>,
    private val onTranslationSelected: (String) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_translation_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.translationRecyclerView)
        val adapter = TranslationAdapter(translations) { translation ->
            onTranslationSelected(translation)
            dismiss()
        }
        recyclerView.adapter = adapter
    }
}
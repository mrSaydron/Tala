package com.example.tala.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseMaterialDialogFragment : DialogFragment() {

    protected open val isCancelableOutside: Boolean = true

    protected abstract fun provideTitle(): String?
    protected abstract fun createContentView(): View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = createContentView()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(provideTitle())
            .setView(view)
            .create().apply {
                setCanceledOnTouchOutside(isCancelableOutside)
            }
    }
}



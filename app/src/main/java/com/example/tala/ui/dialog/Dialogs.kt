package com.example.tala.ui.dialog

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Dialogs {
    fun confirm(
        context: Context,
        title: String,
        message: String,
        onOk: () -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                onOk()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onCancel?.invoke()
            }
            .show()
    }

    fun chooseFrom(
        context: Context,
        title: String,
        items: List<String>,
        onChosen: (String) -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        val arr = items.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(arr) { dialog, which ->
                dialog.dismiss()
                onChosen(arr[which])
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onCancel?.invoke()
            }
            .show()
    }
}



package com.joory.whiteboardapp.functions

import android.app.Dialog
import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog


class Dialogs(context: Context) {
    var dialog: Dialog = BottomSheetDialog(context)

    fun showDialog(layout: Int) {
        dialog.setContentView(layout)
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
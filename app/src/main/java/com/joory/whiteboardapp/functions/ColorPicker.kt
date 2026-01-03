package com.joory.whiteboardapp.functions

import androidx.fragment.app.FragmentManager
import com.abhishek.colorpicker.ColorPickerDialog

class ColorPicker(var fragmentManager: FragmentManager) {
    fun colorsDialog(func: (input: Int) -> Unit) {
        val dialog = ColorPickerDialog()
        dialog.setOnOkCancelListener { isOk, color ->
            if (isOk) {
                func(color)
            }
        }
        dialog.show(fragmentManager)
    }

}

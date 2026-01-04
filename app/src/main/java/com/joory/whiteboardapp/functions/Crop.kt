package com.joory.whiteboardapp.functions

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.joory.whiteboardapp.MyCanvas
import com.joory.whiteboardapp.shapes.ImageShape
import java.io.File

class Crop {
    fun startCrop(
        canvas: MyCanvas,
        file: File,
        cropImage: ActivityResultLauncher<CropImageContractOptions>
    ) {
        if (canvas.objectIndex != null) {
            val shape = canvas.draws[canvas.objectIndex!!]
            if (shape is ImageShape) {
                val bitmap = shape.bitmap
                if (bitmap != null) {
                    try {
                        // Write to temp file
                        val file = File(file, "crop_temp.png")
                        val fOut = java.io.FileOutputStream(file)
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fOut)
                        fOut.flush()
                        fOut.close()

                        val options = CropImageOptions()
                        options.imageSourceIncludeGallery = true
                        options.imageSourceIncludeCamera = true

                        cropImage.launch(
                            CropImageContractOptions(
                                uri = Uri.fromFile(file),
                                cropImageOptions = options
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

}
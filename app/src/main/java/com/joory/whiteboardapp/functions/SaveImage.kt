package com.joory.whiteboardapp.functions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import com.joory.whiteboardapp.MyCanvas
import com.joory.whiteboardapp.R
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class SaveImage {
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveImage(myCanvas: MyCanvas) {
        if (myCanvas.objectIndex != null) {
            myCanvas.tmpObjectIndex = myCanvas.objectIndex
            myCanvas.objectIndex = null
            myCanvas.invalidate()
        }
        val bitmap = createBitmap(myCanvas.width, myCanvas.height)
        val canvas = Canvas(bitmap)
        myCanvas.draw(canvas)
        val imagePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/Whiteboard/"
        if (!File(imagePath).exists()) {
            File(imagePath).mkdirs()
        }
        val file = File(imagePath + LocalDateTime.now().toString().replace(":", ".") + ".png")
        val newFile = FileOutputStream(file)
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, newFile)
            MediaScannerConnection.scanFile(
                myCanvas.context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
            newFile.flush()
            newFile.close()
            myCanvas.myMain?.ads?.showAds()
            if (myCanvas.tmpObjectIndex != null) {
                myCanvas.objectIndex = myCanvas.tmpObjectIndex
                myCanvas.tmpObjectIndex = null
                myCanvas.invalidate()
            }
            Toast.makeText(
                myCanvas.context,
                myCanvas.resources.getText(R.string.image_saved),
                Toast.LENGTH_SHORT
            )
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
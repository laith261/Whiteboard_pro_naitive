package com.joory.whiteboardapp.functions

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.scale
import com.joory.whiteboardapp.MyCanvas

class SetImageBg {
    fun setImageBackgroundProcess(myCanvas: MyCanvas) {
        if (myCanvas.imgUri == null) return
        val bitmap =
            ImageUtils.decodeSampledBitmapFromUri(
                myCanvas.context,
                myCanvas.imgUri!!,
                myCanvas.width,
                myCanvas.height
            )
                ?: return

        val myMatrix = Matrix()
        myMatrix.setRotate(myCanvas.oren.toFloat())
        val theWidth =
            if (myCanvas.oren > 0 && myCanvas.oren != 180) bitmap.height else bitmap.width
        val theHeight =
            if (myCanvas.oren > 0 && myCanvas.oren != 180) bitmap.width else bitmap.height

        if (theWidth > theHeight) {
            val widthAspect = theHeight.toFloat() / theWidth
            myCanvas.imgBG =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, myMatrix, true)
                    .scale(myCanvas.width, (myCanvas.width * widthAspect).toInt())
        } else {
            val heightAspect = theWidth.toFloat() / theHeight
            myCanvas.imgBG =
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    myMatrix,
                    true
                ).scale((myCanvas.height * heightAspect).toInt(), myCanvas.height)
        }
        myCanvas.imgUri = null
        myCanvas.invalidate()
    }

}
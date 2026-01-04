package com.joory.whiteboardapp.functions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream

object ImageUtils {

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var inputStream: InputStream? = null
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            inputStream = context.contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

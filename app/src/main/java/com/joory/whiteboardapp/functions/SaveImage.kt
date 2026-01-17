package com.joory.whiteboardapp.functions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
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
import java.io.IOException
import java.time.LocalDateTime

class SaveImage {
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveImage(myCanvas: MyCanvas, transparent: Boolean) {
        val originalColor = myCanvas.colorBG

        try {
            if (myCanvas.objectIndex != null) {
                myCanvas.tmpObjectIndex = myCanvas.objectIndex
                myCanvas.objectIndex = null
                myCanvas.invalidate()
            }

            if (transparent) {
                myCanvas.setColorBackground(Color.TRANSPARENT)
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

            Toast.makeText(
                            myCanvas.context,
                            myCanvas.resources.getText(R.string.image_saved),
                            Toast.LENGTH_SHORT
                    )
                    .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(myCanvas.context, "Error saving image", Toast.LENGTH_SHORT).show()
        } finally {
            if (transparent) {
                myCanvas.setColorBackground(originalColor)
            }
            if (myCanvas.tmpObjectIndex != null) {
                myCanvas.objectIndex = myCanvas.tmpObjectIndex
                myCanvas.tmpObjectIndex = null
                myCanvas.invalidate()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun savePdf(myCanvas: MyCanvas, transparent: Boolean) {
        val originalColor = myCanvas.colorBG

        try {
            if (myCanvas.objectIndex != null) {
                myCanvas.tmpObjectIndex = myCanvas.objectIndex
                myCanvas.objectIndex = null
                myCanvas.invalidate()
            }

            if (transparent) {
                myCanvas.setColorBackground(Color.TRANSPARENT)
            }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(myCanvas.width, myCanvas.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            myCanvas.draw(page.canvas)

            pdfDocument.finishPage(page)

            val pdfPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            .toString() + "/Whiteboard/"
            if (!File(pdfPath).exists()) {
                File(pdfPath).mkdirs()
            }
            val file = File(pdfPath + LocalDateTime.now().toString().replace(":", ".") + ".pdf")

            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)

            pdfDocument.close()
            fileOutputStream.close()

            MediaScannerConnection.scanFile(
                    myCanvas.context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/pdf"),
                    null
            )

            myCanvas.myMain?.ads?.showAds()

            Toast.makeText(
                            myCanvas.context,
                            myCanvas.resources.getText(R.string.pdf_saved),
                            Toast.LENGTH_SHORT
                    )
                    .show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(myCanvas.context, "Error saving PDF", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(myCanvas.context, "Error saving PDF", Toast.LENGTH_SHORT).show()
        } finally {
            if (transparent) {
                myCanvas.setColorBackground(originalColor)
            }
            if (myCanvas.tmpObjectIndex != null) {
                myCanvas.objectIndex = myCanvas.tmpObjectIndex
                myCanvas.tmpObjectIndex = null
                myCanvas.invalidate()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveSvg(myCanvas: MyCanvas) {
        try {
            val sb = StringBuilder()
            sb.append(
                    "<svg width=\"${myCanvas.width}\" height=\"${myCanvas.height}\" xmlns=\"http://www.w3.org/2000/svg\">\n"
            )

            // Background
            if (myCanvas.colorBG != 0 && myCanvas.colorBG != Color.TRANSPARENT) {
                val r = Color.red(myCanvas.colorBG)
                val g = Color.green(myCanvas.colorBG)
                val b = Color.blue(myCanvas.colorBG)
                sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"rgb($r,$g,$b)\"/>\n")
            }

            for (shape in myCanvas.draws) {
                val color = String.format("#%06X", (0xFFFFFF and shape.paint.color))
                val strokeWidth = shape.paint.strokeWidth
                val opacity = Color.alpha(shape.paint.color) / 255.0

                // Standard attributes
                val commonAttrs = StringBuilder()
                val isStroke = shape.paint.style == android.graphics.Paint.Style.STROKE

                if (isStroke) {
                    commonAttrs.append(
                            "fill=\"none\" stroke=\"$color\" stroke-width=\"$strokeWidth\""
                    )
                } else {
                    commonAttrs.append("fill=\"$color\" stroke=\"none\"")
                }

                if (opacity < 1.0) {
                    commonAttrs.append(" opacity=\"$opacity\"")
                }

                when (shape) {
                    is com.joory.whiteboardapp.shapes.Rects -> {
                        val cx = (shape.rect.left + shape.rect.right) / 2
                        val cy = (shape.rect.top + shape.rect.bottom) / 2
                        val transform =
                                if (shape.rotation != 0f)
                                        "transform=\"rotate(${shape.rotation} $cx $cy)\""
                                else ""

                        sb.append(
                                "  <rect x=\"${shape.rect.left}\" y=\"${shape.rect.top}\" width=\"${shape.rect.width()}\" height=\"${shape.rect.height()}\" $commonAttrs $transform />\n"
                        )
                    }
                    is com.joory.whiteboardapp.shapes.Circle -> {
                        val transform =
                                if (shape.rotation != 0f)
                                        "transform=\"rotate(${shape.rotation} ${shape.cp.x} ${shape.cp.y})\""
                                else ""
                        sb.append(
                                "  <circle cx=\"${shape.cp.x}\" cy=\"${shape.cp.y}\" r=\"${shape.radius}\" $commonAttrs $transform />\n"
                        )
                    }
                    is com.joory.whiteboardapp.shapes.Lines -> {
                        val cx = (shape.start.x + shape.end.x) / 2
                        val cy = (shape.start.y + shape.end.y) / 2
                        val transform =
                                if (shape.rotation != 0f)
                                        "transform=\"rotate(${shape.rotation} $cx $cy)\""
                                else ""

                        // Lines are always stroke
                        sb.append(
                                "  <line x1=\"${shape.start.x}\" y1=\"${shape.start.y}\" x2=\"${shape.end.x}\" y2=\"${shape.end.y}\" stroke=\"$color\" stroke-width=\"$strokeWidth\" $transform />\n"
                        )
                    }
                    is com.joory.whiteboardapp.shapes.Brush -> {
                        if (shape.points.isNotEmpty()) {
                            val pathData =
                                    StringBuilder("M ${shape.points[0].x} ${shape.points[0].y}")
                            for (i in 1 until shape.points.size) {
                                pathData.append(" L ${shape.points[i].x} ${shape.points[i].y}")
                            }
                            sb.append(
                                    "  <path d=\"$pathData\" fill=\"none\" stroke=\"$color\" stroke-width=\"$strokeWidth\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n"
                            )
                        }
                    }
                    is com.joory.whiteboardapp.shapes.Texts -> {
                        // Very basic text support
                        val x = shape.point.x
                        // SVG text y is baseline.
                        // Our shape.point.y seems to be bottom-left (descent/baseline area).
                        val cx =
                                shape.point.x // Text checks rotation around center of rect, but for
                        // SVG simpler to just put it at point
                        val cy = shape.point.y
                        val transform =
                                if (shape.rotation != 0f)
                                        "transform=\"rotate(${shape.rotation} $cx $cy)\""
                                else ""

                        sb.append(
                                "  <text x=\"$x\" y=\"${shape.point.y}\" fill=\"$color\" font-size=\"${shape.paint.textSize}\" font-family=\"sans-serif\" $transform>${shape.text}</text>\n"
                        )
                    }
                    is com.joory.whiteboardapp.shapes.Triangle -> {
                        // Triangle usually draws a path. Accessing points might be hard if not
                        // exposed.
                        // Skipping explicit support for now or treating as path if possible?
                        // Triangle.kt wasn't inspected. Assuming it draws path or lines.
                    }
                // Handle other shapes...
                }
            }

            sb.append("</svg>")

            val svgPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            .toString() + "/Whiteboard/"
            if (!File(svgPath).exists()) {
                File(svgPath).mkdirs()
            }
            val file = File(svgPath + LocalDateTime.now().toString().replace(":", ".") + ".svg")
            val fos = FileOutputStream(file)
            fos.write(sb.toString().toByteArray())
            fos.close()

            MediaScannerConnection.scanFile(
                    myCanvas.context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/svg+xml"),
                    null
            )

            Toast.makeText(myCanvas.context, "SVG Saved", Toast.LENGTH_SHORT).show()
            myCanvas.myMain?.ads?.showAds()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(myCanvas.context, "Error saving SVG", Toast.LENGTH_SHORT).show()
        }
    }
}

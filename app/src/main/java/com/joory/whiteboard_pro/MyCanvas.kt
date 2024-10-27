package com.joory.whiteboard_pro

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.ArrayMap
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.joory.whiteboard_pro.shapes.Arrow
import com.joory.whiteboard_pro.shapes.Brush
import com.joory.whiteboard_pro.shapes.Circle
import com.joory.whiteboard_pro.shapes.Lines
import com.joory.whiteboard_pro.shapes.Rects
import com.joory.whiteboard_pro.shapes.Select
import com.joory.whiteboard_pro.shapes.Shape
import com.joory.whiteboard_pro.shapes.Shapes
import com.joory.whiteboard_pro.shapes.Texts
import com.joory.whiteboard_pro.shapes.Triangle
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime


class MyCanvas(context: Context?, args: AttributeSet?) : View(context, args) {
    private lateinit var myMain: MainActivity
    var draws = ArrayList<Shape>()
    var paint: Paint = Paint().apply {
        strokeWidth = 5f
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    var undo = ArrayList<Shape>()
    var tool: Shapes = Shapes.Brush
    var tools = ArrayMap<Shapes, Shape>()
    private var colorBG: Int = Color.WHITE
    private lateinit var bitmap: Bitmap
    private var imgBG: Bitmap? = null
    lateinit var dialog: Dialog
    var objectIndex: Int? = null
    private var example: Shape? = null

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        tools[Shapes.Rect] = Rects()
        tools[Shapes.Arrow] = Arrow()
        tools[Shapes.Circle] = Circle()
        tools[Shapes.Line] = Lines()
        tools[Shapes.Brush] = Brush()
        tools[Shapes.Select] = Select()
        tools[Shapes.Text] = Texts()
        tools[Shapes.Triangle] = Triangle()
        myMain = MainActivity.getmInstanceActivity()!!
        setBG(canvas)
        newDrawing(canvas)
        if (objectIndex != null) {
            drawSelectedBox(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e!!.action) {
            MotionEvent.ACTION_DOWN -> {
                if (tool == Shapes.Select) {
                    checkObjectTouching(e)
                }

                if (tool != Shapes.Select || !isTouchingSameObject(e)) {
                    objectIndex=null
                    myMain.selectedItemButton()
                    draws.add(tools[tool]!!.create(e))
                    updateStyle()
                }

            }

            MotionEvent.ACTION_MOVE -> {
                if (objectIndex == null) {
                    draws.last().update(e)
                } else {
                    draws[objectIndex!!].move(e)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (tool == Shapes.Text && objectIndex == null) {
                    setTextDialog()
                }

                if (tool != Shapes.Select) {
                    objectIndex = draws.indexOf(draws.last())
                    myMain.selectedItemButton()
                    invalidate()
                }
            }
        }
        return true
    }

    private fun isTouchingSameObject(e: MotionEvent): Boolean {
        if (objectIndex==null) return false
        for (i in draws.reversed()) {
            if (i.isTouchingObject(e)) {
                if (objectIndex == draws.indexOf(i)) {
                    return true
                }
            }
        }
        return false
    }

    private fun newDrawing(canvas: Canvas) {
        for (i in draws) {
            i.draw(canvas)
        }
        example?.draw(canvas)
    }

    private fun updateStyle() {
        draws.last().updateObject(paint)
    }

    fun setColorBackground(color: Int) {
        colorBG = color
        imgBG = null
        invalidate()
    }

    fun setImageBackground(img: InputStream) {
        bitmap = BitmapFactory.decodeStream(img)
        imgBG = Bitmap.createScaledBitmap(bitmap, width, height, true)
        invalidate()
    }

    private fun setBG(canvas: Canvas) {
        canvas.drawColor(colorBG)
        if (imgBG != null) {
            canvas.drawBitmap(imgBG!!, 0f, 0f, null)
        }
    }

    fun undo() {
        if (draws.isNotEmpty()) {
            undo.add(draws.last())
            draws.remove(draws.last())
            objectIndex = null
            invalidate()
        }
    }

    fun redo() {
        if (undo.isNotEmpty()) {
            draws.add(undo.last())
            undo.remove(draws.last())
            invalidate()
        }
    }

    fun createExample(width: Int, height: Int) {
        example = Lines().example(width, height)
    }

    fun updateExample() {
        if (example != null) {
            example!!.updateObject(paint.apply { color = Color.BLACK })
            invalidate()
        }
    }

    fun removeExample() {
        example = null
        invalidate()
    }

    private fun setTextDialog() {
        dialog.setContentView(R.layout.text_dialog)
        dialog.create()
        dialog.show()
        val text = dialog.findViewById<EditText>(R.id.theText)
        dialog.findViewById<Button>(R.id.button).setOnClickListener {
            draws.last().text = text.text.toString()
            invalidate()
            dialog.dismiss()
        }
    }

    private fun checkObjectTouching(e: MotionEvent): Boolean {
        for (i in draws.reversed()) {
            if (i.isTouchingObject(e)) {
                objectIndex = draws.indexOf(i)
                myMain.selectedItemButton()
                myMain.hideButtons()
                invalidate()
                return true
            }
        }
        myMain.selectedItemButton()
        objectIndex = null
        invalidate()
        return false
    }

    private fun drawSelectedBox(canvas: Canvas) {
        draws[objectIndex!!].drawSelectedBox(canvas)
    }

    @SuppressLint("SdCardPath", "NewApi")
    fun saveImage() {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/" + LocalDateTime.now().toString().replace(":", ".") + ".png"
        )
        val newFile = FileOutputStream(file)
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, newFile)
            MediaScannerConnection.scanFile(
                this.context,
                arrayOf(file.absolutePath), arrayOf("image/png"), null
            )
            newFile.flush()
            newFile.close()
            myMain.showAds()
            Toast.makeText(this.context, "image Saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteItem() {
        if (objectIndex != null) {
            undo.add(draws[objectIndex!!])
            draws.removeAt(objectIndex!!)
            objectIndex = null
            invalidate()
        }
    }

    fun getCanvasPaint(): Paint {
        return if (objectIndex != null) draws[objectIndex!!].paint else paint
    }

    fun duplicateItem() {
        if (objectIndex != null) {
            Log.i("count1", draws.count().toString())
            draws.add(draws[objectIndex!!].deepCopy())
            Log.i("count2", draws.count().toString())
            invalidate()
        }
    }
}

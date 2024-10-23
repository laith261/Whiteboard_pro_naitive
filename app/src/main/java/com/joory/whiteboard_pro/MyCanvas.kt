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
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
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
    var draws = ArrayList<Shape>()
    var paint: Paint = Paint().apply {
        strokeWidth = 5f
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private var undo = ArrayList<Shape>()
    var tool: Shapes = Shapes.Brush
    var tools = ArrayMap<Shapes, Shape>()
    private var colorBG: Int = Color.WHITE
    private lateinit var bitmap: Bitmap
    private var imgBG: Bitmap? = null
    lateinit var dialog: Dialog
    var objectIndex: Int? = null
    var example: Shape? = null

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
        setBG(canvas)
        newDrawing(canvas)
        if (objectIndex != null) {
            drawSelectedBox(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (tool != Shapes.Select) {
            when (e!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    draws.add(tools[tool]!!.create(e))
                    updateStyle()
                }
                MotionEvent.ACTION_MOVE -> {
                    draws.last().update(e)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    if (tool == Shapes.Text) {
                        setTextDialog()
                    }
                }
            }
        } else {
            when (e!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    checkObjectTouching(e)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (objectIndex != null) {
                        draws[objectIndex!!].move(e)
                        invalidate()
                    }
                }
            }
        }

        return true
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
            example=null
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
            dialog.hide()
        }
    }

    private fun checkObjectTouching(e: MotionEvent) {
        for (i in draws) {
            if (i.isTouchingObject(e)) {
                objectIndex = draws.indexOf(i)
                invalidate()
                break
            } else {
                objectIndex = null
                invalidate()
            }
        }

    }

    private fun drawSelectedBox(canvas: Canvas) {
        draws[objectIndex!!].drawSelectedBox(canvas)
    }

    @SuppressLint("SdCardPath", "NewApi")
    fun saveImage() {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)

        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/"+LocalDateTime.now().toString().replace(":",".") +".png")
        val newFile=FileOutputStream(file);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, newFile)
            MediaScannerConnection.scanFile(this.context,
                arrayOf(file.absolutePath), arrayOf("image/png"),null)
            newFile.flush()
            newFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun deleteItem(){
        if(objectIndex!=null){
            undo.add(draws.get(objectIndex!!))
            draws.removeAt(objectIndex!!)
            objectIndex=null
            invalidate()
        }
    }
}

package com.joory.whiteboard_pro

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.ArrayMap
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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
        textSize = 50f
    }
    var undo = ArrayList<Shape>()
    var tool: Shapes = Shapes.Brush
    var tools = ArrayMap<Shapes, Shape>()
    private var colorBG: Int = Color.WHITE
    private var imgBG: Bitmap? = null
    lateinit var dialog: Dialog
    var objectIndex: Int? = null
    private var tmpObjectIndex: Int? = null
    private var example: Shape? = null
    var sideLength: Float = 100f
    private var file: InputStream? = null
    private var oren: Int = 0

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        tools[Shapes.Rect] = Rects()
        tools[Shapes.Arrow] = Arrow()
        tools[Shapes.Circle] = Circle()
        tools[Shapes.Line] = Lines()
        tools[Shapes.Brush] = Brush()
        tools[Shapes.Select] = Select()
        tools[Shapes.Text] = Texts()
//        tools[Shapes.Triangle] = Triangle()
        myMain = MainActivity.getmInstanceActivity()!!
        if (file != null) {
            setImageBackgroundProcess()
        }
        setBG(canvas)
        startDrawing(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e!!.action) {
            MotionEvent.ACTION_DOWN -> {
                if (tool == Shapes.Select) {
                    checkObjectTouching(e)
                }

                if (tool != Shapes.Select && !isTouchingSameObject(e)) {
                    objectIndex = null
                    myMain.selectedItemButton()
                    draws.add(tools[tool]!!.create(e))
                    updateStyle()
                }

            }

            MotionEvent.ACTION_MOVE -> {
                if (objectIndex == null && tool != Shapes.Select) {
                    draws.last().update(e)
                }
                if (objectIndex != null) {
                    draws[objectIndex!!].move(e)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (tool == Shapes.Text && objectIndex == null) {
                    setTextDialog()
                }

                if (tool in arrayOf(Shapes.Circle, Shapes.Rect, Shapes.Line)) {
                    objectIndex = draws.indexOf(draws.last())
                    myMain.selectedItemButton()
                    invalidate()
                }
                myMain.doButtonsAlpha()
            }
        }
        return true
    }

    private fun isTouchingSameObject(e: MotionEvent): Boolean {
        if (objectIndex == null) return false
        for (i in draws.reversed()) {
            if (i.isTouchingObject(e)) {
                if (objectIndex == draws.indexOf(i)) {
                    return true
                }
            }
        }
        return false
    }

    private fun startDrawing(canvas: Canvas) {
        for (i in draws) {
            i.draw(canvas)
        }
        if (objectIndex != null) {
            draws[objectIndex!!].drawSelectedBox(canvas)
        }
        example?.draw(canvas)
    }

    private fun updateStyle() {
        draws.last().updateObject(paint)
        draws.last().updateSideLength(sideLength)
    }

    fun setColorBackground(color: Int) {
        colorBG = color
        imgBG = null
        invalidate()
    }

    fun setImageBackground(img: InputStream, oren: Int) {
        file = img
        this.oren = oren
        invalidate()
    }

    private fun setImageBackgroundProcess() {
        val bitmap = BitmapFactory.decodeStream(file)!!
        val myMatrix = Matrix()
        myMatrix.setRotate(oren.toFloat())
        val theWidth = if (oren > 0 && oren != 180) bitmap.height else bitmap.width
        val theHeight = if (oren > 0 && oren != 180) bitmap.width else bitmap.height

        if (theWidth > theHeight) {
            val widthAspect = theHeight.toFloat() / theWidth
            imgBG = Bitmap.createScaledBitmap(
                Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    myMatrix, true
                ), width, (width * widthAspect).toInt(), true
            )
        } else {
            val heightAspect = theWidth.toFloat() / theHeight
            imgBG = Bitmap.createScaledBitmap(
                Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    myMatrix, true
                ), (height * heightAspect).toInt(), height, true
            )
        }
        file = null
        invalidate()
    }

    private fun setBG(canvas: Canvas) {
        canvas.drawColor(colorBG)
        if (imgBG != null) {
            val left = (width - imgBG!!.width) / 2
            val top = (height - imgBG!!.height) / 2
            canvas.drawBitmap(imgBG!!, left.toFloat(), top.toFloat(), null)
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
        dialog.findViewById<ImageView>(R.id.addText).setOnClickListener {
            if (text.text.isNotEmpty()) {
                draws.last().text = text.text.toString()
                objectIndex = draws.indexOf(draws.last())
                myMain.selectedItemButton()
            }
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
        objectIndex = null
        myMain.selectedItemButton()
        invalidate()
        return false
    }

    @SuppressLint("SdCardPath", "NewApi")
    fun saveImage() {
        if (objectIndex != null) {
            tmpObjectIndex = objectIndex
            objectIndex = null
            invalidate()
        }
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        val imagePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/Whiteboard/"
        if (!File(imagePath).exists()) {
            File(imagePath).mkdirs()
        }
        val file = File(
            imagePath + LocalDateTime.now().toString().replace(":", ".") + ".png"
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
            if (tmpObjectIndex != null) {
                objectIndex = tmpObjectIndex
                tmpObjectIndex = null
                invalidate()
            }
            Toast.makeText(
                this.context,
                resources.getText(R.string.image_saved),
                Toast.LENGTH_SHORT
            ).show()
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
            draws.add(draws[objectIndex!!].deepCopy())
            invalidate()
        }
    }

    fun clearCanvas() {
        undo.clear()
        undo.addAll(draws)
        draws.clear()
        objectIndex = null
        myMain.doButtonsAlpha()
        myMain.selectedItemButton()
        imgBG = null
        invalidate()
    }

    fun changeStyle() {
        val isFill = getCanvasPaint().style == Paint.Style.STROKE
        val style = if (isFill) Paint.Style.FILL else Paint.Style.STROKE
        paint.style = style
        if (objectIndex != null) {
            draws[objectIndex!!].paint.style = style
        }
        myMain.hideButtons()
        invalidate()
    }

    fun objectColorSet(color: Int) {
        getCanvasPaint().color = color
        paint.color = color
        invalidate()
    }
}
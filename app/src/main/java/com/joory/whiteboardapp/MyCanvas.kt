package com.joory.whiteboardapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.joory.whiteboardapp.shapes.ImageShape
import com.joory.whiteboardapp.shapes.Lines
import com.joory.whiteboardapp.shapes.Shape
import com.joory.whiteboardapp.shapes.Shapes

class MyCanvas(context: Context?, args: AttributeSet?) : View(context, args) {
    var myMain: MainActivity? = null
    var draws = ArrayList<Shape>()
    var paint: Paint =
            Paint().apply {
                strokeWidth = 5f
                color = Color.BLACK
                style = Paint.Style.FILL
                textSize = 50f
            }
    var undo = ArrayList<Shape>()
    var tool: Shapes = Shapes.Brush

    private var colorBG: Int = Color.WHITE
    var imgBG: Bitmap? = null
    var objectIndex: Int? = null
    var tmpObjectIndex: Int? = null
    private var example: Shape? = null
    var sideLength: Float = 100f
    var imgUri: android.net.Uri? = null
    var oren: Int = 0
    private var deleteBmp: Bitmap? = null
    private var duplicateBmp: Bitmap? = null
    private var rotateBmp: Bitmap? = null
    private var resizeBmp: Bitmap? = null

    init {
        deleteBmp = getBitmapFromVectorDrawable(context, R.drawable.ic_cancel)
        duplicateBmp = getBitmapFromVectorDrawable(context, R.drawable.ic_content_copy)
        rotateBmp = getBitmapFromVectorDrawable(context, R.drawable.rotate)
        resizeBmp = getBitmapFromVectorDrawable(context, R.drawable.resize)
        // Scale them down if necessary, e.g., to 60x60 or 50x50
        deleteBmp = deleteBmp?.scale(40, 40, true)
        duplicateBmp = duplicateBmp?.scale(40, 40, true)
        rotateBmp = rotateBmp?.scale(40, 40, true)
        resizeBmp = resizeBmp?.scale(40, 40, true)
    }

    private fun getBitmapFromVectorDrawable(context: Context?, drawableId: Int): Bitmap? {
        val drawable =
                androidx.core.content.ContextCompat.getDrawable(context!!, drawableId)
                        ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        myMain = MainActivity.getInstanceActivity()

        if (imgUri != null && myMain != null) {
            myMain!!.setImageBg.setImageBackgroundProcess(this)
        }
        setBG(canvas)
        // Save a layer to support erasing (PorterDuff.Mode.CLEAR) without clearing the background
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        startDrawing(canvas)
        canvas.restoreToCount(saveCount)
    }

    private var isResizing = false
    private var isRotating = false
    private var isDrawing = false
    private var ignoreUpEvent = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e!!.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = false
                ignoreUpEvent = false
                if (objectIndex != null && draws[objectIndex!!].isTouchingResize(e)) {
                    isResizing = true
                    return true
                }

                if (objectIndex != null && draws[objectIndex!!].isTouchingRotate(e)) {
                    isRotating = true
                    return true
                }

                if (objectIndex != null) {
                    if (objectIndex!! < draws.size) {
                        draws[objectIndex!!].startMove(e)
                        if (draws[objectIndex!!].isTouchingDelete(e)) {
                            deleteItem()
                            ignoreUpEvent = true
                            return true
                        }
                        if (draws[objectIndex!!].isTouchingDuplicate(e)) {
                            duplicateItem()
                            return true
                        }
                    } else {
                        objectIndex = null
                    }
                }

                if (tool == Shapes.Select) {
                    checkObjectTouching(e)
                }

                if (tool != Shapes.Select && tool != Shapes.Text && !isTouchingSameObject(e)) {
                    objectIndex = null
                    draws.add(tool.shape.create(e))
                    isDrawing = true
                    updateStyle()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing && objectIndex != null) {
                    draws[objectIndex!!].resize(e)
                    invalidate()
                    return true
                }
                if (isRotating && objectIndex != null) {
                    draws[objectIndex!!].rotateShape(e)
                    invalidate()
                    return true
                }
                if (isDrawing && objectIndex == null && tool != Shapes.Select) {
                    if (draws.isNotEmpty()) {
                        draws.last().update(e)
                    }
                }
                if (objectIndex != null) {
                    draws[objectIndex!!].move(e)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isResizing = false
                isRotating = false
                if (tool == Shapes.Text && objectIndex == null && !ignoreUpEvent) {
                    setTextDialog(e.x, e.y)
                }

                if (tool.selectAble) {
                    if (isDrawing && draws.isNotEmpty()) {
                        objectIndex = draws.indexOf(draws.last())
                        invalidate()
                    }
                }
                isDrawing = false
                myMain?.doButtonsAlpha()
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
        if (objectIndex != null && objectIndex!! < draws.size) {
            draws[objectIndex!!].drawSelectedBox(
                    canvas,
                    deleteBmp,
                    duplicateBmp,
                    rotateBmp,
                    resizeBmp
            )
        } else {
            objectIndex = null
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

    fun setImageBackground(img: android.net.Uri, oren: Int) {
        imgUri = img
        this.oren = oren
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
            example!!.updateObject(Paint(paint).apply { color = Color.BLACK })
            invalidate()
        }
    }

    fun removeExample() {
        example = null
        invalidate()
    }

    private fun setTextDialog(x: Float, y: Float) {
        if (myMain == null) return
        myMain!!.dialogs.showDialog(R.layout.text_dialog)
        val text = myMain!!.dialogs.dialog.findViewById<EditText>(R.id.theText)
        myMain!!.dialogs.dialog.findViewById<ImageView>(R.id.addText).setOnClickListener {
            if (text.text.isNotEmpty()) {
                val newText = com.joory.whiteboardapp.shapes.Texts()
                newText.text = text.text.toString()
                newText.point = android.graphics.PointF(x, y)
                newText.paint.textSize = 50f

                draws.add(newText)
                objectIndex = draws.size - 1
            }
            invalidate()
            myMain!!.dialogs.dismiss()
        }
    }

    private fun checkObjectTouching(e: MotionEvent): Boolean {
        for (i in draws.reversed()) {
            if (i.isTouchingObject(e)) {
                objectIndex = draws.indexOf(i)
                i.startMove(e)
                myMain?.showButtons()
                invalidate()
                return true
            }
        }
        objectIndex = null
        myMain?.showButtons()
        invalidate()
        return false
    }

    fun deleteItem() {
        if (objectIndex != null && objectIndex!! < draws.size) {
            undo.add(draws[objectIndex!!])
            draws.removeAt(objectIndex!!)
            objectIndex = null
            invalidate()
        } else {
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
        android.app.AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.clear_title))
                //            .setMessage("Are you sure you want to clear the canvas?")
                .setMessage(resources.getString(R.string.clear_message))
                .setPositiveButton(resources.getString(R.string.yes)) { dialog, _ ->
                    undo.clear()
                    undo.addAll(draws)
                    draws.clear()
                    objectIndex = null
                    objectIndex = null
                    myMain?.doButtonsAlpha()
                    myMain?.showButtons()
                    imgBG = null
                    invalidate()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    fun changeStyle() {
        val isFill = getCanvasPaint().style == Paint.Style.STROKE
        val style = if (isFill) Paint.Style.FILL else Paint.Style.STROKE
        paint.style = style
        if (objectIndex != null) {
            draws[objectIndex!!].paint.style = style
        }
        invalidate()
    }

    fun objectColorSet(color: Int) {
        getCanvasPaint().color = color
        paint.color = color
        invalidate()
    }

    fun addImageShape(bitmap: Bitmap) {
        val imageShape = ImageShape()
        // Center image
        val cx = width / 2f
        val cy = height / 2f
        imageShape.setBitmapAndInit(bitmap, cx, cy)

        draws.add(imageShape)
        objectIndex = draws.size - 1
        myMain?.showButtons()
        invalidate()
    }

    fun removeLayer(index: Int) {
        if (index in 0 until draws.size) {
            val item = draws[index]
            undo.add(item)
            draws.removeAt(index)
            if (objectIndex == index) {
                objectIndex = null
            } else if (objectIndex != null && objectIndex!! > index) {
                objectIndex = objectIndex!! - 1
            }
            invalidate()
            myMain?.doButtonsAlpha()
        }
    }

    fun duplicateLayer(index: Int) {
        if (index in 0 until draws.size) {
            draws.add(draws[index].deepCopy())
            invalidate()
        }
    }

    fun selectObject(index: Int) {
        if (index in 0 until draws.size) {
            objectIndex = index
            myMain?.showButtons()
            invalidate()
        }
    }
}

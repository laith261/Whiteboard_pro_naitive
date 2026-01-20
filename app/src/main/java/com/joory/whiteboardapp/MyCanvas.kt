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

    var colorBG: Int = Color.WHITE
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

    private val drawMatrix = android.graphics.Matrix()
    private val inverseMatrix = android.graphics.Matrix()
    private val scaleDetector: android.view.ScaleGestureDetector
    private val gestureDetector: android.view.GestureDetector
    private var lastFocusX = 0f
    private var lastFocusY = 0f

    init {
        scaleDetector =
                android.view.ScaleGestureDetector(
                        context!!,
                        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(
                                    detector: android.view.ScaleGestureDetector
                            ): Boolean {
                                val scaleFactor = detector.scaleFactor
                                // Optional: Clamp scale factor if needed, e.g., 0.1f to 5.0f
                                // For now allowing free zoom
                                drawMatrix.postScale(
                                        scaleFactor,
                                        scaleFactor,
                                        width / 2f,
                                        height / 2f
                                )

                                val dx = detector.focusX - lastFocusX
                                val dy = detector.focusY - lastFocusY
                                drawMatrix.postTranslate(dx, dy)

                                lastFocusX = detector.focusX
                                lastFocusY = detector.focusY

                                invalidate()
                                return true
                            }

                            override fun onScaleBegin(
                                    detector: android.view.ScaleGestureDetector
                            ): Boolean {
                                lastFocusX = detector.focusX
                                lastFocusY = detector.focusY

                                // Cancel any active drawing or interaction
                                if (isDrawing) {
                                    if (draws.isNotEmpty()) {
                                        draws.removeAt(draws.size - 1)
                                    }
                                    isDrawing = false
                                }
                                isResizing = false
                                isRotating = false
                                objectIndex = null
                                invalidate()
                                return true
                            }
                        }
                )
        deleteBmp = getBitmapFromVectorDrawable(context, R.drawable.ic_cancel)
        duplicateBmp = getBitmapFromVectorDrawable(context, R.drawable.ic_content_copy)
        rotateBmp = getBitmapFromVectorDrawable(context, R.drawable.rotate)
        resizeBmp = getBitmapFromVectorDrawable(context, R.drawable.resize)
        // Scale them down if necessary, e.g., to 60x60 or 50x50
        deleteBmp = deleteBmp?.scale(40, 40, true)
        duplicateBmp = duplicateBmp?.scale(40, 40, true)
        rotateBmp = rotateBmp?.scale(40, 40, true)
        resizeBmp = resizeBmp?.scale(40, 40, true)

        gestureDetector =
                android.view.GestureDetector(
                        context,
                        object : android.view.GestureDetector.SimpleOnGestureListener() {
                            override fun onDoubleTap(e: MotionEvent): Boolean {
                                // Transform event to canvas coordinates
                                drawMatrix.invert(inverseMatrix)
                                val transformedEvent = MotionEvent.obtain(e)
                                transformedEvent.transform(inverseMatrix)

                                for (i in draws.indices.reversed()) {
                                    val shape = draws[i]
                                    if (shape is com.joory.whiteboardapp.shapes.Texts &&
                                                    shape.isTouchingObject(transformedEvent)
                                    ) {
                                        editTextDialog(i)
                                        transformedEvent.recycle()
                                        return true
                                    }
                                }
                                transformedEvent.recycle()
                                return super.onDoubleTap(e)
                            }
                        }
                )
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

        // Draw void background (outside the page)
        canvas.drawColor(android.graphics.Color.LTGRAY)

        canvas.save()
        canvas.concat(drawMatrix)

        setBG(canvas)
        // Save a layer to support erasing (PorterDuff.Mode.CLEAR) without clearing the background
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        startDrawing(canvas)
        canvas.restoreToCount(saveCount)
        canvas.restore()
    }

    private var isResizing = false
    private var isRotating = false
    private var isDrawing = false
    private var ignoreUpEvent = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (e == null) return false

        scaleDetector.onTouchEvent(e)
        gestureDetector.onTouchEvent(e)
        if (scaleDetector.isInProgress) return true

        // Transform event to canvas coordinates
        drawMatrix.invert(inverseMatrix)
        val transformedEvent = MotionEvent.obtain(e)
        transformedEvent.transform(inverseMatrix)

        when (transformedEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = false
                ignoreUpEvent = false
                if (objectIndex != null && draws[objectIndex!!].isTouchingResize(transformedEvent)
                ) {
                    isResizing = true
                    transformedEvent.recycle()
                    return true
                }

                if (objectIndex != null && draws[objectIndex!!].isTouchingRotate(transformedEvent)
                ) {
                    isRotating = true
                    transformedEvent.recycle()
                    return true
                }

                if (objectIndex != null) {
                    if (objectIndex!! < draws.size) {
                        draws[objectIndex!!].startMove(transformedEvent)
                        if (draws[objectIndex!!].isTouchingDelete(transformedEvent)) {
                            deleteItem()
                            ignoreUpEvent = true
                            transformedEvent.recycle()
                            return true
                        }
                        if (draws[objectIndex!!].isTouchingDuplicate(transformedEvent)) {
                            duplicateItem()
                            transformedEvent.recycle()
                            return true
                        }
                    } else {
                        objectIndex = null
                    }
                }

                if (tool == Shapes.Select) {
                    checkObjectTouching(transformedEvent)
                }

                if (tool != Shapes.Select &&
                                tool != Shapes.Text &&
                                !isTouchingSameObject(transformedEvent)
                ) {
                    objectIndex = null
                    draws.add(tool.shape.create(transformedEvent))
                    isDrawing = true
                    updateStyle()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing && objectIndex != null) {
                    draws[objectIndex!!].resize(transformedEvent)
                    invalidate()
                    transformedEvent.recycle()
                    return true
                }
                if (isRotating && objectIndex != null) {
                    draws[objectIndex!!].rotateShape(transformedEvent)
                    invalidate()
                    transformedEvent.recycle()
                    return true
                }
                if (isDrawing && objectIndex == null && tool != Shapes.Select) {
                    if (draws.isNotEmpty()) {
                        draws.last().update(transformedEvent)
                    }
                }
                if (objectIndex != null) {
                    draws[objectIndex!!].move(transformedEvent)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isResizing = false
                isRotating = false
                if (tool == Shapes.Text && objectIndex == null && !ignoreUpEvent) {
                    setTextDialog(transformedEvent.x, transformedEvent.y)
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
        transformedEvent.recycle()
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
            val values = FloatArray(9)
            drawMatrix.getValues(values)
            val scaleX = values[android.graphics.Matrix.MSCALE_X]

            draws[objectIndex!!].drawSelectedBox(
                    canvas,
                    deleteBmp,
                    duplicateBmp,
                    rotateBmp,
                    resizeBmp,
                    scaleX
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
        // Draw page white/color background
        val bgPaint =
                Paint().apply {
                    color = colorBG
                    style = Paint.Style.FILL
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw Page Border
        val borderPaint =
                Paint().apply {
                    color = Color.GRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

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
                // Apply current canvas color style logic if desired, or keep default
                newText.paint.color = paint.color

                draws.add(newText)
                objectIndex = draws.size - 1
            }
            invalidate()
            myMain!!.dialogs.dismiss()
        }
    }

    private fun editTextDialog(index: Int) {
        if (myMain == null || index !in draws.indices) return
        val shape = draws[index] as? com.joory.whiteboardapp.shapes.Texts ?: return

        myMain!!.dialogs.showDialog(R.layout.text_dialog)
        val text = myMain!!.dialogs.dialog.findViewById<EditText>(R.id.theText)
        text.setText(shape.text)
        text.setSelection(shape.text.length)

        myMain!!.dialogs.dialog.findViewById<ImageView>(R.id.addText).setOnClickListener {
            if (text.text.isNotEmpty()) {
                shape.text = text.text.toString()
                invalidate()
            }
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

    fun resetZoom() {
        drawMatrix.reset()
        invalidate()
    }
}

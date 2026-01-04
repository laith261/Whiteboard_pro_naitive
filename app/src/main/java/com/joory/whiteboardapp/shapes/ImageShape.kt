package com.joory.whiteboardapp.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation

class ImageShape(var bitmap: Bitmap? = null) : Shape {
    override var paint = Paint()
    private var rect = RectF() // Current bounds
    override var sideLength: Float = 0.0f
    override lateinit var text: String
    override var rotation: Float = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> = mutableListOf(Tools.Crop)

    init {
        updateRectFromBitmap()
    }

    private fun updateRectFromBitmap() {
        if (bitmap != null) {
            // Default position (0,0) or we might want to center it later
            if (rect.isEmpty) {
                val aspectRatio = bitmap!!.height.toFloat() / bitmap!!.width.toFloat()
                val w = 150f
                val h = w * aspectRatio
                rect.set(0f, 0f, w, h)
            }
        }
    }

    fun setBitmapAndInit(bmp: Bitmap, centerX: Float, centerY: Float) {
        this.bitmap = bmp
        val aspectRatio = bmp.height.toFloat() / bmp.width.toFloat()
        val w = 350f
        val h = w * aspectRatio
        rect.set(centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h / 2)
    }

    override fun draw(canvas: Canvas) {
        if (bitmap == null) return
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2

        canvas.withRotation(rotation, cx, cy) {
            val destRect = RectF(rect)
            // drawBitmap(bitmap, srcRect, destRect, paint)
            // We use null for srcRect to use entire bitmap
            canvas.drawBitmap(bitmap!!, null, destRect, paint)
        }
    }

    override fun updateObject(paint: Paint?) {
        // Image doesn't really use paint color/style, but maybe alpha?
        if (paint != null) {
            this.paint.alpha = paint.alpha
        }
    }

    override fun create(e: MotionEvent): Shape {
        // This is called by MyCanvas if we selected the tool and touched.
        // But for ImageShape, we likely add it programmatically.
        // If we do support touch creation, we would need a bitmap.
        // Returning this or copy.
        return this
    }

    override fun update(e: MotionEvent) {
        // Drag to resize during creation? Not needed for now.
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)
        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    override fun drawSelectedBox(
        canvas: Canvas,
        deleteBmp: Bitmap?,
        duplicateBmp: Bitmap?,
        rotateBmp: Bitmap?,
        resizeBmp: Bitmap?
    ) {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2

        canvas.withRotation(rotation, cx, cy) {
            val selectedPaint = Paint()
            selectedPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            selectedPaint.style = Paint.Style.STROKE
            selectedPaint.color = android.graphics.Color.BLACK
            selectedPaint.strokeWidth = 2f
            drawRect(rect, selectedPaint)

            // Common paint for button backgrounds
            val btnBgPaint = Paint()
            btnBgPaint.color = "#5369e7".toColorInt()
            btnBgPaint.style = Paint.Style.FILL

            // Draw resize handle
            if (resizeBmp != null) {
                drawCircle(rect.right, rect.bottom, 30f, btnBgPaint)
                drawBitmap(resizeBmp, rect.right - 20, rect.bottom - 20, null)
            } else {
                selectedPaint.pathEffect = null
                selectedPaint.style = Paint.Style.FILL
                selectedPaint.color = android.graphics.Color.BLUE
                drawCircle(rect.right, rect.bottom, 15f, selectedPaint)
            }

            // Draw rotate handle
            if (rotateBmp != null) {
                drawCircle(rect.left, rect.bottom, 30f, btnBgPaint)
                drawBitmap(rotateBmp, rect.left - 20, rect.bottom - 20, null)
            } else {
                selectedPaint.color = android.graphics.Color.RED
                drawCircle(rect.left, rect.bottom, 15f, selectedPaint)
            }

            // Draw delete button (Top-Left)
            if (deleteBmp != null) {
                drawCircle(rect.left, rect.top, 30f, btnBgPaint)
                drawBitmap(deleteBmp, rect.left - 20, rect.top - 20, null)
            }

            // Draw duplicate button (Top-Right)
            if (duplicateBmp != null) {
                drawCircle(rect.right, rect.top, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, rect.right - 20, rect.top - 20, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.left
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.right
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.right
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.left
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun rotateShape(e: MotionEvent) {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val dx = e.x - cx
        val dy = e.y - cy

        val initialHandleAngle =
            Math.toDegrees(
                kotlin.math.atan2(
                    (rect.bottom - cy).toDouble(),
                    (rect.left - cx).toDouble()
                )
            )
                .toFloat()
        val currentTouchAngle =
            Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        rotation = currentTouchAngle - initialHandleAngle
    }

    private fun rotatePoint(point: PointF, center: PointF, angleDegrees: Float): PointF {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val s = kotlin.math.sin(rad)
        val c = kotlin.math.cos(rad)
        val px = point.x - center.x
        val py = point.y - center.y
        val xnew = px * c - py * s
        val ynew = px * s + py * c
        return PointF((xnew + center.x).toFloat(), (ynew + center.y).toFloat())
    }

    override fun resize(e: MotionEvent) {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2

        // 1. Calculate anchor point (Top-Left) position on screen BEFORE resize
        val anchorX = rect.left
        val anchorY = rect.top
        val screenAnchor = rotatePoint(PointF(anchorX, anchorY), PointF(cx, cy), rotation)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        rect.right = rotatedPoint.x
        rect.bottom = rotatedPoint.y

        // 3. Calculate where the anchor IS represented now with the new center
        val newCx = (rect.left + rect.right) / 2
        val newCy = (rect.top + rect.bottom) / 2
        val newScreenAnchor = rotatePoint(PointF(anchorX, anchorY), PointF(newCx, newCy), rotation)

        // 4. Calculate drift and offset rect to correct it
        val dx = screenAnchor.x - newScreenAnchor.x
        val dy = screenAnchor.y - newScreenAnchor.y

        rect.offset(dx, dy)
    }

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - rect.left
        dragOffsetY = e.y - rect.top
    }

    override fun move(e: MotionEvent) {
        rect.offsetTo(e.x - dragOffsetX, e.y - dragOffsetY)
    }

    override fun deepCopy(): Shape {
        val newShape = ImageShape(bitmap!!.config?.let { bitmap?.copy(it, true) })
        newShape.rect = RectF(this.rect)
        newShape.rotation = this.rotation
        newShape.paint.set(this.paint)
        return newShape
    }

    fun crop(newBitmap: Bitmap) {
        this.bitmap = newBitmap
        val aspectRatio = newBitmap.height.toFloat() / newBitmap.width.toFloat()
        val currentWidth = rect.width()
        val currentHeight = currentWidth * aspectRatio
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        rect.set(
            cx - currentWidth / 2,
            cy - currentHeight / 2,
            cx + currentWidth / 2,
            cy + currentHeight / 2
        )
    }
}

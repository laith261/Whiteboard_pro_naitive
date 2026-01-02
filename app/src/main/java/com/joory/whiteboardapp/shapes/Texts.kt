package com.joory.whiteboardapp.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation

class Texts : Shape {
    override var paint: Paint = Paint()
    override var text: String = ""
    private var point = PointF(0f, 0f)
    override var sideLength: Float = 0.0f

    override var rotation: Float = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> =
        mutableListOf(Tools.Style, Tools.TextSize, Tools.StrokeWidth, Tools.Color)


    override fun draw(canvas: Canvas) {
        val rect = getRectBorder()
        val cx = rect.centerX()
        val cy = rect.centerY()

        canvas.withRotation(rotation, cx, cy) { drawText(text, point.x, point.y, paint) }
    }

    override fun create(e: MotionEvent): Shape {
        val newText = Texts()
        newText.paint.textSize = 50f
        newText.point = PointF(e.x, e.y)
        return newText
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.textSize = paint.textSize
        }
    }

    override fun update(e: MotionEvent) {}

    private fun getRectBorder(): RectF {
        val rect = Rect()
        paint.getTextBounds(text, 0, text.length, rect)
        // Original logic: rect.offsetTo(point.x.toInt(), point.y.toInt() - rect.height())
        // This puts the rect top-left at point.x, point.y - height.
        // This matches the drawing logic if point is near the baseline.
        rect.offsetTo(point.x.toInt(), point.y.toInt() - rect.height())
        return RectF(rect)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun isTouchingObject(e: MotionEvent): Boolean {
        val rect = getRectBorder()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawSelectedBox(canvas: Canvas, deleteBmp: Bitmap?, duplicateBmp: Bitmap?) {
        val rect = getRectBorder()
        // Add padding same as before: left-5, top-5, right+10, bottom+10
        rect.set(rect.left - 5, rect.top - 5, rect.right + 10, rect.bottom + 10)

        val cx = getRectBorder().centerX() // Center of ACTUAL text, not padded box?
        // Let's rotate around center of text always.
        val cy = getRectBorder().centerY()

        canvas.withRotation(rotation, cx, cy) {
            val selectedPaint = Paint()
            selectedPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            selectedPaint.style = Paint.Style.STROKE
            drawRect(rect, selectedPaint)

            // Draw resize handle (Bottom-Right)
            selectedPaint.pathEffect = null
            selectedPaint.style = Paint.Style.FILL
            selectedPaint.color = android.graphics.Color.BLUE
            drawCircle(rect.right, rect.bottom, 15f, selectedPaint)

            // Draw rotate handle (Bottom-Left)
            selectedPaint.color = android.graphics.Color.RED
            drawCircle(rect.left, rect.bottom, 15f, selectedPaint)

            // Common paint for button backgrounds
            val btnBgPaint = Paint()
            btnBgPaint.color = "#5369e7".toColorInt()
            btnBgPaint.style = Paint.Style.FILL

            if (deleteBmp != null) {
                drawCircle(rect.left, rect.top, 30f, btnBgPaint)
                drawBitmap(deleteBmp, rect.left - 30, rect.top - 30, null)
            }
            if (duplicateBmp != null) {
                drawCircle(rect.right, rect.top, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, rect.right - 30, rect.top - 30, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val rect = RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.left
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val rect = RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.right
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val paddedRect =
            RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = rotatedPoint.x - paddedRect.right
        val dy = rotatedPoint.y - paddedRect.bottom
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val paddedRect =
            RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = rotatedPoint.x - paddedRect.left
        val dy = rotatedPoint.y - paddedRect.bottom
        return (dx * dx + dy * dy) <= 4900
    }

    override fun rotateShape(e: MotionEvent) {
        val rect = getRectBorder()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val dx = e.x - cx
        val dy = e.y - cy

        // Handle is at bottom-left of padded rect relative to center.
        // Similar to other shapes, we track the angle difference.
        // Padded Bottom-Left: (l-5, b+10).
        val paddedRect = RectF(rect.left - 5, rect.top - 5, rect.right + 10, rect.bottom + 10)
        val initialHandleAngle =
            Math.toDegrees(
                kotlin.math.atan2(
                    (paddedRect.bottom - cy).toDouble(),
                    (paddedRect.left - cx).toDouble()
                )
            )
                .toFloat()
        val currentTouchAngle =
            Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        rotation = currentTouchAngle - initialHandleAngle
    }

    override fun resize(e: MotionEvent) {
        val oldRect = getRectBorder()
        val anchorTop = oldRect.top
        val anchorLeft = oldRect.left
        val cx = oldRect.centerX()
        val cy = oldRect.centerY()

        // Calculate mouse position relative to center of the *original* rect
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        // Calculate intended dimensions based on distance from Anchor (Top-Left)
        // We want the new Bottom-Right to be at rotatedPoint.
        var targetHeight = rotatedPoint.y - anchorTop
        var targetWidth = rotatedPoint.x - anchorLeft

        // Ensure minimum dimensions
        if (targetHeight < 10) targetHeight = 10f
        if (targetWidth < 10) targetWidth = 10f

        // Calculate scale factor. Use the larger ratio to allow intuitive resizing
        val oldHeight = oldRect.height()
        val oldWidth = oldRect.width()

        val scaleH = targetHeight / oldHeight
        val scaleW = targetWidth / oldWidth

        // Use the maximum scale to keep resizing proportional and responsive to the faster axis
        val scale = kotlin.math.max(scaleH, scaleW)

        paint.textSize *= scale

        // Clamp textSize
        if (paint.textSize < 10f) paint.textSize = 10f
        if (paint.textSize > 1000f) paint.textSize = 1000f

        // Recalculate bounds to update point.y to maintain Top-Left anchor
        val newRect = Rect()
        paint.getTextBounds(text, 0, text.length, newRect)
        val newHeight = newRect.height()

        // Update point.y so that (point.y - newHeight) == anchorTop
        point.y = anchorTop + newHeight
    }

    private fun rotatePoint(point: PointF, center: PointF, angleDegrees: Float): PointF {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val s = kotlin.math.sin(rad)
        val c = kotlin.math.cos(rad)
        val px = point.x - center.x
        val py = point.y - center.y
        val xNew = px * c - py * s
        val yNew = px * s + py * c
        return PointF((xNew + center.x).toFloat(), (yNew + center.y).toFloat())
    }

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - point.x
        dragOffsetY = e.y - point.y
    }

    override fun move(e: MotionEvent) {
        point = PointF(e.x - dragOffsetX, e.y - dragOffsetY)
    }
}

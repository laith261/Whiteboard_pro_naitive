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

class Rects : Shape {
    override var paint = Paint()
    private var start: PointF = PointF(0f, 0f)
    private var end: PointF = PointF(0f, 0f)
    private var rect = RectF(start.x, start.y, end.x, end.y)
    override var sideLength: Float = 0.0f
    override lateinit var text: String
    override var rotation: Float = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> =
            mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color)

    override fun draw(canvas: Canvas) {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        canvas.withRotation(rotation, cx, cy) {
            drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
        }
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
        val left = kotlin.math.min(start.x, end.x)
        val right = kotlin.math.max(start.x, end.x)
        val top = kotlin.math.min(start.y, end.y)
        val bottom = kotlin.math.max(start.y, end.y)
        rect = RectF(left, top, right, bottom)
    }

    override fun create(e: MotionEvent): Shape {
        val newRect = Rects()
        newRect.start = PointF(e.x, e.y)
        newRect.end = PointF(e.x, e.y) // Initialize end to start to avoid 0,0 glitch
        // Do NOT copy this.paint here. It's the prototype paint which is default.
        // MyCanvas calls updateStyle() immediately after create(), which sets the correct paint.
        return newRect
    }

    override fun update(e: MotionEvent) {
        end = PointF(e.x, e.y)
        updateObject()
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
            drawRect(rect, selectedPaint)

            // Common paint for button backgrounds
            val btnBgPaint = Paint()
            btnBgPaint.color = "#5369e7".toColorInt()
            btnBgPaint.style = Paint.Style.FILL

            // Draw resize handle
            if (resizeBmp != null) {
                drawCircle(rect.right + 30f, rect.bottom + 30f, 30f, btnBgPaint)
                drawBitmap(resizeBmp, rect.right + 10, rect.bottom + 10, null)
            } else {
                selectedPaint.pathEffect = null
                selectedPaint.style = Paint.Style.FILL
                selectedPaint.color = android.graphics.Color.BLUE
                drawCircle(rect.right + 30f, rect.bottom + 30f, 15f, selectedPaint)
            }

            // Draw rotate handle
            if (rotateBmp != null) {
                drawCircle(rect.left - 30f, rect.bottom + 30f, 30f, btnBgPaint)
                drawBitmap(rotateBmp, rect.left - 50, rect.bottom + 10, null)
            } else {
                selectedPaint.color = android.graphics.Color.RED
                drawCircle(rect.left - 30f, rect.bottom + 30f, 15f, selectedPaint)
            }

            // Draw delete button (Top-Left)
            if (deleteBmp != null) {
                drawCircle(rect.left - 30f, rect.top - 30f, 30f, btnBgPaint)
                drawBitmap(deleteBmp, rect.left - 50, rect.top - 50, null)
            }

            // Draw duplicate button (Top-Right)
            if (duplicateBmp != null) {
                drawCircle(rect.right + 30f, rect.top - 30f, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, rect.right + 10, rect.top - 50, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        // Button center is approx (rect.left - 30, rect.top - 30)
        val btnX = rect.left - 30f
        val btnY = rect.top - 30f
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        // Button center is approx (rect.right + 30, rect.top - 30)
        val btnX = rect.right + 30f
        val btnY = rect.top - 30f
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.right + 30f
        val handleY = rect.bottom + 30f
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.left - 30f
        val handleY = rect.bottom + 30f
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun rotateShape(e: MotionEvent) {
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val dx = e.x - cx
        val dy = e.y - cy
        // Angle of bottom-left corner relative to center:
        // atan2(height/2, -width/2). It depends on aspect ratio!
        // So simply setting rotation = angle - 135 is wrong for non-square rects.
        // It should be rotation = currentTouchAngle - initialHandleAngle.
        // Initial Handle Angle = atan2(rect.bottom - cy, rect.left - cx)

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

        // 2. Perform Resize
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)
        var newRight = rotatedPoint.x
        var newBottom = rotatedPoint.y

        if (newRight < rect.left + 10) newRight = rect.left + 10
        if (newBottom < rect.top + 10) newBottom = rect.top + 10

        /// Temporarily apply new size to calculate drift
        rect.right = newRight
        rect.bottom = newBottom

        // 3. Calculate where the anchor IS represented now with the new center
        val newCx = (rect.left + rect.right) / 2
        val newCy = (rect.top + rect.bottom) / 2
        val newScreenAnchor = rotatePoint(PointF(anchorX, anchorY), PointF(newCx, newCy), rotation)

        // 4. Calculate drift and offset rect to correct it
        val dx = screenAnchor.x - newScreenAnchor.x
        val dy = screenAnchor.y - newScreenAnchor.y

        rect.offset(dx, dy)

        // Update end point for consistency (and start since we offset)
        start = PointF(rect.left, rect.top)
        end = PointF(rect.right, rect.bottom)
    }

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - rect.left
        dragOffsetY = e.y - rect.top
    }

    override fun move(e: MotionEvent) {
        rect.offsetTo(e.x - dragOffsetX, e.y - dragOffsetY)
    }
}

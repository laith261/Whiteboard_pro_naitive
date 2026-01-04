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
import kotlin.math.pow
import kotlin.math.sqrt

class Circle : Shape {
    private var cp = PointF(0f, 0f)
    private var radius = 50f
    override var paint = Paint()
    override var sideLength: Float = 0.0f

    override var rotation: Float = 0f
    override var shapeTools: MutableList<Tools> =
        mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color)

    override fun draw(canvas: Canvas) {
        // Technically circle rotation doesn't change its look, but we might have text later or
        // patterns.
        canvas.drawCircle(cp.x, cp.y, radius, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newCircle = Circle()
        newCircle.cp = PointF(e.x, e.y)
        // newCircle.paint.set(this.paint) // Removed
        return newCircle
    }

    override fun update(e: MotionEvent) {
        radius = distance(cp, PointF(e.x, e.y))
    }

    private fun distance(center: PointF, end: PointF): Float {
        return sqrt((end.x - center.x).toDouble().pow(2.0) + (end.y - center.y).toDouble().pow(2.0))
            .toFloat()
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        // Rotating a circle around center: Check distance to center. Rotation doesn't affect
        // distance.
        // But for box consistency, let's keep it simple.
        val rect = RectF(cp.x - radius, cp.y - radius, cp.x + radius, cp.y + radius)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)
        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    override fun drawSelectedBox(
        canvas: Canvas,
        deleteBmp: Bitmap?,
        duplicateBmp: Bitmap?,
        rotateBmp: Bitmap?,
        resizeBmp: Bitmap?
    ) {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)

        canvas.withRotation(rotation, cp.x, cp.y) {
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

            if (deleteBmp != null) {
                drawCircle(rect.left, rect.top, 30f, btnBgPaint)
                drawBitmap(deleteBmp, rect.left - 20, rect.top - 20, null)
            }
            if (duplicateBmp != null) {
                drawCircle(rect.right, rect.top, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, rect.right - 20, rect.top - 20, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)

        val btnX = rect.left
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)

        val btnX = rect.right
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)

        val handleX = rect.right
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)

        val handleX = rect.left
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - cp.x
        dragOffsetY = e.y - cp.y
    }

    override fun move(e: MotionEvent) {
        cp.x = e.x - dragOffsetX
        cp.y = e.y - dragOffsetY
    }

    override fun rotateShape(e: MotionEvent) {
        val dx = e.x - cp.x
        val dy = e.y - cp.y
        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        rotation = angle - 135f
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
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), cp, -rotation)
        val dx = kotlin.math.abs(rotatedPoint.x - cp.x)
        val dy = kotlin.math.abs(rotatedPoint.y - cp.y)
        val boxHalfSize = kotlin.math.max(dx, dy)
        val newRadius = boxHalfSize - 5 // Subtract padding
        if (newRadius > 0) {
            radius = newRadius
        }
    }
}

package com.joory.whiteboardapp.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Triangle : Shape {
    override var sideLength = 150f
    override var paint = Paint()
    var fp = PointF(0f, 0f)
    private var triangle = ATriangle(0f, 0f, sideLength)
    override var rotation: Float = 0f
    override lateinit var text: String
    override var shapeTools: MutableList<Tools> =
        mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color)

    override fun draw(canvas: Canvas) {
        canvas.withRotation(rotation, triangle.cp.x, triangle.cp.y) {
            drawPath(triangle.path, paint)
        }
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newTriangle = Triangle()
        // newTriangle.paint.set(this.paint) // Removed
        newTriangle.paint.isDither = true
        newTriangle.paint.strokeJoin = Paint.Join.ROUND
        newTriangle.paint.strokeCap = Paint.Cap.ROUND
        newTriangle.fp = PointF(e.x, e.y)
        newTriangle.sideLength = this.sideLength
        // Also need to initialize 'triangle' (ATriangle) to match?
        // ATriangle is initialized with (0,0, sideLength) by default.
        // update(e) is called usually after create?
        // Rects.create set start.
        // Triangle.create sets fp.
        // update() uses fp? No, update uses e.x, e.y to create ATriangle.
        // MyCanvas calls create then draws.add. Then user drags -> update.
        // But initially, ATriangle is at 0,0.
        // We should probably initialize ATriangle to fp?
        newTriangle.triangle = ATriangle(e.x, e.y, newTriangle.sideLength)

        return newTriangle
    }

    override fun update(e: MotionEvent) {
        triangle = ATriangle(e.x, e.y, sideLength)
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y

        // Rotate point back to check against unrotated box
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    override fun drawSelectedBox(canvas: Canvas, deleteBmp: Bitmap?, duplicateBmp: Bitmap?) {
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val rect = RectF(cx - radius - 10, cy - radius - 10, cx + radius + 10, cy + radius + 10)

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
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val rect = RectF(cx - radius - 10, cy - radius - 10, cx + radius + 10, cy + radius + 10)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.left
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val rect = RectF(cx - radius - 10, cy - radius - 10, cx + radius + 10, cy + radius + 10)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.right
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val rect = RectF(cx - radius - 10, cy - radius - 10, cx + radius + 10, cy + radius + 10)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.right
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val rect = RectF(cx - radius - 10, cy - radius - 10, cx + radius + 10, cy + radius + 10)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.left
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - triangle.cp.x
        dragOffsetY = e.y - triangle.cp.y
    }

    override fun move(e: MotionEvent) {
        triangle = ATriangle(e.x - dragOffsetX, e.y - dragOffsetY, sideLength)
    }

    override fun rotateShape(e: MotionEvent) {
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        val dx = e.x - cx
        val dy = e.y - cy
        // Calculate angle from center to touch.
        // We want the handle (at bottom-left: 135 deg relative to center usually? No, rect is axis
        // aligned)
        // Rect bottom-left is at (cx-r, cy+r). Angle is atan2(r, -r) = 135 deg (if Y down).
        // So rotation = touchAngle - 135.

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        rotation = angle - 135f
    }

    private fun rotatePoint(point: PointF, center: PointF, angleDegrees: Float): PointF {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val s = sin(rad)
        val c = cos(rad)
        val px = point.x - center.x
        val py = point.y - center.y
        val xnew = px * c - py * s
        val ynew = px * s + py * c
        return PointF((xnew + center.x).toFloat(), (ynew + center.y).toFloat())
    }

    override fun resize(e: MotionEvent) {
        val cx = triangle.cp.x
        val cy = triangle.cp.y
        // Rotate touch point back to axis-aligned space for size calculation
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = kotlin.math.abs(rotatedPoint.x - cx)
        val dy = kotlin.math.abs(rotatedPoint.y - cy)
        val boxHalfSize = kotlin.math.max(dx, dy)
        val newRadius = boxHalfSize - 10 // Subtract padding
        if (newRadius > 0) {
            updateSideLength(newRadius * 2)
        }
    }

    override fun updateSideLength(length: Float) {
        super.updateSideLength(length)
        triangle = ATriangle(triangle.cp.x, triangle.cp.y, sideLength)
    }

    class ATriangle(px: Float, py: Float, var sideLength: Float) {
        var cp: PointF = PointF(px, py)
        var path: Path = Path()

        init {
            drawTriangle()
        }

        private fun drawTriangle() {
            path.reset()
            val radius = sideLength / 2

            // Angles for equilateral triangle: -90, 30, 150 degrees
            val angles = listOf(-Math.PI / 2, Math.PI / 6, 5 * Math.PI / 6)

            // First point (Top)
            var cx = cp.x + cos(angles[0]).toFloat() * radius
            var cy = cp.y + sin(angles[0]).toFloat() * radius
            path.moveTo(cx, cy)

            // Second point (Bottom Right)
            cx = cp.x + cos(angles[1]).toFloat() * radius
            cy = cp.y + sin(angles[1]).toFloat() * radius
            path.lineTo(cx, cy)

            // Third point (Bottom Left)
            cx = cp.x + cos(angles[2]).toFloat() * radius
            cy = cp.y + sin(angles[2]).toFloat() * radius
            path.lineTo(cx, cy)

            path.close()
        }
    }
}

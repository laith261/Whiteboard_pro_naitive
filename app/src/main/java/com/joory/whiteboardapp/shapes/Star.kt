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
import kotlin.math.cos
import kotlin.math.sin

class Star : Shape {
    override var sideLength = 150f
    override var paint = Paint()
    var fp = PointF(0f, 0f)
    private var star = AStar(0f, 0f, sideLength)
    override var rotation: Float = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> =
        mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color)

    override fun draw(canvas: Canvas) {
        canvas.withRotation(rotation, star.cp.x, star.cp.y) { drawPath(star.path, paint) }
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newStar = Star()
        newStar.sideLength = this.sideLength
        newStar.paint.isDither = true
        newStar.paint.strokeJoin = Paint.Join.ROUND
        newStar.paint.strokeCap = Paint.Cap.ROUND
        newStar.fp = PointF(e.x, e.y)
        newStar.update(e)
        return newStar
    }

    override fun update(e: MotionEvent) {
        star = AStar(e.x, e.y, sideLength)
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    override fun drawSelectedBox(
        canvas: Canvas,
        deleteBmp: Bitmap?,
        duplicateBmp: Bitmap?,
        rotateBmp: Bitmap?,
        resizeBmp: Bitmap?
    ) {
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y
        val rect = RectF(cx - radius - 5, cy - radius - 5, cx + radius + 5, cy + radius + 5)

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
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y
        val rect = RectF(cx - radius - 5, cy - radius - 5, cx + radius + 5, cy + radius + 5)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.left
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y
        val rect = RectF(cx - radius - 5, cy - radius - 5, cx + radius + 5, cy + radius + 5)
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.right
        val btnY = rect.top
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y
        val rect = RectF(cx - radius - 5, cy - radius - 5, cx + radius + 5, cy + radius + 5)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.right
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val radius = sideLength / 2
        val cx = star.cp.x
        val cy = star.cp.y
        val rect = RectF(cx - radius - 5, cy - radius - 5, cx + radius + 5, cy + radius + 5)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val handleX = rect.left
        val handleY = rect.bottom
        val dx = rotatedPoint.x - handleX
        val dy = rotatedPoint.y - handleY
        return (dx * dx + dy * dy) <= 4900
    }

    override fun rotateShape(e: MotionEvent) {
        val cx = star.cp.x
        val cy = star.cp.y
        val dx = e.x - cx
        val dy = e.y - cy
        // Rotation handle is at bottom-left (+135 deg relative to center if rect is centered).
        // If unrotated rect is aligned X/Y, bottom-left is (-x, +y).
        // Adjust for handle initial position angle relative to shape.
        // Assuming consistent handle position at bottom-left corner of bounding box.
        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
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
        val cx = star.cp.x
        val cy = star.cp.y
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)
        val dx = kotlin.math.abs(rotatedPoint.x - cx)
        val dy = kotlin.math.abs(rotatedPoint.y - cy)
        val boxHalfSize = kotlin.math.max(dx, dy)
        val newRadius = boxHalfSize - 5 // Subtract padding
        if (newRadius > 0) {
            updateSideLength(newRadius * 2)
        }
    }

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - star.cp.x
        dragOffsetY = e.y - star.cp.y
    }

    override fun move(e: MotionEvent) {
        star = AStar(e.x - dragOffsetX, e.y - dragOffsetY, sideLength)
    }

    override fun updateSideLength(length: Float) {
        super.updateSideLength(length)
        star = AStar(star.cp.x, star.cp.y, sideLength)
    }

    class AStar(px: Float, py: Float, var sideLength: Float) {
        var cp: PointF = PointF(px, py)
        var path: Path = Path()

        init {
            drawStar()
        }

        private fun drawStar() {
            val spikes = 5
            val outerRadius = sideLength / 2
            val innerRadius = outerRadius / 2.5f

            var rot = Math.PI / 2 * 3
            // var x = cp.x
            // var y = cp.y
            val step = Math.PI / spikes

            path.reset()

            // Calculate first point
            var cx = cp.x + cos(rot) * outerRadius
            var cy = cp.y + sin(rot) * outerRadius
            path.moveTo(cx.toFloat(), cy.toFloat())
            rot += step

            for (i in 0 until spikes) {
                cx = cp.x + cos(rot) * innerRadius
                cy = cp.y + sin(rot) * innerRadius
                path.lineTo(cx.toFloat(), cy.toFloat())
                rot += step

                cx = cp.x + cos(rot) * outerRadius
                cy = cp.y + sin(rot) * outerRadius
                path.lineTo(cx.toFloat(), cy.toFloat())
                rot += step
            }
            path.close()
        }
    }
}

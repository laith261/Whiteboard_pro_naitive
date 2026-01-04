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
import java.lang.Math.toDegrees
import kotlin.math.atan2

class Lines : Shape {
    override var paint = Paint()
    var start: PointF = PointF(0f, 0f)
    private var end: PointF = PointF(0f, 0f)
    private var angle = 0f
    private var inSerine = false
    private var dist: PointF = PointF(0f, 0f)
    override var sideLength: Float = 0.0f

    override var rotation: Float = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> = mutableListOf(Tools.Style, Tools.Color)

    override fun draw(canvas: Canvas) {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        canvas.withRotation(rotation, cx, cy) { drawLine(start.x, start.y, end.x, end.y, paint) }
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newLine = Lines()
        newLine.start = PointF(e.x, e.y)
        newLine.end = PointF(e.x, e.y) // Avoid 0,0 glitch
        // newLine.paint.set(this.paint) // Removed
        return newLine
    }

    override fun update(e: MotionEvent) {
        end = PointF(e.x, e.y)
        angle = angle()
        inSerine = inSerineAngle()
        dist = PointF(end.x - start.x, end.y - start.y)
    }

    private fun getRectBorder(): RectF {
        val leftTop =
            PointF(
                if (end.x > start.x) start.x else end.x,
                if (end.y > start.y) start.y else end.y
            )
                .apply {
                    if (inSerine) {
                        x -= 15
                        y -= 15
                    }
                }
        val rightBottom =
            PointF(
                if (end.x < start.x) start.x else end.x,
                if (end.y < start.y) start.y else end.y
            )
                .apply {
                    if (inSerine) {
                        x += 15
                        y += 15
                    }
                }
        return RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y)
    }

    private fun inSerineAngle(): Boolean {
        val angles = arrayOf(355..365, 85..95, 175..185, 265..275)
        for (angle in angles) {
            if (this.angle.toInt() in angle) {
                return true
            }
        }
        return false
    }

    fun example(width: Int, height: Int): Lines {
        start = PointF(((width / 2).toFloat()), (height * 0.25).toFloat())
        end = PointF(((width / 2).toFloat()), (height * 0.75).toFloat())
        return this
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        // Check if rotated point is near the unrotated line
        // A simple way for expanded line check is using the rect border of the line
        val rect = getRectBorder()
        return rect.contains(rotatedPoint.x, rotatedPoint.y)
    }

    override fun drawSelectedBox(
        canvas: Canvas,
        deleteBmp: Bitmap?,
        duplicateBmp: Bitmap?,
        rotateBmp: Bitmap?,
        resizeBmp: Bitmap?
    ) {
        val rect = getRectBorder()
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2

        canvas.withRotation(rotation, cx, cy) {
            val selectedPaint = Paint()
            selectedPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            selectedPaint.style = Paint.Style.STROKE
            drawRect(rect, selectedPaint)

            // Common paint for button backgrounds
            val btnBgPaint = Paint()
            btnBgPaint.color = "#5369e7".toColorInt()
            btnBgPaint.style = Paint.Style.FILL

            // Draw resize handle at the end point
            if (resizeBmp != null) {
                drawCircle(end.x, end.y, 30f, btnBgPaint)
                drawBitmap(resizeBmp, end.x - 20, end.y - 20, null)
            } else {
                selectedPaint.pathEffect = null
                selectedPaint.style = Paint.Style.FILL
                selectedPaint.color = android.graphics.Color.BLUE
                drawCircle(end.x, end.y, 15f, selectedPaint)
            }

            // Draw rotate handle at the start point
            if (rotateBmp != null) {
                drawCircle(start.x, start.y, 30f, btnBgPaint)
                drawBitmap(rotateBmp, start.x - 20, start.y - 20, null)
            } else {
                selectedPaint.color = android.graphics.Color.RED
                drawCircle(start.x, start.y, 15f, selectedPaint)
            }

            val isPositiveSlope = (end.y - start.y) * (end.x - start.x) > 0
            // If slope is positive (\), occupied corners are TL and BR. Use BL and TR for buttons.
            // If slope is negative (/), occupied corners are TR and BL. Use TL and BR for buttons.

            val deleteX = rect.left
            val deleteY = if (isPositiveSlope) rect.bottom else rect.top

            val duplicateX = rect.right
            val duplicateY = if (isPositiveSlope) rect.top else rect.bottom

            if (deleteBmp != null) {
                drawCircle(deleteX, deleteY, 30f, btnBgPaint)
                drawBitmap(deleteBmp, deleteX - 20, deleteY - 20, null)
            }
            if (duplicateBmp != null) {
                drawCircle(duplicateX, duplicateY, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, duplicateX - 20, duplicateY - 20, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val rect = getRectBorder()
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val isPositiveSlope = (end.y - start.y) * (end.x - start.x) > 0
        val btnX = rect.left
        val btnY = if (isPositiveSlope) rect.bottom else rect.top

        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val rect = getRectBorder()
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val isPositiveSlope = (end.y - start.y) * (end.x - start.x) > 0
        val btnX = rect.right
        val btnY = if (isPositiveSlope) rect.top else rect.bottom

        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = rotatedPoint.x - end.x
        val dy = rotatedPoint.y - end.y
        return (dx * dx + dy * dy) <= 4900
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = rotatedPoint.x - start.x
        val dy = rotatedPoint.y - start.y
        return (dx * dx + dy * dy) <= 4900
    }

    override fun rotateShape(e: MotionEvent) {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val dx = e.x - cx
        val dy = e.y - cy

        // Use angle of start point relative to center as initial handle angle
        val initialHandleAngle =
            toDegrees(atan2((start.y - cy).toDouble(), (start.x - cx).toDouble())).toFloat()
        val currentTouchAngle = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        rotation = currentTouchAngle - initialHandleAngle
    }

    override fun startMove(e: MotionEvent) {
        dragOffsetX = e.x - start.x
        dragOffsetY = e.y - start.y
        dist = PointF(end.x - start.x, end.y - start.y)
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
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        // Get touch point relative to unrotated coordinate system
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        // Vector of the line
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lineLength = kotlin.math.sqrt(dx * dx + dy * dy)

        // Normalize line vector
        val ux = dx / lineLength
        val uy = dy / lineLength

        // Vector from start to touch point
        val vx = rotatedPoint.x - start.x
        val vy = rotatedPoint.y - start.y

        // Project v onto u (dot product) to get distance along the line
        val projection = vx * ux + vy * uy

        // Update end point based on projection, constraining it to the line axis
        // We enforce a minimum length of 10 to avoid flipping or zero length
        val newLength = if (projection < 10) 10f else projection

        end = PointF(start.x + ux * newLength, start.y + uy * newLength)

        dist = PointF(end.x - start.x, end.y - start.y)
        angle = angle()
    }

    private fun angle(): Float {
        val deltaX = start.x - end.x
        val deltaY = start.y - end.y
        val theAngle = toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
        if (angle < 5) {
            return angle + 365
        }
        return if (theAngle < 0) theAngle + 360 else theAngle
    }

    override fun move(e: MotionEvent) {
        start = PointF(e.x - dragOffsetX, e.y - dragOffsetY)
        end = PointF(start.x + dist.x, start.y + dist.y)
    }
}

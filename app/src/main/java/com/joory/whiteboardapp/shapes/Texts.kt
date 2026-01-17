package com.joory.whiteboardapp.shapes

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation

class Texts : Shape {
    override var paint: Paint = Paint()
    override var text: String = ""
    var point = PointF(0f, 0f)
    override var sideLength: Float = 0.0f

    override var rotation: Float = 0f
    var fontPath: String? = null
    var isBold: Boolean = false
    var isItalic: Boolean = false
    var isUnderline: Boolean = false

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    override var shapeTools: MutableList<Tools> =
            mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color, Tools.Font)

    override fun draw(canvas: Canvas) {
        val rect = getRectBorder()
        val cx = rect.centerX()
        val cy = rect.centerY()

        canvas.withRotation(rotation, cx, cy) {
            val lines = text.split("\n")
            // Calculate total height to find where to start drawing (so point remains bottom-left)
            // Or simpler: We know the rect top is rect.top.
            // Baseline of first line is typically rect.top + (-paint.ascent()) approx,
            // but we constructed rect using fontSpacing.
            // Let's rely on the rect we calculated in getRectBorder.

            var yOffset = rect.top - paint.ascent()

            for (line in lines) {
                drawText(line, rect.left, yOffset, paint)
                yOffset += paint.fontSpacing
            }
        }
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

    fun updateFont(path: String) {
        fontPath = path
        applyTypefaceAndStyle()
    }

    fun updateStyle(bold: Boolean, italic: Boolean, underline: Boolean) {
        isBold = bold
        isItalic = italic
        isUnderline = underline
        applyTypefaceAndStyle()
    }

    @SuppressLint("WrongConstant")
    private fun applyTypefaceAndStyle() {
        try {
            val baseTypeface =
                    if (fontPath != null) {
                        android.graphics.Typeface.createFromFile(fontPath)
                    } else {
                        android.graphics.Typeface.DEFAULT
                    }

            var style = android.graphics.Typeface.NORMAL
            if (isBold) style = style or android.graphics.Typeface.BOLD
            if (isItalic) style = style or android.graphics.Typeface.ITALIC

            paint.typeface = android.graphics.Typeface.create(baseTypeface, style)
            paint.isUnderlineText = isUnderline
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun update(e: MotionEvent) {}

    private fun getRectBorder(): RectF {
        val lines = text.split("\n")
        var maxWidth = 0f
        for (line in lines) {
            val w = paint.measureText(line)
            if (w > maxWidth) maxWidth = w
        }

        val totalHeight = lines.size * paint.fontSpacing

        // Ensure point is at Bottom-Left
        // top = point.y - totalHeight
        // left = point.x
        val left = point.x
        val top = point.y - totalHeight

        return RectF(left, top, left + maxWidth, point.y)
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
    override fun drawSelectedBox(
            canvas: Canvas,
            deleteBmp: Bitmap?,
            duplicateBmp: Bitmap?,
            rotateBmp: Bitmap?,
            resizeBmp: Bitmap?
    ) {
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

            // Common paint for button backgrounds
            val btnBgPaint = Paint()
            btnBgPaint.color = "#5369e7".toColorInt()
            btnBgPaint.style = Paint.Style.FILL

            // Draw rotate handle (Bottom-Left)
            if (rotateBmp != null) {
                drawCircle(rect.left - 30f, rect.bottom + 30f, 30f, btnBgPaint)
                drawBitmap(rotateBmp, rect.left - 50, rect.bottom + 10, null)
            } else {
                selectedPaint.color = android.graphics.Color.RED
                drawCircle(rect.left - 30f, rect.bottom + 30f, 15f, selectedPaint)
            }

            if (deleteBmp != null) {
                drawCircle(rect.left - 30f, rect.top - 30f, 30f, btnBgPaint)
                drawBitmap(deleteBmp, rect.left - 50, rect.top - 50, null)
            }
            if (duplicateBmp != null) {
                drawCircle(rect.right + 30f, rect.top - 30f, 30f, btnBgPaint)
                drawBitmap(duplicateBmp, rect.right + 10, rect.top - 50, null)
            }
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val rect = RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val btnX = rect.left - 30f
        val btnY = rect.top - 30f
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

        val btnX = rect.right + 30f
        val btnY = rect.top - 30f
        val dx = rotatedPoint.x - btnX
        val dy = rotatedPoint.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingResize(e: MotionEvent): Boolean {
        return false
    }

    override fun isTouchingRotate(e: MotionEvent): Boolean {
        val rawRect = getRectBorder()
        val cx = rawRect.centerX()
        val cy = rawRect.centerY()
        val paddedRect =
                RectF(rawRect.left - 5, rawRect.top - 5, rawRect.right + 10, rawRect.bottom + 10)

        val rotatedPoint = rotatePoint(PointF(e.x, e.y), PointF(cx, cy), -rotation)

        val dx = rotatedPoint.x - (paddedRect.left - 30f)
        val dy = rotatedPoint.y - (paddedRect.bottom + 30f)
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
        val newHeight = getRectBorder().height()

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

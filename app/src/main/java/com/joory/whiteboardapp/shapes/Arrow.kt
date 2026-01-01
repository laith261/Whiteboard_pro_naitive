package com.joory.whiteboardapp.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt
import kotlin.math.atan2

class Arrow : Shape {
    override var sideLength = 100f
    override var paint = Paint()
    private var triangle = Triangle()
    private var line = Lines()
    override var shapeTools: MutableList<Tools> =
            mutableListOf(Tools.Style, Tools.StrokeWidth, Tools.Color)

    override fun draw(canvas: Canvas) {
        triangle.draw(canvas)
        line.draw(canvas)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
            triangle.updateObject(paint)
            line.updateObject(paint)
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newArrow = Arrow()
        // newArrow.paint.set(this.paint) // Removed
        newArrow.paint.isDither = true
        newArrow.paint.strokeJoin = Paint.Join.ROUND
        newArrow.paint.strokeCap = Paint.Cap.ROUND

        newArrow.sideLength = this.sideLength
        newArrow.triangle.sideLength = this.sideLength
        // Since Triangle and Line create returns 'this' (or will be refactored to return new),
        // we should check if they need to be assigned.
        // If Triangle.create returns new instance, we should assign it?
        // No, Arrow has its own Triangle instance. We call create on that instance.
        // If Triangle.create returns a NEW instance, then calling newArrow.triangle.create(e)
        // returns a new Triangle object
        // which is NOT assigned to newArrow.triangle unless we do: newArrow.triangle =
        // newArrow.triangle.create(e) as Triangle
        // BUT Triangle.create logic (original) updated 'this'.
        // If I refactor Triangle.create to return NEW instance, then Arrow code here MUST be
        // updated to capture it.
        // However, I haven't refactored Triangle.kt YET.
        // I should stick to the plan: Refactor Triangle.kt NEXT.
        // Logic here depends on what Triangle.kt does.
        // If I change Triangle.kt to return new instance, then:
        // newArrow.triangle = newArrow.triangle.create(e) as Triangle
        // But better: newArrow.triangle has a default Triangle().
        // We probably just want to initialize it?
        // Let's assume Triangle.create will be refactored to return new instance.
        // Then calling it on a fresh instance like newArrow.triangle is weird.
        // Actually, if newArrow.triangle is a fresh Triangle, calling create(e) on it (if create
        // returns new) creates ANOTHER one.
        // Better: newArrow.triangle.create uses the logic directly.
        // Or: newArrow.triangle = (tools[Shapes.Triangle] as Triangle).create(e) as Triangle? No.

        // If Triangle.create returns 'this' (mutates), then newArrow.triangle.create(e) is correct.
        // If Triangle.create returns 'new', then newArrow.triangle.create(e) returns a new object
        // and throws away the result?
        // Wait. `create` usually creates a shape at position e.
        // For Arrow, it wants `triangle` at position e.
        // So I should assign result:
        newArrow.triangle = newArrow.triangle.create(e) as Triangle
        newArrow.line = newArrow.line.create(e) as Lines

        return newArrow
    }

    override fun update(e: MotionEvent) {
        line.update(e)
        triangle.update(e)
        val dx = e.x - line.start.x
        val dy = e.y - line.start.y
        triangle.rotation = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
    }

    override fun startMove(e: MotionEvent) {
        line.startMove(e)
        triangle.startMove(e)
    }

    override fun move(e: MotionEvent) {
        line.move(e)
        triangle.move(e)
    }

    override fun updateSideLength(length: Float) {
        super.updateSideLength(length)
        triangle.updateSideLength(sideLength)
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val rect =
                if (triangle.fp.y < line.start.y) {
                    RectF(
                            triangle.fp.x - (sideLength / 2) - 5,
                            triangle.fp.y - sideLength - 5,
                            line.start.x + (sideLength / 2) + 5,
                            line.start.y + 5
                    )
                } else {
                    RectF(
                            line.start.x - (sideLength / 2) - 5,
                            line.start.y - 5,
                            triangle.fp.x + (sideLength / 2) + 5,
                            triangle.fp.y + (sideLength / 2) + 5
                    )
                }
        return rect.contains(e.x, e.y)
    }

    override fun drawSelectedBox(canvas: Canvas, deleteBmp: Bitmap?, duplicateBmp: Bitmap?) {
        val rect =
                if (triangle.fp.y < line.start.y) {
                    RectF(
                            triangle.fp.x - (sideLength / 2) - 5,
                            triangle.fp.y - (sideLength / 2) - 5,
                            line.start.x + (sideLength / 2) + 5,
                            line.start.y + 5
                    )
                } else {
                    RectF(
                            line.start.x - (sideLength / 2) - 5,
                            line.start.y - 5,
                            triangle.fp.x + (sideLength / 2) + 5,
                            triangle.fp.y + (sideLength / 2) + 5
                    )
                }
        val selectedPaint = Paint()
        selectedPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        selectedPaint.style = Paint.Style.STROKE
        canvas.drawRect(rect, selectedPaint)

        // Common paint for button backgrounds
        val btnBgPaint = Paint()
        btnBgPaint.color = "#5369e7".toColorInt()
        btnBgPaint.style = Paint.Style.FILL

        if (deleteBmp != null) {
            canvas.drawCircle(rect.left, rect.top, 30f, btnBgPaint)
            canvas.drawBitmap(deleteBmp, rect.left - 30, rect.top - 30, null)
        }
        if (duplicateBmp != null) {
            canvas.drawCircle(rect.right, rect.top, 30f, btnBgPaint)
            canvas.drawBitmap(duplicateBmp, rect.right - 30, rect.top - 30, null)
        }
    }

    override fun isTouchingDelete(e: MotionEvent): Boolean {
        val rect =
                if (triangle.fp.y < line.start.y) {
                    RectF(
                            triangle.fp.x - (sideLength / 2) - 5,
                            triangle.fp.y - (sideLength / 2) - 5,
                            line.start.x + (sideLength / 2) + 5,
                            line.start.y + 5
                    )
                } else {
                    RectF(
                            line.start.x - (sideLength / 2) - 5,
                            line.start.y - 5,
                            triangle.fp.x + (sideLength / 2) + 5,
                            triangle.fp.y + (sideLength / 2) + 5
                    )
                }
        val btnX = rect.left
        val btnY = rect.top
        val dx = e.x - btnX
        val dy = e.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }

    override fun isTouchingDuplicate(e: MotionEvent): Boolean {
        val rect =
                if (triangle.fp.y < line.start.y) {
                    RectF(
                            triangle.fp.x - (sideLength / 2) - 5,
                            triangle.fp.y - (sideLength / 2) - 5,
                            line.start.x + (sideLength / 2) + 5,
                            line.start.y + 5
                    )
                } else {
                    RectF(
                            line.start.x - (sideLength / 2) - 5,
                            line.start.y - 5,
                            triangle.fp.x + (sideLength / 2) + 5,
                            triangle.fp.y + (sideLength / 2) + 5
                    )
                }
        val btnX = rect.right
        val btnY = rect.top
        val dx = e.x - btnX
        val dy = e.y - btnY
        return (dx * dx + dy * dy) <= 2500
    }
}

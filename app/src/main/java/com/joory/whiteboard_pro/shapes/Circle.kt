package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.pow
import kotlin.math.sqrt

class Circle : Shape {
    private var cp = PointF(0f, 0f)
    private var radius = 50f
    override var paint = Paint()
    override var sideLength: Float = 0.0f

    override fun draw(canvas: Canvas) {
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
        cp = PointF(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        radius = distance(cp, PointF(e.x, e.y))
    }


    private fun distance(center: PointF, end: PointF): Float {
        return sqrt(
            (end.x - center.x).toDouble().pow(2.0)
                    + (end.y - center.y).toDouble().pow(2.0)
        ).toFloat()
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val rect = RectF(cp.x - radius, cp.y - radius, cp.x + radius, cp.y + radius)
        return rect.contains(e.x, e.y)
    }

    override fun drawSelectedBox(canvas: Canvas) {
        val rect = RectF(cp.x - radius - 5, cp.y - radius - 5, cp.x + radius + 5, cp.y + radius + 5)
        val selectedPaint = Paint()
        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
        selectedPaint.style = Paint.Style.STROKE
        canvas.drawRect(rect, selectedPaint)

    }

    override fun move(e: MotionEvent) {
        cp = PointF(e.x, e.y)
    }
}
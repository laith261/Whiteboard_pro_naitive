package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent

class Rects : Shape {
    override var paint = Paint()
    private var start: PointF = PointF(0f, 0f)
    private var end: PointF = PointF(0f, 0f)
    private var rect = RectF(start.x, start.y, end.x, end.y)
    override var sideLength: Float = 0.0f
    override lateinit var text: String
    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
        rect = if (start.x > end.x) {
            RectF(end.x, start.y, start.x, end.y)

        } else {
            RectF(start.x, start.y, end.x, end.y)

        }
    }

    override fun create(e: MotionEvent): Shape {
        start = PointF(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        end = PointF(e.x, e.y)
        updateObject()
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        return rect.contains(e.x, e.y)
    }

    override fun drawSelectedBox(canvas: Canvas) {
        val selectedPaint = Paint()
        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
        selectedPaint.style = Paint.Style.STROKE
        canvas.drawRect(rect, selectedPaint)
    }

    override fun move(e: MotionEvent) {
        var Thewidth=rect.right-rect.left
        var Theheight=rect.bottom-rect.top
        rect.offsetTo(e.x-(Thewidth/2), e.y-(Theheight/2))
    }
}
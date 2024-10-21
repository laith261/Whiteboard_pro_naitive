package com.joory.whiteboard_pro.shapes

import android.graphics.*
import android.view.MotionEvent


class Lines : Shape {
    override var paint = Paint()
    private var start: PointF = PointF(0f, 0f)
    private var end: PointF = PointF(0f, 0f)
    private var dist: PointF = PointF(0f, 0f)

    override fun draw(canvas: Canvas) {
        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint!=null){
            this.paint.color=paint.color
            this.paint.strokeWidth=paint.strokeWidth
            this.paint.style=paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        start = PointF(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        end = PointF(e.x, e.y)
        dist = PointF(end.x - start.x, end.y - start.y)
    }

    private fun getRectBorder(): RectF {
        val leftTop =
            PointF(if (end.x > start.x) start.x else end.x, if (end.y > start.y) start.y else end.y)
        val rightBottom =
            PointF(if (end.x < start.x) start.x else end.x, if (end.y < start.y) start.y else end.y)
        return RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y)
    }

    fun example(width: Int, height: Int): Lines {
        start = PointF(((width / 2).toFloat()), (height * 0.25).toFloat())
        end = PointF(((width / 2).toFloat()), (height * 0.75).toFloat())
        return this
    }

    override fun isTouchingObject(e: MotionEvent): Boolean {
        val rect = getRectBorder()
        return rect.contains(e.x, e.y)
    }

    override fun drawSelectedBox(canvas: Canvas) {
        val rect = getRectBorder()
        val selectedPaint = Paint()
        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
        selectedPaint.style = Paint.Style.STROKE
        canvas.drawRect(rect, selectedPaint)

    }

    override fun move(e: MotionEvent) {
        start = PointF(e.x, e.y)
        end = PointF(start.x + dist.x, start.y + dist.y)
    }
}
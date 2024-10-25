package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import java.lang.Math.toDegrees


class Lines : Shape {
    override var paint = Paint()
    private var start: PointF = PointF(0f, 0f)
    private var end: PointF = PointF(0f, 0f)
    private var angle = 0f
    private var inSerine=false
    private var dist: PointF = PointF(0f, 0f)

    override fun draw(canvas: Canvas) {
        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        start = PointF(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        end = PointF(e.x, e.y)
        angle = angle()
        inSerine=inSerineAngle()
        dist = PointF(end.x - start.x, end.y - start.y)
    }

    private fun getRectBorder(): RectF {
        val leftTop =
            PointF(if (end.x > start.x) start.x else end.x, if (end.y > start.y) start.y else end.y).apply {
                if (inSerine){
                    x-=15
                    y-=15
                }
            }
        val rightBottom =
            PointF(if (end.x < start.x) start.x else end.x, if (end.y < start.y) start.y else end.y).apply {
                if (inSerine){
                    x+=15
                    y+=15
                }
            }
        return RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y)
    }

    private fun inSerineAngle():Boolean{
        val angles = arrayOf(355..365, 85..95,175..185, 265.. 275)
        for (angle in angles){
            if (this.angle.toInt() in angle){
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

    private fun angle(): Float {
        val deltaX = start.x - end.x
        val deltaY = start.y - end.y
        val theAngle = toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
        if(angle<5) {
            return angle+365
        }
        return if (theAngle < 0) theAngle+ 360 else theAngle
    }

    override fun move(e: MotionEvent) {
        start = PointF(e.x, e.y)
        end = PointF(start.x + dist.x, start.y + dist.y)
    }
}
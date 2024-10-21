package com.joory.whiteboard_pro.shapes

import android.graphics.*
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RequiresApi

class Texts : Shape {
    override var paint: Paint = Paint()
    override var text: String = ""
    private var point = PointF(0f, 0f)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun draw(canvas: Canvas) {
        canvas.drawText(text, point.x, point.y, paint)
    }

    override fun create(e: MotionEvent): Shape {
        paint.textSize = 50f
        point = PointF(e.x, e.y)
        return this
    }

    override fun updateObject(paint: Paint?) {
        if (paint!=null){
            this.paint.color=paint.color
            this.paint.strokeWidth=paint.strokeWidth
            this.paint.style=paint.style
            this.paint.textSize=paint.textSize
        }
    }

    override fun update(e: MotionEvent) {

    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun isTouchingObject(e: MotionEvent): Boolean {
        val rect = Rect()
       paint.getTextBounds(text,0,text.length,rect)
        rect.offsetTo(point.x.toInt(), point.y.toInt()-rect.height())
        Log.i("touchin","${rect.left} ${rect.top} ${rect.right} ${rect.bottom} ")
        Log.i("isTouching",rect.contains(e.x.toInt(), e.y.toInt()).toString())
        return rect.contains(e.x.toInt(), e.y.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawSelectedBox(canvas: Canvas) {
        val rect = Rect()
        paint.getTextBounds(text,0,text.length,rect)
        rect.offsetTo(point.x.toInt(), point.y.toInt()-rect.height())
       rect.set(rect.left-5,rect.top-5,rect.right+10,rect.bottom+10)
        val selectedPaint = Paint()
        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
        selectedPaint.style = Paint.Style.STROKE
        canvas.drawRect(rect, selectedPaint)
    }

    override fun move(e: MotionEvent) {
        point = PointF(e.x, e.y)
    }
}
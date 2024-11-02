package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent

class Brush : Shape {
    override var paint: Paint = Paint()
    private var path = Path()
    override lateinit var text: String
    override var sideLength: Float = 0.0f

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
        }
    }

    override fun create(e: MotionEvent): Shape {
        paint.style = Paint.Style.STROKE
        path.moveTo(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        path.lineTo(e.x, e.y)
    }

}
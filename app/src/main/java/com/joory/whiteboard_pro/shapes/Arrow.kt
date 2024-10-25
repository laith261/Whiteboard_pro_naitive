package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent


class Arrow : Shape {
    override var paint = Paint()
    private var triangle = Triangle()
    private var line = Lines()
    private var sideLength = 75f

    override fun draw(canvas: Canvas) {
        triangle.draw(canvas)
        line.draw(canvas)
    }

    override fun updateObject(paint: Paint?) {
        if (paint!=null){
            this.paint.color=paint.color
            this.paint.strokeWidth=paint.strokeWidth
            this.paint.style=paint.style
            triangle.updateObject(paint)
            line.updateObject(paint)
        }
    }


    override fun create(e: MotionEvent): Shape {
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        triangle.sideLength = sideLength
        triangle.create(e)
        line.create(e)
        return this
    }

    override fun update(e: MotionEvent) {
        line.update(e)
        triangle.update(e)
    }

}
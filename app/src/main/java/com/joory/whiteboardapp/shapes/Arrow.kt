package com.joory.whiteboardapp.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent


class Arrow : Shape {
    override var sideLength = 100f
    override var paint = Paint()
    private var triangle = Triangle()
    private var line = Lines()

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

    override fun updateSideLength(length: Float) {
        super.updateSideLength(length)
        triangle.updateSideLength(sideLength)
    }
//
//    override fun isTouchingObject(e: MotionEvent): Boolean {
//       val rect = if(triangle.fp.y < line.start.y){
//            RectF(triangle.fp.x-(sideLength/2)-5,triangle.fp.y-sideLength-5,line.start.x+(sideLength/2)+5,line.start.y+5)
//        }else{
//            RectF(line.start.x-(sideLength/2)-5,line.start.y-5,triangle.fp.x+(sideLength/2)+5,triangle.fp.y+(sideLength/2)+5)
//        }
//        return rect.contains(e.x, e.y)
//    }
//
//    override fun drawSelectedBox(canvas: Canvas) {
//        val rect = if(triangle.fp.y < line.start.y){
//            RectF(triangle.fp.x-(sideLength/2)-5,triangle.fp.y-(sideLength/2)-5,line.start.x+(sideLength/2)+5,line.start.y+5)
//        }else{
//            RectF(line.start.x-(sideLength/2)-5,line.start.y-5,triangle.fp.x+(sideLength/2)+5,triangle.fp.y+(sideLength/2)+5)
//        }
//        val selectedPaint = Paint()
//        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
//        selectedPaint.style = Paint.Style.STROKE
//        canvas.drawRect(rect, selectedPaint)
//    }
}
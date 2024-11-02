package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import java.lang.Math.toDegrees
import java.lang.Math.toRadians

class Triangle : Shape {
    override var sideLength = 150f
    override var paint = Paint()
    var fp = PointF(0f, 0f)
    private var triangle = ATriangle(0f, 0f, sideLength, fp)
    override lateinit var text: String

    override fun draw(canvas: Canvas) {
        canvas.drawPath(triangle.path, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
            this.paint.style = paint.style
        }
    }

    override fun create(e: MotionEvent): Shape {
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        fp = PointF(e.x, e.y)
        return this
    }

    override fun update(e: MotionEvent) {
        triangle = ATriangle(e.x, e.y, sideLength, fp)
    }

//     override fun isTouchingObject(e: MotionEvent): Boolean {
//        val rect= RectF(triangle.cp.x-(sideLength/2),triangle.cp.y-(sideLength/2),triangle.cp.x+(sideLength/2),triangle.cp.y+(sideLength/2))
//        return rect.contains(e.x, e.y)
//    }
//
//    override fun drawSelectedBox(canvas: Canvas) {
//        val rect=RectF(triangle.cp.x-(sideLength/2)-10,triangle.cp.y-sideLength-10,triangle.cp.x+(sideLength/2)+10,triangle.cp.y+10).apply {
//            val xy=triangle.rotate(PointF(left,top))
//            val yx=triangle.rotate(PointF(right,bottom))
//            left=xy.x
//            top=xy.y
//            right=yx.x
//            bottom=yx.y
//        }
//        val selectedPaint = Paint()
//        selectedPaint.pathEffect = DashPathEffect(FloatArray(10), 5f)
//        selectedPaint.style = Paint.Style.STROKE
//        canvas.drawRect(rect, selectedPaint)
//
//    }
//
//    override fun updateSideLength(length: Float) {
//        super.updateSideLength(length)
//        triangle=ATriangle(triangle.cp.x,triangle.cp.y,sideLength,fp)
//    }
    class ATriangle(px: Float, py: Float, var sideLength: Float, private var fp: PointF) {
        var cp: PointF = PointF(px, py)
        private var p1: PointF = PointF(px, py - sideLength)
        private var p2: PointF = PointF(px + (sideLength / 2), py)
        private var p3: PointF = PointF(px - (sideLength / 2), py)
        var path: Path = Path()
        private var angle = 0f

        init {
            angle()
            p1 = rotate(p1)
            p2 = rotate(p2)
            p3 = rotate(p3)
            path.moveTo(p1.x, p1.y)
            path.lineTo(p2.x, p2.y)
            path.lineTo(p3.x, p3.y)
            path.close()
        }

         fun rotate(point: PointF): PointF {
            val s = kotlin.math.sin(toRadians(angle.toDouble())).toFloat()
            val c = kotlin.math.cos(toRadians(angle.toDouble())).toFloat()
            point.x -= cp.x
            point.y -= cp.y
            val xn = point.x * c - point.y * s
            val yn = point.x * s + point.y * c
            return PointF(xn + cp.x, yn + cp.y)
        }

        private fun angle() {
            val deltaX = fp.x - cp.x
            val deltaY = fp.y - cp.y
            val theAngle =
                toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
            angle = (theAngle.toDouble()).toFloat() - 90f
        }
    }
}
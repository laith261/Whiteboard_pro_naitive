package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import java.lang.Math.*

class Triangle : Shape {
    var sideLength = 150f
    override var paint = Paint()
    private var fp = PointF(0f, 0f)
    private var tringle = ATriangle(0f, 0f, sideLength, fp)
    override lateinit var text: String

    override fun draw(canvas: Canvas) {
        canvas.drawPath(tringle.path, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint!=null){
            this.paint.color=paint.color
            this.paint.strokeWidth=paint.strokeWidth
            this.paint.style=paint.style
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
        tringle = ATriangle(e.x, e.y, sideLength, fp)
    }

    class ATriangle(px: Float, py: Float, sideLength: Float, private var fp: PointF) {
        private var cp: PointF = PointF(px, py)
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

        private fun rotate(point: PointF): PointF {
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
            val theAngle = toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
            angle = (theAngle.toDouble()).toFloat() - 90f
        }
    }
}
package com.joory.whiteboardapp.shapes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.joory.whiteboardapp.models.SerializablePoint

class Eraser : Shape {
    override var paint: Paint = Paint()
    private var path = Path()
    override lateinit var text: String
    override var sideLength: Float = 0.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Serializable data
    var points = mutableListOf<SerializablePoint>()

    // Only StrokeWidth is relevant for Eraser, Color is fixed to White
    override var shapeTools: MutableList<Tools> = mutableListOf(Tools.StrokeWidth)

    init {
        paint.color = Color.TRANSPARENT
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 30f // Default thicker stroke for eraser
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    override fun draw(canvas: Canvas) {
        if (path.isEmpty && points.isNotEmpty()) {
            restore()
        }
        // Enforce settings
        paint.color = Color.TRANSPARENT
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        paint.style = Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            // Ignore color updates, only take stroke width
            this.paint.strokeWidth = paint.strokeWidth
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newEraser = Eraser()
        newEraser.paint.strokeWidth = this.paint.strokeWidth // Preserve width
        newEraser.path.moveTo(e.x, e.y)
        newEraser.points.add(SerializablePoint(e.x, e.y))
        return newEraser
    }

    override fun update(e: MotionEvent) {
        path.lineTo(e.x, e.y)
        points.add(SerializablePoint(e.x, e.y))
    }

    override fun restore() {
        path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
    }

    override fun startMove(e: MotionEvent) {
        lastTouchX = e.x
        lastTouchY = e.y
    }

    override fun move(e: MotionEvent) {
        val dx = e.x - lastTouchX
        val dy = e.y - lastTouchY
        path.offset(dx, dy)
        // Update points
        for (p in points) {
            p.x += dx
            p.y += dy
        }
        lastTouchX = e.x
        lastTouchY = e.y
    }
}

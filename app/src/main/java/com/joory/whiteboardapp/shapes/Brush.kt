package com.joory.whiteboardapp.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.joory.whiteboardapp.models.SerializablePoint

class Brush : Shape {
    override var paint: Paint = Paint()
    private var path = Path()
    override lateinit var text: String
    override var sideLength: Float = 0.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    override var shapeTools: MutableList<Tools> = mutableListOf(Tools.StrokeWidth, Tools.Color)

    // Serializable data
    var points = mutableListOf<SerializablePoint>()

    override fun draw(canvas: Canvas) {
        // Ensure path is valid if points exist but path is empty (legacy check, though restore()
        // should handle it)
        if (path.isEmpty && points.isNotEmpty()) {
            restore()
        }
        canvas.drawPath(path, paint)
    }

    override fun updateObject(paint: Paint?) {
        if (paint != null) {
            this.paint.color = paint.color
            this.paint.strokeWidth = paint.strokeWidth
        }
    }

    override fun create(e: MotionEvent): Shape {
        val newBrush = Brush()
        newBrush.paint.color = this.paint.color
        newBrush.paint.strokeWidth = this.paint.strokeWidth
        newBrush.paint.style = Paint.Style.STROKE
        newBrush.path.moveTo(e.x, e.y)
        newBrush.points.add(SerializablePoint(e.x, e.y))
        return newBrush
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

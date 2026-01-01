package com.joory.whiteboardapp.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent

class Select : Shape {
    override lateinit var text: String
    override var paint: Paint = Paint()
    override var sideLength: Float = 0.0f
    override fun draw(canvas: Canvas) {}

    override fun create(e: MotionEvent): Shape {
        return this
    }

    override fun update(e: MotionEvent) {}
}

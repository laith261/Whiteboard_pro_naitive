package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent

class Select : Shape {
    override lateinit var text: String
    override var paint: Paint = Paint()
    override fun draw(canvas: Canvas) {

    }

    override fun create(e: MotionEvent): Shape {

        return this
    }

    override fun update(e: MotionEvent) {

    }

}
package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent

interface Shape {
    var paint: Paint
    var text: String
        get() = ""
        set(value) = TODO()

    fun draw(canvas: Canvas)
    fun create(e: MotionEvent): Shape
    fun update(e: MotionEvent)
    fun updateObject(paint:Paint?=null) {

    }

    fun isTouchingObject(e: MotionEvent): Boolean {
        return false
    }

    fun drawSelectedBox(canvas: Canvas) {

    }

    fun move(e: MotionEvent) {

    }
}
package com.joory.whiteboard_pro.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import com.google.gson.Gson

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
    fun deepCopy():Shape {
        val JSON = Gson().toJson(this)
        var item=Gson().fromJson(JSON, this::class.java)
        item.paint.set(this.paint)
        return item
    }
}
package com.joory.whiteboardapp.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import com.google.gson.Gson
import com.joory.whiteboardapp.R

enum class Tools(var buttonId: Int) {
    Style(R.id.style),
    StrokeWidth(R.id.strokewidth),
    Color(R.id.color),
    Crop(R.id.crop),
}

interface Shape {
    var sideLength: Float
    var paint: Paint
    var rotation: Float
        get() = 0f
        set(value) {
            rotation = value
        }

    var text: String
        get() = ""
        set(value) {
            text = value
        }

    // In your Shape interface
    val shapeTools: MutableList<Tools>
        get() = mutableListOf()

    fun isTouchingRotate(e: MotionEvent): Boolean {
        return false
    }

    fun isTouchingDelete(e: MotionEvent): Boolean {
        return false
    }

    fun isTouchingDuplicate(e: MotionEvent): Boolean {
        return false
    }

    fun rotateShape(e: MotionEvent) {}

    fun draw(canvas: Canvas)
    fun create(e: MotionEvent): Shape
    fun update(e: MotionEvent)
    fun updateObject(paint: Paint? = null) {}

    fun isTouchingObject(e: MotionEvent): Boolean {
        return false
    }

    fun drawSelectedBox(
            canvas: Canvas,
            deleteBmp: Bitmap? = null,
            duplicateBmp: Bitmap? = null,
            rotateBmp: Bitmap? = null,
            resizeBmp: Bitmap? = null
    ) {}

    fun startMove(e: MotionEvent) {}

    fun move(e: MotionEvent) {}

    fun isTouchingResize(e: MotionEvent): Boolean {
        return false
    }

    fun resize(e: MotionEvent) {}

    fun updateSideLength(length: Float) {
        sideLength = length
    }

    fun deepCopy(): Shape {
        val JSON = Gson().toJson(this)
        val item = Gson().fromJson(JSON, this::class.java)
        item.paint.set(this.paint)
        return item
    }
}

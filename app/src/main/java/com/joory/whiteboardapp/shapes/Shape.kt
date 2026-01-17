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
    Font(R.id.font_style),
}

interface Shape {
    var sideLength: Float
    var paint: Paint
    var rotation: Float
        get() = 0f
        set(value) {}

    var text: String
        get() = ""
        set(value) {}

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

    // Called after deserialization to restore transient fields like Path
    fun restore() {}

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
        val json = Gson().toJson(this)
        val item = Gson().fromJson(json, this::class.java)
        item.paint.set(this.paint)
        return item
    }
}

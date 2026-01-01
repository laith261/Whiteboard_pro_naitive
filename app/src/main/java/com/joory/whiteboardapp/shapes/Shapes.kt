package com.joory.whiteboardapp.shapes

enum class Shapes(shape: Shape) {
    Rect(Rects()),
    Line(Lines()),
    Circle(Circle()),
    Arrow(Arrow()),
    Brush(Brush()),
    Text(Texts()),
    Select(Select()),
    Triangle(Triangle()),
    Star(Star()),
    Hexagon(Hexagon()),
    Eraser(Eraser()),
    Image(ImageShape())
}

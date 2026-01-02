package com.joory.whiteboardapp.shapes

import com.joory.whiteboardapp.R

enum class Shapes(shape: Shape, dot: Int?) {
    Rect(Rects(), R.id.rect_dot),
    Line(Lines(), R.id.line_dot),
    Circle(Circle(), R.id.circle_dot),
    Arrow(Arrow(), R.id.arrow_dot),
    Brush(Brush(), R.id.brush_dot),
    Text(Texts(), R.id.texts_dot),
    Select(Select(), R.id.select_dot),
    Triangle(Triangle(), R.id.texts_dot),
    Star(Star(), R.id.star_dot),
    Hexagon(Hexagon(), R.id.hexagon_dot),
    Eraser(Eraser(), R.id.eraser_dot),
    Image(ImageShape(), null);

}

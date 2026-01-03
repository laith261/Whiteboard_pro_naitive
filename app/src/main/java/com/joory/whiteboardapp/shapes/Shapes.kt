package com.joory.whiteboardapp.shapes

import com.joory.whiteboardapp.R

enum class Shapes(val shape: Shape,val dot: Int,val buttonId: Int) {
    Rect(Rects(), R.id.rect_dot,R.id.rect),
    Line(Lines(), R.id.line_dot,R.id.line),
    Circle(Circle(), R.id.circle_dot,R.id.circle),
    Arrow(Arrow(), R.id.arrow_dot,R.id.arrow),
    Brush(Brush(), R.id.brush_dot,R.id.brush),
    Text(Texts(), R.id.texts_dot,R.id.texts),
    Select(Select(), R.id.select_dot,R.id.select),
    Triangle(Triangle(), R.id.texts_dot,R.id.tringle),
    Star(Star(), R.id.star_dot,R.id.star),
    Hexagon(Hexagon(), R.id.hexagon_dot,R.id.hexagon),
    Eraser(Eraser(), R.id.eraser_dot,R.id.eraser),
    Image(ImageShape(), R.id.image_dot,R.id.add_image_shape);
}

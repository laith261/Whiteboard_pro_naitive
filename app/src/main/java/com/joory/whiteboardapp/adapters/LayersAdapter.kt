package com.joory.whiteboardapp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joory.whiteboardapp.R
import com.joory.whiteboardapp.shapes.Shape
import java.util.Collections

class LayersAdapter(
        private val layers: MutableList<Shape>,
        private val onDuplicate: (Int) -> Unit,
        private val onDelete: (Int) -> Unit,
        private val onSelect: (Int) -> Unit,
        private val onReorder: (Int, Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<LayersAdapter.LayerViewHolder>() {

    class LayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtLayerName)
        val btnSelect: ImageButton = itemView.findViewById(R.id.btnSelect)
        val btnDuplicate: ImageButton = itemView.findViewById(R.id.btnDuplicate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)
        val imgShapePreview: ImageView = itemView.findViewById(R.id.imgShapePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layer, parent, false)
        return LayerViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        // Reverse index for display (Top of list = Top of stack = Last in array)
        // But for Drag/Drop it's easier to map 1:1 if we reverse the list passed or just handle
        // indices carefully.
        // Let's assume the list passed is already reversed OR we reverse the displaying logic.
        // Actually, typically intuitive layer usage: Top item in list = Topmost layer (drawn last).
        // Since `draws` has index 0 as bottom-most, we should probably display the list in reverse
        // order?
        // Or we keep it simple: List index 0 = Array index 0 (Bottom).
        // User asked for "Layers" which usually means Top item = Front.
        // Let's assume the MainActivity will pass a Reversed List or we handle it here.
        // Actually to support reordering easily with ItemTouchHelper, it's best if the RecyclerView
        // list matches the Adapter list.
        // So MainActivity should give us the direct reference.
        // If we want Top = Front, we should probably reverse the data when showing, but that makes
        // index mapping tricky.
        // Let's stick to Index 0 = Back (Bottom) for now, or if it feels wrong we can change.
        // Actually, standard layer UI: Top of list is Top Layer.
        // So index 0 in Adapter = index (size-1) in `draws`.
        // This is complex for Drag & Drop mapping.
        // Let's implement: List index 0 = Bottom Layer (First drawn).
        // If user wants Top Layer at top, we need to reverse.

        // Simplicity: Index 0 = Bottom Layer. The last item in the list is the top layer.

        val shape = layers[position]
        holder.txtName.text = shape::class.java.simpleName

        val iconRes =
                when (shape::class.java.simpleName) {
                    "Rects" -> R.drawable.ic_square_shape
                    "Circle" -> R.drawable.ic_circle_shape
                    "Triangle" -> R.drawable.ic_triangle_shape
                    "Lines" -> R.drawable.line
                    "Arrow" -> R.drawable.arrow
                    "Star" -> R.drawable.ic_star
                    "Hexagon" -> R.drawable.ic_hexagon
                    "Texts" -> R.drawable.t
                    "Brush" -> R.drawable.brush
                    "Eraser" -> R.drawable.eraser
                    "ImageShape" -> R.drawable.ic_image
                    else -> R.drawable.stationery // Fallback
                }
        holder.imgShapePreview.setImageResource(iconRes)

        val isSelectable =
                when (shape::class.java.simpleName) {
                    "Brush", "Eraser", "Arrow" -> false
                    else -> true
                }

        if (isSelectable) {
            holder.btnSelect.visibility = View.VISIBLE
            holder.btnSelect.setOnClickListener { onSelect(position) }
        } else {
            holder.btnSelect.visibility = View.GONE
            holder.btnSelect.setOnClickListener(null)
        }

        holder.btnDuplicate.setOnClickListener { onDuplicate(position) }

        holder.btnDelete.setOnClickListener { onDelete(position) }

        holder.imgDragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = layers.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(layers, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(layers, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        onReorder(fromPosition, toPosition)
    }
}

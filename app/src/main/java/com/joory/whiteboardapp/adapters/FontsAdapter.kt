package com.joory.whiteboardapp.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joory.whiteboardapp.R
import java.io.File

class FontsAdapter(
        private val fonts: MutableList<File>,
        private val onFontSelected: (File) -> Unit,
        private val onDeleteClick: (File) -> Unit,
        private val protectedFonts: List<String> = emptyList()
) : RecyclerView.Adapter<FontsAdapter.FontViewHolder>() {

    class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFontName: TextView = itemView.findViewById(R.id.tvFontName)
        val btnDelete: android.widget.ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_font, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val fontFile = fonts[position]
        holder.tvFontName.text = fontFile.nameWithoutExtension
        try {
            val typeface = Typeface.createFromFile(fontFile)
            holder.tvFontName.typeface = typeface
        } catch (e: Exception) {
            e.printStackTrace()
        }

        holder.itemView.setOnClickListener { onFontSelected(fontFile) }

        if (protectedFonts.contains(fontFile.name)) {
            holder.btnDelete.visibility = View.GONE
        } else {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(fontFile) }
        }
    }

    override fun getItemCount(): Int = fonts.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newFonts: List<File>) {
        fonts.clear()
        fonts.addAll(newFonts)
        notifyDataSetChanged()
    }
}

package com.joory.whiteboardapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.joory.whiteboardapp.R
import com.joory.whiteboardapp.models.Project
import java.io.File

class WorksAdapter(
        private var works: List<Project>,
        private val onWorkClick: (Project) -> Unit,
        private val onWorkLongClick: (Project) -> Unit,
        private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<WorksAdapter.WorkViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    fun getSelectedIds(): Set<String> = selectedIds

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun updateData(newWorks: List<Project>) {
        works = newWorks
        notifyDataSetChanged()
    }

    class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivWork)
        val tvName: android.widget.TextView = itemView.findViewById(R.id.tvName)
        val btnOptions: android.widget.ImageButton = itemView.findViewById(R.id.btnOptions)
        val overlay: View = itemView.findViewById(R.id.viewSelectionOverlay)
        val check: ImageView = itemView.findViewById(R.id.ivCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_work, parent, false)
        return WorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        val project = works[position]

        holder.tvName.text = project.name

        if (project.thumbnailPath.isNotEmpty()) {
            val file = File(project.thumbnailPath)
            if (file.exists()) {
                try {
                    holder.imageView.setImageURI(android.net.Uri.fromFile(file))
                } catch (e: Exception) {
                    holder.imageView.setImageResource(R.drawable.stationery)
                }
            } else {
                holder.imageView.setImageResource(R.drawable.stationery)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.stationery)
        }

        // Update selection visuals
        if (selectedIds.contains(project.id)) {
            holder.overlay.visibility = View.VISIBLE
            holder.check.visibility = View.VISIBLE
        } else {
            holder.overlay.visibility = View.GONE
            holder.check.visibility = View.GONE
        }

        holder.btnOptions.setOnClickListener {
            // Options button always opens options, regardless of selection mode
            onWorkLongClick(project)
        }

        holder.itemView.setOnClickListener {
            if (selectedIds.isNotEmpty()) {
                toggleSelection(project.id)
            } else {
                onWorkClick(project)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(project.id)
            true
        }
    }

    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    override fun getItemCount(): Int = works.size
}

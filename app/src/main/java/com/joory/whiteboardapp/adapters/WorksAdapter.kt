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
        private val onWorkLongClick: (Project) -> Unit
) : RecyclerView.Adapter<WorksAdapter.WorkViewHolder>() {

    fun updateData(newWorks: List<Project>) {
        works = newWorks
        notifyDataSetChanged()
    }

    class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivWork)
        val tvName: android.widget.TextView = itemView.findViewById(R.id.tvName)
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

        holder.itemView.setOnClickListener { onWorkClick(project) }
        holder.itemView.setOnLongClickListener {
            onWorkLongClick(project)
            true
        }
    }

    override fun getItemCount(): Int = works.size
}

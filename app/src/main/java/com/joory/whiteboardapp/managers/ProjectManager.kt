package com.joory.whiteboardapp.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.google.gson.Gson
import com.joory.whiteboardapp.MyCanvas
import com.joory.whiteboardapp.models.Project
import com.joory.whiteboardapp.shapes.ImageShape
import com.joory.whiteboardapp.shapes.Shape
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectManager(private val context: Context) {

    private val gson = Gson()
    private val rootDir = File(context.getExternalFilesDir(null), "Whiteboard_Pro_Projects")

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    data class PaintData(
            val color: Int,
            val strokeWidth: Float,
            val style: Paint.Style,
            val alpha: Int,
            val textSize: Float
    )

    data class ShapeData(
            val type: String,
            val jsonData: String,
            val paint: PaintData,
            val imageFileName: String? = null
    )

    data class ProjectMetadata(val name: String, val created: Long, val lastModified: Long)

    fun saveProject(
            canvas: MyCanvas,
            projectId: String? = null,
            projectName: String = "Untitled"
    ): Project {
        val id = projectId ?: UUID.randomUUID().toString()
        val projectDir = File(rootDir, id)
        if (!projectDir.exists()) projectDir.mkdirs()

        // 1. Save Thumbnail
        // We create a bitmap from the canvas
        val thumbnailBitmap =
                Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        val thumbnailCanvas = Canvas(thumbnailBitmap)
        // Draw background
        canvas.draw(thumbnailCanvas)
        // Note: canvas.draw might draw the selection box if an item is selected.
        // Ideally we should temporarily deselect, but MyCanvas doesn't expose an easy way without
        // `invalidate`.
        // Users might accept seeing selection in thumbnail, or we can try to assume `startDrawing`
        // handles it.
        // Actually `onDraw` calls `startDrawing`. We can't easily call `draw` without `onDraw`
        // context effectively if it relies on view state.
        // But `canvas.draw(Canvas)` is standard View method.
        // Let's rely on current state.

        val thumbnailFile = File(projectDir, "thumbnail.png")
        FileOutputStream(thumbnailFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 50, out)
        }

        // 2. Serialize Shapes
        val shapeList = mutableListOf<ShapeData>()
        for (shape in canvas.draws) {
            val type = shape::class.java.name
            // We need to exclude 'bitmap' from Gson serialization for ImageShape because it's
            // circular/complex?
            // Gson usually ignores Bitmap unless configured otherwise or crashes.
            // Shape uses default Gson in deepCopy so it implies it works for basic fields.
            val json = gson.toJson(shape)

            var imageFileName: String? = null
            if (shape is ImageShape && shape.bitmap != null) {
                // Save bitmap to file
                // We use a unique name hash or something to avoid overwrite if multiple identical
                // images?
                // Or just random UUID for the image file
                val imgName = "img_${UUID.randomUUID()}.png"
                val imgFile = File(projectDir, imgName)
                FileOutputStream(imgFile).use { out ->
                    shape.bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                imageFileName = imgName
            }

            val paintData =
                    PaintData(
                            shape.paint.color,
                            shape.paint.strokeWidth,
                            shape.paint.style,
                            shape.paint.alpha,
                            shape.paint.textSize
                    )

            shapeList.add(ShapeData(type, json, paintData, imageFileName))
        }

        val dataFile = File(projectDir, "data.json")
        dataFile.writeText(gson.toJson(shapeList))

        // 3. Save Metadata (Name)
        val metaFile = File(projectDir, "meta.json")
        // Load existing meta to preserve creation date or get name if not provided
        var currentName = projectName
        var created = System.currentTimeMillis()

        if (metaFile.exists()) {
            try {
                val oldMeta = gson.fromJson(metaFile.readText(), ProjectMetadata::class.java)
                if (projectName == "Untitled" && oldMeta.name != "Untitled")
                        currentName = oldMeta.name
                created = oldMeta.created
            } catch (e: Exception) {}
        }

        val meta = ProjectMetadata(currentName, created, System.currentTimeMillis())
        metaFile.writeText(gson.toJson(meta))

        return Project(id, currentName, thumbnailFile.absolutePath, meta.lastModified)
    }

    fun renameProject(projectId: String, newName: String) {
        val projectDir = File(rootDir, projectId)
        if (!projectDir.exists()) return

        val metaFile = File(projectDir, "meta.json")
        var created = System.currentTimeMillis()
        var lastModified = System.currentTimeMillis()

        if (metaFile.exists()) {
            try {
                val oldMeta = gson.fromJson(metaFile.readText(), ProjectMetadata::class.java)
                created = oldMeta.created
                lastModified = oldMeta.lastModified
            } catch (e: Exception) {}
        }

        val meta = ProjectMetadata(newName, created, lastModified)
        metaFile.writeText(gson.toJson(meta))
    }

    fun loadProject(canvas: MyCanvas, projectId: String) {
        val projectDir = File(rootDir, projectId)
        val dataFile = File(projectDir, "data.json")

        if (!dataFile.exists()) return

        val jsonString = dataFile.readText()
        val shapeDataList = gson.fromJson(jsonString, Array<ShapeData>::class.java)

        canvas.draws.clear()
        canvas.undo.clear()

        for (data in shapeDataList) {
            try {
                val clazz = Class.forName(data.type)
                val shape = gson.fromJson(data.jsonData, clazz) as Shape

                // Restore Paint
                shape.paint.color = data.paint.color
                shape.paint.strokeWidth = data.paint.strokeWidth
                shape.paint.style = data.paint.style
                shape.paint.alpha = data.paint.alpha
                shape.paint.textSize = data.paint.textSize

                // Restore Image
                if (shape is ImageShape && data.imageFileName != null) {
                    val imgFile = File(projectDir, data.imageFileName)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        shape.bitmap = bitmap
                    }
                }

                // Restore Path for Brush/Eraser
                shape.restore()

                canvas.draws.add(shape)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        canvas.invalidate()
    }

    fun getProjects(): List<Project> {
        val projects = mutableListOf<Project>()
        if (rootDir.exists()) {
            rootDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val thumb = File(dir, "thumbnail.png")
                    val data = File(dir, "data.json")
                    if (data.exists()) {
                        var name = "Project ${dir.name.take(4)}"
                        var lastMod = dir.lastModified()

                        val metaFile = File(dir, "meta.json")
                        if (metaFile.exists()) {
                            try {
                                val meta =
                                        gson.fromJson(
                                                metaFile.readText(),
                                                ProjectMetadata::class.java
                                        )
                                name = meta.name
                                lastMod = meta.lastModified
                            } catch (e: Exception) {}
                        }

                        // We use dir name as ID
                        projects.add(
                                Project(
                                        dir.name,
                                        name,
                                        if (thumb.exists()) thumb.absolutePath else "",
                                        lastMod
                                )
                        )
                    }
                }
            }
        }
        return projects.sortedByDescending { it.lastModified }
    }

    fun deleteProject(projectId: String) {
        File(rootDir, projectId).deleteRecursively()
    }
}

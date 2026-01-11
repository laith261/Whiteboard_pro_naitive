package com.joory.whiteboardapp.models

data class Project(
        val id: String,
        var name: String,
        val thumbnailPath: String,
        var lastModified: Long
)

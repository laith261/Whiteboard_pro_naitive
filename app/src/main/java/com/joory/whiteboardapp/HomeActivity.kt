package com.joory.whiteboardapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.joory.whiteboardapp.adapters.WorksAdapter
import com.joory.whiteboardapp.managers.ProjectManager
import com.joory.whiteboardapp.models.Project

class HomeActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var fab: FloatingActionButton
    private lateinit var projectManager: ProjectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        projectManager = ProjectManager(this)

        recycler = findViewById(R.id.recyclerWorks)
        emptyView = findViewById(R.id.viewEmpty)
        fab = findViewById(R.id.fabNew)

        recycler.layoutManager = GridLayoutManager(this, 2)

        fab.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        // Animate FAB up and down
        val distance =
                android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP,
                        5f,
                        resources.displayMetrics
                )
        val animator = android.animation.ObjectAnimator.ofFloat(fab, "translationY", 0f, -distance)
        animator.duration = 1000
        animator.repeatCount = android.animation.ObjectAnimator.INFINITE
        animator.repeatMode = android.animation.ObjectAnimator.REVERSE
        animator.start()

        MobileAds.initialize(this) {}
        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        checkPermissionAndLoadWorks()
    }

    override fun onResume() {
        super.onResume()
        loadWorks()
    }

    private fun checkPermissionAndLoadWorks() {
        loadWorks() // Load regardless of permissions
        if (!hasStoragePermission()) {
            requestStoragePermission()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.READ_MEDIA_IMAGES
                            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    1001
            )
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1001
            )
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            loadWorks()
        }
    }

    private fun loadWorks() {
        val projects = projectManager.getProjects()

        if (projects.isEmpty()) {
            showEmptyState()
        } else {
            showWorks(projects)
        }
    }

    private fun showEmptyState() {
        recycler.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }

    private fun showWorks(projects: List<Project>) {
        recycler.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        val adapter =
                WorksAdapter(
                        projects,
                        { project ->
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("EXTRA_PROJECT_ID", project.id)
                            startActivity(intent)
                        },
                        { project -> showProjectOptions(project) },
                        { count -> updateSelectionMode(count) }
                )
        recycler.adapter = adapter

        val etSearch = findViewById<android.widget.EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        val query = s.toString().trim()
                        val filtered =
                                if (query.isEmpty()) {
                                    projects
                                } else {
                                    projects.filter { it.name.contains(query, ignoreCase = true) }
                                }
                        adapter.updateData(filtered)
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                }
        )
    }

    private fun showProjectOptions(project: Project) {
        val options =
                arrayOf(
                        getString(R.string.action_rename),
                        getString(R.string.action_share_image),
                        getString(R.string.action_export_image),
                        getString(R.string.delete)
                )
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(project.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showRenameDialog(project)
                        1 -> shareProjectImage(project)
                        2 -> exportProjectImage(project)
                        3 -> confirmDeleteProject(project)
                    }
                }
                .show()
    }

    private fun showRenameDialog(project: Project) {
        val input = android.widget.EditText(this)
        input.setText(project.name)
        input.setSingleLine()

        val container = android.widget.FrameLayout(this)
        val params =
                android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_rename_project))
                .setView(container)
                .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        projectManager.renameProject(project.id, newName)
                        loadWorks()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
    }

    private fun shareProjectImage(project: Project) {
        try {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val offScreenCanvas = MyCanvas(this, null)
            offScreenCanvas.layout(0, 0, width, height)
            // START OF FIX: Removed createExample call to prevent drawing the example line
            // offScreenCanvas.createExample(width, height)
            // END OF FIX

            projectManager.loadProject(offScreenCanvas, project.id)

            // Generate Bitmap
            val bitmap =
                    android.graphics.Bitmap.createBitmap(
                            width,
                            height,
                            android.graphics.Bitmap.Config.ARGB_8888
                    )
            val canvas = android.graphics.Canvas(bitmap)
            offScreenCanvas.draw(canvas)

            // Save to Cache for Sharing
            val cachePath = java.io.File(cacheDir, "shared_images")
            cachePath.mkdirs()
            val fileName = "share_${System.currentTimeMillis()}.png"
            val file = java.io.File(cachePath, fileName)
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // Get URI using FileProvider
            val uri =
                    androidx.core.content.FileProvider.getUriForFile(
                            this,
                            "${applicationContext.packageName}.provider",
                            file
                    )

            // Create Share Intent
            val shareIntent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_image)))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                            this,
                            "Failed to share image",
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }

    private fun updateSelectionMode(count: Int) {
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvTitle)
        val fab = findViewById<FloatingActionButton>(R.id.fabNew)

        if (count > 0) {
            tvTitle.text = getString(R.string.selected_count, count)

            // Transform FAB to Delete button
            fab.show()
            fab.setImageResource(R.drawable.ic_delete_outline)
            fab.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFFF44336.toInt()) // Red color
            fab.setOnClickListener {
                val adapter = recycler.adapter as? WorksAdapter
                if (adapter != null) {
                    confirmDeleteSelected(adapter)
                }
            }
        } else {
            tvTitle.text = getString(R.string.title_my_works)

            // Transform FAB back to New Project button
            fab.show()
            fab.setImageResource(R.drawable.ic_add)
            fab.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.purple_500)
                    )
            fab.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        }
    }

    private fun confirmDeleteSelected(adapter: WorksAdapter) {
        val count = adapter.getSelectedIds().size
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_delete_project))
                .setMessage(getString(R.string.msg_delete_selected_confirm, count))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    val ids = adapter.getSelectedIds()
                    for (id in ids) {
                        projectManager.deleteProject(id)
                    }
                    adapter.clearSelection()
                    loadWorks()
                    android.widget.Toast.makeText(
                                    this,
                                    getString(R.string.msg_projects_deleted, count),
                                    android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
    }

    override fun onBackPressed() {
        val adapter = recycler.adapter as? WorksAdapter
        if (adapter != null && adapter.getSelectedIds().isNotEmpty()) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    private fun exportProjectImage(project: Project) {
        try {
            // Create an off-screen canvas to render the project
            // We use display metrics to guess size, or default to 1080x1920 if unknown
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val offScreenCanvas = MyCanvas(this, null)
            // We need to set the layout params or manually measure/layout so it has a size
            offScreenCanvas.layout(0, 0, width, height)

            // START OF FIX: Removed createExample call to prevent drawing the example line
            // offScreenCanvas.createExample(width, height)
            // END OF FIX

            projectManager.loadProject(offScreenCanvas, project.id)

            // SaveImage expects a view that returns a bitmap or draws to canvas
            val saveImage = com.joory.whiteboardapp.functions.SaveImage()
            saveImage.saveImage(offScreenCanvas, false)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                            this,
                            getString(R.string.msg_export_failed),
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
            e.printStackTrace()
        }
    }

    private fun confirmDeleteProject(project: Project) {
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_delete_project))
                .setMessage(getString(R.string.msg_delete_project_confirm))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    projectManager.deleteProject(project.id)
                    loadWorks()
                    android.widget.Toast.makeText(
                                    this,
                                    getString(R.string.msg_project_deleted),
                                    android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
    }
}

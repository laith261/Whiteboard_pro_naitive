package com.joory.whiteboardapp

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.exifinterface.media.ExifInterface
import com.canhub.cropper.CropImageContract
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.badge.BadgeDrawable
import com.joory.whiteboardapp.functions.Ads
import com.joory.whiteboardapp.functions.ColorPicker
import com.joory.whiteboardapp.functions.Crop
import com.joory.whiteboardapp.functions.Dialogs
import com.joory.whiteboardapp.functions.ImageUtils
import com.joory.whiteboardapp.functions.Permission
import com.joory.whiteboardapp.functions.SaveImage
import com.joory.whiteboardapp.functions.SetImageBg
import com.joory.whiteboardapp.shapes.ImageShape
import com.joory.whiteboardapp.shapes.Shapes
import java.io.InputStream
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    companion object {
        private var weakActivity: WeakReference<MainActivity>? = null
        fun getInstanceActivity(): MainActivity? {
            return weakActivity?.get()
        }
    }

    private lateinit var canvas: MyCanvas
    private lateinit var colorButton: ImageButton
    private lateinit var strokeButton: ImageButton
    private lateinit var styleButton: ImageButton
    private lateinit var colorBg: ImageButton
    private lateinit var scroll: HorizontalScrollView
    private lateinit var undoButton: ImageView
    private lateinit var redoButton: ImageView
    private lateinit var imageBg: ImageButton
    private lateinit var tools: ImageButton
    private lateinit var save: ImageButton
    private lateinit var clear: ImageButton
    private lateinit var cropButton: ImageButton
    private lateinit var badgeDrawable: BadgeDrawable
    lateinit var ads: Ads
    lateinit var dialogs: Dialogs
    lateinit var permission: Permission
    lateinit var colorPicker: ColorPicker
    lateinit var crop: Crop
    lateinit var saveImage: SaveImage
    lateinit var setImageBg: SetImageBg
    lateinit var fontButton: ImageButton

    private val defaultFonts =
            listOf("BebasNeue.ttf", "Caveat.ttf", "Oswald.ttf", "PlayfairDisplay.ttf")

    private fun copyDefaultFonts() {
        val fontDir = java.io.File(filesDir, "fonts")
        if (!fontDir.exists()) fontDir.mkdirs()

        // Clean up old font names
        val legacyFonts =
                listOf(
                        "BebasNeue-Regular.ttf",
                        "Caveat-VariableFont_wght.ttf",
                        "Oswald-VariableFont_wght.ttf",
                        "PlayfairDisplay-VariableFont_wght.ttf"
                )
        for (legacy in legacyFonts) {
            val oldFile = java.io.File(fontDir, legacy)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }

        for (fontName in defaultFonts) {
            val file = java.io.File(fontDir, fontName)
            if (!file.exists()) {
                try {
                    assets.open("fonts/$fontName").use { input ->
                        java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private lateinit var projectManager: com.joory.whiteboardapp.managers.ProjectManager
    private var currentProjectId: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // functions
        initializeViews()
        copyDefaultFonts()
        permission.hasWriteStoragePermission()
        ads.loadFullScreenAd()
        showButtons()

        projectManager = com.joory.whiteboardapp.managers.ProjectManager(this)

        if (intent.hasExtra("EXTRA_PROJECT_ID")) {
            currentProjectId = intent.getStringExtra("EXTRA_PROJECT_ID")
            currentProjectId?.let { projectManager.loadProject(canvas, it) }
        }

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
        }
    }

    private fun initializeViews() {
        weakActivity = WeakReference(this)
        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        canvas = findViewById(R.id.canvas)
        colorButton = findViewById(R.id.color)
        styleButton = findViewById(R.id.style)
        strokeButton = findViewById(R.id.strokewidth)
        colorBg = findViewById(R.id.colorbg)
        scroll = findViewById(R.id.myscroll)
        undoButton = findViewById(R.id.undo)
        redoButton = findViewById(R.id.redo)
        save = findViewById(R.id.save)
        clear = findViewById(R.id.clear)
        cropButton = findViewById(R.id.crop)
        fontButton = findViewById(R.id.font_style)
        tools = findViewById(R.id.tools)
        imageBg = findViewById(R.id.imgbg)
        badgeDrawable = BadgeDrawable.create(this)
        supportActionBar?.hide()
        ads = Ads(this, canvas)
        dialogs = Dialogs(this)
        permission = Permission(this)
        colorPicker = ColorPicker(supportFragmentManager)
        crop = Crop()
        saveImage = SaveImage()
        setImageBg = SetImageBg()
    }

    fun View.onToolsClick() {
        dialogs.showDialog(R.layout.tools_dailog)
        choseTool(Shapes.Circle)
        choseTool(Shapes.Select)
        choseTool(Shapes.Arrow)
        choseTool(Shapes.Brush)
        choseTool(Shapes.Text)
        choseTool(Shapes.Line)
        choseTool(Shapes.Rect)
        choseTool(Shapes.Triangle)
        choseTool(Shapes.Star)
        choseTool(Shapes.Hexagon)
        choseTool(Shapes.Eraser)
        choseTool(Shapes.Image)
        // details for the chosen tool
        dialogs.dialog.findViewById<View>(canvas.tool.dot).visibility = View.VISIBLE
        dialogs.dialog.findViewById<ImageView>(Shapes.Image.buttonId).setOnClickListener {
            startForShapeImageResult.launch(
                    PickVisualMediaRequest.Builder().setMediaType(PickVisualMedia.ImageOnly).build()
            )
            dialogs.dismiss()
        }
    }

    fun View.onStyleClick() {
        canvas.changeStyle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun View.onLayersClick() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_layers)

        val recycler =
                dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerLayers)
        val close = dialog.findViewById<View>(R.id.btnClose)

        close?.setOnClickListener { dialog.dismiss() }

        val adapter =
                com.joory.whiteboardapp.adapters.LayersAdapter(
                        canvas.draws,
                        onDuplicate = { position ->
                            canvas.duplicateLayer(position)
                            dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(
                                            R.id.recyclerLayers
                                    )
                                    ?.adapter
                                    ?.notifyItemInserted(canvas.draws.size - 1)
                        },
                        onDelete = { position ->
                            canvas.removeLayer(position)
                            dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(
                                            R.id.recyclerLayers
                                    )
                                    ?.adapter
                                    ?.notifyItemRemoved(position)
                            dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(
                                            R.id.recyclerLayers
                                    )
                                    ?.adapter
                                    ?.notifyItemRangeChanged(position, canvas.draws.size)
                        },
                        onSelect = { position ->
                            canvas.selectObject(position)
                            dialog.dismiss()
                        },
                        onReorder = { from, to ->
                            // The adapter already swapped the list, but we need to tell canvas to
                            // invalidate if we rely on draws order (which we do)
                            // Actually the adapter uses the same list reference? Yes.
                            // But we should double check if adapter swapping reflects in draws
                            // properly.
                            // yes Collections.swap(layers, ...) where layers IS canvas.draws.
                            // So we just need to invalidate canvas.
                            canvas.invalidate()
                        },
                        onStartDrag = { holder -> itemTouchHelper.startDrag(holder) }
                )

        itemTouchHelper =
                androidx.recyclerview.widget.ItemTouchHelper(
                        object :
                                androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                                        androidx.recyclerview.widget.ItemTouchHelper.UP or
                                                androidx.recyclerview.widget.ItemTouchHelper.DOWN,
                                        0
                                ) {
                            override fun onMove(
                                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                                    viewHolder:
                                            androidx.recyclerview.widget.RecyclerView.ViewHolder,
                                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                            ): Boolean {
                                val fromPos = viewHolder.adapterPosition
                                val toPos = target.adapterPosition
                                adapter.moveItem(fromPos, toPos)
                                return true
                            }

                            override fun onSwiped(
                                    viewHolder:
                                            androidx.recyclerview.widget.RecyclerView.ViewHolder,
                                    direction: Int
                            ) {
                                // No swipe implementation
                            }

                            override fun isLongPressDragEnabled(): Boolean {
                                return false // We use handle
                            }
                        }
                )

        itemTouchHelper.attachToRecyclerView(recycler)
        recycler?.adapter = adapter

        dialog.show()
    }

    private lateinit var itemTouchHelper: androidx.recyclerview.widget.ItemTouchHelper

    fun View.onClearClick() {
        canvas.clearCanvas()
    }

    fun View.onColorClick() {
        colorPicker.colorsDialog(::objectColorSet)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        // Save if we are editing an existing project OR if we have created a new one (canvas not
        // empty)
        if (currentProjectId != null || canvas.draws.isNotEmpty()) {
            val project = projectManager.saveProject(canvas, currentProjectId)
            currentProjectId = project.id
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun View.onSaveClick() {
        // Auto-save project (update thumbnail) before exporting
        val project = projectManager.saveProject(canvas, currentProjectId)
        currentProjectId = project.id

        saveImage.saveImage(canvas)
    }

    fun View.onFontClick() {
        showFontsDialog()
    }

    fun View.onImageBgClick() {
        startForProfileImageResult.launch(
                PickVisualMediaRequest.Builder().setMediaType(PickVisualMedia.ImageOnly).build()
        )
    }

    fun View.onUndoClick() {
        canvas.undo()
        doButtonsAlpha()
    }

    fun View.onRedoClick() {
        canvas.redo()
        doButtonsAlpha()
    }

    fun View.onColorBgClick() {
        colorPicker.colorsDialog(::backgroundColorSet)
    }

    fun View.showStrokeDialog() {
        dialogs.showDialog(R.layout.size_dailog)
        val seek = dialogs.dialog.findViewById<SeekBar>(R.id.sizeSeek)
        seek.progress = canvas.getCanvasPaint().strokeWidth.toInt()
        seek.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                            seek: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                    ) {
                        canvas.getCanvasPaint().strokeWidth = progress.toFloat()
                        if (canvas.objectIndex == null) {
                            canvas.updateExample()
                        }
                        canvas.invalidate()
                    }

                    override fun onStartTrackingTouch(seek: SeekBar) {
                        if (canvas.objectIndex == null) {
                            canvas.createExample(canvas.width, canvas.height)
                            canvas.invalidate()
                        }
                    }

                    override fun onStopTrackingTouch(myseek: SeekBar) {
                        canvas.removeExample()
                    }
                }
        )
    }

    fun hideButtons() {
        strokeButton.visibility = View.GONE
        styleButton.visibility = View.GONE
        colorButton.visibility = View.GONE
        cropButton.visibility = View.GONE
        fontButton.visibility = View.GONE
    }

    fun showButtons() {
        hideButtons()
        val shapeTools = canvas.tool.shape
        val selectedShape = canvas.objectIndex?.let { canvas.draws[it] }
        val tool = selectedShape ?: shapeTools
        for (i in tool.shapeTools) {
            findViewById<ImageButton>(i.buttonId).visibility = View.VISIBLE
        }
    }

    // change opacity of undo and redo buttons
    fun doButtonsAlpha() {
        undoButton.alpha = if (canvas.draws.isEmpty()) 0.5F else 1F
        redoButton.alpha = if (canvas.undo.isEmpty()) 0.5F else 1F
    }

    // resolve the image
    private val startForProfileImageResult =
            registerForActivityResult(PickVisualMedia()) { result: Uri? -> setImageBack(result) }

    private fun setImageBack(result: Uri?) {
        if (result != null) {
            val fileStream: InputStream? = contentResolver.openInputStream(result)
            if (fileStream != null) {
                val oren = theOren(fileStream)
                canvas.setImageBackground(result, oren)
            }
        }
    }

    private val startForShapeImageResult =
            registerForActivityResult(PickVisualMedia()) { result: Uri? ->
                if (result != null) {
                    val bitmap = ImageUtils.decodeSampledBitmapFromUri(this, result, 1080, 1080)
                    if (bitmap != null) {
                        canvas.addImageShape(bitmap)
                    }
                }
            }

    private val startForFontResult =
            registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null && canvas.objectIndex != null) {
                    var extension = "ttf"
                    var isValid = false

                    // Validate file extension
                    if (uri.scheme == android.content.ContentResolver.SCHEME_CONTENT) {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val index =
                                        cursor.getColumnIndex(
                                                android.provider.OpenableColumns.DISPLAY_NAME
                                        )
                                if (index >= 0) {
                                    val name = cursor.getString(index)
                                    if (name.endsWith(".ttf", true)) {
                                        extension = "ttf"
                                        isValid = true
                                    } else if (name.endsWith(".otf", true)) {
                                        extension = "otf"
                                        isValid = true
                                    }
                                }
                            }
                        }
                    } else {
                        // try path
                        val path = uri.lastPathSegment
                        if (path != null) {
                            if (path.endsWith(".ttf", true)) {
                                extension = "ttf"
                                isValid = true
                            } else if (path.endsWith(".otf", true)) {
                                extension = "otf"
                                isValid = true
                            }
                        }
                    }

                    if (!isValid) {
                        android.widget.Toast.makeText(
                                        this,
                                        getString(R.string.invalid_font),
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                        return@registerForActivityResult
                    }

                    // Copy file to internal storage
                    val fontFile = java.io.File(filesDir, "fonts")
                    if (!fontFile.exists()) fontFile.mkdirs()
                    val fileName = "custom_font_${System.currentTimeMillis()}.$extension"
                    val destFile = java.io.File(fontFile, fileName)

                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val shape = canvas.draws[canvas.objectIndex!!]
                        if (shape is com.joory.whiteboardapp.shapes.Texts) {
                            shape.updateFont(destFile.absolutePath)
                            canvas.invalidate()
                        }

                        if (fontsDialog?.isShowing == true) {
                            val recycler =
                                    fontsDialog?.findViewById<
                                            androidx.recyclerview.widget.RecyclerView>(
                                            R.id.recyclerFonts
                                    )
                            val adapter =
                                    recycler?.adapter as?
                                            com.joory.whiteboardapp.adapters.FontsAdapter
                            val files = fontFile.listFiles()?.toList() ?: emptyList()
                            adapter?.updateData(files)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

    // check image rotation
    private fun theOren(file: InputStream): Int {
        val exif = ExifInterface(file)
        return exif.rotationDegrees
    }

    // pick color Dialog

    // set tool for drawing
    private fun choseTool(theShape: Shapes) {
        dialogs.dialog.findViewById<ImageView>(theShape.buttonId).setOnClickListener {
            canvas.objectIndex = null
            canvas.tool = theShape
            canvas.invalidate()
            showButtons()
            dialogs.dismiss()
        }
    }

    // set object color
    private fun objectColorSet(color: Int) {
        canvas.objectColorSet(color)
    }

    // background color
    private fun backgroundColorSet(color: Int) {
        canvas.setColorBackground(color)
    }

    private fun handleSendImage(intent: Intent) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
            setImageBack(it)
        }
    }

    private val cropImage =
            registerForActivityResult(CropImageContract()) { result ->
                if (result.isSuccessful) {
                    val uriContent = result.uriContent
                    if (uriContent != null) {
                        val bitmap =
                                ImageUtils.decodeSampledBitmapFromUri(this, uriContent, 1080, 1080)
                        if (bitmap != null && canvas.objectIndex != null) {
                            val shape = canvas.draws[canvas.objectIndex!!]
                            if (shape is ImageShape) {
                                shape.crop(bitmap)
                                canvas.invalidate()
                            }
                        }
                    }
                }
            }

    fun View.startCrop() {
        crop.startCrop(canvas, cacheDir, cropImage)
    }

    private var fontsDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    private fun showFontsDialog() {
        fontsDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        fontsDialog?.setContentView(R.layout.dialog_fonts)

        val btnClose = fontsDialog?.findViewById<View>(R.id.btnClose)
        val btnAdd = fontsDialog?.findViewById<View>(R.id.btnAddFont)
        val recycler =
                fontsDialog?.findViewById<androidx.recyclerview.widget.RecyclerView>(
                        R.id.recyclerFonts
                )
        val etPreview = fontsDialog?.findViewById<android.widget.EditText>(R.id.etPreview)

        val btnBold = fontsDialog?.findViewById<ImageButton>(R.id.btnBold)
        val btnItalic = fontsDialog?.findViewById<ImageButton>(R.id.btnItalic)
        val btnUnderline = fontsDialog?.findViewById<ImageButton>(R.id.btnUnderline)

        var isBold = false
        var isItalic = false
        var isUnderline = false
        var currentFontPath: String? = null

        if (canvas.objectIndex != null) {
            val shape = canvas.draws[canvas.objectIndex!!]
            if (shape is com.joory.whiteboardapp.shapes.Texts) {
                isBold = shape.isBold
                isItalic = shape.isItalic
                isUnderline = shape.isUnderline
                currentFontPath = shape.fontPath
                etPreview?.setText(shape.text.ifEmpty { "Sample Text" })
            }
        }

        @SuppressLint("WrongConstant")
        fun updatePreview() {
            try {
                val baseTypeface =
                        if (currentFontPath != null) {
                            android.graphics.Typeface.createFromFile(currentFontPath)
                        } else {
                            android.graphics.Typeface.DEFAULT
                        }

                var style = android.graphics.Typeface.NORMAL
                if (isBold) style = style or android.graphics.Typeface.BOLD
                if (isItalic) style = style or android.graphics.Typeface.ITALIC

                etPreview?.typeface = android.graphics.Typeface.create(baseTypeface, style)
                etPreview?.paint?.isUnderlineText = isUnderline
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateButtonState() {
            // using a grey background for selected state
            val activeColor = 0xFFCCCCCC.toInt()
            val inactiveColor = 0x00000000
            btnBold?.setBackgroundColor(if (isBold) activeColor else inactiveColor)
            btnItalic?.setBackgroundColor(if (isItalic) activeColor else inactiveColor)
            btnUnderline?.setBackgroundColor(if (isUnderline) activeColor else inactiveColor)
            updatePreview()
        }
        updateButtonState()

        fun updateShape() {
            if (canvas.objectIndex != null) {
                val shape = canvas.draws[canvas.objectIndex!!]
                if (shape is com.joory.whiteboardapp.shapes.Texts) {
                    shape.updateStyle(isBold, isItalic, isUnderline)
                    canvas.invalidate()
                }
            }
        }

        btnBold?.setOnClickListener {
            isBold = !isBold
            updateButtonState()
            updateShape()
        }
        btnItalic?.setOnClickListener {
            isItalic = !isItalic
            updateButtonState()
            updateShape()
        }
        btnUnderline?.setOnClickListener {
            isUnderline = !isUnderline
            updateButtonState()
            updateShape()
        }

        btnClose?.setOnClickListener { fontsDialog?.dismiss() }

        btnAdd?.setOnClickListener {
            // Check permissions if needed, but simple file picker usually handles basic read
            startForFontResult.launch("*/*")
        }

        val fontDir = java.io.File(filesDir, "fonts")
        if (!fontDir.exists()) fontDir.mkdirs()
        val files = fontDir.listFiles()?.toList() ?: emptyList()

        recycler?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter =
                com.joory.whiteboardapp.adapters.FontsAdapter(
                        files.toMutableList(),
                        onFontSelected = { file ->
                            if (canvas.objectIndex != null) {
                                val shape = canvas.draws[canvas.objectIndex!!]
                                if (shape is com.joory.whiteboardapp.shapes.Texts) {
                                    shape.updateFont(file.absolutePath)
                                    currentFontPath = file.absolutePath
                                    updatePreview()
                                    canvas.invalidate()
                                }
                            }
                        },
                        onDeleteClick = { file ->
                            androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.delete_font_title))
                                    .setMessage(getString(R.string.delete_font_message))
                                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                                        if (file.exists()) file.delete()
                                        val updatedFiles =
                                                fontDir.listFiles()?.toList() ?: emptyList()
                                        (recycler?.adapter as?
                                                        com.joory.whiteboardapp.adapters.FontsAdapter)
                                                ?.updateData(updatedFiles)
                                    }
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .show()
                        },
                        protectedFonts = defaultFonts
                )
        recycler?.adapter = adapter

        fontsDialog?.show()
    }

    fun View.onTextSizeClick() {
        showTextSizeDialog()
    }

    private var textSizeDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    private fun showTextSizeDialog() {
        textSizeDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        textSizeDialog?.setContentView(R.layout.dialog_text_size)

        val btnClose = textSizeDialog?.findViewById<View>(R.id.btnClose)
        val npSize = textSizeDialog?.findViewById<android.widget.NumberPicker>(R.id.npSize)

        btnClose?.setOnClickListener { textSizeDialog?.dismiss() }

        // Configure NumberPicker
        npSize?.minValue = 10
        npSize?.maxValue = 300
        npSize?.wrapSelectorWheel = false

        // Set current value
        if (canvas.objectIndex != null) {
            val shape = canvas.draws[canvas.objectIndex!!]
            if (shape is com.joory.whiteboardapp.shapes.Texts) {
                val currentSize = shape.paint.textSize.toInt()
                if (currentSize in 10..300) {
                    npSize?.value = currentSize
                } else {
                    npSize?.value = 50 // Default
                }
            }
        }

        npSize?.setOnValueChangedListener { _, _, newVal ->
            if (canvas.objectIndex != null) {
                val shape = canvas.draws[canvas.objectIndex!!]
                if (shape is com.joory.whiteboardapp.shapes.Texts) {
                    shape.paint.textSize = newVal.toFloat()
                    shape.updateObject(
                            shape.paint
                    ) // Ensure internal updates if needed, though direct setTextSize works
                    // We might need to call something to refresh bounds if text size changes?
                    // draw() uses getRectBorder() which uses paint.measureText(), so invalidate()
                    // is enough.
                    canvas.invalidate()
                }
            }
        }

        textSizeDialog?.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when {
            intent.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
        }
    }
}

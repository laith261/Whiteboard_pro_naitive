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
        lateinit var weakActivity: WeakReference<MainActivity>
        fun getInstanceActivity(): MainActivity? {
            return weakActivity.get()
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // functions
        initializeViews()
        permission.hasWriteStoragePermission()
        ads.loadFullScreenAd()
        showButtons()

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
        }
    }

    private fun initializeViews() {
        weakActivity = WeakReference<MainActivity>(this)
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
    fun View.onSaveClick() {
        saveImage.saveImage(canvas)
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

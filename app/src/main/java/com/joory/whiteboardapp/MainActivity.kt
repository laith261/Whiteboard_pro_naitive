package com.joory.whiteboardapp


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.exifinterface.media.ExifInterface
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.badge.BadgeDrawable
import com.joory.whiteboardapp.functions.Ads
import com.joory.whiteboardapp.functions.ColorPicker
import com.joory.whiteboardapp.functions.Dialogs
import com.joory.whiteboardapp.functions.Permission
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
    private lateinit var crop: ImageButton
    private lateinit var badgeDrawable: BadgeDrawable
    lateinit var ads: Ads
    lateinit var dialogs: Dialogs
    lateinit var permission: Permission
    lateinit var colorPicker: ColorPicker

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()

        // functions
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
        crop = findViewById(R.id.crop)
        tools = findViewById(R.id.tools)
        imageBg = findViewById(R.id.imgbg)
        badgeDrawable = BadgeDrawable.create(this)
        supportActionBar?.hide()
        ads = Ads(this, canvas)
        dialogs = Dialogs(this)
        permission = Permission(this)
        colorPicker = ColorPicker(supportFragmentManager)
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

    fun View.onClearClick() {
        canvas.clearCanvas()
    }

    fun View.onColorClick() {
        colorPicker.colorsDialog(::objectColorSet)
    }


    fun View.onSaveClick() {
        canvas.saveImage()
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
        val title = dialogs.dialog.findViewById<TextView>(R.id.title)
        title.text = this@MainActivity.resources.getText(R.string.stroke_width)
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
        crop.visibility = View.GONE
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
                canvas.setImageBackground(contentResolver.openInputStream(result)!!, oren)
            }
        }
    }

    private val startForShapeImageResult =
        registerForActivityResult(PickVisualMedia()) { result: Uri? ->
            if (result != null) {
                val fileStream = contentResolver.openInputStream(result)
                val bitmap = BitmapFactory.decodeStream(fileStream)
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
                    val fileStream: InputStream? = contentResolver.openInputStream(uriContent)
                    if (fileStream != null) {
                        val bitmap = BitmapFactory.decodeStream(fileStream)
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
        }

    fun View.startCrop() {
        if (canvas.objectIndex != null) {
            val shape = canvas.draws[canvas.objectIndex!!]
            if (shape is ImageShape) {
                val bitmap = shape.bitmap
                if (bitmap != null) {
                    try {
                        // Write to temp file
                        val file = java.io.File(this@MainActivity.cacheDir, "crop_temp.png")
                        val fOut = java.io.FileOutputStream(file)
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fOut)
                        fOut.flush()
                        fOut.close()

                        val options = CropImageOptions()
                        options.imageSourceIncludeGallery = true
                        options.imageSourceIncludeCamera = true

                        cropImage.launch(
                            CropImageContractOptions(
                                uri = Uri.fromFile(file),
                                cropImageOptions = options
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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

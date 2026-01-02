package com.joory.whiteboardapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.exifinterface.media.ExifInterface
import com.abhishek.colorpicker.ColorPickerDialog
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.joory.whiteboardapp.functions.Ads
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
    private lateinit var myDialog: Dialog
    private lateinit var colorButton: ImageButton
    private lateinit var textSizeButton: ImageButton
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
    lateinit var ads: Ads;

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // variables declare
        initializeViews()

        // functions
        hasWriteStoragePermission()
        ads.loadFullScreenAd()
        showButtons()
        setupClickListeners()
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
        myDialog = BottomSheetDialog(this)
        canvas = findViewById(R.id.canvas)
        colorButton = findViewById(R.id.color)
        styleButton = findViewById(R.id.style)
        textSizeButton = findViewById(R.id.textSize)
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
        canvas.dialog = myDialog
        badgeDrawable = BadgeDrawable.create(this)
        supportActionBar?.hide()
        ads = Ads(this, canvas)
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "InlinedApi")
    private fun setupClickListeners() {

        // crop
        crop.setOnClickListener { startCrop() }

        // tools
        tools.setOnClickListener {
            bottomSheet(R.layout.tools_dailog)
            choseTool(Shapes.Circle, R.id.circle)
            choseTool(Shapes.Select, R.id.select)
            choseTool(Shapes.Arrow, R.id.arrow)
            choseTool(Shapes.Brush, R.id.brush)
            choseTool(Shapes.Text, R.id.texts)
            choseTool(Shapes.Line, R.id.line)
            choseTool(Shapes.Rect, R.id.rect)
            choseTool(Shapes.Triangle, R.id.tringle)
            choseTool(Shapes.Star, R.id.star)
            choseTool(Shapes.Hexagon, R.id.hexagon)
            choseTool(Shapes.Eraser, R.id.eraser)

            // details for the chosen tool
            val currentToolDotId =
                when (canvas.tool) {
                    Shapes.Circle -> R.id.circle_dot
                    Shapes.Select -> R.id.select_dot
                    Shapes.Arrow -> R.id.arrow_dot
                    Shapes.Brush -> R.id.brush_dot
                    Shapes.Text -> R.id.texts_dot
                    Shapes.Line -> R.id.line_dot
                    Shapes.Rect -> R.id.rect_dot
                    Shapes.Triangle -> R.id.tringle_dot
                    Shapes.Star -> R.id.star_dot
                    Shapes.Hexagon -> R.id.hexagon_dot
                    Shapes.Eraser -> R.id.eraser_dot
                    else -> null
                }

            if (currentToolDotId != null) {
                myDialog.findViewById<View>(currentToolDotId)?.visibility = View.VISIBLE
            }

            myDialog.findViewById<ImageView>(R.id.add_image_shape).setOnClickListener {
                startForShapeImageResult.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(PickVisualMedia.ImageOnly)
                        .build()
                )
                myDialog.dismiss()
            }
        }

        // change style
        styleButton.setOnClickListener { canvas.changeStyle() }

        // clear canvas
        clear.setOnClickListener { canvas.clearCanvas() }

        // object color
        colorButton.setOnClickListener {
            colorsDialog(::objectColorSet)
        }

        // stroke width
        strokeButton.setOnClickListener {
            showStrokeDialog()
        }

        // save image
        save.setOnClickListener { canvas.saveImage() }

        // text size
        textSizeButton.setOnClickListener {
            sizeDialog()
        }

        // pick img
        imageBg.setOnClickListener {
            startForProfileImageResult.launch(
                PickVisualMediaRequest.Builder().setMediaType(PickVisualMedia.ImageOnly).build()
            )
        }

        // undo
        undoButton.setOnClickListener {
            canvas.undo()
            doButtonsAlpha()
        }

        // redo
        redoButton.setOnClickListener {
            canvas.redo()
            doButtonsAlpha()
        }

        // background color
        colorBg.setOnClickListener { colorsDialog(::backgroundColorSet) }
    }

    private fun showStrokeDialog() {
        bottomSheet(R.layout.size_dailog)
        val title = myDialog.findViewById<TextView>(R.id.title)
        title.text = resources.getText(R.string.stroke_width)
        val seek = myDialog.findViewById<SeekBar>(R.id.sizeSeek)
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
        textSizeButton.visibility = View.GONE
        styleButton.visibility = View.GONE
        colorButton.visibility = View.GONE
        crop.visibility = View.GONE
    }

    fun showButtons() {
        hideButtons()
        val shapeTools = canvas.tools[canvas.tool]
        val selectedShape = canvas.objectIndex?.let { canvas.draws[it] }
        val tool= selectedShape ?: shapeTools
        if (tool != null) {
            for (i in tool.shapeTools) {
                findViewById<ImageButton>(i.buttonId).visibility = View.VISIBLE
            }
        }
    }

    // change opacity of undo and redo buttons
    fun doButtonsAlpha() {
        undoButton.alpha = if (canvas.draws.isEmpty()) 0.5F else 1F
        redoButton.alpha = if (canvas.undo.isEmpty()) 0.5F else 1F
    }

    // show bottom sheet dialog
    private fun bottomSheet(layout: Int) {
        myDialog.setContentView(layoutInflater.inflate(layout, null))
        myDialog.show()
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
    private fun colorsDialog(func: (input: Int) -> Unit) {
        val dialog = ColorPickerDialog()
        dialog.setOnOkCancelListener { isOk, color ->
            if (isOk) {
                func(color)
            }
        }
        dialog.show(supportFragmentManager)
    }

    // set tool for drawing
    private fun choseTool(theShape: Shapes, id: Int) {
        myDialog.findViewById<ImageView>(id).setOnClickListener {
            canvas.objectIndex = null
            canvas.tool = theShape
            canvas.invalidate()
            showButtons()
            myDialog.dismiss()
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

    private fun hasWriteStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
            }
        }
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

    private fun startCrop() {
        if (canvas.objectIndex != null) {
            val shape = canvas.draws[canvas.objectIndex!!]
            if (shape is ImageShape) {
                val bitmap = shape.bitmap
                if (bitmap != null) {
                    try {
                        // Write to temp file
                        val file = java.io.File(cacheDir, "crop_temp.png")
                        val fOut = java.io.FileOutputStream(file)
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fOut)
                        fOut.flush()
                        fOut.close()

                        val options = CropImageOptions()
                        options.imageSourceIncludeGallery = true
                        options.imageSourceIncludeCamera = true

                        cropImage.launch(
                            com.canhub.cropper.CropImageContractOptions(
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun MainActivity.sizeDialog() {
        bottomSheet(R.layout.size_dailog)
        val title = myDialog.findViewById<TextView>(R.id.title)
        title.text = resources.getText(R.string.text_size)
        val seek = myDialog.findViewById<SeekBar>(R.id.sizeSeek)
        seek.max = 100
        seek.min = 25
        seek.progress = canvas.getCanvasPaint().textSize.toInt()
        seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    canvas.getCanvasPaint().textSize = progress.toFloat()
                    canvas.invalidate()
                }

                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(myseek: SeekBar) {}
            }
        )
    }
}

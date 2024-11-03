package com.joory.whiteboard_pro

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.joory.whiteboard_pro.shapes.Lines
import com.joory.whiteboard_pro.shapes.Shapes
import com.joory.whiteboard_pro.shapes.Texts
import java.io.InputStream
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var weakActivity: WeakReference<MainActivity>
        fun getmInstanceActivity(): MainActivity? {
            return weakActivity.get()
        }
    }

    lateinit var canvas: MyCanvas
    private lateinit var myDialog: Dialog
    private lateinit var colorButton: ImageButton
    private lateinit var styleButton: ImageButton
    private lateinit var colorBg: ImageButton
    private lateinit var scroll: HorizontalScrollView
    private lateinit var undoButton: ImageView
    private lateinit var redoButton: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var duplicateButton: ImageView
    private lateinit var sideLength: ImageButton
    private lateinit var badgeDrawable: BadgeDrawable
    private var mInterstitialAd: InterstitialAd? = null
    val mainHandler = android.os.Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        weakActivity = WeakReference<MainActivity>(this)
        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        myDialog = BottomSheetDialog(this)
        canvas = findViewById(R.id.canvas)
        colorButton = findViewById(R.id.color)
        styleButton = findViewById(R.id.style)
        sideLength = findViewById(R.id.sideLength)
        colorBg = findViewById(R.id.colorbg)
        scroll = findViewById(R.id.myscroll)
        undoButton = findViewById(R.id.undo)
        redoButton = findViewById(R.id.redo)
        canvas.dialog = myDialog
        badgeDrawable = BadgeDrawable.create(this)
        supportActionBar?.hide()
        deleteButton = findViewById(R.id.delete)
        duplicateButton = findViewById(R.id.duplicate)
        backgroundColor()
        showAdInterval()
        duplicateItem()
        fullScreenAd()
        hideButtons()
        toolsDialog()
        changeStyle()
        clearCanvas()
        objectColor()
        strokeWidth()
        sideLength()
        deleteItem()
        saveImage()
        textSize()
        pickImg()
        undo()
        redo()
    }

    private fun duplicateItem() {
        duplicateButton.setOnClickListener {
            canvas.duplicateItem()
        }
    }

    private fun deleteItem() {
        deleteButton.setOnClickListener {
            canvas.deleteItem()
            doButtonsAlpha()
            selectedItemButton()
        }
    }

    private fun saveImage() {
        findViewById<ImageButton>(R.id.save).setOnClickListener {
            canvas.saveImage()
        }
    }


    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun textSize() {
        findViewById<ImageButton>(R.id.textSize).setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = "Text Size"
            val seek = myDialog.findViewById<SeekBar>(R.id.sizeSeek)
            seek.max = 100
            seek.min = 25
            seek.progress = canvas.getCanvasPaint().textSize.toInt()
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                // Handle when the progress changes
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    canvas.getCanvasPaint().textSize = progress.toFloat()
                    canvas.invalidate()
                }

                // Handle when the user starts tracking touch
                override fun onStartTrackingTouch(seek: SeekBar) {

                }

                // Handle when the user stops tracking touch
                override fun onStopTrackingTouch(myseek: SeekBar) {

                }
            })
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    // change Sizes
    fun strokeWidth() {
        findViewById<ImageButton>(R.id.strokewidth).setOnClickListener {
            bottomSheet(R.layout.size_dailog)

            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = "Stroke Width"
            val seek = myDialog.findViewById<SeekBar>(R.id.sizeSeek)
            seek.progress = canvas.getCanvasPaint().strokeWidth.toInt()
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                // Handle when the progress changes
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    canvas.getCanvasPaint().strokeWidth = progress.toFloat()
                    if (canvas.objectIndex == null) {
                        canvas.updateExample()
                    }
                    canvas.invalidate()
                }

                // Handle when the user starts tracking touch
                override fun onStartTrackingTouch(seek: SeekBar) {
                    // Write custom code here if needed
                    if (canvas.objectIndex == null) {
                        canvas.createExample(canvas.width, canvas.height)
                        canvas.invalidate()
                    }
                }

                // Handle when the user stops tracking touch
                override fun onStopTrackingTouch(myseek: SeekBar) {
                    canvas.removeExample()
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun sideLength() {
        sideLength.setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = "Side Length"
            val seek = myDialog.findViewById<SeekBar>(R.id.sizeSeek)
            seek.min = 100
            seek.max = 300
            seek.progress =
                if (canvas.objectIndex != null) canvas.draws[canvas.objectIndex!!].sideLength.toInt() else canvas.sideLength.toInt()
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                // Handle when the progress changes
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    if (canvas.objectIndex != null) {
                        canvas.draws[canvas.objectIndex!!].updateSideLength(progress.toFloat())
                    } else {
                        canvas.sideLength = progress.toFloat()
                    }
                    canvas.invalidate()
                }

                // Handle when the user starts tracking touch
                override fun onStartTrackingTouch(seek: SeekBar) {

                }

                // Handle when the user stops tracking touch
                override fun onStopTrackingTouch(myseek: SeekBar) {

                }
            })
        }
    }

    // change style
    private fun changeStyle() {
        styleButton.setOnClickListener((View.OnClickListener {
            canvas.getCanvasPaint().style =
                if (canvas.getCanvasPaint().style == Paint.Style.STROKE) Paint.Style.FILL else Paint.Style.STROKE
            canvas.invalidate()
        }))
    }

    // clear canvas
    private fun clearCanvas() {
        findViewById<ImageButton>(R.id.clear).setOnClickListener((View.OnClickListener {
            canvas.undo.clear()
            canvas.undo.addAll(canvas.draws)
            canvas.draws.clear()
            canvas.objectIndex = null
            doButtonsAlpha()
            selectedItemButton()
            canvas.invalidate()
        }))
    }

    // set object colorBg
    private fun objectColor() {
        colorButton.setOnClickListener {
            colorsDialog(::objectColorSet)
        }
    }

    private fun objectColorSet(color: Int) {
        canvas.getCanvasPaint().color = color
        canvas.paint.color = color
        canvas.invalidate()
    }

    // set background colorBg
    private fun backgroundColor() {
        colorBg.setOnClickListener {
            colorsDialog(::backgroundColorSet)
        }
    }

    private fun backgroundColorSet(color: Int) {
        canvas.setColorBackground(color)
    }

    // set tool for drawing
    private fun choseTool(theShape: Shapes, id: Int) {
        myDialog.findViewById<ImageView>(id).setOnClickListener {
            canvas.tool = theShape
            canvas.objectIndex = null
            selectedItemButton()
            hideButtons()
            canvas.invalidate()
            myDialog.dismiss()
        }
    }

    // show tools myDialog
    private fun toolsDialog() {
        findViewById<ImageButton>(R.id.tools).setOnClickListener((View.OnClickListener {
            bottomSheet(R.layout.tools_dailog)
            choseTool(Shapes.Line, R.id.line)
            choseTool(Shapes.Circle, R.id.circle)
            choseTool(Shapes.Rect, R.id.rect)
            choseTool(Shapes.Arrow, R.id.arrow)
            choseTool(Shapes.Brush, R.id.brush)
            choseTool(Shapes.Select, R.id.select)
            choseTool(Shapes.Text, R.id.texts)
//            choseTool(Shapes.Triangle, R.id.tringle)
        }))
    }

    // undo and redo
    private fun undo() {
        undoButton.setOnClickListener {
            canvas.undo()
            doButtonsAlpha()
        }
    }

    private fun redo() {
        redoButton.setOnClickListener {
            canvas.redo()
            doButtonsAlpha()
        }
    }

    // a colorBg myDialog for all
    private fun colorsDialog(func: (input: Int) -> Unit) {
        val dialog = com.abhishek.colorpicker.ColorPickerDialog()
        dialog.setOnOkCancelListener { isOk, color ->
            if (isOk) {
                func(color)
            }
        }
        dialog.show(supportFragmentManager)
    }

    // pick image
    @SuppressLint("InlinedApi")
    private fun pickImg() {
        findViewById<ImageButton>(R.id.imgbg).setOnClickListener((View.OnClickListener {
            startForProfileImageResult.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(PickVisualMedia.ImageOnly)
                    .build()
            )
        }))
    }

    private fun normalizeImageForUri(file: InputStream): Int {
        val exif = ExifInterface(file)
        return exif.rotationDegrees
    }


    private val startForProfileImageResult =
        registerForActivityResult(PickVisualMedia()) { result: Uri? ->
            if (result != null) {
                val fileStream: InputStream? = contentResolver.openInputStream(result)
                if (fileStream != null) {
                    val oren = normalizeImageForUri(fileStream)
                    canvas.setImageBackground(contentResolver.openInputStream(result)!!, oren)
                } else {
                    Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }


    private fun bottomSheet(layout: Int) {
        myDialog.setContentView(layoutInflater.inflate(layout, null))
        myDialog.show()
    }

    fun hideButtons() {
        val textSizeButton = findViewById<ImageButton>(R.id.textSize)
        val strokeButton = findViewById<ImageButton>(R.id.strokewidth)
        sideLength.visibility =
            if (canvas.tool == Shapes.Arrow /*|| canvas.tool == Shapes.Triangle*/) View.VISIBLE else View.GONE
        styleButton.setImageResource(if (canvas.getCanvasPaint().style != Paint.Style.STROKE) R.drawable.shapes else R.drawable.shapes_white)
        strokeButton.visibility =
            if (canvas.tool != Shapes.Text && canvas.tool != Shapes.Select) View.VISIBLE else View.GONE
        textSizeButton.visibility =
            if (canvas.tool == Shapes.Text) View.VISIBLE else View.GONE
        styleButton.visibility =
            if (canvas.tool == Shapes.Brush || canvas.tool == Shapes.Text || canvas.tool == Shapes.Line || canvas.tool == Shapes.Select) View.GONE else View.VISIBLE
        if (canvas.tool == Shapes.Select && canvas.objectIndex != null) {
            strokeButton.visibility =
                if (canvas.draws[canvas.objectIndex!!]::class == Texts()::class) View.GONE else View.VISIBLE
            textSizeButton.visibility =
                if (canvas.draws[canvas.objectIndex!!]::class == Texts()::class) View.VISIBLE else View.GONE
            styleButton.visibility =
                if (canvas.draws[canvas.objectIndex!!]::class == Texts()::class || canvas.draws[canvas.objectIndex!!]::class == Lines()::class) View.GONE else View.VISIBLE
        }
    }

//    @OptIn(ExperimentalBadgeUtils::class)
//    fun indicator(new: Int) {
//        badgeDrawable.isVisible = true
//        badgeDrawable.backgroundColor = Color.RED
//        badgeDrawable.verticalOffset = 20
//        badgeDrawable.horizontalOffset = 10
//        BadgeUtils.attachBadgeDrawable(badgeDrawable, myDialog.findViewById(new))
//    }

    private fun fullScreenAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this, "ca-app-pub-1226999690478326/5310835378", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    fullScreenAd()
                }
            })
    }

    fun showAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }
        fullScreenAd()
    }

    private fun showAdInterval() {
        mainHandler.post(object : Runnable {
            override fun run() {
                showAds()
                mainHandler.postDelayed(this, 1000 * 60 * 3)
            }
        })
    }

    fun selectedItemButton() {
        deleteButton.visibility = if (canvas.objectIndex != null) View.VISIBLE else View.GONE
        duplicateButton.visibility = if (canvas.objectIndex != null) View.VISIBLE else View.GONE
    }

    fun doButtonsAlpha() {
        undoButton.alpha = if (canvas.draws.isEmpty()) 0.5F else 1F
        redoButton.alpha = if (canvas.undo.isEmpty()) 0.5F else 1F
    }
}
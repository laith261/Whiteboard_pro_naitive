package com.joory.whiteboard_pro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
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
    private var mainHandler = android.os.Handler(Looper.getMainLooper())
    private val showAdDelay = Runnable { showAds() }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
when{
intent?.action == Intent.ACTION_SEND -> {
            if (intent.type?.startsWith("image/") == true) {
                handleSendImage(intent) 
            }
        }
}
        // variables declare
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

        // functions
        hasWriteStoragePermission()
        loadFullScreenAd()
        backgroundColor()
        duplicateItem()
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


    fun hideButtons() {
        // declare buttons
        val textSizeButton = findViewById<ImageButton>(R.id.textSize)
        val strokeButton = findViewById<ImageButton>(R.id.strokewidth)

        // declare lists
        val arrowSizeList = arrayOf(Shapes.Arrow /*, canvas.tool == Shapes.Triangle*/)
        val strokeWidthList = arrayOf(Shapes.Text, Shapes.Select)
        val styleList = arrayOf(Shapes.Rect, Shapes.Circle, Shapes.Arrow)

        // set the state
        styleButton.setImageResource(if (canvas.getCanvasPaint().style != Paint.Style.STROKE) R.drawable.shapes else R.drawable.shapes_white)
        sideLength.visibility = if (canvas.tool in arrowSizeList) View.VISIBLE else View.GONE
        strokeButton.visibility = if (canvas.tool !in strokeWidthList) View.VISIBLE else View.GONE
        textSizeButton.visibility = if (canvas.tool == Shapes.Text) View.VISIBLE else View.GONE
        styleButton.visibility = if (canvas.tool in styleList) View.VISIBLE else View.GONE

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


    // finished functions 

    // duplicate object
    private fun duplicateItem() {
        duplicateButton.setOnClickListener {
            canvas.duplicateItem()
        }
    }

    //delete object
    private fun deleteItem() {
        deleteButton.setOnClickListener {
            canvas.deleteItem()
            doButtonsAlpha()
            selectedItemButton()
        }
    }

    // save image
    private fun saveImage() {
        findViewById<ImageButton>(R.id.save).setOnClickListener {
            canvas.saveImage()
        }
    }

    // show action button for selected object
    fun selectedItemButton() {
        deleteButton.visibility = if (canvas.objectIndex != null) View.VISIBLE else View.GONE
        duplicateButton.visibility = if (canvas.objectIndex != null) View.VISIBLE else View.GONE
    }

    // change opacity of undo and redo buttons
    fun doButtonsAlpha() {
        undoButton.alpha = if (canvas.draws.isEmpty()) 0.5F else 1F
        redoButton.alpha = if (canvas.undo.isEmpty()) 0.5F else 1F
    }

    // full screen ads load
    private fun loadFullScreenAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this, "ca-app-pub-1226999690478326/5310835378", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    resetAdInterval()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    loadFullScreenAd()
                }
            })
    }

    // full screen ads show
    fun showAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {}

                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadFullScreenAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    loadFullScreenAd()
                }

                override fun onAdImpression() {}

                override fun onAdShowedFullScreenContent() {}
            }
            mInterstitialAd?.show(this)
        }
        loadFullScreenAd()
    }

    // set full screen ads time out call
    private fun showAdInterval() {
        mainHandler.postDelayed(showAdDelay, 1000 * 60 * 3)
    }

    // reset full screen ads time out call
    private fun resetAdInterval() {
        mainHandler.removeCallbacks(showAdDelay)
        showAdInterval()
    }

    // show bottom sheet dialog
    private fun bottomSheet(layout: Int) {
        myDialog.setContentView(layoutInflater.inflate(layout, null))
        myDialog.show()
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

    // resolve the image
    private val startForProfileImageResult =
        registerForActivityResult(PickVisualMedia()) { result: Uri? ->
            if (result != null) {
                val fileStream: InputStream? = contentResolver.openInputStream(result)
                if (fileStream != null) {
                    val oren = theOren(fileStream)
                    canvas.setImageBackground(contentResolver.openInputStream(result)!!, oren)
                }
            }
        }

    // check image rotation
    private fun theOren(file: InputStream): Int {
        val exif = ExifInterface(file)
        return exif.rotationDegrees
    }

    // undo action
    private fun undo() {
        undoButton.setOnClickListener {
            canvas.undo()
            doButtonsAlpha()
        }
    }

    // undo action
    private fun redo() {
        redoButton.setOnClickListener {
            canvas.redo()
            doButtonsAlpha()
        }
    }

    // pick color Dialog 
    private fun colorsDialog(func: (input: Int) -> Unit) {
        val dialog = com.abhishek.colorpicker.ColorPickerDialog()
        dialog.setOnOkCancelListener { isOk, color ->
            if (isOk) {
                func(color)
            }
        }
        dialog.show(supportFragmentManager)
    }

    // show tools dialog
    private fun toolsDialog() {
        findViewById<ImageButton>(R.id.tools).setOnClickListener((View.OnClickListener {
            bottomSheet(R.layout.tools_dailog)
            //
            choseTool(Shapes.Circle, R.id.circle)
            choseTool(Shapes.Select, R.id.select)
            choseTool(Shapes.Arrow, R.id.arrow)
            choseTool(Shapes.Brush, R.id.brush)
            choseTool(Shapes.Text, R.id.texts)
            choseTool(Shapes.Line, R.id.line)
            choseTool(Shapes.Rect, R.id.rect)
//            choseTool(Shapes.Triangle, R.id.tringle)
        }))
    }

    // set tool for drawing
    private fun choseTool(theShape: Shapes, id: Int) {
        myDialog.findViewById<ImageView>(id).setOnClickListener {
            canvas.objectIndex = null
            canvas.tool = theShape
            selectedItemButton()
            canvas.invalidate()
            hideButtons()
            //
            myDialog.dismiss()
        }
    }

    // change paint
    private fun changeStyle() {
        styleButton.setOnClickListener((View.OnClickListener {
            canvas.changeStyle()
        }))
    }

    // clear canvas
    private fun clearCanvas() {
        findViewById<ImageButton>(R.id.clear).setOnClickListener((View.OnClickListener {
            canvas.clearCanvas()
        }))
    }

    // set object color
    private fun objectColor() {
        colorButton.setOnClickListener {
            colorsDialog(::objectColorSet)
        }
    }

    // set object color
    private fun objectColorSet(color: Int) {
        canvas.objectColorSet(color)
    }

    // pick background color
    private fun backgroundColor() {
        colorBg.setOnClickListener {
            colorsDialog(::backgroundColorSet)
        }
    }

    // background color
    private fun backgroundColorSet(color: Int) {
        canvas.setColorBackground(color)
    }

    // arrow size seek dialog
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun sideLength() {
        sideLength.setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = resources.getString(R.string.arrow_side)
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

    // change border width
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    fun strokeWidth() {
        findViewById<ImageButton>(R.id.strokewidth).setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = resources.getText(R.string.stroke_width)
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

    // set text size
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun textSize() {
        findViewById<ImageButton>(R.id.textSize).setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = myDialog.findViewById<TextView>(R.id.title)
            title.text = resources.getText(R.string.text_size)
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
    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
        // Update UI to reflect image being shared
    }
}
}
package com.joory.whiteboard_pro

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.joory.whiteboard_pro.shapes.Shapes
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    lateinit var canvas: MyCanvas
    private lateinit var dialog: Dialog
    private lateinit var colorButton: ImageButton
    private lateinit var styleButton: ImageButton
    private lateinit var colorBg: ImageButton
    private lateinit var scroll: HorizontalScrollView
    private lateinit var deleteButton: ImageView
    private lateinit var badgeDrawable: BadgeDrawable


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        dialog = BottomSheetDialog(this)
        canvas = findViewById(R.id.canvas)
        colorButton = findViewById(R.id.color)
        styleButton = findViewById(R.id.style)
        colorBg = findViewById(R.id.colorbg)
        scroll = findViewById(R.id.myscroll)
        canvas.dialog = dialog
        badgeDrawable = BadgeDrawable.create(this)
        supportActionBar?.hide()
        deleteButton = findViewById(R.id.delete)
        hideButtons()
        backgroundColor()
        toolsDialog()
        changeStyle()
        clearCanvas()
        objectColor()
        strokeWidth()
        deleteItem()
        saveImage()
        textSize()
        pickImg()
        undo()
        redo()
    }

    private fun deleteItem() {
        deleteButton.setOnClickListener{
            canvas.deleteItem()
        }
    }

    private fun saveImage() {
        findViewById<ImageButton>(R.id.save).setOnClickListener {
            canvas.saveImage()
        }
    }

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun textSize() {
        findViewById<ImageButton>(R.id.textSize).setOnClickListener {
            bottomSheet(R.layout.size_dailog)
            val title = dialog.findViewById<TextView>(R.id.title)
            title.text = "Text Size"
            val seek = dialog.findViewById<SeekBar>(R.id.sizeSeek)
            seek.max = 75
            seek.min = 15
            seek.progress = getPaint().textSize.toInt()
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                // Handle when the progress changes
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    getPaint().textSize = progress.toFloat()
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


    @kotlin.OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    // change Sizes
    fun strokeWidth() {
        findViewById<ImageButton>(R.id.strokewidth).setOnClickListener {
            bottomSheet(R.layout.size_dailog)

            val title = dialog.findViewById<TextView>(R.id.title)
            title.text = "Stroke Width"
            val seek = dialog.findViewById<SeekBar>(R.id.sizeSeek)
            seek.progress = getPaint().strokeWidth.toInt()
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                // Handle when the progress changes
                override fun onProgressChanged(
                    seek: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    getPaint().strokeWidth = progress.toFloat()
                    if(canvas.objectIndex==null){
                        canvas.updateExample()
                    }
                    canvas.invalidate()
                }

                // Handle when the user starts tracking touch
                override fun onStartTrackingTouch(seek: SeekBar) {
                    // Write custom code here if needed
                    if(canvas.objectIndex==null){
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

    // change style
    private fun changeStyle() {
        styleButton.setOnClickListener((View.OnClickListener {
            getPaint().style =
                if (getPaint().style == Paint.Style.STROKE) Paint.Style.FILL else Paint.Style.STROKE
            styleButton.setImageResource(if (getStyle().style != Paint.Style.STROKE) R.drawable.shapes else R.drawable.shapes_white)
        }))
    }

    // clear canvas
    private fun clearCanvas() {
        findViewById<ImageButton>(R.id.clear).setOnClickListener((View.OnClickListener {
            canvas.draws.clear()
            canvas.objectIndex = null
            canvas.invalidate()
        }))
    }

    // set object colorBg
    private fun objectColor() {
        colorButton.setOnClickListener {
            colorsDialog(::objectColorSet)
        }
    }

    private fun objectColorSet(envelope: ColorEnvelope) {
        canvas.paint.color = envelope.color
    }

    // set background colorBg
    private fun backgroundColor() {
        colorBg.setOnClickListener {
            colorsDialog(::backgroundColorSet)
        }
    }

    private fun backgroundColorSet(envelope: ColorEnvelope) {
        canvas.setColorBackground(envelope.color)
    }

    // set tool for drawing
    private fun choseTool(theShape: Shapes, id: Int) {
        dialog.findViewById<ImageView>(id).setOnClickListener {
            canvas.tool = theShape
            canvas.objectIndex = null
            hideButtons()
            canvas.invalidate()
            deleteButton.visibility = if (theShape == Shapes.Select) View.VISIBLE else View.GONE
            dialog.hide()
        }
    }

    // show tools dialog
    @kotlin.OptIn(DelicateCoroutinesApi::class)
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
            choseTool(Shapes.Triangle, R.id.tringle)


        }))
    }

    // undo and redo
    private fun undo() {
        findViewById<ImageView>(R.id.undo).setOnClickListener {
            canvas.undo()
        }
    }

    private fun redo() {
        findViewById<ImageView>(R.id.redo).setOnClickListener {
            canvas.redo()
        }
    }

    // a colorBg dialog for all
    private fun colorsDialog(func: (input: ColorEnvelope) -> Unit) {
        ColorPickerDialog.Builder(this)
            .setTitle("Pick a Color")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton("select",
                ColorEnvelopeListener { envelope, _ -> func(envelope) })
            .setNegativeButton(
                "cancel"
            ) { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(true) // the default value is true.
            .attachBrightnessSlideBar(true) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last sidebar and buttons.
            .show()
    }

    // pick image
    private fun pickImg() {
        findViewById<ImageButton>(R.id.imgbg).setOnClickListener((View.OnClickListener {
            ImagePicker.with(this)
                .galleryOnly()    //User can only select image from Gallery
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }))
    }

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data!!
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val imgs: InputStream? = contentResolver.openInputStream(data.data!!)
                    canvas.setImageBackground(imgs!!)
                }

                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun bottomSheet(layout: Int) {
        dialog.setContentView(layoutInflater.inflate(layout, null))
        dialog.show()
    }

    private fun hideButtons() {
        val textSizeButton = findViewById<ImageButton>(R.id.textSize)
        val styleButton = findViewById<ImageButton>(R.id.style)
        textSizeButton.visibility = if (canvas.tool == Shapes.Text) View.VISIBLE else View.GONE
        styleButton.visibility = if (canvas.tool == Shapes.Brush) View.GONE else View.VISIBLE
    }

    @OptIn(ExperimentalBadgeUtils::class)
    fun indicator(new: Int) {
        badgeDrawable.isVisible = true
        badgeDrawable.backgroundColor = Color.RED
        badgeDrawable.verticalOffset = 20
        badgeDrawable.horizontalOffset = 10
        BadgeUtils.attachBadgeDrawable(badgeDrawable, dialog.findViewById(new))
    }
}
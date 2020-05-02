package com.rycbar.doodle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.WorkerThread
import androidx.appcompat.widget.AppCompatImageView
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate", "unused")
class DoodleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener {

    var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val typedArray =
        attrs?.let { context.obtainStyledAttributes(it, R.styleable.DoodleView, 0, 0) }
    private var paint: Paint
    var inkColour by Delegates.observable(
        typedArray?.getColor(
            R.styleable.DoodleView_inkColour,
            Color.BLACK
        ) ?: Color.BLACK
    ) { _, _, _ -> paint = refreshPaint() }
    var strokeSize by Delegates.observable(
        typedArray?.getDimension(
            R.styleable.DoodleView_strokeSize,
            1f
        ) ?: 1f
    ) { _, _, _ -> paint = refreshPaint() }

    private var lastFinger: PointF? = null

    init {
        setOnTouchListener(this)
        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val oldBitmap = bitmap
            val width = abs(right - left) + 1
            val height = abs(bottom - top) + 1
            val bmp = if (oldBitmap == null) {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } else Bitmap.createScaledBitmap(oldBitmap, width, height, true)
            bitmap = bmp
            canvas = Canvas(bmp)

            setImageBitmap(bmp)
        }
        paint = refreshPaint()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        fun eventToCanvas(event: MotionEvent?) =
            event?.run { PointF(x.coerceIn(0f, width.toFloat()), y.coerceIn(0f, height.toFloat())) }

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> if (event.pointerCount == 1) lastFinger =
                eventToCanvas(event)
            MotionEvent.ACTION_UP -> lastFinger = null
            MotionEvent.ACTION_MOVE -> lastFinger?.let { from ->
                val newPoint = eventToCanvas(event) ?: return@let
                Log.v("Paint", "$from -> $newPoint")
                canvas?.drawLine(from.x, from.y, newPoint.x, newPoint.y, paint)
                postInvalidate(
                    min(from.x, newPoint.x).toInt(),
                    min(from.y, newPoint.y).toInt(),
                    max(from.x, newPoint.x).toInt(),
                    max(from.y, newPoint.y).toInt()
                )
                lastFinger = newPoint
            }
        }

        return true
    }

    @WorkerThread
    fun capturePng(): ByteArray {
        val bitmap = bitmap ?: return byteArrayOf()
        val outStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        return outStream.toByteArray()
    }

    @WorkerThread
    fun captureJpeg(quality: Int, fillColour: Int = Color.WHITE): ByteArray {
        val bitmap = bitmap ?: return byteArrayOf()
        val outStream = ByteArrayOutputStream()
        val bmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val backgroundCanvas = Canvas(bmp).also { it.drawColor(fillColour) }
        backgroundCanvas.drawBitmap(bitmap, 0f, 0f, null)
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
        return outStream.toByteArray()
    }

    fun clear() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        postInvalidate()
    }

    private fun refreshPaint(): Paint = Paint().apply {
        color = inkColour
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeSize
    }
}
package com.rycbar.doodle

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate", "unused")
class DoodleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr), View.OnTouchListener {

    private val typedArray = attrs?.let { context.obtainStyledAttributes(it, R.styleable.DoodleView, 0, 0) }
    private var paint: Paint
    var inkColour by Delegates.observable(typedArray?.getColor(R.styleable.DoodleView_inkColour, Color.BLACK) ?: Color.BLACK) { _, _, _ -> paint = refreshPaint() }
    var strokeSize by Delegates.observable(typedArray?.getDimension(R.styleable.DoodleView_strokeSize, 1f) ?: 1f) { _, _, _ -> paint = refreshPaint() }

    private var lastFinger: PointF? = null
    private val actionsQueue = ArrayList<(canvas: Canvas) -> Unit>()

    init {
        setOnTouchListener(this)
        paint = refreshPaint()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?:return
        actionsQueue.forEach { it.invoke(canvas) }
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
                actionsQueue.add{it.drawLine(from.x, from.y, newPoint.x, newPoint.y, paint)}
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

    @MainThread
    suspend fun capturePng(): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return withContext(Dispatchers.Default) {
            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.toByteArray()
        }
    }

    @MainThread
    suspend fun captureJpeg(quality: Int, fillColour: Int = Color.WHITE): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return withContext(Dispatchers.Default){
            val outStream = ByteArrayOutputStream()
            val bmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val backgroundCanvas = Canvas(bmp).also { it.drawColor(fillColour) }
            backgroundCanvas.drawBitmap(bitmap, 0f, 0f, null)
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            outStream.toByteArray()
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        clear()
        background = BitmapDrawable(resources, bitmap)
    }

    fun clear() {
        background = null
        actionsQueue.add { actionsQueue.clear(); postInvalidate() }
        postInvalidate()
    }

    private fun refreshPaint(): Paint = Paint().apply {
        color = inkColour
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeSize
    }
}
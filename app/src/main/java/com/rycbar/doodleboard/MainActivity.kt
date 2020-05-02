package com.rycbar.doodleboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        harvestButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val byteArray = doodle.capturePng()
                withContext(Dispatchers.Default) { File(this@MainActivity.filesDir, "Sketch.png").writeBytes(byteArray) }

                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                showBitmap(bitmap)
            }
        }
        cleanButton.setOnClickListener { doodle.clear() }
        reloadButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val byteArray = withContext(Dispatchers.Default) { File(this@MainActivity.filesDir, "Sketch.png").readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                doodle.setBitmap(bitmap)
            }
        }
    }

    private suspend fun showBitmap(bitmap: Bitmap?) {
        postView.setImageBitmap(bitmap)
        postView.visibility = View.VISIBLE
        delay(3000)
        postView.visibility = View.GONE
    }
}

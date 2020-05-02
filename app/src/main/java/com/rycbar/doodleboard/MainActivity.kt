package com.rycbar.doodleboard

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        harvestButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val byteArray = withContext(Dispatchers.Default) { doodle.capturePng() }
                val bitmap = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(
                        byteArray,
                        0,
                        byteArray.size
                    )
                }
                postView.setImageBitmap(bitmap)
                postView.visibility = View.VISIBLE
                delay(3000)
                postView.visibility = View.GONE
            }
        }
        cleanButton.setOnClickListener { doodle.clear() }
    }
}

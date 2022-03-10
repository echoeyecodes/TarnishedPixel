package com.echoeyecodes.testproject.activities

import android.graphics.*
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.values
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.echoeyecodes.testproject.R
import com.echoeyecodes.testproject.databinding.ActivityImageBinding
import com.echoeyecodes.testproject.utils.AndroidUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private val binding by lazy { ActivityImageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        imageView = binding.image

        lifecycleScope.launch {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image)
            meanFilter(bitmap)
        }

    }

    private suspend fun meanFilter(bitmap: Bitmap) {
        val kernel =
            floatArrayOf(1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f)
        withContext(Dispatchers.IO) {
            val diameter = 3 // 3x3 matrix
            val cols = bitmap.width
            val rows = bitmap.height
            val step = (diameter - 1) / 2

            val imagePixels = IntArray(rows * cols)
            val newImagePixels = IntArray(rows * cols)

            bitmap.getPixels(imagePixels, 0, cols, 0, 0, cols, rows)
            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    val startCoord = Point(i - step, j - step)
                    val endCoord = Point(i + step, j + step)

                    val matrix = ArrayList<Int>()
                    for (_col in startCoord.x..endCoord.x) {
                        for (_row in startCoord.y..endCoord.y) {
                            if (_row < 0 || _row >= rows || _col < 0 || _col >= cols) {
                                //out of matrix bounds
                                matrix.add(0)
                            } else {
                                val pixel = imagePixels[_row * cols + _col]
                                matrix.add(pixel)
                            }
                        }
                    }
                    val sum = matrix.mapIndexed { index, value -> kernel[index] * value }.sum()
                    val divisor = kernel.sum()
                    val average = (sum / divisor).toInt()
                    newImagePixels[j * cols + i] = average
                }
            }
            val copyBitmap = Bitmap.createBitmap(newImagePixels, cols, rows, Bitmap.Config.ARGB_8888)
            runOnUiThread {
                Glide.with(this@ImageActivity).load(copyBitmap).into(imageView)
            }
        }
    }
}
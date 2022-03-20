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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
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
//            meanFilter(bitmap)
            if(OpenCVLoader.initDebug()){
                blurImage(bitmap)
            }
        }

    }

    private suspend fun blurImage(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val cols = bitmap.width
            val rows = bitmap.height

            val imageSrc = Mat()

            Utils.bitmapToMat(bitmap, imageSrc)
            val blurredImage = imageSrc.clone()

            val size = 32.0

            val rowStart = 0
            val rowEnd = (0.5*rows).toInt()
            val colStart = 0
            val colEnd = (0.5*cols).toInt()

            /**
             * Then blurred will have the blurred region.
             * This is because submat doesn't copy the data in blurred, but rather references it.
             * So when the blur is applied it only blurs the parts in blurred referenced by mask.
             * https://stackoverflow.com/a/26823577
             */
            val blurredPartition = blurredImage.submat(rowStart, rowEnd, colStart, colEnd)

            val circleMask = Mat()
            Imgproc.circle(circleMask, org.opencv.core.Point(rowEnd/2.0, rowEnd/2.0), rowEnd/2, Scalar(1.0))
            Imgproc.blur(blurredPartition, blurredPartition, Size(size, size))
            blurredPartition.copyTo(blurredImage, circleMask)

            val copy = Bitmap.createBitmap(blurredImage.width(), blurredImage.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(blurredImage, copy)
            runOnUiThread {
                Glide.with(this@ImageActivity).load(copy).into(imageView)
            }
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
                    val startCoord = Point(j - step, i - step)
                    val endCoord = Point(j + step, i + step)

                    val matrix = ArrayList<Int>()
                    for (_col in startCoord.y..endCoord.y) {
                        for (_row in startCoord.x..endCoord.x) {
                            if (_row < 0 || _row >= rows || _col < 0 || _col >= cols) {
                                //out of matrix bounds
                                matrix.add(0)
                            } else {
                                val pixel = imagePixels[_row * cols + _col]
                                matrix.add(pixel)
                            }
                        }
                    }

                    val currentPixel = imagePixels[j * cols + i]
                    var alpha = currentPixel shr 24 and 0xFF
                    var red = currentPixel ushr 24 and 0xFF
                    var blue = currentPixel ushr 8 and 0xFF
                    var green = currentPixel and 0xFF

                    matrix.forEachIndexed { index, value ->
                        val multiplier = 1 / 9f

                        alpha += value shr 24 and 0xFF
                        red += ((value ushr 16 and 0xFF) * multiplier).toInt()
                        green += ((value ushr 8 and 0xFF) * multiplier).toInt()
                        blue += ((value and 0xFF) * multiplier).toInt()

                    }
                    val sum = ((alpha) shl 24) or ((red) shl 16) or ((green) shl 8) or (blue)
                    newImagePixels[j * cols + i] = sum
                }
            }
            val copyBitmap =
                Bitmap.createBitmap(newImagePixels, cols, rows, Bitmap.Config.ARGB_8888)
            runOnUiThread {
                Glide.with(this@ImageActivity).load(copyBitmap).into(imageView)
            }
        }
    }
}
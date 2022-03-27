package com.echoeyecodes.testproject.activities

import android.graphics.*
import android.graphics.Point
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.echoeyecodes.testproject.R
import com.echoeyecodes.testproject.databinding.ActivityImageBinding
import com.echoeyecodes.testproject.utils.AndroidUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlinx.multik.api.d2arrayIndices
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList

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
            if (OpenCVLoader.initDebug()) {
                smoothEdgeBlur(bitmap)
            }
        }

    }

    private fun alphaBlend(imageSrc:Mat, blurredImage: Mat, blurredMask: Mat): Mat{
        val destination = Mat(imageSrc.size(), imageSrc.type())

//        val scalarDivide = Mat(blurredMask.size(), blurredMask.type())
//        Core.divide(blurredMask, Scalar.all(255.0), scalarDivide)
//
//        val scalarAdd = Mat(blurredImage.size(), blurredImage.type())
//        Core.add(blurredMask, Scalar.all(1.0), scalarAdd)
//
//        val first = scalarDivide.mul(blurredImage)
//        val second = scalarAdd.mul(imageSrc)
//
//        Core.add(first, second, destination)
//
//        return destination

        for (col in 0 until destination.cols()){
            for (row in 0 until destination.rows()){
                val pixel = imageSrc.get(row, col)
                val blurredPixel = blurredImage.get(row, col)
                val blurVal = blurredMask.get(row, col)[0] / 255

                val newPixelValue = pixel.mapIndexed { index, value ->
                    (blurVal * blurredPixel[index]) + ((1.0f - blurVal) * value)
                }.toDoubleArray()

                destination.put(row, col, *newPixelValue)
            }
        }
        return destination
    }

    private suspend fun smoothEdgeBlur(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val imageSrc = Mat()

            Utils.bitmapToMat(bitmap, imageSrc)

            val innerMask = Mat.zeros(imageSrc.size(), CV_8UC1)
            //thickness set to -1 for inner fill
            Imgproc.circle(
                innerMask,
                org.opencv.core.Point(imageSrc.width() / 2.0, imageSrc.height() / 1.5),
                600,
                Scalar.all(255.0),
                -1
            )

            val blurredImage = Mat(imageSrc.size(), imageSrc.type())
            Imgproc.blur(imageSrc, blurredImage, Size(64.0, 64.0))
//            Imgproc.GaussianBlur(imageSrc, blurredImage, Size(11.0, 11.0), 0.0)

            val blurredMask = Mat(innerMask.size(), innerMask.type())
            Imgproc.blur(innerMask, blurredMask, Size(64.0, 64.0))

            val merge = alphaBlend(imageSrc, blurredImage, blurredMask)
//            Core.addWeighted(originalCut, 1.0, blurredCut, 1.0, 0.0, merge)

            val copy = Bitmap.createBitmap(merge.width(), merge.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(merge, copy)
            runOnUiThread { Glide.with(this@ImageActivity).load(copy).into(imageView) }
        }
    }

    private suspend fun edgeBlur(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val imageSrc = Mat()

            Utils.bitmapToMat(bitmap, imageSrc)

            val innerMask = Mat.zeros(imageSrc.size(), imageSrc.type()).apply {
                setTo(Scalar.all(0.0))
            }
            //thickness set to -1 for inner fill
            Imgproc.circle(
                innerMask,
                org.opencv.core.Point(imageSrc.width() / 2.0, imageSrc.height() / 1.5),
                600,
                Scalar.all(255.0),
                -1
            )

            val outerMask = Mat()
            Core.bitwise_not(innerMask, outerMask)

            val blurredImage = Mat()
            Imgproc.blur(imageSrc, blurredImage, Size(64.0, 64.0))
//            Imgproc.GaussianBlur(imageSrc, blurredImage, Size(11.0, 11.0), 0.0)

            //original image minus cutout
            val originalCut = Mat()
            Core.bitwise_and(imageSrc, outerMask, originalCut)

            //blurred image minus cutout
            val blurredCut = Mat()
            Core.bitwise_and(blurredImage, innerMask, blurredCut)

//            val kernel = Mat.ones(Size(10.0,10.0), Imgproc.MORPH_CROSS)
//            Imgproc.dilate(blurredCut, blurredCut, kernel)

            val merge = Mat()
            Core.bitwise_or(originalCut, blurredCut, merge)
//            Core.addWeighted(originalCut, 1.0, blurredCut, 1.0, 0.0, merge)

            val copy = Bitmap.createBitmap(merge.width(), merge.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(merge, copy)
            runOnUiThread { Glide.with(this@ImageActivity).load(copy).into(imageView) }
        }
    }

    private suspend fun blurImage(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val cols = bitmap.width
            val rows = bitmap.height

            val imageSrc = Mat()

            Utils.bitmapToMat(bitmap, imageSrc)
            val blurredImage = imageSrc.clone()

            val size = 33.0

            val rowStart = 0
            val rowEnd = (0.5 * rows).toInt()
            val colStart = 0
            val colEnd = (0.5 * cols).toInt()

            /**
             * Then blurred will have the blurred region.
             * This is because submat doesn't copy the data in blurred, but rather references it.
             * So when the blur is applied it only blurs the parts in blurred referenced by mask.
             * https://stackoverflow.com/a/26823577
             */
            val blurredPartition = blurredImage.submat(rowStart, rowEnd, colStart, colEnd)
            Imgproc.GaussianBlur(blurredPartition, blurredPartition, Size(size, size), 0.0, 0.0)

            val copy = Bitmap.createBitmap(
                blurredImage.width(),
                blurredImage.height(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(blurredImage, copy)
            runOnUiThread {
                Glide.with(this@ImageActivity).load(copy).into(imageView)
            }
        }
    }
}
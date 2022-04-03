package com.echoeyecodes.testproject.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


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

        val total = imageSrc.width() * imageSrc.height()
        val channels = imageSrc.channels()
        val size = total * channels

        val array = FloatArray(size)
        val array1 = FloatArray(size)
        val array2 = FloatArray(size)

        val array3 = FloatArray(size)
        imageSrc.convertTo(imageSrc, CV_32F)
        blurredImage.convertTo(blurredImage, CV_32F)
        blurredMask.convertTo(blurredMask, CV_32F)

        val destination = Mat(imageSrc.size(), imageSrc.type())

        imageSrc.get(0,0, array)
        blurredImage.get(0,0, array1)
        blurredMask.get(0,0, array2)

        for (index in 0 until size){
            val pixel = array[index]
            val blurredPixel = array1[index]
            val blurVal = (array2[index]) / 255.0f

            val newValue = ((blurVal * blurredPixel) + ((1.0f - blurVal) * pixel))
            array3[index] = newValue
        }
        destination.put(0,0, array3)
        destination.convertTo(destination, CV_8UC3)
        return destination
    }

    private suspend fun smoothEdgeBlur(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val imageSrc = Mat()

            Utils.bitmapToMat(bitmap, imageSrc)

            val innerMask = Mat.zeros(imageSrc.size(), imageSrc.type())
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
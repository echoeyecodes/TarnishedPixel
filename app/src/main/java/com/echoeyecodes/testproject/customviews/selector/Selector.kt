package com.echoeyecodes.testproject.customviews.selector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.echoeyecodes.testproject.R
import com.echoeyecodes.testproject.utils.getScreenSize
import java.lang.Math.abs
import kotlin.math.max
import kotlin.math.min


class Selector(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    enum class TouchPoint {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    private var startX = 0f
    private var startY = 0f

    private var _width = 0
    private var _height = 0

    private var screenSize = getScreenSize()
    private var screenWidth = screenSize.first
    private var screenHeight = screenSize.second

    private var touchPoint: TouchPoint? = null

    companion object {
        const val SIZE = 100f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val paint = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.white, null)
        }
        canvas.drawRect(RectF(0f, 0f, SIZE, SIZE), paint)
        canvas.drawRect(
            RectF((width - SIZE), 0f, (width.toFloat()), SIZE),
            paint
        )
        canvas.drawRect(RectF(0f, (height - SIZE), SIZE, height.toFloat()), paint)
        canvas.drawRect(
            RectF(
                (width - SIZE),
                (height - SIZE),
                width.toFloat(),
                height.toFloat()
            ), paint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val _x = event.rawX
        val _y = event.rawY

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x - event.rawX
                startY = y - event.rawY

                _width = width
                _height = height

                if (isTopLeft(event.x, event.y)) {
                    touchPoint = TouchPoint.TOP_LEFT
                } else if (isTopRight(event.x, event.y)) {
                    touchPoint = TouchPoint.TOP_RIGHT
                } else if (isBottomLeft(event.x, event.y)) {
                    touchPoint = TouchPoint.BOTTOM_LEFT
                } else if (isBottomRight(event.x, event.y)) {
                    touchPoint = TouchPoint.BOTTOM_RIGHT
                } else {
                    touchPoint = TouchPoint.CENTER
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val positionX = _x + startX
                val positionY = _y + startY

                val positionX1 = _x + (_width - abs(startX))
                val positionY1 = _y + (_height - abs(startY))

                /** perform positional checks using
                 * relative coordinates of touch points
                 */
//                moveView(positionX, positionY)
                when (touchPoint) {
                    TouchPoint.TOP_LEFT -> {
                        resizeTopLeft(positionX, positionY)
                    }
                    TouchPoint.TOP_RIGHT -> {
                        resizeTopRight(positionX1, positionY)
                    }
                    TouchPoint.BOTTOM_LEFT -> {
                        resizeBottomLeft(positionX, positionY1)
                    }
                    TouchPoint.BOTTOM_RIGHT -> {
                        resizeBottomRight(positionX1, positionY1)
                    }
                    TouchPoint.CENTER -> {
                        moveView(positionX, positionY)
                    }
                }
//                resizeTopRight(positionX+startX+ SIZE/2, positionY)
//                moveView(positionX, positionY)
                true
            }
            MotionEvent.ACTION_UP -> {
                touchPoint = null
                true
            }
            else -> false
        }
    }

    private fun isLeft(positionX: Float): Boolean {
        return (positionX in 0.0f..SIZE)
    }

    private fun isRight(positionX: Float): Boolean {
        return (positionX >= (width - SIZE) && positionX <= width)
    }

    private fun isTop(positionY: Float): Boolean {
        return (positionY in 0.0f..SIZE)
    }

    private fun isBottom(positionY: Float): Boolean {
        return (positionY in (height - SIZE)..height.toFloat())
    }

    private fun isTopLeft(positionX: Float, positionY: Float): Boolean {
        return isTop(positionY) && isLeft(positionX)
    }

    private fun isTopRight(positionX: Float, positionY: Float): Boolean {
        return isTop(positionY) && isRight(positionX)
    }

    private fun isBottomLeft(positionX: Float, positionY: Float): Boolean {
        return isBottom(positionY) && isLeft(positionX)
    }

    private fun isBottomRight(positionX: Float, positionY: Float): Boolean {
        return isBottom(positionY) && isRight(positionX)
    }

    private fun moveView(positionX: Float, positionY: Float) {
        val _x = rangeWidth(positionX.toInt())
        val _y = rangeHeight(positionY.toInt())

        layout(_x, _y, _x + width, _y + height)
    }

    private fun resizeTopLeft(positionX: Float, positionY: Float) {
        val _left = rangeLeft(positionX.toInt())
        val _top = rangeTop(positionY.toInt())

        layout(_left, _top, right, bottom)
    }

    private fun resizeBottomLeft(positionX: Float, positionY: Float) {
        val _left = rangeLeft(positionX.toInt())
        val _bottom = rangeBottom(positionY.toInt())

        layout(_left, top, right, _bottom)
    }

    private fun resizeBottomRight(positionX: Float, positionY: Float) {
        val _right = rangeRight(positionX.toInt())
        val _bottom = rangeBottom(positionY.toInt())

        layout(left, top, _right, _bottom)
    }

    private fun resizeTopRight(positionX: Float, positionY: Float) {
        val _right = rangeRight(positionX.toInt())
        val _top = rangeTop(positionY.toInt())

        layout(left, _top, _right, bottom)
    }

    private fun rangeWidth(w: Int): Int {
        return min(max(w, 0), screenWidth - width)
    }

    private fun rangeHeight(h: Int): Int {
        return min(max(h, 0), screenHeight - height)
    }

    private fun rangeLeft(w: Int): Int {
        return min(max(w, 0), right - (SIZE.toInt() * 2))
    }

    private fun rangeRight(w: Int): Int {
        return min(max(w, left + (SIZE.toInt() * 2)), screenWidth)
    }

    private fun rangeTop(h: Int): Int {
        return min(max(h, 0), bottom - (SIZE.toInt() * 2))
    }

    private fun rangeBottom(h: Int): Int {
        return min(max(h, top + (SIZE.toInt() * 2)), screenHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 0 + paddingLeft + paddingRight
        val desiredHeight = 0 + paddingTop + paddingBottom

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                min(desiredWidth, widthSize)
            }
            else -> {
                desiredWidth
            }
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                min(desiredHeight, heightSize)
            }
            else -> {
                desiredHeight
            }
        }

        setMeasuredDimension(width, height)
    }
}
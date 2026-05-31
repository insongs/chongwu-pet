package com.chongwu.pet.interaction

import android.view.MotionEvent
import kotlin.math.*

/**
 * 触摸引擎 - 手势识别 + 身体部位检测
 */
class TouchEngine {

    enum class Gesture {
        TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, PINCH, DRAG, NONE
    }

    enum class BodyPart {
        HEAD, HORN_LEFT, HORN_RIGHT, BODY, LEG, TAIL, NONE
    }

    data class TouchResult(
        val gesture: Gesture,
        val bodyPart: BodyPart,
        val x: Float,
        val y: Float
    )

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private var lastTapTime = 0L
    private var isLongPress = false
    private var isDragging = false
    private var pointerCount = 0

    // 拖拽回调
    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((Float, Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    fun processTouch(event: MotionEvent, viewWidth: Float, viewHeight: Float): TouchResult? {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchStartTime = System.currentTimeMillis()
                isLongPress = false
                isDragging = false
                pointerCount = 1
                onDragStart?.invoke()
                return null
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerCount = 2
                return null
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 20f && !isLongPress) {
                    isDragging = true
                    onDragMove?.invoke(dx, dy)
                }
                return null
            }
            MotionEvent.ACTION_UP -> {
                pointerCount = 0
                val dt = System.currentTimeMillis() - touchStartTime
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val dist = sqrt(dx * dx + dy * dy)

                if (isDragging) {
                    onDragEnd?.invoke()
                    isDragging = false
                    return TouchResult(Gesture.DRAG, BodyPart.NONE, event.x, event.y)
                }

                if (dt > 800) {
                    return TouchResult(Gesture.LONG_PRESS, detectBodyPart(event.x, event.y, viewWidth, viewHeight), event.x, event.y)
                }

                val now = System.currentTimeMillis()
                val isDoubleTap = now - lastTapTime < 300
                lastTapTime = now

                if (isDoubleTap) {
                    return TouchResult(Gesture.DOUBLE_TAP, BodyPart.NONE, event.x, event.y)
                }

                return TouchResult(Gesture.TAP, detectBodyPart(event.x, event.y, viewWidth, viewHeight), event.x, event.y)
            }
        }
        return null
    }

    private fun detectBodyPart(x: Float, y: Float, vw: Float, vh: Float): BodyPart {
        val cx = vw / 2f
        val cy = vh / 2f
        val s = min(vw.coerceAtLeast(1f), vh.coerceAtLeast(1f)) / 300f
        return when {
            dist(x, y, cx, cy - 70 * s) < 40 * s -> {
                if (dist(x, y, cx - 32 * s, cy - 110 * s) < 20 * s) BodyPart.HORN_LEFT
                else if (dist(x, y, cx + 32 * s, cy - 110 * s) < 20 * s) BodyPart.HORN_RIGHT
                else BodyPart.HEAD
            }
            dist(x, y, cx, cy + 20 * s) < 55 * s -> BodyPart.BODY
            dist(x, y, cx - 35 * s, cy + 70 * s) < 25 * s -> BodyPart.LEG
            dist(x, y, cx + 35 * s, cy + 70 * s) < 25 * s -> BodyPart.LEG
            dist(x, y, cx + 65 * s, cy + 10 * s) < 20 * s -> BodyPart.TAIL
            else -> BodyPart.NONE
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
}

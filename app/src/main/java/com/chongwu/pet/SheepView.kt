package com.chongwu.pet

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chongwu.pet.ai.SheepState
import com.chongwu.pet.ecology.Environment
import com.chongwu.pet.interaction.TouchEngine
import com.chongwu.pet.render.SheepRenderer
import kotlin.math.*

class SheepView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val brain = SheepState()
    val environment = Environment(context)
    private val renderer = SheepRenderer()
    private val touchEngine = TouchEngine()

    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((Float, Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private var animTime = 0f
    private var legPhase = 0f
    private var jumpH = 0f
    private var bounce = 0f
    private var tilt = 0f
    private var sx = 1f
    private var sy = 1f
    var fxHearts = 0f; var fxStars = 0f; var fxExclaim = 0f; var fxNote = 0f; var fxBlush = 0f
    var offsetX = 0f; var offsetY = 0f; var isClimbing = false

    private val handler = Handler(Looper.getMainLooper())
    private val animLoop = object : Runnable {
        override fun run() {
            val dt = 0.016f; animTime += dt
            legPhase += dt * (if (isClimbing) 6f else if (brain.currentState == SheepState.State.HOPPING) 4f else 1.2f)
            val timeInfo = environment.getTimeInfo()
            brain.update(dt, touchEngine != null, timeInfo.hour, environment.getWeather().name)
            environment.updateWeather(dt)
            environment.updateGrass(dt, environment.getWeather() == Environment.Weather.RAINY)
            when (brain.currentState) {
                SheepState.State.IDLE -> { bounce = sin(animTime*1.5f)*1.5f; tilt = sin(animTime*0.8f)*0.5f }
                SheepState.State.HOPPING, SheepState.State.PLAYING -> { jumpH = abs(sin(animTime*5f))*25f; tilt = sin(animTime*5f)*3f; sx=1f+cos(animTime*5f)*0.05f; sy=1f-cos(animTime*5f)*0.05f }
                SheepState.State.SLEEPING -> { bounce = sin(animTime*1f)*1f; tilt = 0f }
                SheepState.State.HEADBUTTING -> { tilt = sin(animTime*12f)*8f; sx=1f+cos(animTime*12f)*0.05f }
                SheepState.State.STARTLED -> { jumpH = abs(sin(animTime*10f))*35f; sx=1f+cos(animTime*10f)*0.1f; sy=1f-cos(animTime*10f)*0.1f }
                SheepState.State.STRETCHING -> { tilt = sin(animTime*2f)*2f; sy=1f+sin(animTime*2f)*0.05f }
                else -> {}
            }
            fxHearts = maxOf(0f, fxHearts - dt); fxStars = maxOf(0f, fxStars - dt)
            fxExclaim = maxOf(0f, fxExclaim - dt); fxNote = maxOf(0f, fxNote - dt); fxBlush = maxOf(0f, fxBlush - dt)
            if (isClimbing) tilt = cos(legPhase*0.5f)*3f
            invalidate(); handler.postDelayed(this, 16L)
        }
    }

    init { handler.post(animLoop); setupTouchEngine() }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacks(animLoop) }

    private fun setupTouchEngine() {
        touchEngine.onDragStart = { onDragStart?.invoke() }
        touchEngine.onDragMove = { dx, dy -> onDragMove?.invoke(dx, dy) }
        touchEngine.onDragEnd = { onDragEnd?.invoke() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = touchEngine.processTouch(event, width.toFloat(), height.toFloat())
        result?.let {
            when (it.gesture) {
                TouchEngine.Gesture.TAP -> { val p = mapBodyPart(it.bodyPart); brain.onTouched(p); triggerFX(p) }
                TouchEngine.Gesture.DOUBLE_TAP -> { brain.needs.affection = minOf(100f, brain.needs.affection+10f); fxHearts=2f; brain.mood=SheepState.Mood.HAPPY }
                TouchEngine.Gesture.LONG_PRESS -> { brain.mood=SheepState.Mood.CONTENT; fxBlush=2.5f }
                else -> {}
            }
        }
        return true
    }

    private fun mapBodyPart(part: TouchEngine.BodyPart): String = when (part) {
        TouchEngine.BodyPart.HEAD -> "HEAD"; TouchEngine.BodyPart.HORN_LEFT, TouchEngine.BodyPart.HORN_RIGHT -> "HORN"
        TouchEngine.BodyPart.BODY -> "BODY"; TouchEngine.BodyPart.LEG -> "LEG"; TouchEngine.BodyPart.TAIL -> "TAIL"
        else -> "NONE"
    }

    private fun triggerFX(part: String) { when (part) { "HEAD" -> fxHearts=1.5f; "HORN" -> fxStars=2f; "BODY" -> {fxHearts=2f;fxBlush=2f}; "LEG" -> fxExclaim=1f; "TAIL" -> fxNote=1.5f } }

    fun feed(foodType: String = "grass") { brain.feed(foodType); fxHearts=1.5f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        val s = min(w, h) / 300f

        canvas.save()
        canvas.translate(w / 2, h * 0.45f)  // 居中偏上
        canvas.scale(s * sx, s * sy)

        renderer.drawSheep(canvas, 1f, animTime, legPhase, jumpH, bounce, tilt, 1f, 1f,
            brain.expression, brain.currentState.name, isClimbing,
            fxHearts, fxStars, fxExclaim, fxNote, fxBlush)
        renderer.drawSleepingZZZ(canvas, animTime, brain.currentState.name)

        canvas.restore()
    }
}

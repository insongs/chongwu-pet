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

    private var animTime = 0f; private var legPhase = 0f
    private var jumpH = 0f; private var bounce = 0f; private var tilt = 0f
    private var sx = 1f; private var sy = 1f
    var fxHearts = 0f; var fxStars = 0f; var fxExclaim = 0f; var fxNote = 0f; var fxBlush = 0f
    var isClimbing = false

    // 小羊世界坐标（相对于屏幕）
    private var sheepScreenX = 0f
    private var sheepScreenY = 0f
    private var targetX = 0f
    private var moveSpeed = 0f
    private var facingRight = true
    private var isTouchingSheep = false

    // 草地
    private var grassInitialized = false
    private val grassList = mutableListOf<Environment.GrassBlade>()

    // 屏幕尺寸
    private var screenW = 1080f; private var screenH = 1920f

    private val handler = Handler(Looper.getMainLooper())
    private val animLoop = object : Runnable {
        override fun run() {
            val dt = 0.016f; animTime += dt
            screenW = width.coerceAtLeast(1).toFloat(); screenH = height.coerceAtLeast(1).toFloat()

            val timeInfo = environment.getTimeInfo()
            brain.update(dt, isTouchingSheep || touchEngine != null, timeInfo.hour, environment.getWeather().name)
            environment.updateWeather(dt)
            environment.updateGrass(dt, environment.getWeather() == Environment.Weather.RAINY)

            // 初始化草地（全屏底部）
            if (!grassInitialized) { initGrass(); grassInitialized = true }
            // 更新草地位置
            val groundY = screenH * 0.78f
            for (i in grassList.indices) grassList[i].x = i * (screenW / grassList.size)

            // AI 驱动动画
            when (brain.currentState) {
                SheepState.State.IDLE -> {
                    bounce = sin(animTime*1.5f)*1.5f; tilt = sin(animTime*0.8f)*0.5f
                    if (animTime.toInt() % 5 == 0 && animTime - animTime.toInt() < dt) {
                        targetX = screenW * 0.1f + Math.random().toFloat() * screenW * 0.6f
                        moveSpeed = 30f+Math.random().toFloat()*50f
                    }
                }
                SheepState.State.HOPPING -> { jumpH=abs(sin(animTime*6f))*30f; tilt=sin(animTime*6f)*4f; sx=1f+cos(animTime*6f)*0.06f; sy=1f-cos(animTime*6f)*0.06f }
                SheepState.State.GRAZING -> { tilt=15f+sin(animTime*3f)*5f; bounce=sin(animTime*4f)*1f; environment.eatGrassAt(sheepScreenX) }
                SheepState.State.SLEEPING -> { bounce=sin(animTime*1f)*3f; tilt=-5f; sy=0.85f+sin(animTime*1f)*0.03f }
                SheepState.State.HEADBUTTING -> { tilt=sin(animTime*12f)*10f; sx=1f+cos(animTime*12f)*0.08f }
                SheepState.State.STARTLED -> { jumpH=abs(sin(animTime*10f))*40f; sx=1f+cos(animTime*10f)*0.12f; sy=1f-cos(animTime*10f)*0.12f }
                SheepState.State.PLAYING -> { jumpH=abs(sin(animTime*5f))*20f; tilt=sin(animTime*5f)*5f; sx=1f+cos(animTime*5f)*0.08f; sy=1f-cos(animTime*5f)*0.08f }
                SheepState.State.EXPLORING -> { legPhase+=dt*3f; bounce=sin(animTime*3f)*1.5f }
                else -> {}
            }

            // 自由移动
            if (brain.currentState == SheepState.State.IDLE || brain.currentState == SheepState.State.EXPLORING) {
                if (abs(sheepScreenX - targetX) > 8f) {
                    val dir = sign(targetX - sheepScreenX)
                    sheepScreenX += dir * moveSpeed * dt; facingRight = dir>0
                } else { targetX = screenW*0.1f + Math.random().toFloat()*screenW*0.6f }
            }
            sheepScreenY = screenH * 0.55f  // 站在草地上方

            fxHearts = maxOf(0f, fxHearts-dt); fxStars = maxOf(0f, fxStars-dt); fxExclaim = maxOf(0f, fxExclaim-dt); fxNote = maxOf(0f, fxNote-dt); fxBlush = maxOf(0f, fxBlush-dt)
            invalidate(); handler.postDelayed(this, 16L)
        }
    }

    init { handler.post(animLoop) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacks(animLoop) }

    private fun initGrass() {
        grassList.clear()
        val count = (screenW / 12).toInt().coerceIn(20, 100)
        for (i in 0 until count) {
            grassList.add(Environment.GrassBlade(i * (screenW/count), 10f+Math.random().toFloat()*25f, 0f))
        }
    }

    // ===== 触摸（只处理小羊身上的触摸）=====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 检测是否点中了小羊
        val sheepLeft = sheepScreenX - 80f
        val sheepRight = sheepScreenX + 80f
        val sheepTop = sheepScreenY - 120f
        val sheepBottom = sheepScreenY + 60f
        val hitSheep = event.x in sheepLeft..sheepRight && event.y in sheepTop..sheepBottom

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!hitSheep) return false  // 没点中小羊，不拦截
                isTouchingSheep = true
                onDragStart?.invoke()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTouchingSheep) return false
                onDragMove?.invoke(event.x - sheepScreenX, event.y - sheepScreenY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isTouchingSheep) return false
                isTouchingSheep = false
                onDragEnd?.invoke()
                // 没拖动就是点击
                val part = touchEngine.detectPart(event.x, event.y, width.toFloat(), height.toFloat()) ?: return true
                val partName = mapBodyPart(part)
                brain.onTouched(partName); triggerFX(partName)
                return true
            }
        }
        return false
    }

    fun detectSheepPart(x: Float, y: Float): TouchEngine.BodyPart {
        return touchEngine.detectPart(x, y, width.toFloat(), height.toFloat()) ?: TouchEngine.BodyPart.NONE
    }

    private fun mapBodyPart(part: TouchEngine.BodyPart): String = when (part) {
        TouchEngine.BodyPart.HEAD -> "HEAD"; TouchEngine.BodyPart.HORN_LEFT, TouchEngine.BodyPart.HORN_RIGHT -> "HORN"
        TouchEngine.BodyPart.BODY -> "BODY"; TouchEngine.BodyPart.LEG -> "LEG"; TouchEngine.BodyPart.TAIL -> "TAIL"
        else -> "NONE"
    }

    private fun triggerFX(part: String) { when (part) { "HEAD" -> fxHearts=1.5f; "HORN" -> fxStars=2f; "BODY" -> {fxHearts=2f;fxBlush=2f}; "LEG" -> fxExclaim=1f; "TAIL" -> fxNote=1.5f } }

    // ===== 全屏绘制 =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = screenW; val h = screenH
        val groundY = h * 0.78f
        val timeInfo = environment.getTimeInfo()
        val weather = environment.getWeather()

        // 1. 天空渐变（昼夜变化）
        val skyTop = if (timeInfo.isNight) Color.argb(255, 20, 25, 50) else if (timeInfo.isMorning) Color.argb(255, 255, 200, 150) else if (timeInfo.isEvening) Color.argb(255, 255, 160, 100) else Color.argb(255, 135, 200, 255)
        val skyBottom = if (timeInfo.isNight) Color.argb(255, 40, 50, 80) else Color.argb(255, 240, 250, 255)
        val skyGrad = LinearGradient(0f, 0f, 0f, groundY, skyTop, skyBottom, Shader.TileMode.CLAMP)
        p.shader = skyGrad; canvas.drawRect(0f, 0f, w, groundY, p); p.shader = null

        // 2. 星空（夜晚）
        if (timeInfo.isNight) {
            p.color = Color.argb(180, 255, 255, 255)
            for (i in 0..30) {
                val sx = (i * 137.5f) % w; val sy = (i * 97.3f) % (groundY * 0.5f)
                val twinkle = sin(animTime * 2f + i) * 0.5f + 0.5f
                val r = 1f + twinkle
                canvas.drawCircle(sx, sy, r, p)
            }
        }

        // 3. 地面（草地）
        p.color = Color.argb(60, 60, 180, 40); canvas.drawRect(0f, groundY, w, h, p)
        p.color = Color.argb(80, 40, 150, 30); canvas.drawRect(0f, groundY+1f, w, groundY+5f, p)

        // 4. 画草
        for (blade in grassList) {
            if (blade.isEaten) continue
            val sway = sin(animTime*2f+blade.swayPhase)*3f
            ps.color = Color.argb(220, 70, 180, 50); ps.strokeWidth = 2.5f; ps.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(blade.x, groundY, blade.x+sway, groundY-blade.height, ps)
            ps.color = Color.argb(140, 120, 220, 80); ps.strokeWidth = 1.5f
            canvas.drawLine(blade.x+1f, groundY-1f, blade.x+sway+1f, groundY-blade.height+2f, ps)
        }

        // 5. 小羊
        canvas.save()
        canvas.translate(sheepScreenX, sheepScreenY)
        val s = min(w, h) / 400f
        canvas.scale(s * sx, s * sy)
        if (!facingRight) canvas.scale(-1f, 1f)
        renderer.drawSheep(canvas, 1f, animTime, legPhase, jumpH, bounce, tilt, sx, sy,
            brain.expression, brain.currentState.name, isClimbing,
            fxHearts, fxStars, fxExclaim, fxNote, fxBlush)
        renderer.drawSleepingZZZ(canvas, animTime, brain.currentState.name)
        canvas.restore()

        // 6. 天气特效
        if (weather == Environment.Weather.RAINY) drawRain(canvas, w, h)
        if (weather == Environment.Weather.SNOWY) drawSnow(canvas, w, h)
    }

    private fun drawRain(canvas: Canvas, w: Float, h: Float) {
        ps.color = Color.argb(120, 180, 200, 255); ps.strokeWidth = 1.5f
        for (i in 0..50) {
            val rx = (i * 47.7f + animTime * 200f) % w
            val ry = (i * 73.3f + animTime * 400f) % h
            canvas.drawLine(rx, ry, rx-2f, ry+15f, ps)
        }
    }

    private fun drawSnow(canvas: Canvas, w: Float, h: Float) {
        p.color = Color.argb(200, 255, 255, 255)
        for (i in 0..40) {
            val sx = (i * 67.3f + sin(animTime + i) * 30f) % w
            val sy = (i * 43.7f + animTime * 100f * (i%3+1)) % h
            canvas.drawCircle(sx, sy, 2f + (i%3).toFloat(), p)
        }
    }

    private val p = Paint()
    private val ps = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
}

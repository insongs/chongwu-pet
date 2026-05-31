package com.chongwu.pet

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * 咩咩宠物 - 纯 Canvas 绘制的卡通小羊
 * 使用 Color.argb() 保证跨版本兼容
 */
class SheepView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 颜色 ====================
    companion object {
        // 使用 argb() 确保在所有 Android 版本上颜色正确
        private val C_WOOL = Color.argb(255, 255, 240, 232)
        private val C_WOOL_LIGHT = Color.argb(255, 255, 245, 238)
        private val C_WOOL_SHADOW = Color.argb(255, 232, 213, 200)
        private val C_SKIN = Color.argb(255, 255, 245, 240)
        private val C_EAR = Color.argb(255, 255, 228, 214)
        private val C_EAR_INNER = Color.argb(255, 255, 181, 200)
        private val C_HORN = Color.argb(255, 139, 115, 85)
        private val C_HORN_DARK = Color.argb(255, 107, 83, 53)
        private val C_EYE = Color.argb(255, 44, 44, 44)
        private val C_NOSE = Color.argb(255, 255, 143, 171)
        private val C_MOUTH = Color.argb(255, 212, 89, 107)
        private val C_CHEEK = Color.argb(255, 255, 181, 200)
        private val C_LEG = Color.argb(255, 255, 219, 184)
        private val C_HOOF = Color.argb(255, 212, 168, 140)
        private const val BASE = 300f
    }

    enum class BodyPart { HEAD, HORN_LEFT, HORN_RIGHT, BODY, LEG, TAIL, NONE }

    var expression = "HAPPY"; private set
    var action = "IDLE"; private set
    var isClimbing = false
    var onPartTouched: ((BodyPart) -> Unit)? = null
    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((Float, Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private var animTime = 0f
    private var legPhase = 0f
    private var jumpH = 0f
    private var bounce = 0f
    private var tilt = 0f
    private var sx = 1f; private var sy = 1f
    private var fxHearts = 0f; private var fxStars = 0f
    private var fxExclaim = 0f; private var fxNote = 0f; private var fxBlush = 0f
    private var dragRX = 0f; private var dragRY = 0f; private var dragging = false

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ps = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val handler = Handler(Looper.getMainLooper())

    // ==================== 动画循环 ====================
    private val animLoop = object : Runnable {
        override fun run() {
            val dt = 0.016f
            animTime += dt
            legPhase += dt * (if (isClimbing) 6f else 1.2f)

            if (action == "IDLE") {
                bounce = sin(animTime * 1.5f) * 1.5f
                tilt = sin(animTime * 0.8f) * 0.5f
            }
            if (action == "JUMPING") {
                jumpH = sin(animTime * 6f) * 60f
                sx = 1f + cos(animTime * 6f) * 0.08f
                sy = 1f - cos(animTime * 6f) * 0.08f
                tilt = if (jumpH > 0) 5f else -5f
                if (animTime > PI / 6f) { action = "IDLE"; jumpH = 0f; sx = 1f; sy = 1f; tilt = 0f }
            }
            if (action == "HOPPING") {
                jumpH = abs(sin(animTime * 8f)) * 25f
                if (animTime > PI / 4f) { action = "IDLE"; jumpH = 0f }
            }

            fxHearts = max(0f, fxHearts - dt)
            fxStars = max(0f, fxStars - dt)
            fxExclaim = max(0f, fxExclaim - dt)
            fxNote = max(0f, fxNote - dt)
            fxBlush = max(0f, fxBlush - dt)
            if (isClimbing) tilt = cos(legPhase * 0.5f) * 3f

            invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    init {
        handler.post(animLoop)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animLoop)
    }

    fun setAction(a: String) { action = a; animTime = 0f; invalidate() }

    // ==================== 触摸 ====================
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                dragRX = e.rawX; dragRY = e.rawY; dragging = false
                onDragStart?.invoke(); return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - dragRX; val dy = e.rawY - dragRY
                if (sqrt(dx * dx + dy * dy) > 15f) { dragging = true; onDragMove?.invoke(dx, dy) }
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    val part = detectPart(e.x, e.y)
                    react(part); onPartTouched?.invoke(part)
                } else { onDragEnd?.invoke(); dragging = false }
            }
        }
        return true
    }

    private fun detectPart(x: Float, y: Float): BodyPart {
        val cx = width / 2f; val cy = height / 2f; val s = min(width.coerceAtLeast(1), height.coerceAtLeast(1)) / BASE
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

    private fun react(part: BodyPart) {
        when (part) {
            BodyPart.HEAD -> { expression = "HAPPY"; setAction("JUMPING"); fxHearts = 1.5f }
            BodyPart.HORN_LEFT, BodyPart.HORN_RIGHT -> { expression = "DIZZY"; fxStars = 2f }
            BodyPart.BODY -> { expression = "CONTENT"; fxHearts = 2f; fxBlush = 2f }
            BodyPart.LEG -> { expression = "SURPRISED"; fxExclaim = 1f }
            BodyPart.TAIL -> { expression = "LAUGHING"; fxNote = 1.5f }
            BodyPart.NONE -> {}
        }
        invalidate()
    }

    // ==================== 核心绘制 ====================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        val s = min(w, h) / BASE

        canvas.save()
        canvas.translate(w / 2, h / 2)
        canvas.scale(s * sx, s * sy)
        canvas.rotate(tilt)
        canvas.translate(0f, -bounce - jumpH)
        if (isClimbing) canvas.translate(sin(legPhase * 2) * 5, 0f)

        // ===== 先画一朵云做底板（确保能看到东西） =====
        p.style = Paint.Style.FILL

        // 尾巴（最底层）
        drawTail(c)
        // 后腿
        drawLeg(c, -35f, 35f, -6f, legPhase, false)
        drawLeg(c, 29f, 35f, 6f, legPhase + PI.toFloat(), false)
        // 身体 - 大云朵形状
        drawBody(c)
        // 前腿
        drawLeg(c, -30f, 40f, -7f, legPhase + PI.toFloat(), true)
        drawLeg(c, 24f, 40f, 7f, legPhase, true)
        // 头
        drawHead(c)
        // 耳朵
        drawEar(c, -18f, true); drawEar(c, 18f, false)
        // 角
        drawHorn(c, -1f); drawHorn(c, 1f)
        // 脸
        drawFace(c)
        // 特效
        drawFX(c)

        canvas.restore()
    }

    // ---- 身体 ----
    private fun drawBody(c: Canvas) {
        p.color = C_WOOL
        c.drawOval(-75f, -32f, 75f, 58f, p)  // 主身体
        // 绒毛 - 围绕身体画一圈小圆
        p.color = C_WOOL_LIGHT
        val fluffPositions = arrayOf(
            floatArrayOf(-60f, -35f, 18f), floatArrayOf(-40f, -40f, 20f), floatArrayOf(-20f, -42f, 18f),
            floatArrayOf(0f, -40f, 16f), floatArrayOf(20f, -42f, 18f), floatArrayOf(40f, -40f, 20f),
            floatArrayOf(60f, -35f, 18f), floatArrayOf(68f, -15f, 16f), floatArrayOf(70f, 5f, 17f),
            floatArrayOf(-68f, -15f, 16f), floatArrayOf(-70f, 5f, 17f), floatArrayOf(-60f, 35f, 15f),
            floatArrayOf(60f, 35f, 15f), floatArrayOf(0f, 50f, 14f), floatArrayOf(-25f, 52f, 12f), floatArrayOf(25f, 52f, 12f)
        )
        for (f in fluffPositions) c.drawCircle(f[0], f[1], f[2] + sin(animTime * 2f + f[0] * 0.1f) * 2f, p)

        p.style = Paint.Style.STROKE; p.color = C_WOOL_SHADOW; p.strokeWidth = 1.5f
        c.drawOval(-74f, -31f, 74f, 57f, p); p.style = Paint.Style.FILL
    }

    // ---- 腿 ----
    private fun drawLeg(c: Canvas, dx: Float, dy: Float, ox: Float, phase: Float, front: Boolean) {
        p.color = C_LEG
        val off = sin(phase) * 6f
        val len = if (front) 32f else 30f
        val w = 14f
        val lx = dx + off * 0.3f
        c.save()
        c.rotate(off * 0.5f, dx, dy)
        c.drawRoundRect(lx - w / 2, dy, lx + w / 2, dy + len, 6f, 6f, p)
        // 蹄子
        p.color = C_HOOF
        c.drawRoundRect(lx - w / 2, dy + len - 8f, lx + w / 2, dy + len, 4f, 4f, p)
        c.restore()
        p.color = C_LEG
    }

    // ---- 头 ----
    private fun drawHead(c: Canvas) {
        p.color = C_SKIN
        c.drawOval(-40f, -90f, 40f, -20f, p)  // 主头部
        // 头部绒毛
        p.color = C_WOOL_LIGHT
        c.drawCircle(-35f, -55f, 12f, p); c.drawCircle(35f, -55f, 12f, p)
        c.drawCircle(0f, -88f, 10f, p); c.drawCircle(-20f, -85f, 9f, p); c.drawCircle(20f, -85f, 9f, p)
    }

    // ---- 耳朵 ----
    private fun drawEar(c: Canvas, dx: Float, left: Boolean) {
        p.color = C_EAR
        val angle = if (left) -25f else 25f
        c.save(); c.rotate(angle, dx * 2.1f, -65f)
        c.drawOval(dx * 2.1f - 12f, -75f, dx * 2.1f + 12f, -50f, p)
        p.color = C_EAR_INNER
        c.drawOval(dx * 2.1f - 7f, -72f, dx * 2.1f + 7f, -54f, p)
        c.restore()
        p.color = C_EAR
    }

    // ---- 角 ----
    private fun drawHorn(c: Canvas, dir: Float) {
        ps.color = C_HORN; ps.strokeWidth = 9f; ps.strokeCap = Paint.Cap.ROUND
        val dx = dir * 24f
        path.reset(); path.moveTo(dx, -84f)
        path.cubicTo(dx + dir * 14f, -100f, dx + dir * 26f, -106f, dx + dir * 18f, -114f)
        path.cubicTo(dx + dir * 10f, -122f, dx - dir * 6f, -118f, dx - dir * 4f, -108f)
        c.drawPath(path, ps)
    }

    // ---- 脸 ----
    private fun drawFace(c: Canvas) {
        // 眼睛
        p.color = C_EYE
        val ex = 15f; val ey = -62f
        when (expression) {
            "HAPPY" -> {
                ps.color = C_EYE; ps.strokeWidth = 3f; ps.style = Paint.Style.STROKE
                c.drawArc(-ex - 7f, ey - 5f, -ex + 7f, ey + 5f, 0f, -180f, false, ps)
                c.drawArc(ex - 7f, ey - 5f, ex + 7f, ey + 5f, 0f, -180f, false, ps)
                ps.style = Paint.Style.FILL
            }
            "SURPRISED" -> {
                c.drawCircle(-ex, ey, 8f, p); c.drawCircle(ex, ey, 8f, p)
                p.color = Color.WHITE; c.drawCircle(-ex + 2f, ey - 2f, 3f, p); c.drawCircle(ex + 2f, ey - 2f, 3f, p)
                p.color = C_EYE
            }
            "CONTENT","LAUGHING" -> {
                ps.color = C_EYE; ps.strokeWidth = 3f
                path.reset(); path.moveTo(-ex - 6f, ey); path.quadTo(-ex, ey + 4f, -ex + 6f, ey); c.drawPath(path, ps)
                path.reset(); path.moveTo(ex - 6f, ey); path.quadTo(ex, ey + 4f, ex + 6f, ey); c.drawPath(path, ps)
            }
            "DIZZY" -> {
                ps.color = C_EYE; ps.strokeWidth = 2f
                c.drawCircle(-ex, ey, 7f, ps); c.drawArc(-ex - 7f, ey - 7f, -ex + 7f, ey + 7f, 0f, 360f, false, ps)
                c.drawCircle(ex, ey, 7f, ps); c.drawArc(ex - 7f, ey - 7f, ex + 7f, ey + 7f, 0f, 360f, false, ps)
            }
            else -> {  // HAPPY, IDLE 等默认圆眼
                c.drawCircle(-ex, ey, 6f, p); c.drawCircle(ex, ey, 6f, p)
                p.color = Color.WHITE; c.drawCircle(-ex + 2f, ey - 2f, 2.5f, p); c.drawCircle(ex + 2f, ey - 2f, 2.5f, p)
                p.color = C_EYE
            }
        }

        // 鼻子
        p.color = C_NOSE; c.drawOval(-5f, -50f, 5f, -45f, p)

        // 嘴巴
        ps.color = C_MOUTH; ps.strokeWidth = 2.5f; ps.style = Paint.Style.STROKE
        when (expression) {
            "HAPPY","CONTENT","LAUGHING" -> {
                path.reset(); path.moveTo(-10f, -40f); path.quadTo(0f, -34f, 10f, -40f); c.drawPath(path, ps)
            }
            "SURPRISED" -> {
                ps.style = Paint.Style.FILL; p.color = C_MOUTH; c.drawOval(-5f, -38f, 5f, -28f, p)
                p.color = C_NOSE; c.drawOval(-3f, -36f, 3f, -30f, p)
            }
            "DIZZY" -> {
                path.reset(); path.moveTo(-10f, -38f); path.quadTo(-5f, -42f, 0f, -38f)
                path.quadTo(5f, -34f, 10f, -38f); c.drawPath(path, ps)
            }
            else -> {
                path.reset(); path.moveTo(-10f, -40f); path.quadTo(0f, -36f, 10f, -40f); c.drawPath(path, ps)
            }
        }
        ps.style = Paint.Style.FILL

        // 腮红
        val ba = min(255, (if (fxBlush > 0) 140 else 70))
        p.color = Color.argb(ba, 255, 181, 200)
        c.drawCircle(-26f, -46f, 10f, p); c.drawCircle(26f, -46f, 10f, p)
    }

    // ---- 尾巴 ----
    private fun drawTail(c: Canvas) {
        p.color = C_WOOL
        val wag = if (expression == "HAPPY" || expression == "LAUGHING") sin(animTime * 7f) * 5f else 0f
        c.save(); c.rotate(wag, 72f, 8f)
        c.drawCircle(74f, 10f, 14f, p)
        p.color = C_WOOL_LIGHT
        c.drawCircle(76f, 6f, 9f, p); c.drawCircle(72f, 14f, 9f, p)
        c.restore()
    }

    // ---- 特效 ----
    private fun drawFX(c: Canvas) {
        p.textAlign = Paint.Align.CENTER
        if (fxHearts > 0) {
            p.color = 0xFFFF69B4.toInt(); p.textSize = 26f
            for (i in 0 until 3) c.drawText("♥", (-15 + i * 15).toFloat(), -105f - 12f * i + sin(animTime * 4f + i) * 4f, p)
        }
        if (fxStars > 0) {
            p.color = 0xFFFFD700.toInt(); p.textSize = 22f
            for (i in 0 until 4) c.drawText("✦", (-24 + i * 16).toFloat(), -115f + sin(animTime * 3f + i * 2f) * 6f, p)
        }
        if (fxExclaim > 0) {
            p.color = 0xFFFF4444.toInt(); p.textSize = 30f
            c.drawText("!", 0f, -105f + sin(animTime * 5f) * 3f, p)
        }
        if (fxNote > 0) {
            p.color = 0xFF9C27B0.toInt(); p.textSize = 22f
            c.drawText("♪", 55f, -85f + sin(animTime * 5f) * 4f, p)
            c.drawText("♫", 65f, -75f + cos(animTime * 4f) * 3f, p)
        }
        if (isClimbing) {
            ps.color = Color.argb(80, 180, 180, 180); ps.strokeWidth = 2f
            for (i in 0..3) { val y = -25f + i * 24f
                c.drawLine(-78f, y, -68f + sin(legPhase * 2f + i) * 5f, y, ps)
                c.drawLine(68f + sin(legPhase * 2f + i + 2f) * 5f, y, 78f, y, ps)
            }
        }
    }
}

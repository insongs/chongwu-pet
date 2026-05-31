package com.chongwu.pet.render
import com.chongwu.pet.ecology.Environment

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 2.5D 小羊渲染引擎
 * 多层 Canvas 叠加实现立体感：渐变色、光照、阴影、毛绒粒子
 */
class SheepRenderer {

    // ==================== 颜色 ====================
    companion object {
        // 毛色
        private val WOOL_BASE = Color.argb(255, 255, 245, 235)
        private val WOOL_LIGHT = Color.argb(255, 255, 250, 245)
        private val WOOL_SHADOW = Color.argb(255, 220, 200, 185)
        private val WOOL_DARK = Color.argb(255, 190, 170, 155)

        // 皮肤
        private val SKIN_BASE = Color.argb(255, 255, 232, 220)
        private val SKIN_SHADOW = Color.argb(255, 235, 200, 185)
        private val EAR_INNER = Color.argb(255, 255, 180, 200)

        // 角
        private val HORN_BASE = Color.argb(255, 160, 135, 105)
        private val HORN_LIGHT = Color.argb(255, 190, 168, 140)
        private val HORN_DARK = Color.argb(255, 120, 95, 70)

        // 眼
        private val EYE_WHITE = Color.argb(255, 255, 255, 255)
        private val EYE_DARK = Color.argb(255, 50, 35, 25)
        private val EYE_HIGHLIGHT = Color.argb(255, 255, 255, 255)

        // 鼻子/嘴
        private val NOSE_PINK = Color.argb(255, 255, 150, 175)
        private val MOUTH_RED = Color.argb(255, 220, 90, 110)

        // 腿/蹄
        private val LEG_BASE = Color.argb(255, 255, 220, 195)
        private val HOOF_DARK = Color.argb(255, 80, 60, 45)

        // 腮红
        private val BLUSH = Color.argb(120, 255, 180, 200)

        private val BASE_SCALE = 300f
    }

    // ==================== 画笔 ====================
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val bgPath = Path()

    // 渐变色缓存
    private val gradientCache = mutableMapOf<String, Shader>()

    private fun getBodyGradient(): RadialGradient {
        val key = "body"
        return gradientCache.getOrPut(key) {
            RadialGradient(
                0f, -10f, 75f,
                intArrayOf(WOOL_LIGHT, WOOL_BASE, WOOL_SHADOW),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun getHeadGradient(): RadialGradient {
        val key = "head"
        return gradientCache.getOrPut(key) {
            RadialGradient(
                -5f, -60f, 45f,
                intArrayOf(SKIN_BASE, SKIN_BASE, SKIN_SHADOW),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun getHornGradient(direction: Float): LinearGradient {
        val key = "horn_$direction"
        return gradientCache.getOrPut(key) {
            LinearGradient(
                0f, -120f, direction * 30f, -80f,
                HORN_LIGHT, HORN_DARK,
                Shader.TileMode.CLAMP
            )
        }
    }

    // ==================== 主绘制入口 ====================
    fun drawSheep(
        canvas: Canvas,
        scale: Float,
        animTime: Float,
        legPhase: Float,
        jumpH: Float,
        bounce: Float,
        tilt: Float,
        scaleX: Float,
        scaleY: Float,
        expression: String,
        action: String,
        isClimbing: Boolean,
        fxHearts: Float,
        fxStars: Float,
        fxExclaim: Float,
        fxNote: Float,
        fxBlush: Float
    ) {
        canvas.save()
        canvas.scale(scale * scaleX, scale * scaleY)
        canvas.rotate(tilt)
        canvas.translate(0f, -bounce - jumpH)
        if (isClimbing) canvas.translate(sin(legPhase * 2) * 5f, 0f)

        // ========== 绘制顺序 (从远到近) ==========
        // 0. 地面阴影
        drawGroundShadow(canvas, animTime)
        // 1. 尾巴
        drawTail(canvas, expression, animTime)
        // 2. 后腿
        drawLeg(canvas, -32f, 38f, legPhase, false)
        drawLeg(canvas, 26f, 38f, legPhase + PI.toFloat(), false)
        // 3. 身体 (毛团)
        drawBody(canvas, animTime, expression)
        // 4. 前腿
        drawLeg(canvas, -28f, 40f, legPhase + PI.toFloat(), true)
        drawLeg(canvas, 22f, 40f, legPhase, true)
        // 5. 头
        drawHead(canvas, animTime, expression)
        // 6. 耳朵
        drawEar(canvas, -20f, true, animTime, expression)
        drawEar(canvas, 20f, false, animTime, expression)
        // 7. 角
        drawHorn(canvas, -1f, expression)
        drawHorn(canvas, 1f, expression)
        // 8. 脸 (眼睛+鼻子+嘴巴)
        drawFace(canvas, expression, animTime, fxBlush)
        // 9. 特效
        drawFX(canvas, animTime, fxHearts, fxStars, fxExclaim, fxNote, isClimbing, legPhase)

        canvas.restore()
    }

    // ==================== 地面阴影 ====================
    private fun drawGroundShadow(canvas: Canvas, animTime: Float) {
        pFill.color = Color.argb(40, 0, 0, 0)
        val pulse = 0.9f + sin(animTime * 1.5f) * 0.1f
        canvas.save()
        canvas.translate(0f, 75f)
        canvas.scale(pulse, 0.4f)
        canvas.drawOval(-70f, -10f, 70f, 10f, pFill)
        canvas.restore()
    }

    // ==================== 尾巴 ====================
    private fun drawTail(canvas: Canvas, expression: String, animTime: Float) {
        pFill.color = WOOL_BASE
        val wag = if (expression == "HAPPY" || expression == "LAUGHING")
            sin(animTime * 7f) * 6f else 0f
        canvas.save()
        canvas.rotate(wag, 78f, 10f)

        // 尾巴毛团 (多层)
        val tailX = 78f
        val tailY = 12f
        pFill.color = WOOL_SHADOW
        canvas.drawCircle(tailX + 2f, tailY + 2f, 16f, pFill)
        pFill.color = WOOL_BASE
        canvas.drawCircle(tailX, tailY, 15f, pFill)
        pFill.color = WOOL_LIGHT
        canvas.drawCircle(tailX + 3f, tailY - 3f, 10f, pFill)
        canvas.drawCircle(tailX - 4f, tailY + 2f, 8f, pFill)

        canvas.restore()
    }

    // ==================== 腿 ====================
    private fun drawLeg(canvas: Canvas, dx: Float, dy: Float, phase: Float, front: Boolean) {
        val off = sin(phase) * 7f  // 左右摆动幅度
        val len = if (front) 34f else 32f
        val legW = 16f
        val lx = dx + off * 0.3f

        canvas.save()
        canvas.rotate(off * 0.5f, dx, dy)

        // 腿阴影
        pFill.color = Color.argb(60, 0, 0, 0)
        canvas.drawRoundRect(lx - legW / 2 + 2f, dy + 2f, lx + legW / 2 + 2f, dy + len + 2f, 5f, 5f, pFill)

        // 腿主体 (渐变)
        pFill.color = LEG_BASE
        canvas.drawRoundRect(lx - legW / 2, dy, lx + legW / 2, dy + len, 5f, 5f, pFill)

        // 腿高光
        pFill.color = Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(lx - legW / 2 + 3f, dy + 3f, lx - legW / 2 + 6f, dy + len - 5f, 2f, 2f, pFill)

        // 蹄子
        pFill.color = HOOF_DARK
        canvas.drawRoundRect(lx - legW / 2 + 1f, dy + len - 10f, lx + legW / 2 - 1f, dy + len, 4f, 4f, pFill)

        // 蹄子高光
        pFill.color = Color.argb(40, 255, 255, 255)
        canvas.drawRoundRect(lx - legW / 2 + 2f, dy + len - 8f, lx - legW / 2 + 5f, dy + len - 2f, 2f, 2f, pFill)

        canvas.restore()
    }

    // ==================== 身体 ====================
    private fun drawBody(canvas: Canvas, animTime: Float, expression: String) {
        // 身体阴影 (立体感)
        pFill.shader = getBodyGradient()
        canvas.drawOval(-80f, -38f, 80f, 65f, pFill)
        pFill.shader = null

        // 身体轮廓线
        pStroke.color = WOOL_SHADOW
        pStroke.strokeWidth = 1.5f
        canvas.drawOval(-80f, -38f, 80f, 65f, pStroke)
        pStroke.strokeWidth = 1f

        // ===== 羊毛粒子 (围绕身体) =====
        drawWoolFluff(canvas, animTime, expression)

        // 身体高光 (右上光照)
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
        highlight.shader = RadialGradient(
            -30f, -25f, 50f,
            intArrayOf(Color.argb(60, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(-75f, -35f, 75f, 62f, highlight)
        highlight.shader = null
    }

    // ==================== 羊毛粒子 ====================
    private val fluffPositions = arrayOf(
        // x, y, radius, phaseOffset
        floatArrayOf(-65f, -38f, 22f, 0f), floatArrayOf(-40f, -42f, 25f, 1f),
        floatArrayOf(-15f, -44f, 22f, 2f), floatArrayOf(15f, -44f, 24f, 3f),
        floatArrayOf(40f, -42f, 25f, 4f), floatArrayOf(65f, -38f, 22f, 5f),
        floatArrayOf(75f, -15f, 20f, 6f), floatArrayOf(78f, 8f, 22f, 7f),
        floatArrayOf(72f, 30f, 20f, 8f), floatArrayOf(60f, 50f, 18f, 9f),
        floatArrayOf(40f, 58f, 20f, 10f), floatArrayOf(15f, 62f, 18f, 11f),
        floatArrayOf(-15f, 62f, 18f, 12f), floatArrayOf(-40f, 58f, 20f, 13f),
        floatArrayOf(-60f, 50f, 18f, 14f), floatArrayOf(-72f, 30f, 20f, 15f),
        floatArrayOf(-78f, 8f, 22f, 16f), floatArrayOf(-75f, -15f, 20f, 17f)
    )

    private fun drawWoolFluff(canvas: Canvas, animTime: Float, expression: String) {
        val isHappy = expression == "HAPPY" || expression == "LAUGHING"
        for (f in fluffPositions) {
            val breathe = sin(animTime * 2f + f[3]) * 2f
            val r = f[2] + breathe

            // 阴影层
            pFill.color = WOOL_SHADOW
            canvas.drawCircle(f[0] + 1f, f[1] + 2f, r + 1f, pFill)

            // 主毛层
            pFill.color = WOOL_BASE
            canvas.drawCircle(f[0], f[1], r, pFill)

            // 高光层 (上偏左)
            pFill.color = WOOL_LIGHT
            canvas.drawCircle(f[0] - r * 0.2f, f[1] - r * 0.3f, r * 0.6f, pFill)

            // 开心时绒毛更蓬松
            if (isHappy) {
                pFill.color = Color.argb(60, 255, 255, 255)
                canvas.drawCircle(f[0] - r * 0.1f, f[1] - r * 0.4f, r * 0.3f, pFill)
            }
        }
    }

    // ==================== 头 ====================
    private fun drawHead(canvas: Canvas, animTime: Float, expression: String) {
        // 头部阴影
        pFill.color = Color.argb(40, 0, 0, 0)
        canvas.drawOval(-38f, -86f, 42f, -16f, pFill)

        // 头部主体 (渐变立体)
        pFill.shader = getHeadGradient()
        canvas.drawOval(-40f, -88f, 40f, -18f, pFill)
        pFill.shader = null

        // 头部绒毛
        val headFluff = arrayOf(
            floatArrayOf(-30f, -52f, 14f), floatArrayOf(30f, -52f, 14f),
            floatArrayOf(0f, -86f, 12f), floatArrayOf(-18f, -84f, 10f),
            floatArrayOf(18f, -84f, 10f)
        )
        for (f in headFluff) {
            val breathe = sin(animTime * 2f + f[0]) * 1f
            pFill.color = WOOL_LIGHT
            canvas.drawCircle(f[0], f[1], f[2] + breathe, pFill)
            pFill.color = Color.argb(80, 255, 255, 255)
            canvas.drawCircle(f[0] - 2f, f[1] - 3f, f[2] * 0.4f, pFill)
        }
    }

    // ==================== 耳朵 ====================
    private fun drawEar(canvas: Canvas, dx: Float, left: Boolean, animTime: Float, expression: String) {
        val baseAngle = if (left) -20f else 20f
        val earWag = when (expression) {
            "HAPPY", "LAUGHING" -> sin(animTime * 5f) * 8f
            else -> 0f
        }
        val angle = baseAngle + earWag

        canvas.save()
        canvas.rotate(angle, dx * 2f, -62f)

        // 耳朵阴影
        pFill.color = Color.argb(40, 0, 0, 0)
        val ex = dx * 2f
        canvas.drawOval(ex - 14f, -78f, ex + 14f, -48f, pFill)

        // 耳朵主体
        pFill.color = SKIN_BASE
        canvas.drawOval(ex - 14f, -80f, ex + 14f, -48f, pFill)

        // 耳朵内侧 (粉色)
        pFill.color = EAR_INNER
        canvas.drawOval(ex - 8f, -75f, ex + 8f, -52f, pFill)

        canvas.restore()
    }

    // ==================== 角 ====================
    private fun drawHorn(canvas: Canvas, dir: Float, expression: String) {
        val dx = dir * 28f
        val baseY = -82f

        // 角阴影
        pStroke.color = Color.argb(60, 0, 0, 0)
        pStroke.strokeWidth = 12f
        pStroke.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(dx + 2f, baseY + 2f)
        path.cubicTo(
            dx + dir * 18f, baseY - 18f,
            dx + dir * 30f, baseY - 28f,
            dx + dir * 22f, baseY - 36f
        )
        canvas.drawPath(path, pStroke)

        // 角主体 (渐变)
        pStroke.color = HORN_BASE
        pStroke.strokeWidth = 11f
        path.reset()
        path.moveTo(dx, baseY)
        path.cubicTo(
            dx + dir * 16f, baseY - 16f,
            dx + dir * 28f, baseY - 26f,
            dx + dir * 20f, baseY - 34f
        )
        canvas.drawPath(path, pStroke)

        // 角高光
        pStroke.color = HORN_LIGHT
        pStroke.strokeWidth = 4f
        path.reset()
        path.moveTo(dx + dir * 2f, baseY + 2f)
        path.cubicTo(
            dx + dir * 12f, baseY - 10f,
            dx + dir * 20f, baseY - 18f,
            dx + dir * 16f, baseY - 28f
        )
        canvas.drawPath(path, pStroke)

        // 角纹路 (横纹)
        pStroke.color = HORN_DARK
        pStroke.strokeWidth = 1.5f
        for (i in 0..3) {
            val t = i * 0.2f + 0.2f
            val cx = dx + dir * (16f * t + 10f * t * t)
            val cy = baseY - 16f * t - 14f * t * t
            val w = 6f * (1f - t * 0.5f)
            canvas.drawLine(cx - w, cy, cx + w, cy, pStroke)
        }
        pStroke.strokeWidth = 1f
    }

    // ==================== 脸 ====================
    private fun drawFace(canvas: Canvas, expression: String, animTime: Float, fxBlush: Float) {
        val eyeX = 18f
        val eyeY = -60f
        val blinkCycle = sin(animTime * 3.5f)
        val isBlinking = blinkCycle > 2.8f
        val isHalfBlink = blinkCycle > 2.5f

        // ===== 眼睛 =====
        drawEye(canvas, -eyeX, eyeY, expression, animTime, isBlinking, isHalfBlink)
        drawEye(canvas, eyeX, eyeY, expression, animTime, isBlinking, isHalfBlink)

        // ===== 鼻子 =====
        pFill.color = NOSE_PINK
        canvas.drawOval(-6f, -48f, 6f, -42f, pFill)
        // 鼻子高光
        pFill.color = Color.argb(100, 255, 255, 255)
        canvas.drawOval(-3f, -47f, -1f, -44f, pFill)

        // ===== 嘴巴 =====
        pStroke.color = MOUTH_RED
        pStroke.strokeWidth = 2.5f
        pStroke.style = Paint.Style.STROKE
        when (expression) {
            "HAPPY", "LAUGHING", "CONTENT" -> {
                path.reset()
                path.moveTo(-12f, -38f)
                path.quadTo(0f, -30f, 12f, -38f)
                canvas.drawPath(path, pStroke)
                // 张嘴笑 (Laughing)
                if (expression == "LAUGHING") {
                    pFill.color = Color.argb(200, 255, 100, 100)
                    pFill.style = Paint.Style.FILL
                    canvas.drawOval(-5f, -36f, 5f, -28f, pFill)
                    // 小舌头
                    pFill.color = Color.argb(200, 255, 150, 150)
                    canvas.drawOval(-3f, -33f, 3f, -27f, pFill)
                    pFill.style = Paint.Style.FILL
                }
            }
            "SURPRISED" -> {
                pFill.style = Paint.Style.FILL
                pFill.color = MOUTH_RED
                canvas.drawOval(-6f, -36f, 6f, -26f, pFill)
                pFill.color = Color.argb(100, 0, 0, 0)
                canvas.drawOval(-3f, -34f, 3f, -28f, pFill)
                pFill.style = Paint.Style.FILL
            }
            "SLEEPING" -> {
                path.reset()
                path.moveTo(-8f, -38f)
                path.quadTo(0f, -42f, 8f, -38f)
                canvas.drawPath(path, pStroke)
            }
            "ANGRY" -> {
                path.reset()
                path.moveTo(-8f, -36f)
                path.quadTo(0f, -40f, 8f, -36f)
                canvas.drawPath(path, pStroke)
            }
            else -> {
                path.reset()
                path.moveTo(-10f, -38f)
                path.quadTo(0f, -34f, 10f, -38f)
                canvas.drawPath(path, pStroke)
            }
        }
        pStroke.style = Paint.Style.FILL

        // ===== 腮红 =====
        val blushAlpha = min(255, (if (fxBlush > 0) 150 else 60).toInt())
        pFill.color = Color.argb(blushAlpha, 255, 180, 200)
        canvas.drawCircle(-28f, -44f, 12f, pFill)
        canvas.drawCircle(28f, -44f, 12f, pFill)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, expression: String, animTime: Float, isBlinking: Boolean, isHalfBlink: Boolean) {
        val eyeW = 14f
        val eyeH = if (isBlinking) 1f else if (isHalfBlink) 5f else 10f

        when (expression) {
            "HAPPY", "LAUGHING" -> {
                // 弯弯的笑眼
                pStroke.color = EYE_DARK
                pStroke.strokeWidth = 3f
                pStroke.style = Paint.Style.STROKE
                canvas.drawArc(x - 8f, y - 5f, x + 8f, y + 5f, 0f, -180f, false, pStroke)
                pStroke.style = Paint.Style.FILL
            }
            "SLEEPING" -> {
                // 闭眼 - 横线
                pStroke.color = EYE_DARK
                pStroke.strokeWidth = 2.5f
                pStroke.style = Paint.Style.STROKE
                canvas.drawLine(x - 6f, y, x + 6f, y, pStroke)
                pStroke.style = Paint.Style.FILL
            }
            "SURPRISED" -> {
                // 大圆眼
                pFill.color = EYE_DARK
                canvas.drawCircle(x, y, 9f, pFill)
                pFill.color = EYE_WHITE
                canvas.drawCircle(x - 3f, y - 3f, 4f, pFill)
            }
            else -> {
                // 默认圆眼
                if (isBlinking || isHalfBlink) {
                    pStroke.color = EYE_DARK
                    pStroke.strokeWidth = 2f
                    pStroke.style = Paint.Style.STROKE
                    val h = if (isBlinking) 1f else 4f
                    canvas.drawOval(x - 7f, y - h, x + 7f, y + h, pStroke)
                    pStroke.style = Paint.Style.FILL
                } else {
                    // 眼白
                    pFill.color = EYE_WHITE
                    canvas.drawOval(x - 7f, y - 5f, x + 7f, y + 5f, pFill)
                    // 瞳孔
                    pFill.color = EYE_DARK
                    canvas.drawOval(x - 4f, y - 4f, x + 4f, y + 4f, pFill)
                    // 高光
                    pFill.color = EYE_HIGHLIGHT
                    canvas.drawCircle(x - 2f, y - 2f, 2.5f, pFill)
                    canvas.drawCircle(x + 2f, y + 1f, 1.2f, pFill)
                }
            }
        }
    }

    // ==================== 睡觉冒泡 zZz ====================
    fun drawSleepingZZZ(canvas: Canvas, animTime: Float, action: String) {
        if (action != "SLEEPING") return
        pFill.color = Color.argb(180, 180, 200, 255)
        pFill.textAlign = Paint.Align.CENTER
        pFill.textSize = 18f
        val offset = sin(animTime * 2f) * 2f
        canvas.drawText("z", 50f, -80f + offset, pFill)
        pFill.textSize = 24f
        canvas.drawText("Z", 60f, -100f + sin(animTime * 2.5f) * 3f, pFill)
        pFill.textSize = 30f
        canvas.drawText("Z", 72f, -120f + sin(animTime * 3f) * 4f, pFill)
    }

    // ==================== 特效 ====================
    private fun drawFX(canvas: Canvas, animTime: Float,
                       fxHearts: Float, fxStars: Float, fxExclaim: Float,
                       fxNote: Float, isClimbing: Boolean, legPhase: Float) {
        val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        if (fxHearts > 0) {
            pText.color = Color.argb((255 * fxHearts).toInt(), 255, 105, 180)
            pText.textSize = 28f
            for (i in 0 until 3) {
                val floatY = sin(animTime * 4f + i * 2f) * 5f
                canvas.drawText("?", (-20 + i * 20).toFloat(), -110f - 15f * i + floatY, pText)
            }
        }
        if (fxStars > 0) {
            pText.color = Color.argb((255 * fxStars).toInt(), 255, 215, 0)
            pText.textSize = 24f
            for (i in 0 until 4) {
                val floatY = sin(animTime * 3f + i * 1.5f) * 6f
                canvas.drawText("?", (-30 + i * 20).toFloat(), -120f + floatY, pText)
            }
        }
        if (fxExclaim > 0) {
            pText.color = Color.argb((255 * fxExclaim).toInt(), 255, 68, 68)
            pText.textSize = 32f
            canvas.drawText("!", 0f, -110f + sin(animTime * 5f) * 3f, pText)
        }
        if (fxNote > 0) {
            pText.color = Color.argb((255 * fxNote).toInt(), 156, 39, 176)
            pText.textSize = 24f
            canvas.drawText("?", 55f, -85f + sin(animTime * 5f) * 4f, pText)
            canvas.drawText("?", 68f, -72f + cos(animTime * 4f) * 3f, pText)
        }
        if (isClimbing) {
            pStroke.color = Color.argb(80, 180, 180, 180)
            pStroke.strokeWidth = 2f
            for (i in 0..3) {
                val y = -25f + i * 26f
                canvas.drawLine(-82f, y, -72f + sin(legPhase * 2f + i) * 5f, y, pStroke)
                canvas.drawLine(72f + sin(legPhase * 2f + i + 2f) * 5f, y, 82f, y, pStroke)
            }
        }
    }

    // ==================== 吃草动画 ====================
    fun drawGrassOnScreen(canvas: Canvas, grassList: List<Environment.GrassBlade>,
                          groundY: Float, animTime: Float, screenWidth: Float) {
        for (blade in grassList) {
            if (blade.isEaten) continue
            val sway = sin(animTime * 2f + blade.swayPhase) * 3f
            pStroke.color = Color.argb(200, 80, 180, 60)
            pStroke.strokeWidth = 2.5f
            pStroke.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(blade.x, groundY, blade.x + sway, groundY - blade.height, pStroke)
            // 草叶高光
            pStroke.color = Color.argb(150, 120, 220, 80)
            pStroke.strokeWidth = 1.5f
            canvas.drawLine(blade.x + 1f, groundY - 1f, blade.x + sway + 1f, groundY - blade.height + 2f, pStroke)
        }
    }
}

package com.chongwu.pet

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * 咩咩宠物主视图 —— Canvas 绘制 & 触摸交互
 *
 * 小羊全部用 Canvas 2D 绘制，无需任何图片资源！
 */
class SheepView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 枚举 ====================

    /** 身体部位 */
    enum class BodyPart {
        HEAD, HORN_LEFT, HORN_RIGHT, BODY, LEG_FL, LEG_FR, LEG_BL, LEG_BR, TAIL, NONE
    }

    /** 表情 */
    enum class Expression {
        HAPPY, SAD, ANGRY, SURPRISED, SLEEPY, DIZZY, INNOCENT, CONTENT, LAUGHING
    }

    /** 动作状态 */
    enum class Action {
        IDLE, WALKING, JUMPING, FALLING, CLIMBING, SLEEPING, SCRATCHING, EATING, DIZZY_SPIN, HOPPING
    }

    // ==================== 状态 ====================

    var expression: Expression = Expression.HAPPY
        private set
    var action: Action = Action.IDLE
        private set
    private var animTime = 0f
    private var bounceOffset = 0f
    private var jumpHeight = 0f
    private var tiltAngle = 0f
    private var scaleX = 1f
    private var scaleY = 1f

    // 攀爬相关
    var isClimbing = false
    var climbDirection = 1f // 1=向上/右, -1=向下/左
    var climbProgress = 0f

    // 交互回调（给 OverlayService 使用）
    var onPartTouched: ((BodyPart) -> Unit)? = null
    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((dx: Float, dy: Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    // 特效可见性
    private var showHearts = false
    private var showStars = false
    private var showTears = false
    private var showZzz = false
    private var showExclamation = false
    private var showNote = false
    private var showBlushBoost = false
    private var effectTime = 0f

    // 行走/攀爬 腿部动画
    private var legPhase = 0f

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rectF = RectF()

    // 主handler
    private val mainHandler = Handler(Looper.getMainLooper())

    // ==================== 动画循环 ====================

    private var isAnimating = false
    private var hasPendingIdleAction = false
    private val animRunnable = object : Runnable {
        override fun run() {
            val dt = 0.016f // ~60fps
            animTime += dt
            legPhase += dt * (if (isClimbing) 6f else if (action == Action.WALKING) 4f else 1.2f)

                        // 空闲时呼吸/微小晃动
            if (action == Action.IDLE) {
                bounceOffset = sin(animTime * 1.5f) * 1.5f
                tiltAngle = sin(animTime * 0.8f) * 0.5f
                // 随机空闲动作（约每8秒触发一次）
                if (sin(animTime * 0.13f) > 0.98f && !hasPendingIdleAction) {
                    hasPendingIdleAction = true
                    mainHandler.postDelayed({
                        val rand = (animTime * 7f).toInt() % 4
                        when (rand) {
                            0 -> { expression = Expression.CONTENT; showHearts = true; effectTime = 1.2f }
                            1 -> { tiltAngle = 15f; mainHandler.postDelayed({ tiltAngle = 0f }, 400) }
                            2 -> { setAction(Action.HOPPING); animTime = 0f; jumpHeight = sin(animTime * 8f) * 25f }
                            3 -> { expression = Expression.INNOCENT }
                        }
                        hasPendingIdleAction = false
                        expression = Expression.HAPPY
                    }, 200)
                }
            }
            if (action == Action.IDLE) {
                bounceOffset = sin(animTime * 1.5f) * 1.5f
                tiltAngle = sin(animTime * 0.8f) * 0.5f
            }

            // 特效倒计时
            if (showHearts || showStars || showTears || showZzz || showExclamation || showNote || showBlushBoost) {
                effectTime -= dt
                if (effectTime <= 0) {
                    showHearts = false; showStars = false; showTears = false
                    showZzz = false; showExclamation = false; showNote = false; showBlushBoost = false
                }
            }

                        // 跳跃/小跳物理
            if (action == Action.HOPPING) {
                jumpHeight = abs(sin(animTime * 8f)) * 25f
                scaleX = 1f + sin(animTime * 8f) * 0.06f
                scaleY = 1f - sin(animTime * 8f) * 0.06f
                if (animTime > PI / 4f) { setAction(Action.IDLE); jumpHeight = 0f; scaleX = 1f; scaleY = 1f }
            }

            // 跳跃物理
            if (action == Action.JUMPING) {
                jumpHeight = sin(animTime * 6f) * 60f
                scaleX = 1f + cos(animTime * 6f) * 0.08f
                scaleY = 1f - cos(animTime * 6f) * 0.08f
                tiltAngle = if (jumpHeight > 0) 5f else -5f
                if (animTime > PI / 6f) {
                    setAction(Action.IDLE)
                    jumpHeight = 0f; scaleX = 1f; scaleY = 1f; tiltAngle = 0f
                }
            }

            // 攀爬
            if (isClimbing) {
                tiltAngle = cos(legPhase * 0.5f) * 3f
                climbProgress += dt * 0.3f * climbDirection
            }

            invalidate()
            isAnimating = true
            mainHandler.postDelayed(this, 16L)
        }
    }

    init {
        paintStroke.style = Paint.Style.STROKE
        startAnimation()
    }

    private fun startAnimation() {
        if (!isAnimating) {
            mainHandler.post(animRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
        mainHandler.removeCallbacks(animRunnable)
    }

    // ==================== 动作触发 ====================

    fun setExpression(expr: Expression) {
        expression = expr
        invalidate()
    }

    fun setAction(act: Action) {
        action = act
        animTime = 0f
        invalidate()
    }

    /** 触摸部位 -> 反应 */
    fun reactToPart(part: BodyPart) {
        when (part) {
            BodyPart.HEAD -> {
                expression = Expression.HAPPY
                setAction(Action.JUMPING)
                showHearts = true; effectTime = 1.5f
            }
            BodyPart.HORN_LEFT, BodyPart.HORN_RIGHT -> {
                expression = Expression.DIZZY
                setAction(Action.DIZZY_SPIN)
                showStars = true; effectTime = 2f
            }
            BodyPart.BODY -> {
                expression = Expression.CONTENT
                showHearts = true; showBlushBoost = true; effectTime = 2f
            }
            BodyPart.LEG_FL, BodyPart.LEG_FR, BodyPart.LEG_BL, BodyPart.LEG_BR -> {
                expression = Expression.SURPRISED
                showExclamation = true; effectTime = 1f
            }
            BodyPart.TAIL -> {
                expression = Expression.LAUGHING
                showNote = true; effectTime = 1.5f
            }
            BodyPart.NONE -> {}
        }
        invalidate()
    }

    // ==================== 触摸处理 ====================

    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var isDragging = false
    private var touchDownTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                dragStartRawX = rawX
                dragStartRawY = rawY
                isDragging = false
                onDragStart?.invoke()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - dragStartRawX
                val dy = rawY - dragStartRawY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 15f) {
                    isDragging = true
                    onDragMove?.invoke(dx, dy)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // 点击：判断点击的身体部位
                    val part = detectBodyPart(event.x, event.y)
                    reactToPart(part)
                    onPartTouched?.invoke(part)
                } else {
                    isDragging = false
                    onDragEnd?.invoke()
                }
            }
        }
        return true
    }

    /** 检测点击到哪个身体部位 */
    private fun detectBodyPart(x: Float, y: Float): BodyPart {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val s = min(w, h) / baseSize

        // 头部区域
        val headCX = cx; val headCY = cy - 70f * s
        if (dist(x, y, headCX, headCY) < 40f * s) {
            // 检查角
            if (dist(x, y, headCX - 32f * s, headCY - 40f * s) < 20f * s) return BodyPart.HORN_LEFT
            if (dist(x, y, headCX + 32f * s, headCY - 40f * s) < 20f * s) return BodyPart.HORN_RIGHT
            return BodyPart.HEAD
        }
        // 身体区域
        val bodyCX = cx; val bodyCY = cy + 20f * s
        if (dist(x, y, bodyCX, bodyCY) < 55f * s) return BodyPart.BODY
        // 腿
        if (dist(x, y, cx - 35f * s, cy + 70f * s) < 18f * s) return BodyPart.LEG_FL
        if (dist(x, y, cx + 35f * s, cy + 70f * s) < 18f * s) return BodyPart.LEG_FR
        if (dist(x, y, cx - 40f * s, cy + 65f * s) < 18f * s) return BodyPart.LEG_BL
        if (dist(x, y, cx + 40f * s, cy + 65f * s) < 18f * s) return BodyPart.LEG_BR
        // 尾巴
        if (dist(x, y, cx + 65f * s, cy + 10f * s) < 20f * s) return BodyPart.TAIL
        return BodyPart.NONE
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))

    companion object {
        private const val baseSize = 300f // 参考尺寸
    }

    // ==================== 绘制 ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.save()

        // 缩放和平移
        val s = min(w, h) / baseSize
        canvas.translate(w / 2f, h / 2f)
        canvas.scale(s * scaleX, s * scaleY)
        canvas.rotate(tiltAngle)
        canvas.translate(0f, -bounceOffset)

        // 跳跃偏移
        if (action == Action.JUMPING) canvas.translate(0f, -jumpHeight)
        // 攀爬偏移
        if (isClimbing) canvas.translate(sin(legPhase * 2f) * 5f, 0f)

        // ---- 绘制顺序（从后到前） ----
        drawTail(canvas)
        drawBackLegs(canvas)
        drawBody(canvas)
        drawFrontLegs(canvas)
        drawHead(canvas)
        drawEars(canvas)
        drawHorns(canvas)
        drawFace(canvas)

        // ---- 特效 ----
        drawEffects(canvas)

        canvas.restore()
    }

    /** 绘制身体（毛茸茸的椭圆形+绒毛） */
    private fun drawBody(canvas: Canvas) {
        paint.color = Color.parseColor("#FFF0E8") // 羊毛色
        paint.style = Paint.Style.FILL
        canvas.drawOval(-70f, -30f, 70f, 55f, paint)

        // 绒毛质感 - 多个小圆
        paint.color = Color.parseColor("#FFF5EE")
        for (i in 0 until 12) {
            val angle = i * (PI / 6)
            val rx = 60f * cos(angle).toFloat()
            val ry = 42f * sin(angle).toFloat()
            val r = 18f + sin(i * 3f + animTime * 2f) * 3f
            canvas.drawCircle(rx, ry, r, paint)
        }

        // 阴影
        paint.color = Color.parseColor("#E8D5C8")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawOval(-68f, -28f, 68f, 53f, paint)
        paint.style = Paint.Style.FILL
    }

    /** 头部 */
    private fun drawHead(canvas: Canvas) {
        paint.color = Color.parseColor("#FFF5F0")
        canvas.drawOval(-38f, -88f, 38f, -22f, paint)

        // 耳朵下的小绒毛
        paint.color = Color.parseColor("#FFF0E8")
        canvas.drawCircle(-42f, -60f, 15f, paint)
        canvas.drawCircle(42f, -60f, 15f, paint)
    }

    /** 耳朵 */
    private fun drawEars(canvas: Canvas) {
        paint.color = Color.parseColor("#FFE4D6")
        // 左耳（下垂）
        canvas.save(); canvas.rotate(-20f, -38f, -70f)
        canvas.drawOval(-50f, -80f, -26f, -54f, paint)
        canvas.restore()
        // 右耳（下垂）
        canvas.save(); canvas.rotate(20f, 38f, -70f)
        canvas.drawOval(26f, -80f, 50f, -54f, paint)
        canvas.restore()

        // 耳朵内侧粉色
        paint.color = Color.parseColor("#FFB5C8")
        canvas.save(); canvas.rotate(-20f, -38f, -70f)
        canvas.drawOval(-46f, -76f, -30f, -58f, paint)
        canvas.restore()
        canvas.save(); canvas.rotate(20f, 38f, -70f)
        canvas.drawOval(30f, -76f, 46f, -58f, paint)
        canvas.restore()
    }

    /** 角 - 螺旋弯曲 */
    private fun drawHorns(canvas: Canvas) {
        paint.color = Color.parseColor("#8B7355")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.strokeCap = Paint.Cap.ROUND

        // 左角
        path.reset(); path.moveTo(-24f, -82f)
        path.cubicTo(-38f, -98f, -50f, -104f, -42f, -112f)
        path.cubicTo(-34f, -120f, -18f, -116f, -20f, -106f)
        canvas.drawPath(path, paint)

        // 右角
        path.reset(); path.moveTo(24f, -82f)
        path.cubicTo(38f, -98f, 50f, -104f, 42f, -112f)
        path.cubicTo(34f, -120f, 18f, -116f, 20f, -106f)
        canvas.drawPath(path, paint)

        // 角上纹路
        paint.color = Color.parseColor("#6B5335")
        paint.strokeWidth = 3f
        for (i in 0..2) {
            val t = i * 0.3f + 0.1f
            val lx = lerp(-24f, -20f, t); val ly = lerp(-82f, -106f, t)
            canvas.drawLine(lx, ly, lx + 6f, ly - 4f, paint)
            val rx = lerp(24f, 20f, t); val ry = lerp(-82f, -106f, t)
            canvas.drawLine(rx, ry, rx - 6f, ry - 4f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
    }

    /** 脸部：眼睛、鼻子、嘴巴、腮红 */
    private fun drawFace(canvas: Canvas) {
        // ---- 眼睛 ----
        val eyeY = -63f
        paint.color = Color.parseColor("#2C2C2C")
        paint.style = Paint.Style.FILL

        when (expression) {
            Expression.HAPPY -> {
                paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
                canvas.drawArc(-22f, -68f, -10f, -58f, 0f, -180f, false, paint)
                canvas.drawArc(10f, -68f, 22f, -58f, 0f, -180f, false, paint)
                paint.style = Paint.Style.FILL
            }
            Expression.SAD -> {
                canvas.drawCircle(-16f, -63f, 6f, paint); canvas.drawCircle(16f, -63f, 6f, paint)
                paint.color = Color.parseColor("#87CEEB")
                canvas.drawOval(-20f, -56f, -12f, -48f, paint); canvas.drawOval(12f, -56f, 20f, -48f, paint)
                paint.color = Color.parseColor("#2C2C2C")
            }
            Expression.ANGRY -> {
                paint.strokeWidth = 4f; paint.style = Paint.Style.STROKE
                canvas.drawLine(-28f, -72f, -14f, -66f, paint); canvas.drawLine(28f, -72f, 14f, -66f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(-16f, -62f, 5f, paint); canvas.drawCircle(16f, -62f, 5f, paint)
            }
            Expression.SURPRISED -> {
                canvas.drawCircle(-16f, -63f, 8f, paint); canvas.drawCircle(16f, -63f, 8f, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(-14f, -65f, 3f, paint); canvas.drawCircle(18f, -65f, 3f, paint)
                paint.color = Color.parseColor("#2C2C2C")
            }
            Expression.SLEEPY -> {
                paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
                canvas.drawLine(-20f, -63f, -8f, -63f, paint); canvas.drawLine(8f, -63f, 20f, -63f, paint)
                paint.style = Paint.Style.FILL
            }
            Expression.DIZZY -> {
                paint.strokeWidth = 2f; paint.style = Paint.Style.STROKE
                canvas.drawCircle(-16f, -63f, 7f, paint); canvas.drawArc(-23f, -70f, -9f, -56f, 0f, 360f, false, paint)
                canvas.drawCircle(16f, -63f, 7f, paint); canvas.drawArc(9f, -70f, 23f, -56f, 0f, 360f, false, paint)
                paint.style = Paint.Style.FILL
            }
            Expression.INNOCENT -> {
                canvas.drawCircle(-16f, -64f, 8f, paint); canvas.drawCircle(16f, -64f, 8f, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(-14f, -66f, 4f, paint); canvas.drawCircle(18f, -66f, 4f, paint)
                paint.color = Color.parseColor("#2C2C2C")
            }
            Expression.CONTENT, Expression.LAUGHING -> {
                paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
                path.reset(); path.moveTo(-22f, -63f); path.quadTo(-16f, -59f, -10f, -63f); canvas.drawPath(path, paint)
                path.reset(); path.moveTo(10f, -63f); path.quadTo(16f, -59f, 22f, -63f); canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // 眼睛高光
        if (expression != Expression.HAPPY && expression != Expression.SLEEPY &&
            expression != Expression.CONTENT && expression != Expression.LAUGHING) {
            paint.color = Color.WHITE
            canvas.drawCircle(-14f, -65f, 2.5f, paint); canvas.drawCircle(18f, -65f, 2.5f, paint)
        }

        // ---- 鼻子 ----
        paint.color = Color.parseColor("#FF8FAB"); paint.style = Paint.Style.FILL
        canvas.drawOval(-5f, -52f, 5f, -46f, paint)

        // ---- 嘴巴 ----
        paint.color = Color.parseColor("#D4596B"); paint.strokeWidth = 2.5f; paint.style = Paint.Style.STROKE
        when (expression) {
            Expression.HAPPY, Expression.LAUGHING, Expression.CONTENT -> {
                path.reset(); path.moveTo(-12f, -42f); path.quadTo(0f, -36f, 12f, -42f); canvas.drawPath(path, paint)
                if (expression == Expression.LAUGHING) {
                    paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#D4596B")
                    canvas.drawOval(-6f, -40f, 6f, -32f, paint)
                    paint.color = Color.parseColor("#FF8FAB"); canvas.drawOval(-4f, -38f, 4f, -34f, paint)
                }
            }
            Expression.SAD -> { path.reset(); path.moveTo(-12f, -40f); path.quadTo(0f, -46f, 12f, -40f); canvas.drawPath(path, paint) }
            Expression.ANGRY -> {
                path.reset(); path.moveTo(-12f, -42f); path.lineTo(-6f, -44f); path.lineTo(0f, -42f)
                path.lineTo(6f, -44f); path.lineTo(12f, -42f); canvas.drawPath(path, paint)
            }
            Expression.SURPRISED -> {
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#D4596B")
                canvas.drawOval(-5f, -40f, 5f, -30f, paint)
                paint.color = Color.parseColor("#FF8FAB"); canvas.drawOval(-3f, -38f, 3f, -32f, paint)
            }
            Expression.DIZZY -> { path.reset(); path.moveTo(-12f, -40f); path.quadTo(-6f, -44f, 0f, -40f); path.quadTo(6f, -36f, 12f, -40f); canvas.drawPath(path, paint) }
            Expression.INNOCENT -> { canvas.drawCircle(0f, -40f, 3f, paint) }
            Expression.SLEEPY -> { canvas.drawArc(-3f, -42f, 3f, -38f, 0f, -180f, false, paint) }
        }
        paint.style = Paint.Style.FILL

        // ---- 腮红 ----
        val blushAlpha = if (showBlushBoost) 120 else 60
        paint.color = Color.argb(blushAlpha, 255, 181, 200)
        canvas.drawCircle(-26f, -46f, 12f, paint); canvas.drawCircle(26f, -46f, 12f, paint)
        paint.alpha = 255
    }

    /** 前腿 */
    private fun drawFrontLegs(canvas: Canvas) {
        paint.color = Color.parseColor("#FFDBB8")
        val lOff = sin(legPhase + PI.toFloat()) * 6f
        val rOff = sin(legPhase) * 6f
        canvas.save(); canvas.rotate(lOff * 0.3f, -30f, 40f)
        canvas.drawRoundRect(-37f, 40f, -23f, 72f, 6f, 6f, paint)
        canvas.restore()
        canvas.save(); canvas.rotate(rOff * 0.3f, 30f, 40f)
        canvas.drawRoundRect(23f, 40f, 37f, 72f, 6f, 6f, paint)
        canvas.restore()
        paint.color = Color.parseColor("#D4A88C")
        canvas.drawRoundRect(-37f, 64f, -23f, 72f, 4f, 4f, paint)
        canvas.drawRoundRect(23f, 64f, 37f, 72f, 4f, 4f, paint)
    }

    /** 后腿 */
    private fun drawBackLegs(canvas: Canvas) {
        paint.color = Color.parseColor("#FFDBB8")
        val lOff = sin(legPhase) * 6f
        val rOff = sin(legPhase + PI.toFloat()) * 6f
        canvas.save(); canvas.rotate(-lOff * 0.3f, -35f, 35f)
        canvas.drawRoundRect(-42f, 38f, -28f, 68f, 6f, 6f, paint)
        canvas.restore()
        canvas.save(); canvas.rotate(-rOff * 0.3f, 35f, 35f)
        canvas.drawRoundRect(28f, 38f, 42f, 68f, 6f, 6f, paint)
        canvas.restore()
        paint.color = Color.parseColor("#D4A88C")
        canvas.drawRoundRect(-42f, 60f, -28f, 68f, 4f, 4f, paint)
        canvas.drawRoundRect(28f, 60f, 42f, 68f, 4f, 4f, paint)
    }

    /** 尾巴 */
    private fun drawTail(canvas: Canvas) {
        paint.color = Color.parseColor("#FFF0E8")
        val wag = if (expression == Expression.HAPPY || expression == Expression.LAUGHING) sin(animTime * 8f) * 6f else 0f
        canvas.save(); canvas.rotate(wag, 72f, 10f)
        canvas.drawCircle(72f, 12f, 16f, paint)
        paint.color = Color.parseColor("#FFF5EE")
        canvas.drawCircle(74f, 8f, 10f, paint); canvas.drawCircle(70f, 16f, 10f, paint)
        canvas.restore()
    }

    // ==================== 特效绘制 ====================

    private fun drawEffects(canvas: Canvas) {
        if (showHearts) {
            paint.color = Color.parseColor("#FF69B4"); paint.textSize = 28f
            for (i in 0 until 3) canvas.drawText("♥", -20f + i * 20f, -100f - 15f * i + sin(animTime * 4f + i) * 5f, paint)
        }
        if (showStars) {
            paint.color = Color.parseColor("#FFD700"); paint.textSize = 24f
            for (i in 0 until 4) canvas.drawText("✦", -30f + i * 20f, -110f + sin(animTime * 3f + i * 2f) * 8f, paint)
        }
        if (showZzz) {
            paint.color = Color.parseColor("#90CAF9"); paint.textSize = 22f
            for (i in 0 until 3) canvas.drawText("z", 50f, -90f - i * 20f + sin(animTime * 2f + i) * 3f, paint)
        }
        if (showExclamation) {
            paint.color = Color.parseColor("#FF4444"); paint.textSize = 32f
            canvas.drawText("!", -5f, -100f + sin(animTime * 5f) * 3f, paint)
        }
        if (showNote) {
            paint.color = Color.parseColor("#9C27B0"); paint.textSize = 24f
            canvas.drawText("♪", 50f, -80f + sin(animTime * 6f) * 4f, paint)
            canvas.drawText("♫", 60f, -70f + cos(animTime * 5f) * 3f, paint)
        }
        if (isClimbing) {
            paint.color = Color.argb(60, 200, 200, 200); paint.strokeWidth = 2f; paint.style = Paint.Style.STROKE
            for (i in 0..3) {
                val yOff = -30f + i * 25f
                canvas.drawLine(-75f, yOff, -65f + sin(legPhase * 2f + i) * 5f, yOff, paint)
                canvas.drawLine(65f + sin(legPhase * 2f + i + 2f) * 5f, yOff, 75f, yOff, paint)
            }
            paint.style = Paint.Style.FILL
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}


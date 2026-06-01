package com.chongwu.pet.interaction

/**
 * 触摸交互引擎 - 身体部位检测 + 手势识别
 */
class TouchEngine {
    
    enum class BodyPart {
        NONE, HEAD, HORN_LEFT, HORN_RIGHT, BODY, LEG, TAIL, EAR_LEFT, EAR_RIGHT, EYE
    }
    
    // 屏幕坐标上的身体部位边界（归一化0-1）
    data class PartRegion(
        val part: BodyPart,
        val cx: Float, val cy: Float,  // 中心 0-1
        val radius: Float  // 检测半径
    )
    
    private val regions = listOf(
        PartRegion(BodyPart.HEAD, 0.5f, 0.35f, 0.12f),
        PartRegion(BodyPart.HORN_LEFT, 0.42f, 0.22f, 0.07f),
        PartRegion(BodyPart.HORN_RIGHT, 0.58f, 0.22f, 0.07f),
        PartRegion(BodyPart.BODY, 0.5f, 0.55f, 0.2f),
        PartRegion(BodyPart.LEG, 0.5f, 0.8f, 0.08f),
        PartRegion(BodyPart.TAIL, 0.5f, 0.88f, 0.06f),
        PartRegion(BodyPart.EAR_LEFT, 0.35f, 0.3f, 0.05f),
        PartRegion(BodyPart.EAR_RIGHT, 0.65f, 0.3f, 0.05f),
        PartRegion(BodyPart.EYE, 0.5f, 0.32f, 0.04f)
    )
    
    // 手势状态
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var lastTapTime = 0f
    private var lastTapX = 0f
    private var lastTapY = 0f
    
    // 反馈参数
    var vibrationIntensity = 0f
    var shakeIntensity = 0f
    
    fun detectPart(screenX: Float, screenY: Float, viewW: Float, viewH: Float): BodyPart? {
        val nx = screenX / viewW
        val ny = screenY / viewH
        
        // 找出最近的部位
        var closestDist = Float.MAX_VALUE
        var closestPart: BodyPart? = null
        
        for (region in regions) {
            val dx = nx - region.cx
            val dy = ny - region.cy
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < region.radius && dist < closestDist) {
                closestDist = dist
                closestPart = region.part
            }
        }
        
        return closestPart
    }
    
    fun handleTap(x: Float, y: Float, viewW: Float, viewH: Float, timeStamp: Float): TapResult {
        val part = detectPart(x, y, viewW, viewH) ?: return TapResult(TapType.MISS, BodyPart.NONE)
        
        // 检测双击
        val doubleTap = if (timeStamp - lastTapTime < 0.4f && 
            kotlin.math.abs(x - lastTapX) < viewW * 0.1f &&
            kotlin.math.abs(y - lastTapY) < viewH * 0.1f) true else false
        
        lastTapTime = timeStamp
        lastTapX = x
        lastTapY = y
        
        return if (doubleTap) {
            TapResult(TapType.DOUBLE_TAP, part)
        } else {
            TapResult(TapType.TAP, part)
        }
    }
    
    fun handleDragStart(x: Float, y: Float) {
        isDragging = true
        dragStartX = x
        dragStartY = y
    }
    
    fun handleDragMove(x: Float, y: Float): DragInfo {
        val dx = x - dragStartX
        val dy = y - dragStartY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        return DragInfo(dx, dy, dist, isDragging)
    }
    
    fun handleDragEnd() {
        isDragging = false
    }
    
    fun getTouchFeedback(part: BodyPart): TouchFeedback {
        return when (part) {
            BodyPart.HEAD -> TouchFeedback(0.3f, "😊", "hearts")
            BodyPart.HORN_LEFT, BodyPart.HORN_RIGHT -> TouchFeedback(0.5f, "✨", "stars")
            BodyPart.BODY -> TouchFeedback(0.2f, "😊", "blush")
            BodyPart.LEG -> TouchFeedback(0.3f, "!!", "exclamation")
            BodyPart.TAIL -> TouchFeedback(0.6f, "🎵", "notes")
            BodyPart.EAR_LEFT, BodyPart.EAR_RIGHT -> TouchFeedback(0.2f, "~", "hearts")
            BodyPart.EYE -> TouchFeedback(0.4f, "👀", "stars")
            BodyPart.NONE -> TouchFeedback(0f, "", "")
        }
    }
    
    data class TapResult(val type: TapType, val part: BodyPart)
    data class DragInfo(val dx: Float, val dy: Float, val distance: Float, val isDragging: Boolean)
    data class TouchFeedback(val intensity: Float, val emoji: String, val effect: String)
    
    enum class TapType { TAP, DOUBLE_TAP, MISS }
}

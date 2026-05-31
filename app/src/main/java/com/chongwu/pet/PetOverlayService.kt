package com.chongwu.pet

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

/**
 * 桌面宠物悬浮窗服务
 *
 * 创建透明悬浮窗 → 显示 SheepView → 通过回调处理拖拽和攀爬。
 */
class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private lateinit var sheepView: SheepView
    private var layoutParams: WindowManager.LayoutParams? = null

    private val screenWidth: Int get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.x
    }
    private val screenHeight: Int get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.y
    }

    private val overlaySize = 240 // dp

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "chongwu_overlay"
        const val ACTION_STOP = "com.chongwu.pet.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlayView.parent == null) showOverlay()
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY_COMPATIBILITY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() { removeOverlay(); super.onDestroy() }

    // ==================== 悬浮窗管理 ====================

    private fun showOverlay() {
        val density = resources.displayMetrics.density
        val sizePx = (overlaySize * density).toInt()

        overlayView = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = true
            isFocusable = false
        }

        // 创建小羊并绑定拖拽回调
        sheepView = SheepView(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

            onDragStart = { isDragging = false; isEdgeClimbing = false }
            onDragMove = { dx, dy -> handleDragDelta(dx, dy) }
            onDragEnd = { finishDrag() }
        }
        overlayView.addView(sheepView)

        layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - sizePx) / 2
            y = (screenHeight - sizePx) / 3
        }

        try { windowManager.addView(overlayView, layoutParams) }
        catch (_: Exception) { stopSelf() }
    }

    private fun removeOverlay() {
        try { if (overlayView.parent != null) windowManager.removeView(overlayView) }
        catch (_: Exception) {}
    }

    // ==================== 拖拽 & 攀爬 ====================

    private var isDragging = false
    private var isEdgeClimbing = false
    private var lastDeltaX = 0f
    private var lastDeltaY = 0f

    private fun handleDragDelta(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        val dist = kotlin.math.abs(dx.toInt()) + kotlin.math.abs(dy.toInt())
        if (dist > 15) isDragging = true

        if (!isEdgeClimbing) {
            params.x = (params.x + (dx - lastDeltaX).toInt())
                .coerceIn(-overlaySize / 3, screenWidth - overlaySize / 3)
            params.y = (params.y + (dy - lastDeltaY).toInt())
                .coerceIn(-overlaySize / 3, screenHeight - overlaySize / 3)

            // 检测贴边 → 进入攀爬模式
            val threshold = 80
            val nearEdge = params.x < threshold || params.x > screenWidth - overlaySize - threshold ||
                    params.y < threshold || params.y > screenHeight - overlaySize - threshold
            if (nearEdge && dist > 30) {
                isEdgeClimbing = true
                sheepView.isClimbing = true
                sheepView.setAction(SheepView.Action.CLIMBING)
            }
        } else {
            climbAlongEdge(params, (dx - lastDeltaX).toInt(), (dy - lastDeltaY).toInt())
        }

        lastDeltaX = dx; lastDeltaY = dy

        try { windowManager.updateViewLayout(overlayView, params) }
        catch (_: Exception) {}
    }

    private fun climbAlongEdge(params: WindowManager.LayoutParams, dx: Int, dy: Int) {
        val edgeThreshold = 80
        when {
            params.y < edgeThreshold -> params.x += dx
            params.y > screenHeight - overlaySize - edgeThreshold -> params.x += dx
            else -> params.y += dy
        }
        params.x = params.x.coerceIn(-overlaySize / 2, screenWidth - overlaySize / 2)
        params.y = params.y.coerceIn(-overlaySize / 2, screenHeight - overlaySize / 2)
    }

    private fun finishDrag() {
        if (isEdgeClimbing) {
            sheepView.isClimbing = false
            sheepView.setAction(SheepView.Action.IDLE)
            isEdgeClimbing = false
        }
        if (isDragging) snapToEdge()
        isDragging = false
        lastDeltaX = 0f; lastDeltaY = 0f
    }

    /** 松手 → 吸附到最近边缘 */
    private fun snapToEdge() {
        val params = layoutParams ?: return
        val threshold = 120

        if (params.y < threshold) params.y = 0
        else if (params.y > screenHeight - overlaySize - threshold) params.y = screenHeight - overlaySize

        if (params.x < threshold) params.x = 0
        else if (params.x > screenWidth - overlaySize - threshold) params.x = screenWidth - overlaySize

        try { windowManager.updateViewLayout(overlayView, params) }
        catch (_: Exception) {}
    }

    // ==================== 通知 ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.overlay_channel_desc); setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPendingIntent)
            .build()
    }
}

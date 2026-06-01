package com.chongwu.pet

import android.app.*
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
 * 全屏悬浮窗服务 - 3D宠物在整屏活动
 */
class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private lateinit var glSheepView: GLSheepView
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        const val NOTIFICATION_ID = 1001; const val CHANNEL_ID = "chongwu_overlay"
        const val ACTION_STOP = "com.chongwu.pet.STOP"
    }

    override fun onCreate() { super.onCreate(); windowManager = getSystemService(WINDOW_SERVICE) as WindowManager; createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (overlayView == null) showOverlay()
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY_COMPATIBILITY
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { 
        if (::glSheepView.isInitialized) glSheepView.cleanup()
        removeOverlay(); super.onDestroy() 
    }

    private fun showOverlay() {
        val frame = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = true; isFocusable = false
        }

        glSheepView = GLSheepView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            onDragStart = { isDragging = false }
            onDragMove = { dx, dy -> handleDragDelta(dx, dy) }
            onDragEnd = { finishDrag() }
        }
        frame.addView(glSheepView)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 0; width = WindowManager.LayoutParams.MATCH_PARENT; height = WindowManager.LayoutParams.MATCH_PARENT }

        overlayView = frame
        try { windowManager.addView(frame, layoutParams) } catch (_: Exception) { stopSelf() }
    }

    private fun removeOverlay() { overlayView?.let { try { windowManager.removeView(it) } catch(_:Exception){} }; overlayView = null }

    // ===== 拖拽 =====
    private var isDragging = false
    private var startX = 0; private var startY = 0
    private var lastDeltaX = 0f; private var lastDeltaY = 0f

    private fun handleDragDelta(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        if (!isDragging) { isDragging = true; startX = params.x; startY = params.y }
        params.x = startX + (dx - lastDeltaX).toInt()
        params.y = startY + (dy - lastDeltaY).toInt()
        lastDeltaX = dx; lastDeltaY = dy
        try { overlayView?.let { windowManager.updateViewLayout(it, params) } } catch(_:Exception){}
    }

    private fun finishDrag() { isDragging = false; lastDeltaX=0f; lastDeltaY=0f }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW)
            ch.description = getString(R.string.overlay_channel_desc); ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification_text)).setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPi).build()
    }
}

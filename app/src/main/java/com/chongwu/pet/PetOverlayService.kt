package com.chongwu.pet

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

/**
 * 全屏悬浮窗服务 - 使用GLPetView渲染3D宠物
 */
class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private lateinit var glPetView: GLPetView
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        const val NOTIFICATION_ID = 1001; const val CHANNEL_ID = "chongwu_overlay"
        const val ACTION_STOP = "com.chongwu.pet.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (overlayView == null) showOverlay()
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY_COMPATIBILITY
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        if (::glPetView.isInitialized) glPetView.cleanup()
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        val frame = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = true; isFocusable = false
        }

        glPetView = GLPetView(this, PetType.SHEEP).apply {
            layoutParams = FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setOverlayService(this@PetOverlayService)
        }
        frame.addView(glPetView)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 0
        }

        overlayView = frame
        try { windowManager.addView(frame, layoutParams) } catch (_: Exception) { stopSelf() }
    }

    private fun removeOverlay() {
        overlayView?.let { try { windowManager.removeView(it) } catch(_:Exception){} }
        overlayView = null
    }

    /** 设置悬浮窗透明度（被GLPetView调用） */
    fun setOverlayAlpha(alpha: Float) {
        layoutParams?.alpha = alpha
        overlayView?.let {
            try { windowManager.updateViewLayout(it, layoutParams) } catch(_:Exception){}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW)
            ch.description = getString(R.string.overlay_channel_desc)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val stopPi = PendingIntent.getService(this, 0, stopIntent, flags)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPi)
            .build()
    }
}

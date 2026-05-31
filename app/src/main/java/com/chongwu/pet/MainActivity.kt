package com.chongwu.pet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * 主入口 Activity
 *
 * 引导用户授予悬浮窗权限 → 启动 PetOverlayService。
 * 后续可扩展为设置界面（调节大小、切换宠物等）。
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST = 100
    }

    private lateinit var statusText: TextView
    private lateinit var actionBtn: Button
    private lateinit var hintText: TextView
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        actionBtn = findViewById(R.id.actionBtn)
        hintText = findViewById(R.id.hintText)

        actionBtn.setOnClickListener {
            if (isRunning) {
                stopPet()
            } else {
                startPet()
            }
        }

        // 引导页帮助文字
        hintText.text = """
            🌟 欢迎来到咩咩宠物世界！
            
            咩咩是一只可爱的桌面小羊，它会：
            • 在屏幕上游走玩耍 🐑
            • 触摸不同部位有不同的反应 ✨
            • 在屏幕边缘攀爬 🧗
            • 等你来发现更多彩蛋！
            
            点击下方按钮，释放咩咩吧！
        """.trimIndent()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            updateStatus()
        }
    }

    private fun updateStatus() {
        isRunning = isServiceRunning(PetOverlayService::class.java)
        if (isRunning) {
            statusText.text = "🐑 咩咩正在屏幕上游玩~"
            actionBtn.text = getString(R.string.stop_service)
            hintText.text = "咩咩正在后台陪伴你！\n拖拽它到屏幕边缘，它就会攀爬起来~"
        } else {
            statusText.text = "😴 咩咩在睡觉..."
            actionBtn.text = getString(R.string.start_service)
        }
    }

    private fun startPet() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showPermissionDialog()
            return
        }

        // 请求忽略电池优化（防止后台被杀）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
                return
            }
        }

        doStartService()
    }

    private fun doStartService() {
        val intent = Intent(this, PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isRunning = true
        updateStatus()
        Toast.makeText(this, "咩咩来了！🐑", Toast.LENGTH_SHORT).show()
    }

    private fun stopPet() {
        stopService(Intent(this, PetOverlayService::class.java))
        isRunning = false
        updateStatus()
        Toast.makeText(this, "咩咩去睡觉了~", Toast.LENGTH_SHORT).show()
    }

    // ==================== 权限引导对话框 ====================

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage(getString(R.string.permission_overlay))
            .setPositiveButton("去授权") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("建议关闭电池优化")
            .setMessage("为了咩咩能在后台一直陪伴你，建议关闭电池优化。\n（不同手机设置位置不同）")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("跳过", null)
            .show()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }
}

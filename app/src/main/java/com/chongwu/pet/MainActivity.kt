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
 * 主入口 - 3D桌面宠物启动器
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

        hintText.text = """
            🌟 花影小羚羊 · 3D桌面宠物
            
            一只会走路、吃草、玩耍的小羊！
            • 🐏 3D Q版立体造型 + 花瓣纹样
            • 🌸 蝴蝶飞舞、草地摇曳的小世界
            • ☀️ 昼夜交替 + 天气变化
            • 👆 触摸身体部位有不同反应
            • 🎵 程序化音效（咩咩叫、蹦跳声）
            • 💤 长时间不互动自动小憩
        """.trimIndent()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        isRunning = isServiceRunning(PetOverlayService::class.java)
        if (isRunning) {
            statusText.text = "🦋 小羚羊正在屏幕上游玩~"
            actionBtn.text = getString(R.string.stop_service)
        } else {
            statusText.text = "🌙 花影小羚羊在休息..."
            actionBtn.text = getString(R.string.start_service)
        }
    }

    private fun startPet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showPermissionDialog()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryDialog()
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
        Toast.makeText(this, "小羚羊来了！🦋", Toast.LENGTH_SHORT).show()
    }

    private fun stopPet() {
        stopService(Intent(this, PetOverlayService::class.java))
        isRunning = false
        updateStatus()
        Toast.makeText(this, "小羚羊去休息了~", Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage(getString(R.string.permission_overlay))
            .setPositiveButton("去授权") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showBatteryDialog() {
        AlertDialog.Builder(this)
            .setTitle("建议关闭电池优化")
            .setMessage("为了让小羚羊在后台陪伴你，建议关闭电池优化。")
            .setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
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

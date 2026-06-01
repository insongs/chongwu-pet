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
import com.chongwu.pet.audio.AudioEngine

/**
 * 主入口 Activity - 3D花影羚羊宠物桌面
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST = 100
    }

    private lateinit var statusText: TextView
    private lateinit var actionBtn: Button
    private lateinit var hintText: TextView
    private var isRunning = false
    private var audioEngine: AudioEngine? = null

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

        // 初始化音效引擎
        audioEngine = AudioEngine(this)
        audioEngine?.init()

        hintText.text = """
            🌟 花影羚羊 · 3D桌面宠物
            
            一只立体的花影小羚羊，它会：
            • 🐏 在屏幕上自由漫步、吃草、蹦跳
            • 🌸 身带花瓣纹样，随季节变化
            • ☀️ 昼夜交替、天气变化影响行为
            • 👆 触摸不同部位有不同的反应
            • 🦋 蝴蝶飞舞、草地生长的小生态
            
            点击下方按钮，让小羚羊来到你的桌面吧！
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
            statusText.text = "🦋 花影羚羊正在屏幕上游玩~"
            actionBtn.text = getString(R.string.stop_service)
            hintText.text = "小羚羊正在后台陪伴你！\n触摸它的不同部位，看看有什么反应~"
        } else {
            statusText.text = "🌙 花影羚羊在休息..."
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
        audioEngine?.playHappy()
        Toast.makeText(this, "花影羚羊来了！🦋", Toast.LENGTH_SHORT).show()
    }

    private fun stopPet() {
        stopService(Intent(this, PetOverlayService::class.java))
        isRunning = false
        updateStatus()
        Toast.makeText(this, "花影羚羊去休息了~", Toast.LENGTH_SHORT).show()
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

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("建议关闭电池优化")
            .setMessage("为了让小羚羊能在后台一直陪伴你，建议关闭电池优化。")
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

    override fun onDestroy() {
        audioEngine?.release()
        super.onDestroy()
    }
}

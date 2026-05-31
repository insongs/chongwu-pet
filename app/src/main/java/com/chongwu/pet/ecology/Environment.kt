package com.chongwu.pet.ecology

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import kotlin.math.*
import kotlin.random.Random

/**
 * 生态感知系统 - 让小羊感知周围环境
 */
class Environment(private val context: Context) {

    // ==================== 时间 ====================
    data class TimeInfo(
        val hour: Int,
        val minute: Int,
        val isNight: Boolean,       // 22:00-5:00
        val isMorning: Boolean,     // 5:00-8:00
        val isNoon: Boolean,        // 11:00-14:00
        val isEvening: Boolean      // 17:00-20:00
    )

    fun getTimeInfo(): TimeInfo {
        val cal = java.util.Calendar.getInstance()
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        return TimeInfo(
            hour = h, minute = m,
            isNight = h >= 22 || h < 5,
            isMorning = h in 5..7,
            isNoon = h in 11..13,
            isEvening = h in 17..19
        )
    }

    // ==================== 天气 ====================
    enum class Weather(val label: String) {
        SUNNY("☀️"), CLOUDY("☁️"), RAINY("🌧️"), SNOWY("❄️"), WINDY("🌬️")
    }

    private var currentWeather: Weather = Weather.SUNNY
    private var weatherTimer: Float = 0f
    private var weatherDuration: Float = 120f + Random.nextFloat() * 300f  // 2-7分钟换一次

    fun getWeather(): Weather = currentWeather

    fun updateWeather(dt: Float) {
        weatherTimer += dt
        if (weatherTimer > weatherDuration) {
            weatherTimer = 0f
            weatherDuration = 120f + Random.nextFloat() * 300f
            // 根据季节概率不同，暂简单随机
            currentWeather = when (Random.nextInt(10)) {
                0 -> Weather.RAINY
                1 -> Weather.SNOWY
                2 -> Weather.WINDY
                3, 4 -> Weather.CLOUDY
                else -> Weather.SUNNY
            }
        }
    }

    // ==================== 屏幕信息 ====================
    data class ScreenInfo(
        val width: Int,
        val height: Int,
        val density: Float
    )

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun getScreenInfo(): ScreenInfo {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return ScreenInfo(
            width = point.x,
            height = point.y,
            density = context.resources.displayMetrics.density
        )
    }

    // ==================== 草地系统 ====================
    data class GrassBlade(
        var x: Float,
        var height: Float,
        var maxHeight: Float,
        var isEaten: Boolean = false,
        var regrowTimer: Float = 0f,
        var swayPhase: Float = Random.nextFloat() * PI.toFloat()
    )

    private val grassPatch = mutableListOf<GrassBlade>()
    private var grassInitialized = false
    val groundY: Float get() = getScreenInfo().height * 0.78f  // 地面在屏幕 78% 位置

    fun getGrass(): List<GrassBlade> {
        if (!grassInitialized) initGrass()
        return grassPatch
    }

    private fun initGrass() {
        val screen = getScreenInfo()
        val count = (screen.width / 12).coerceIn(20, 80)
        grassPatch.clear()
        for (i in 0 until count) {
            val mh = 20f + Random.nextFloat() * 35f
            grassPatch.add(GrassBlade(
                x = i * (screen.width.toFloat() / count),
                height = mh,
                maxHeight = mh
            ))
        }
        grassInitialized = true
    }

    fun updateGrass(dt: Float, isRaining: Boolean) {
        val growRate = if (isRaining) 8f else 2f
        for (blade in grassPatch) {
            blade.swayPhase += dt * 2f
            if (blade.isEaten) {
                blade.regrowTimer += dt * growRate
                if (blade.regrowTimer > 30f) {
                    blade.isEaten = false
                    blade.height = blade.maxHeight
                    blade.regrowTimer = 0f
                }
            }
        }
    }

    fun eatGrassAt(x: Float) {
        grassPatch.minByOrNull { abs(it.x - x) }?.let { blade ->
            if (!blade.isEaten) {
                blade.isEaten = true
                blade.height = 2f
            }
        }
    }

    // ==================== 图标检测 (简化) ====================
    data class AppIconInfo(
        val x: Float,
        val y: Float,
        val size: Float
    )

    // 简化的图标位置检测 - 实际可以用 AccessibilityService
    fun getPotentialIconPositions(): List<AppIconInfo> {
        val screen = getScreenInfo()
        val iconSize = 80f * screen.density
        val icons = mutableListOf<AppIconInfo>()
        // 模拟底部两行图标
        for (row in 0..1) {
            for (col in 0..3) {
                icons.add(AppIconInfo(
                    x = screen.width * 0.15f + col * screen.width * 0.2f,
                    y = screen.height - (1 - row) * iconSize * 1.5f - iconSize * 2,
                    size = iconSize
                ))
            }
        }
        return icons
    }
}

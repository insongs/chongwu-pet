package com.chongwu.pet.ecology

import android.content.Context
import kotlin.math.*

/**
 * 生态模拟系统 - 天气、昼夜、草地、花朵
 */
class Environment(private val context: Context) {
    
    enum class Weather(val displayName: String) {
        SUNNY("晴天"),
        CLOUDY("多云"),
        RAINY("下雨"),
        SNOWY("下雪"),
        WINDY("刮风"),
        STORMY("雷暴"),
        FOGGY("起雾")
    }
    
    // 时间
    private var timeOfDay = 6f  // 6:00 AM
    var daySpeed = 0.02f  // 每帧时间增量
    
    // 天气
    private var currentWeather = Weather.SUNNY
    private var weatherTimer = 0f
    private var weatherDuration = 60f  // 持续帧数
    private var transitionProgress = 0f  // 0-1 天气过渡
    
    // 草地系统
    private val grassBlades = mutableListOf<GrassBlade>()
    var grassGrowthRate = 0.01f
    var maxGrassCount = 200
    var groundY = 0f
    
    // 花朵
    private val flowers = mutableListOf<Flower>()
    
    // 蝴蝶
    private val butterflies = mutableListOf<Butterfly>()
    
    // 环境特效
    var fogDensity = 0f
    var windStrength = 0f
    var rainIntensity = 0f
    var snowIntensity = 0f
    
    data class GrassBlade(
        var x: Float, var height: Float, var maxHeight: Float,
        var swayPhase: Float = Math.random().toFloat() * 6.28f,
        var isEaten: Boolean = false, var regrowTimer: Float = 0f,
        var colorR: Float = 0.3f, var colorG: Float = 0.7f, var colorB: Float = 0.2f
    )
    
    data class Flower(
        var x: Float, var z: Float, var size: Float = 0.05f,
        var colorR: Float, var colorG: Float, var colorB: Float,
        var swayPhase: Float = Math.random().toFloat() * 6.28f,
        var isWilted: Boolean = false
    )
    
    data class Butterfly(
        var x: Float, var y: Float, var z: Float,
        var targetX: Float, var targetY: Float, var targetZ: Float,
        var speed: Float = 0.5f + Math.random().toFloat() * 0.5f,
        var phase: Float = Math.random().toFloat() * 6.28f,
        var colorR: Float, var colorG: Float, var colorB: Float,
        var isActive: Boolean = true
    )
    
    fun initGrass(screenW: Float, groundY: Float) {
        this.groundY = groundY
        grassBlades.clear()
        val spacing = screenW / maxGrassCount
        for (i in 0 until maxGrassCount) {
            val height = 0.05f + Math.random().toFloat() * 0.12f
            grassBlades.add(GrassBlade(
                x = i * spacing + Math.random().toFloat() * spacing * 0.5f,
                height = height,
                maxHeight = height * (1f + Math.random().toFloat() * 0.5f)
            ))
        }
        
        // 初始化花朵
        flowers.clear()
        for (i in 0..8) {
            val colors = listOf(
                Triple(1f, 0.3f, 0.5f), Triple(0.8f, 0.4f, 0.9f),
                Triple(1f, 0.8f, 0.2f), Triple(0.5f, 0.7f, 1f),
                Triple(1f, 0.5f, 0.7f), Triple(0.3f, 0.9f, 0.6f)
            )
            val (cr, cg, cb) = colors[i % colors.size]
            flowers.add(Flower(
                x = -0.5f + Math.random().toFloat() * 1f,
                z = -0.8f + Math.random().toFloat() * 1.6f,
                size = 0.03f + Math.random().toFloat() * 0.04f,
                colorR = cr, colorG = cg, colorB = cb
            ))
        }
        
        // 初始化蝴蝶
        butterflies.clear()
        for (i in 0..3) {
            val colors = listOf(
                Triple(1f, 0.6f, 0.8f), Triple(0.6f, 0.8f, 1f),
                Triple(1f, 0.9f, 0.4f), Triple(0.8f, 0.5f, 1f)
            )
            val (cr, cg, cb) = colors[i % colors.size]
            butterflies.add(Butterfly(
                x = -0.5f + Math.random().toFloat(), 
                y = 0.3f + Math.random().toFloat() * 0.5f,
                z = -0.5f + Math.random().toFloat(),
                targetX = -0.5f + Math.random().toFloat(),
                targetY = 0.2f + Math.random().toFloat() * 0.8f,
                targetZ = -0.5f + Math.random().toFloat(),
                colorR = cr, colorG = cg, colorB = cb
            ))
        }
    }
    
    fun getTimeInfo(): TimeInfo {
        val hour = timeOfDay.toInt()
        val minute = ((timeOfDay - hour) * 60).toInt()
        return TimeInfo(
            hour = hour, minute = minute,
            isMorning = hour in 5..8,
            isEvening = hour in 17..20,
            isNight = hour < 5 || hour >= 20,
            daylight = when {
                hour in 6..17 -> 1f
                hour < 5 || hour >= 20 -> 0.1f
                hour == 5 -> 0.3f
                hour == 18 -> 0.8f
                hour == 19 -> 0.4f
                else -> 0.5f
            }
        )
    }
    
    fun updateWeather(dt: Float) {
        weatherTimer += dt
        if (weatherTimer > weatherDuration) {
            weatherTimer = 0f
            weatherDuration = 30f + Math.random().toFloat() * 60f
            transitionProgress = 0f
            
            // 根据当前天气决定下一个天气
            val roll = Math.random().toFloat()
            val isNight = getTimeInfo().isNight
            currentWeather = when {
                isNight && roll < 0.3f -> Weather.RAINY
                isNight && roll < 0.5f -> Weather.CLOUDY
                !isNight && roll < 0.5f -> Weather.SUNNY
                !isNight && roll < 0.7f -> Weather.CLOUDY
                roll < 0.8f -> Weather.WINDY
                roll < 0.9f -> Weather.RAINY
                roll < 0.95f && !isNight -> Weather.STORMY
                else -> Weather.FOGGY
            }
        }
        
        transitionProgress = minOf(1f, transitionProgress + dt * 0.5f)
        
        // 更新环境特效
        when (currentWeather) {
            Weather.SUNNY -> { rainIntensity *= 0.9f; snowIntensity *= 0.9f; windStrength *= 0.9f; fogDensity *= 0.9f }
            Weather.RAINY -> { rainIntensity = 0.5f + 0.5f * sin(weatherTimer * 0.3f); snowIntensity *= 0.9f; windStrength = 0.2f; fogDensity = 0.1f }
            Weather.SNOWY -> { snowIntensity = 0.4f + 0.3f * sin(weatherTimer * 0.2f); rainIntensity = 0f; windStrength = 0.1f; fogDensity = 0.2f }
            Weather.WINDY -> { windStrength = 0.5f + 0.3f * sin(weatherTimer * 0.5f); rainIntensity *= 0.9f; snowIntensity *= 0.9f }
            Weather.STORMY -> { rainIntensity = 1f; windStrength = 0.8f; fogDensity = 0.3f }
            Weather.FOGGY -> { fogDensity = 0.5f + 0.2f * sin(weatherTimer * 0.1f) }
            Weather.CLOUDY -> { rainIntensity *= 0.95f; fogDensity = 0.05f; windStrength = 0.1f }
        }
        
        // 更新蝴蝶
        for (bfly in butterflies) {
            bfly.x += (bfly.targetX - bfly.x) * 0.01f
            bfly.y += (bfly.targetY - bfly.y) * 0.01f
            bfly.z += (bfly.targetZ - bfly.z) * 0.01f
            bfly.phase += dt * 3f
            
            if (Math.random().toFloat() < 0.01f) {
                bfly.targetX = -0.8f + Math.random().toFloat() * 1.6f
                bfly.targetY = 0.2f + Math.random().toFloat() * 1.0f
                bfly.targetZ = -0.8f + Math.random().toFloat() * 1.6f
            }
        }
    }
    
    fun updateGrass(dt: Float, isRaining: Boolean) {
        for (blade in grassBlades) {
            // 草摇摆
            val windEffect = windStrength * 0.5f
            blade.swayPhase += dt * (2f + windEffect * 3f)
            
            // 草生长/再生
            if (blade.isEaten) {
                blade.regrowTimer += dt
                if (blade.regrowTimer > 3f + Math.random().toFloat() * 5f) {
                    blade.isEaten = false
                    blade.height = 0.01f
                    blade.regrowTimer = 0f
                }
            } else if (blade.height < blade.maxHeight) {
                val growthRate = if (isRaining) grassGrowthRate * 2f else grassGrowthRate
                blade.height = minOf(blade.maxHeight, blade.height + growthRate * dt * 5f)
            }
            
            // 草颜色随季节/天气变化
            if (isRaining) {
                blade.colorR = 0.25f; blade.colorG = 0.75f; blade.colorB = 0.25f
            } else {
                blade.colorR = 0.3f + sin(blade.swayPhase * 0.1f) * 0.05f
                blade.colorG = 0.6f + sin(blade.swayPhase * 0.1f) * 0.1f
                blade.colorB = 0.2f
            }
        }
    }
    
    fun eatGrassAt(x: Float): Boolean {
        for (blade in grassBlades) {
            if (!blade.isEaten && abs(blade.x - x) < 0.03f) {
                blade.isEaten = true
                blade.height = 0.01f
                return true
            }
        }
        return false
    }
    
    fun getWeather(): Weather = currentWeather
    fun getWeatherName(): String = currentWeather.displayName
    fun getButterflies(): List<Butterfly> = butterflies
    fun getFlowers(): List<Flower> = flowers
    fun getGrass(): List<GrassBlade> = grassBlades
    
    data class TimeInfo(
        val hour: Int, val minute: Int,
        val isMorning: Boolean, val isEvening: Boolean, val isNight: Boolean,
        val daylight: Float
    )
}

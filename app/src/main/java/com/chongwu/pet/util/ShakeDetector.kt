package com.chongwu.pet.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ShakeDetector(private val context: Context) {

    enum class Sensitivity(val threshold: Float) {
        LOW(20f), MEDIUM(12f), HIGH(6f)
    }

    var sensitivity = Sensitivity.MEDIUM
    var onShake: (() -> Unit)? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeTime = 0L
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var isListening = false

    private val listener = object : SensorEventListener {
        private val SHAKE_COOLDOWN_MS = 800L

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val dx = x - lastX; val dy = y - lastY; val dz = z - lastZ
            val acceleration = kotlin.math.sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat()
            lastX = x; lastY = y; lastZ = z

            val now = System.currentTimeMillis()
            if (acceleration > sensitivity.threshold && now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now
                onShake?.invoke()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager.unregisterListener(listener)
            isListening = false
        }
    }

    fun setSensitivity(level: Int) {
        sensitivity = when {
            level <= 1 -> Sensitivity.LOW
            level >= 3 -> Sensitivity.HIGH
            else -> Sensitivity.MEDIUM
        }
    }
}

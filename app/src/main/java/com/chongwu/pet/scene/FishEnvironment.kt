package com.chongwu.pet.scene

import kotlin.math.*

class FishEnvironment {

    data class Bubble(var x: Float, var y: Float, var z: Float,
                      var speed: Float, var size: Float, var life: Float, var maxLife: Float)

    data class WaterPlant(var x: Float, var z: Float,
                          var height: Float, var swayPhase: Float, var colorG: Float)

    data class LightRay(var x: Float, var intensity: Float, var phase: Float)

    val bubbles = mutableListOf<Bubble>()
    val waterPlants = mutableListOf<WaterPlant>()
    val lightRays = mutableListOf<LightRay>()
    var bubbleTimer = 0f
    var waterOpacity = 0.15f    // 水的视觉效果

    fun init() {
        waterPlants.clear()
        for (i in 0..5) {
            waterPlants.add(WaterPlant(
                x = -0.7f + i * 0.28f,
                z = -0.3f + r() * 0.6f,
                height = 0.15f + r() * 0.2f,
                swayPhase = r() * PI.toFloat() * 2f,
                colorG = 0.3f + r() * 0.3f
            ))
        }
        lightRays.clear()
        for (i in 0..3) {
            lightRays.add(LightRay(
                x = -0.6f + i * 0.4f,
                intensity = 0.1f + r() * 0.2f,
                phase = r() * PI.toFloat() * 2f
            ))
        }
    }

    fun update(dt: Float) {
        bubbleTimer += dt
        // 生成新气泡
        if (bubbleTimer > 0.3f) {
            bubbleTimer = 0f
            bubbles.add(Bubble(
                x = (r() - 0.5f) * 1.2f,
                y = -0.6f,
                z = (r() - 0.5f) * 0.6f,
                speed = 0.2f + r() * 0.3f,
                size = 0.01f + r() * 0.02f,
                life = 1f, maxLife = 1f
            ))
        }
        // 更新气泡
        val iter = bubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.y += b.speed * dt
            b.x += sin(b.life * 5f) * dt * 0.1f
            b.life -= dt * 0.3f
            if (b.life <= 0f || b.y > 0.7f) iter.remove()
        }

        // 水草摇摆
        for (plant in waterPlants) {
            plant.swayPhase += dt * 2f
        }

        // 光柱动画
        for (ray in lightRays) {
            ray.phase += dt * 0.5f
        }
    }

    private fun r() = Math.random().toFloat()
}

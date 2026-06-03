package com.chongwu.pet.scene

import kotlin.math.*

class SheepEnvironment {

    data class GrassBlade(var x: Float, var z: Float, var height: Float, var maxHeight: Float,
                          var swayPhase: Float, var isEaten: Boolean = false, var regrowTimer: Float = 0f,
                          var colorR: Float = 0.3f, var colorG: Float = 0.6f, var colorB: Float = 0.2f)

    data class Flower(var x: Float, var z: Float, var size: Float,
                      var colorR: Float, var colorG: Float, var colorB: Float, var swayPhase: Float)

    data class Butterfly(var x: Float, var y: Float, var z: Float,
                         var targetX: Float, var targetY: Float, var targetZ: Float,
                         var phase: Float, var colorR: Float, var colorG: Float, var colorB: Float)

    val grassBlades = mutableListOf<GrassBlade>()
    val flowers = mutableListOf<Flower>()
    val butterflies = mutableListOf<Butterfly>()
    var windStrength = 0f

    fun init() {
        grassBlades.clear()
        for (i in 0..120) {
            val h = 0.03f + r() * 0.12f
            grassBlades.add(GrassBlade(
                x = -0.8f + r() * 1.6f,
                z = -0.6f + r() * 1.2f,
                height = h, maxHeight = h,
                swayPhase = r() * PI.toFloat() * 2f
            ))
        }
        flowers.clear()
        val flowerColors = listOf(
            Triple(1f,0.3f,0.5f), Triple(0.9f,0.5f,0.1f),
            Triple(0.5f,0.7f,1f), Triple(1f,0.8f,0.3f), Triple(0.8f,0.4f,1f)
        )
        for (i in 0..5) {
            val (cr, cg, cb) = flowerColors[i % flowerColors.size]
            flowers.add(Flower(
                x = -0.6f + r() * 1.2f, z = -0.5f + r() * 1f,
                size = 0.02f + r() * 0.03f,
                colorR = cr, colorG = cg, colorB = cb,
                swayPhase = r() * PI.toFloat() * 2f
            ))
        }
        butterflies.clear()
        val bColors = listOf(Triple(1f,0.6f,0.8f), Triple(0.6f,0.8f,1f), Triple(1f,0.9f,0.4f))
        for (i in 0..2) {
            val (cr, cg, cb) = bColors[i % bColors.size]
            butterflies.add(Butterfly(
                x = (r()-0.5f)*1.5f, y = 0.3f+r()*0.4f, z = (r()-0.5f)*1f,
                targetX = (r()-0.5f)*1.5f, targetY = 0.2f+r()*0.6f, targetZ = (r()-0.5f)*1f,
                phase = r() * PI.toFloat() * 2f,
                colorR = cr, colorG = cg, colorB = cb
            ))
        }
    }

    fun update(dt: Float) {
        windStrength = 0.1f + sin(timer * 0.3f) * 0.1f
        timer += dt

        for (blade in grassBlades) {
            blade.swayPhase += dt * (2f + windStrength * 3f)
            if (blade.isEaten) {
                blade.regrowTimer += dt
                if (blade.regrowTimer > 3f + r() * 5f) {
                    blade.isEaten = false; blade.height = 0.01f; blade.regrowTimer = 0f
                }
            } else if (blade.height < blade.maxHeight) {
                blade.height = minOf(blade.maxHeight, blade.height + dt * 0.05f)
            }
        }

        for (flower in flowers) flower.swayPhase += dt * 1.5f

        for (bf in butterflies) {
            bf.x += (bf.targetX - bf.x) * 0.01f
            bf.y += (bf.targetY - bf.y) * 0.01f
            bf.z += (bf.targetZ - bf.z) * 0.01f
            bf.phase += dt * 3f
            if (r() < 0.01f) {
                bf.targetX = (r() - 0.5f) * 1.5f
                bf.targetY = 0.2f + r() * 0.8f
                bf.targetZ = (r() - 0.5f) * 1f
            }
        }
    }

    fun eatGrassAt(x: Float): Boolean {
        for (blade in grassBlades) {
            if (!blade.isEaten && abs(blade.x - x) < 0.03f) {
                blade.isEaten = true; blade.height = 0.01f; return true
            }
        }
        return false
    }

    private var timer = 0f
    private fun r() = Math.random().toFloat()
}

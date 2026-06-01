package com.chongwu.pet.render.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.chongwu.pet.ai.SheepState
import com.chongwu.pet.ecology.Environment
import com.chongwu.pet.model.Camera
import com.chongwu.pet.model.Model3D
import com.chongwu.pet.model.PrimitiveBuilder
import com.chongwu.pet.model.SheepModel3D
import kotlin.math.*

/**
 * 主OpenGL渲染器 - 管理场景绘制
 */
class PetRenderer {
    
    private val shader = ShaderProgram()
    private var isReady = false
    
    // 场景对象
    private val camera = Camera()
    private lateinit var sheepModel: SheepModel3D
    private var sheepRoot: Model3D? = null
    private var ground: Model3D? = null
    private var skyDome: Model3D? = null
    
    // 动画状态
    private val pose = SheepModel3D.Pose()
    private var animTime = 0f
    var sheepState: SheepState? = null
    var environment: Environment? = null
    
    // 摄像机控制
    var camTheta = 30f
    var camPhi = 25f
    var camDistance = 5.5f
    var camTargetOffset = 0f
    
    // 特效参数
    var fxHearts = 0f; var fxStars = 0f; var fxNotes = 0f
    var fxBlush = 0f; var fxExclaim = 0f
    
    // 粒子系统
    private val particles = mutableListOf<Particle>()
    private var particleTimer = 0f
    
    // 环境模型
    private val grassModels = mutableListOf<Model3D>()
    private val flowerModels = mutableListOf<Model3D>()
    private val butterflyModels = mutableListOf<Model3D>()
    
    // 光方向
    private val lightDir = floatArrayOf(0.5f, 1.0f, 0.3f)
    private var ambient = 0.4f
    
    data class Particle(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        var life: Float, var maxLife: Float,
        var r: Float, var g: Float, var b: Float,
        var size: Float
    )
    
    fun init(width: Float, height: Float) {
        shader.create()
        shader.use()
        
        // 构建场景
        sheepModel = SheepModel3D()
        sheepRoot = sheepModel.build()
        sheepRoot?.uploadToGPU()
        
        // 地面
        val groundPlane = PrimitiveBuilder.createPlane(8f, 8f, 8, 8)
        groundPlane.colorR = 0.3f; groundPlane.colorG = 0.7f; groundPlane.colorB = 0.2f
        ground = Model3D()
        ground!!.children.add(groundPlane)
        ground!!.translateY = -0.7f
        ground!!.uploadToGPU()
        
        // 初始化草地
        initGrassModels()
        
        // 初始化花朵
        initFlowerModels()
        
        // 初始化蝴蝶
        initButterflyModels()
        
        // 天空球罩（简化 - 用半圆）
        skyDome = PrimitiveBuilder.createSphere(5f, 16, 16)
        skyDome.colorR = 0.6f; skyDome.colorG = 0.8f; skyDome.colorB = 1.0f
        skyDome.colorA = 0.3f
        skyDome.uploadToGPU()
        
        camera.fov = 40f
        camera.distance = camDistance
        camera.theta = camTheta
        camera.phi = camPhi
        
        isReady = true
    }
    
    private fun initGrassModels() {
        grassModels.clear()
        val env = environment ?: return
        for (blade in env.getGrass()) {
            val gModel = Model3D()
            // 用细长的三角形代表草
            val verts = floatArrayOf(
                -0.005f, 0f, 0f,
                0.005f, 0f, 0f,
                0f, blade.height, 0f
            )
            val norms = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)
            val idxs = shortArrayOf(0, 1, 2)
            gModel.vertices = verts; gModel.normals = norms; gModel.indices = idxs
            gModel.colorR = blade.colorR; gModel.colorG = blade.colorG; gModel.colorB = blade.colorB
            gModel.translateX = blade.x / 2f
            gModel.translateY = -0.7f
            gModel.translateZ = -1f
            gModel.uploadToGPU()
            grassModels.add(gModel)
        }
    }
    
    private fun initFlowerModels() {
        val env = environment ?: return
        flowerModels.clear()
        for (flower in env.getFlowers()) {
            val fModel = PrimitiveBuilder.createEllipsoid(0.02f, 0.02f, 0.02f, 6, 6)
            fModel.colorR = flower.colorR; fModel.colorG = flower.colorG; fModel.colorB = flower.colorB
            val fRoot = Model3D()
            fRoot.children.add(fModel)
            fRoot.translateX = flower.x
            fRoot.translateY = -0.65f
            fRoot.translateZ = flower.z
            fRoot.uploadToGPU()
            flowerModels.add(fRoot)
        }
    }
    
    private fun initButterflyModels() {
        val env = environment ?: return
        butterflyModels.clear()
        for (bfly in env.getButterflies()) {
            val bModel = PrimitiveBuilder.createEllipsoid(0.02f, 0.01f, 0.005f, 6, 4)
            bModel.colorR = bfly.colorR; bModel.colorG = bfly.colorG; bModel.colorB = bfly.colorB
            val bRoot = Model3D()
            bRoot.children.add(bModel)
            bRoot.translateX = bfly.x; bRoot.translateY = bfly.y; bRoot.translateZ = bfly.z
            bRoot.uploadToGPU()
            butterflyModels.add(bRoot)
        }
    }
    
    fun update(dt: Float) {
        if (!isReady) return
        animTime += dt
        
        val env = environment ?: return
        val brain = sheepState ?: return
        
        // 更新相机
        camera.distance = camDistance
        camera.theta = camTheta + sin(animTime * 0.3f) * 2f
        camera.phi = camPhi + sin(animTime * 0.5f) * 1f
        camera.targetOffsetY = camTargetOffset
        
        // 更新光照
        val timeInfo = env.getTimeInfo()
        val dayLight = timeInfo.daylight
        ambient = 0.15f + dayLight * 0.35f
        
        val sunAngle = (timeInfo.hour - 6f) / 12f * PI
        lightDir[0] = sin(sunAngle.toFloat()) * 0.8f
        lightDir[1] = cos(sunAngle.toFloat()) * 0.8f
        lightDir[2] = 0.3f
        
        // ==== 计算动画姿势 ====
        val state = brain.currentState
        
        // 放松呼吸
        val breath = sin(animTime * 2f) * 0.02f
        pose.bodyTilt = 0f
        pose.bodySway = sin(animTime * 1.5f) * 0.5f
        
        // 腿部动画
        var legCycle = 0f
        var bodyBob = 0f
        val isMoving = brain.isMoving()
        
        when (state) {
            SheepState.State.IDLE -> {
                pose.headTilt = sin(animTime * 1.5f) * 3f
                pose.headSway = sin(animTime * 0.8f) * 2f
                pose.tailWag = sin(animTime * 2f) * 5f
                pose.squashY = breath * 0.5f
                pose.squashX = -breath * 0.25f
                pose.blink = if ((animTime * 0.5f).toInt() % 10 == 0) -0.7f else 0f
            }
            
            SheepState.State.WALKING, SheepState.State.EXPLORING -> {
                legCycle = animTime * brain.moveSpeed * 3f
                bodyBob = abs(sin(legCycle)) * 0.03f
                pose.leftFrontLegAngle = sin(legCycle) * 20f
                pose.rightFrontLegAngle = sin(legCycle + PI.toFloat()) * 20f
                pose.leftBackLegAngle = sin(legCycle + PI.toFloat()) * 20f
                pose.rightBackLegAngle = sin(legCycle) * 20f
                pose.bodyTilt = sin(animTime * 3f) * 1f
                pose.tailWag = sin(animTime * 4f) * 10f
                pose.bobOffset = bodyBob * 10f
                pose.squashY = -bodyBob * 5f
                pose.headTilt = 5f
            }
            
            SheepState.State.GRAZING -> {
                pose.headTilt = 35f + sin(animTime * 3f) * 5f
                pose.bodyTilt = 10f
                pose.tailWag = sin(animTime * 2.5f) * 3f
                pose.leftFrontLegAngle = 15f
                pose.rightFrontLegAngle = 15f
                pose.blink = sin(animTime * 2f) * 0.2f
            }
            
            SheepState.State.SLEEPING -> {
                pose.headTilt = 20f
                pose.bodyTilt = -15f
                pose.squashY = -0.1f
                pose.squashX = 0.05f
                pose.squashZ = 0.05f
                pose.leftFrontLegAngle = 30f
                pose.rightFrontLegAngle = -30f
                pose.leftBackLegAngle = -30f
                pose.rightBackLegAngle = 30f
                pose.blink = -1f
                pose.headSway = sin(animTime * 1f) * 1f
            }
            
            SheepState.State.HOPPING -> {
                val jumpPhase = animTime * 8f
                val hopHeight = abs(sin(jumpPhase))
                pose.bobOffset = hopHeight * 15f
                pose.squashY = -hopHeight * 0.3f
                pose.squashX = hopHeight * 0.15f
                pose.squashZ = hopHeight * 0.15f
                pose.leftFrontLegAngle = -hopHeight * 30f
                pose.rightFrontLegAngle = hopHeight * 30f
                pose.leftBackLegAngle = hopHeight * 30f
                pose.rightBackLegAngle = -hopHeight * 30f
                pose.tailWag = sin(animTime * 10f) * 20f
                pose.headTilt = hopHeight * 10f
            }
            
            SheepState.State.PLAYING -> {
                pose.bodySway = sin(animTime * 5f) * 5f
                pose.headTilt = sin(animTime * 4f) * 10f + 10f
                pose.headSway = sin(animTime * 3f) * 8f
                pose.leftFrontLegAngle = sin(animTime * 6f) * 25f
                pose.rightFrontLegAngle = sin(animTime * 6f + PI.toFloat()) * 25f
                pose.tailWag = sin(animTime * 6f) * 30f
                pose.squashY = sin(animTime * 5f) * 0.05f
            }
            
            SheepState.State.HEADBUTTING -> {
                val bt = animTime * 10f
                pose.headTilt = -sin(bt).coerceAtLeast(0f) * 20f
                pose.headSway = sin(bt) * 5f
                pose.bodyTilt = sin(bt) * 5f
                pose.leftFrontLegAngle = 10f
                pose.rightFrontLegAngle = -10f
            }
            
            SheepState.State.STARTLED -> {
                val st = animTime * 12f
                pose.bobOffset = abs(sin(st)) * 20f
                pose.squashX = cos(st) * 0.15f
                pose.squashZ = cos(st) * 0.15f
                pose.headTilt = -10f
                pose.leftFrontLegAngle = -60f
                pose.rightFrontLegAngle = 60f
                pose.tailWag = 0f
            }
            
            SheepState.State.SINGING -> {
                pose.headTilt = sin(animTime * 6f) * 5f + 5f
                pose.bodySway = sin(animTime * 3f) * 2f
                pose.squashY = sin(animTime * 6f) * 0.02f
                pose.tailWag = sin(animTime * 6f) * 5f
            }
            
            SheepState.State.CURIOUS -> {
                pose.headTilt = 15f + sin(animTime * 3f) * 8f
                pose.headSway = sin(animTime * 2f) * 5f
                pose.bodyTilt = 5f
            }
            
            SheepState.State.GREETING -> {
                pose.headTilt = -5f + sin(animTime * 6f) * 5f
                pose.tailWag = sin(animTime * 8f) * 25f
                pose.bodySway = sin(animTime * 4f) * 3f
            }
            
            else -> {}
        }
        
        // 花瓣旋转
        pose.flowerSpin = animTime * 20f
        
        // 更新花粉粒子
        particleTimer += dt
        if (particleTimer > 0.1f && state == SheepState.State.HOPPING) {
            particleTimer = 0f
            addParticle(
                0f, 0f, 0f,
                (Math.random().toFloat() - 0.5f) * 0.3f, 0.3f + Math.random().toFloat() * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.3f,
                0.5f + Math.random().toFloat() * 0.5f,
                0.8f, 0.5f, 0.9f, 0.02f
            )
        }
        
        // 更新粒子
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            p.vy -= 0.2f * dt  // 重力
            p.life -= dt
            if (p.life <= 0f) iter.remove()
        }
        
        // 更新蝴蝶位置
        for (i in butterflyModels.indices) {
            val bfly = env.getButterflies().getOrNull(i) ?: continue
            val bModel = butterflyModels[i]
            bModel.translateX = bfly.x
            bModel.translateY = bfly.y
            bModel.translateZ = bfly.z + 0.5f
            bModel.rotateY = sin(bfly.phase) * 30f
            bModel.rotateX = sin(bfly.phase * 0.5f) * 15f
        }
    }
    
    fun render(width: Float, height: Float) {
        if (!isReady) return
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        
        // 更新相机
        camera.update(width, height)
        
        shader.use()
        
        // 设置着色器uniforms
        val vp = camera.vpMatrix
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        
        val lightDirN = FloatArray(3)
        val len = sqrt(lightDir[0]*lightDir[0] + lightDir[1]*lightDir[1] + lightDir[2]*lightDir[2])
        lightDirN[0] = lightDir[0]/len; lightDirN[1] = lightDir[1]/len; lightDirN[2] = lightDir[2]/len
        
        GLES20.glUniform3f(shader.uLightDirLoc, lightDirN[0], lightDirN[1], lightDirN[2])
        GLES20.glUniform1f(shader.uAmbientLoc, ambient)
        GLES20.glUniform1f(shader.uTimeLoc, animTime)
        
        // 绘制天空球
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        
        // 天空颜色
        val env = environment ?: return
        val timeInfo = env.getTimeInfo()
        val skyColor = if (timeInfo.isNight) floatArrayOf(0.08f, 0.1f, 0.2f)
            else if (timeInfo.isMorning) floatArrayOf(0.8f, 0.6f, 0.4f)
            else if (timeInfo.isEvening) floatArrayOf(0.8f, 0.4f, 0.2f)
            else floatArrayOf(0.4f, 0.7f, 1.0f)
        
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(false)
        skyDome?.let {
            GLES20.glUniform4f(shader.uColorLoc, skyColor[0], skyColor[1], skyColor[2], 1f)
            it.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        }
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        
        // 绘制地面
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, 0f, -0.7f, 0f)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        
        ground?.let {
            val weather = env.getWeather()
            val groundColor = when (weather) {
                Environment.Weather.RAINY -> floatArrayOf(0.2f, 0.5f, 0.15f)
                Environment.Weather.SNOWY -> floatArrayOf(0.8f, 0.85f, 0.9f)
                Environment.Weather.STORMY -> floatArrayOf(0.15f, 0.4f, 0.1f)
                else -> floatArrayOf(0.3f, 0.7f, 0.2f)
            }
            GLES20.glUniform4f(shader.uColorLoc, groundColor[0], groundColor[1], groundColor[2], 1f)
            it.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        }
        
        // 绘制草地
        for ((i, blade) in env.getGrass().withIndex()) {
            if (blade.isEaten) continue
            if (i >= grassModels.size) break
            val gModel = grassModels[i]
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, (blade.x - 2f) / 2f, -0.7f, -1f)
            val sway = sin(animTime * 2f + blade.swayPhase) * env.windStrength * 0.1f
            Matrix.rotateM(model, 0, sway, 0f, 0f, 1f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
            GLES20.glUniform4f(shader.uColorLoc, blade.colorR, blade.colorG, blade.colorB, 1f)
            gModel.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        }
        
        // 绘制花朵
        for (flower in flowerModels) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, flower.translateX, flower.translateY, flower.translateZ)
            val sway = sin(animTime * 2f + Math.random().toFloat() * 6.28f) * env.windStrength * 0.05f
            Matrix.rotateM(model, 0, sway, 0f, 0f, 1f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
            flower.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        }
        
        // 绘制小羊
        val sheepPosX = sin(animTime * brainMoveSpeed()) * 0.3f
        val sheepPosY = -0.1f + pose.bobOffset * 0.01f
        
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, sheepPosX, sheepPosY, 0f)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        
        sheepModel.applyPose(pose)
        sheepRoot?.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        
        // 绘制蝴蝶
        for (bfly in butterflyModels) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, bfly.translateX, bfly.translateY + sin(animTime * 2f) * 0.05f, bfly.translateZ)
            Matrix.rotateM(model, 0, bfly.rotateY, 0f, 1f, 0f)
            Matrix.rotateM(model, 0, bfly.rotateX, 1f, 0f, 0f)
            Matrix.scaleM(model, 0, 1f + sin(animTime * 8f + bfly.translateX) * 0.3f, 1f, 1f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
            bfly.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        }
        
        // 绘制粒子
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        
        for (p in particles) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, p.x, p.y, p.z + 0.5f)
            val s = p.size * (p.life / p.maxLife)
            Matrix.scaleM(model, 0, s, s, s)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
            GLES20.glUniform4f(shader.uColorLoc, p.r, p.g, p.b, p.life / p.maxLife)
            
            val tempM = FloatArray(16)
            Matrix.setIdentityM(tempM, 0)
            // 用一个小球体代表粒子
            val particleModel = PrimitiveBuilder.createSphere(1f, 4, 4)
            particleModel.draw(tempM, mvp, shader.aPositionLoc, -1, shader.uModelLoc, shader.uColorLoc)
        }
        
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        
        // 绘制雨水特效
        if (env.rainIntensity > 0.1f) {
            drawRain(vp, width, height, env.rainIntensity)
        }
    }
    
    private fun addParticle(x: Float, y: Float, z: Float, vx: Float, vy: Float, vz: Float, 
                            life: Float, r: Float, g: Float, b: Float, size: Float) {
        particles.add(Particle(x, y, z, vx, vy, vz, life, life, r, g, b, size))
    }
    
    private var brainMoveSpeed: () -> Float = { 0.3f }
    fun setBrainMoveSpeed(speed: () -> Float) { brainMoveSpeed = speed }
    
    fun addParticlesForEffect(type: String, count: Int = 10) {
        when (type) {
            "hearts" -> for (i in 0 until count) addParticle(
                (Math.random().toFloat() - 0.5f) * 0.3f, 0.2f + Math.random().toFloat() * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.2f, 0.3f + Math.random().toFloat() * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.2f,
                0.8f + Math.random().toFloat() * 0.5f,
                1f, 0.3f, 0.5f, 0.03f)
            "stars" -> for (i in 0 until count) addParticle(
                (Math.random().toFloat() - 0.5f) * 0.4f, 0.3f + Math.random().toFloat() * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.4f,
                (Math.random().toFloat() - 0.5f) * 0.3f, 0.2f + Math.random().toFloat() * 0.4f,
                (Math.random().toFloat() - 0.5f) * 0.3f,
                0.6f + Math.random().toFloat() * 0.4f,
                1f, 0.8f, 0.2f, 0.025f)
            "notes" -> for (i in 0 until count) addParticle(
                (Math.random().toFloat() - 0.5f) * 0.2f, 0.1f + Math.random().toFloat() * 0.2f,
                (Math.random().toFloat() - 0.5f) * 0.2f,
                (Math.random().toFloat() - 0.5f) * 0.1f, 0.2f,
                (Math.random().toFloat() - 0.5f) * 0.1f,
                0.5f + Math.random().toFloat() * 0.3f,
                0.5f, 0.3f, 1f, 0.02f)
            "blush" -> for (i in 0 until count) addParticle(
                (Math.random().toFloat() - 0.5f) * 0.3f, -0.1f,
                (Math.random().toFloat() - 0.5f) * 0.3f,
                (Math.random().toFloat() - 0.5f) * 0.1f, 0.1f,
                (Math.random().toFloat() - 0.5f) * 0.1f,
                0.4f + Math.random().toFloat() * 0.3f,
                1f, 0.4f, 0.6f, 0.04f)
        }
    }
    
    private fun drawRain(vp: FloatArray, width: Float, height: Float, intensity: Float) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glLineWidth(2f)
        
        val count = (intensity * 40).toInt().coerceAtMost(60)
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        
        for (i in 0 until count) {
            val rx = (i * 47.7f + animTime * 200f) % 5f - 2.5f
            val ry = (i * 73.3f + animTime * 400f + (i % 3) * 2f) % 5f - 2.5f
            val rz = (i * 31.1f) % 3f - 1.5f
            
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, rx, ry - 0.5f, rz)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
            
            // 用线条绘制雨滴
            val rainLine = Model3D()
            rainLine.vertices = floatArrayOf(0f, 0.1f, 0f, 0f, -0.1f, 0f)
            rainLine.normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
            rainLine.colorR = 0.5f; rainLine.colorG = 0.7f; rainLine.colorB = 1.0f; rainLine.colorA = 0.3f
            rainLine.draw(model, mvp, shader.aPositionLoc, -1, shader.uModelLoc, shader.uColorLoc)
        }
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
    
    fun cleanup() {
        if (isReady) {
            GLES20.glDeleteProgram(shader.programId)
        }
    }
    
    /** 获取相机引用（用于外部控制） */
    fun getCamera(): Camera = camera
}

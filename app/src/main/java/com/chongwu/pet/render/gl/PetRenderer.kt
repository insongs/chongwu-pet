package com.chongwu.pet.render.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.chongwu.pet.PetType
import com.chongwu.pet.model.Camera
import com.chongwu.pet.model.FishModel3D
import com.chongwu.pet.model.Model3D
import com.chongwu.pet.model.PrimitiveBuilder
import com.chongwu.pet.model.SheepModel3D
import com.chongwu.pet.scene.SheepEnvironment
import kotlin.math.*

/**
 * 主OpenGL渲染器 - 支持小羊和小鱼
 */
class PetRenderer {
    
    private val shader = ShaderProgram()
    private var isReady = false
    
    // 场景对象
    private val camera = Camera()
    private var currentPetType = PetType.SHEEP
    private var sheepModel: SheepModel3D? = null
    private var fishModel: FishModel3D? = null
    private var sheepRoot: Model3D? = null
    private var fishRoot: Model3D? = null
    private var activePet: Model3D? = null
    
    // 宠物位置
    var petX = 0f
    var petZ = 0f
    var petScale = 1f
    
    // 动画状态
    private var animTime = 0f
    private var sheepPose = SheepModel3D.Pose()
    private var fishPose = FishModel3D.Pose()
    
    // 特效
    var fxHearts = 0f; var fxStars = 0f; var fxNotes = 0f
    var fxBlush = 0f; var fxExclaim = 0f; var fxBubbles = 0f
    
    // 粒子系统
    private val particles = mutableListOf<Particle>()
    
    // 环境
    private var ground: Model3D? = null
    private var skyDome: Model3D? = null
    private val grassModels = mutableListOf<Model3D>()
    private val flowerModels = mutableListOf<Model3D>()
    private val butterflyModels = mutableListOf<Model3D>()
    private val sheepEnv = SheepEnvironment()
    
    // 光照
    private val lightDir = floatArrayOf(0.5f, 1.0f, 0.3f)
    private var ambient = 0.4f
    private var timeOfDay = 6f
    
    data class Particle(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        var life: Float, var maxLife: Float,
        var r: Float, var g: Float, var b: Float, var size: Float
    )
    
    fun switchPet(type: PetType) {
        currentPetType = type
        activePet = when (type) {
            PetType.SHEEP -> sheepRoot
            PetType.FISH -> fishRoot
        }
    }
    
    fun init(width: Float, height: Float) {
        shader.create()
        shader.use()
        
        // 构建小羊
        val sm = SheepModel3D()
        sheepModel = sm
        sheepRoot = sm.build()
        sheepRoot?.uploadToGPU()
        
        // 构建小鱼
        val fm = FishModel3D()
        fishModel = fm
        fishRoot = fm.build()
        fishRoot?.uploadToGPU()
        
        activePet = sheepRoot // 默认小羊
        
        // 地面
        val groundPlane = PrimitiveBuilder.createPlane(8f, 8f, 8, 8)
        groundPlane.colorR = 0.3f; groundPlane.colorG = 0.7f; groundPlane.colorB = 0.2f
        ground = Model3D()
        ground!!.children.add(groundPlane)
        ground!!.translateY = -0.7f
        ground!!.uploadToGPU()
        
        // 天空
        skyDome = PrimitiveBuilder.createSphere(5f, 16, 16)
        skyDome!!.colorR = 0.6f; skyDome!!.colorG = 0.8f; skyDome!!.colorB = 1.0f
        skyDome!!.colorA = 0.3f
        skyDome!!.uploadToGPU()
        
        // 初始化草地
        sheepEnv.init()
        initGrassModels()
        initFlowerModels()
        initButterflyModels()
        
        camera.fov = 40f
        camera.distance = 5.5f
        camera.theta = 30f
        camera.phi = 25f
        
        GLES20.glClearColor(0.05f, 0.08f, 0.12f, 1.0f)
        
        isReady = true
    }
    
    private fun initGrassModels() {
        grassModels.clear()
        for (blade in sheepEnv.grassBlades) {
            val gModel = Model3D()
            gModel.vertices = floatArrayOf(-0.005f, 0f, 0f, 0.005f, 0f, 0f, 0f, blade.height, 0f)
            gModel.normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)
            gModel.indices = shortArrayOf(0, 1, 2)
            gModel.colorR = blade.colorR; gModel.colorG = blade.colorG; gModel.colorB = blade.colorB
            gModel.translateX = blade.x * 0.5f
            gModel.translateY = -0.7f
            gModel.translateZ = blade.z * 0.5f
            gModel.uploadToGPU()
            grassModels.add(gModel)
        }
    }
    
    private fun initFlowerModels() {
        flowerModels.clear()
        for (flower in sheepEnv.flowers) {
            val fModel = PrimitiveBuilder.createEllipsoid(0.02f, 0.02f, 0.02f, 6, 6)
            fModel.colorR = flower.colorR; fModel.colorG = flower.colorG; fModel.colorB = flower.colorB
            val fRoot = Model3D()
            fRoot.children.add(fModel)
            fRoot.translateX = flower.x * 0.5f
            fRoot.translateY = -0.65f
            fRoot.translateZ = flower.z * 0.5f
            fRoot.uploadToGPU()
            flowerModels.add(fRoot)
        }
    }
    
    private fun initButterflyModels() {
        butterflyModels.clear()
        for (bfly in sheepEnv.butterflies) {
            val bModel = PrimitiveBuilder.createEllipsoid(0.02f, 0.01f, 0.005f, 6, 4)
            bModel.colorR = bfly.colorR; bModel.colorG = bfly.colorG; bModel.colorB = bfly.colorB
            val bRoot = Model3D()
            bRoot.children.add(bModel)
            bRoot.translateX = bfly.x * 0.5f
            bRoot.translateY = bfly.y
            bRoot.translateZ = bfly.z * 0.5f
            bRoot.uploadToGPU()
            butterflyModels.add(bRoot)
        }
    }
    
    fun update(dt: Float) {
        if (!isReady) return
        animTime += dt
        timeOfDay += dt * 0.005f
        if (timeOfDay > 24f) timeOfDay -= 24f
        
        // 更新光照
        val sunAngle = (timeOfDay - 6f) / 12f * PI
        val dayLight = when {
            timeOfDay in 6f..17f -> 1f
            timeOfDay < 5f || timeOfDay >= 20f -> 0.1f
            timeOfDay in 5f..6f -> 0.3f
            else -> 0.5f
        }
        ambient = 0.15f + dayLight.toFloat() * 0.35f
        lightDir[0] = sin(sunAngle.toFloat()) * 0.8f
        lightDir[1] = cos(sunAngle.toFloat()) * 0.8f
        lightDir[2] = 0.3f
        
        // 更新羊的风
        sheepEnv.windStrength = 0.1f + sin(animTime * 0.3f) * 0.1f
        
        // 更新蝴蝶
        for ((i, bfly) in sheepEnv.butterflies.withIndex()) {
            if (i >= butterflyModels.size) break
            bfly.phase += dt * 3f
            butterflyModels[i].translateX = bfly.x * 0.5f
            butterflyModels[i].translateY = bfly.y + sin(bfly.phase) * 0.03f
            butterflyModels[i].translateZ = bfly.z * 0.5f
            butterflyModels[i].rotateY = sin(bfly.phase) * 30f
            butterflyModels[i].rotateX = sin(bfly.phase * 0.5f) * 15f
        }
        
        // 更新草地摇摆
        for ((i, blade) in sheepEnv.grassBlades.withIndex()) {
            if (i >= grassModels.size) break
            blade.swayPhase += dt * 2f
        }
        
        // 更新动画姿势
        updatePose(dt)
        
        // 更新相机
        camera.distance = 5.5f
        camera.theta = 30f + sin(animTime * 0.3f) * 2f
        camera.phi = 25f + sin(animTime * 0.5f) * 1f
        camera.targetOffsetY = 0.5f
        
        // 更新特效
        fxHearts = maxOf(0f, fxHearts - dt); fxStars = maxOf(0f, fxStars - dt)
        fxNotes = maxOf(0f, fxNotes - dt); fxBlush = maxOf(0f, fxBlush - dt)
        fxExclaim = maxOf(0f, fxExclaim - dt); fxBubbles = maxOf(0f, fxBubbles - dt)
        
        // 更新粒子
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            p.vy -= 0.5f * dt
            p.life -= dt
            if (p.life <= 0f) iter.remove()
        }
    }
    
    private fun updatePose(dt: Float) {
        when (currentPetType) {
            PetType.SHEEP -> {
                val breath = sin(animTime * 2f) * 0.02f
                sheepPose.bodySway = sin(animTime * 1.5f) * 0.5f
                sheepPose.headTilt = sin(animTime * 1.5f) * 3f
                sheepPose.tailWag = sin(animTime * 2f) * 5f
                sheepPose.squashY = breath * 0.5f
                sheepPose.blink = if ((animTime * 0.5f).toInt() % 10 == 0) -0.7f else 0f
                sheepPose.flowerSpin = animTime * 20f
                
                // 轻微摆动
                sheepPose.leftFrontLegAngle = sin(animTime * 1.5f) * 3f
                sheepPose.rightFrontLegAngle = sin(animTime * 1.5f + PI.toFloat()) * 3f
                sheepPose.leftBackLegAngle = sin(animTime * 1.5f + PI.toFloat()) * 3f
                sheepPose.rightBackLegAngle = sin(animTime * 1.5f) * 3f
                
                sheepModel?.applyPose(sheepPose)
            }
            PetType.FISH -> {
                fishPose.bodySway = sin(animTime * 2f) * 5f
                fishPose.tailSway = sin(animTime * 4f) * 20f
                fishPose.pectoralAngle = sin(animTime * 3f) * 10f
                fishPose.mouthOpen = sin(animTime * 1.5f) * 0.3f + 0.5f
                fishPose.blink = if ((animTime * 0.6f).toInt() % 8 == 0) -0.8f else 0f
                fishPose.bodyBob = sin(animTime * 1.2f) * 0.02f
                
                fishModel?.applyPose(fishPose)
            }
        }
    }
    
    fun render(width: Float, height: Float) {
        if (!isReady) return
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        
        camera.update(width, height)
        shader.use()
        
        val vp = camera.vpMatrix
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        
        val ldLen = sqrt(lightDir[0]*lightDir[0] + lightDir[1]*lightDir[1] + lightDir[2]*lightDir[2])
        GLES20.glUniform3f(shader.uLightDirLoc, lightDir[0]/ldLen, lightDir[1]/ldLen, lightDir[2]/ldLen)
        GLES20.glUniform1f(shader.uAmbientLoc, ambient)
        GLES20.glUniform1f(shader.uTimeLoc, animTime)
        
        // === 天空 ===
        val skyColor = when {
            timeOfDay < 5f || timeOfDay >= 20f -> floatArrayOf(0.05f, 0.08f, 0.15f)
            timeOfDay in 5f..7f -> floatArrayOf(0.7f, 0.4f, 0.3f)
            timeOfDay in 17f..19f -> floatArrayOf(0.7f, 0.35f, 0.2f)
            else -> floatArrayOf(0.3f, 0.6f, 0.9f)
        }
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        setShaderUniforms(mvp, model)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(false)
        GLES20.glUniform4f(shader.uColorLoc, skyColor[0], skyColor[1], skyColor[2], 1f)
        skyDome?.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        
        // === 地面 ===
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, 0f, -0.7f, 0f)
        val groundColor = if (currentPetType == PetType.FISH) floatArrayOf(0.1f, 0.2f, 0.4f) else floatArrayOf(0.3f, 0.6f, 0.2f)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        setShaderUniforms(mvp, model)
        GLES20.glUniform4f(shader.uColorLoc, groundColor[0], groundColor[1], groundColor[2], 1f)
        ground?.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        
        // === 草地 ===
        if (currentPetType == PetType.SHEEP) {
            for ((i, blade) in sheepEnv.grassBlades.withIndex()) {
                if (i >= grassModels.size) break
                val gModel = grassModels[i]
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, (blade.x) * 0.5f, -0.7f, blade.z * 0.5f)
                val sway = sin(animTime * 2f + blade.swayPhase) * sheepEnv.windStrength * 0.15f
                Matrix.rotateM(model, 0, sway, 0f, 0f, 1f)
                Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
                setShaderUniforms(mvp, model)
                GLES20.glUniform4f(shader.uColorLoc, blade.colorR, blade.colorG, blade.colorB, 1f)
                gModel.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
            }
            
            // 花朵
            for (flower in flowerModels) {
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, flower.translateX, flower.translateY, flower.translateZ)
                Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
                setShaderUniforms(mvp, model)
                flower.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
            }
            
            // 蝴蝶
            for (bfly in butterflyModels) {
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, bfly.translateX, bfly.translateY, bfly.translateZ)
                Matrix.rotateM(model, 0, bfly.rotateY, 0f, 1f, 0f)
                Matrix.rotateM(model, 0, bfly.rotateX, 1f, 0f, 0f)
                Matrix.scaleM(model, 0, 1f + sin(animTime * 8f) * 0.3f, 1f, 1f)
                Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
                setShaderUniforms(mvp, model)
                bfly.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
            }
        }
        
        // === 宠物 ===
        Matrix.setIdentityM(model, 0)
        val pScale = when (currentPetType) {
            PetType.SHEEP -> petScale * 0.5f
            PetType.FISH -> petScale * 0.6f
        }
        Matrix.translateM(model, 0, petX * 0.5f, -0.1f, petZ * 0.5f)
        Matrix.scaleM(model, 0, pScale, pScale, pScale)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        setShaderUniforms(mvp, model)
        GLES20.glUniform4f(shader.uColorLoc, 1f, 1f, 1f, 1f)
        activePet?.draw(model, mvp, shader.aPositionLoc, shader.aNormalLoc, shader.uModelLoc, shader.uColorLoc)
        
        // === 粒子 ===
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        for (p in particles) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, p.x, p.y, p.z + 0.5f)
            val s = p.size * (p.life / p.maxLife)
            Matrix.scaleM(model, 0, s, s, s)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            setShaderUniforms(mvp, model)
            GLES20.glUniform4f(shader.uColorLoc, p.r, p.g, p.b, p.life / p.maxLife)
            // 简单球体代替粒子
            val tempM = FloatArray(16)
            Matrix.setIdentityM(tempM, 0)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }
    
    private fun setShaderUniforms(mvp: FloatArray, model: FloatArray) {
        GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
    }
    
    fun addParticlesForEffect(type: String, count: Int = 10) {
        when (type) {
            "hearts" -> for (i in 0 until count) addParticle(
                (r()-0.5f)*0.3f, 0.2f+r()*0.3f, (r()-0.5f)*0.3f,
                (r()-0.5f)*0.2f, 0.3f+r()*0.3f, (r()-0.5f)*0.2f,
                0.8f+r()*0.5f, 1f, 0.3f, 0.5f, 0.03f)
            "stars" -> for (i in 0 until count) addParticle(
                (r()-0.5f)*0.4f, 0.3f+r()*0.3f, (r()-0.5f)*0.4f,
                (r()-0.5f)*0.3f, 0.2f+r()*0.4f, (r()-0.5f)*0.3f,
                0.6f+r()*0.4f, 1f, 0.8f, 0.2f, 0.025f)
            "notes" -> for (i in 0 until count) addParticle(
                (r()-0.5f)*0.2f, 0.1f+r()*0.2f, (r()-0.5f)*0.2f,
                (r()-0.5f)*0.1f, 0.2f, (r()-0.5f)*0.1f,
                0.5f+r()*0.3f, 0.5f, 0.3f, 1f, 0.02f)
            "blush" -> for (i in 0 until count) addParticle(
                (r()-0.5f)*0.3f, -0.1f, (r()-0.5f)*0.3f,
                (r()-0.5f)*0.1f, 0.1f, (r()-0.5f)*0.1f,
                0.4f+r()*0.3f, 1f, 0.4f, 0.6f, 0.04f)
            "bubbles" -> for (i in 0 until count) addParticle(
                (r()-0.5f)*0.2f, 0f, (r()-0.5f)*0.2f,
                (r()-0.5f)*0.1f, 0.2f+r()*0.3f, (r()-0.5f)*0.1f,
                0.6f+r()*0.4f, 0.5f, 0.7f, 1f, 0.015f)
        }
    }
    
    private fun addParticle(x: Float, y: Float, z: Float, vx: Float, vy: Float, vz: Float,
                            life: Float, r: Float, g: Float, b: Float, size: Float) {
        particles.add(Particle(x, y, z, vx, vy, vz, life, life, r, g, b, size))
    }
    
    fun getCamera(): Camera = camera
    
    fun cleanup() {
        if (isReady) {
            GLES20.glDeleteProgram(shader.programId)
        }
    }
    
    private fun r() = Math.random().toFloat()
}

package com.chongwu.pet

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.chongwu.pet.ai.SheepState
import com.chongwu.pet.ecology.Environment
import com.chongwu.pet.interaction.TouchEngine
import com.chongwu.pet.render.gl.PetRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 3D宠物视图 - 基于GLSurfaceView
 */
class GLSheepView(context: Context) : GLSurfaceView(context) {
    
    val brain = SheepState()
    val environment = Environment(context)
    private val renderer = PetRenderer()
    private val touchEngine = TouchEngine()
    
    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((Float, Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    
    // 屏幕尺寸
    private var screenW = 1080f
    private var screenH = 1920f
    
    // 小羊移动
    private var sheepX = 0f
    private var targetX = 0f
    private var moveSpeed = 30f
    private var facingRight = true
    
    // 触摸
    private var isTouchingSheep = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // 音效
    var audioEngine: com.chongwu.pet.audio.AudioEngine? = null
    
    // 运行状态
    var isRunning = false
    
    init {
        setEGLContextClientVersion(2)
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                // 初始化渲染器
                renderer.sheepState = brain
                renderer.environment = environment
                renderer.init(screenW, screenH)
                renderer.setBrainMoveSpeed { brainMoveSpeed() }
            }
            
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                screenW = width.toFloat()
                screenH = height.toFloat()
                GLES20.glViewport(0, 0, width, height)
            }
            
            override fun onDrawFrame(gl: GL10?) {
                val dt = 0.016f
                val timeInfo = environment.getTimeInfo()
                
                // 更新AI
                brain.update(dt, isTouchingSheep, timeInfo.hour, environment.getWeatherName())
                
                // 更新环境
                environment.updateWeather(dt)
                environment.updateGrass(dt, environment.getWeather() == Environment.Weather.RAINY)
                
                // 更新小羊位置
                updateSheepPosition(dt)
                
                // 渲染
                renderer.update(dt)
                renderer.render(screenW, screenH)
                
                // 音效
                triggerSoundIfNeeded()
            }
        })
        
        renderMode = RENDERMODE_CONTINUOUSLY
        isRunning = true
    }
    
    private var lastSoundTime = 0f
    private var soundTimer = 0f
    
    private fun triggerSoundIfNeeded() {
        soundTimer += 0.016f
        val state = brain.currentState
        
        // 特定状态触发音效
        when {
            state == SheepState.State.SINGING && soundTimer - lastSoundTime > 3f -> {
                audioEngine?.playBleat()
                lastSoundTime = soundTimer
            }
            state == SheepState.State.STARTLED -> {
                audioEngine?.playStartle()
                lastSoundTime = soundTimer
            }
            state == SheepState.State.HOPPING && soundTimer - lastSoundTime > 2f -> {
                audioEngine?.playHop()
                lastSoundTime = soundTimer
            }
            state == SheepState.State.GRAZING && Math.random() < 0.01f -> {
                audioEngine?.playEat()
            }
        }
    }
    
    private fun updateSheepPosition(dt: Float) {
        val state = brain.currentState
        
        when {
            state == SheepState.State.SLEEPING -> { /* 不动 */ }
            brain.isMoving() -> {
                if (Math.abs(sheepX - targetX) > 0.01f) {
                    val dir = Math.signum(targetX - sheepX)
                    sheepX += dir * moveSpeed * dt * brain.moveSpeed * 0.3f
                    facingRight = dir > 0
                } else {
                    targetX = -0.8f + Math.random().toFloat() * 1.6f
                }
            }
            state == SheepState.State.GRAZING -> {
                // 慢慢移动吃草
                if (Math.random() < 0.01f) {
                    targetX = -0.8f + Math.random().toFloat() * 1.6f
                }
                if (Math.abs(sheepX - targetX) > 0.05f) {
                    sheepX += Math.signum(targetX - sheepX) * 8f * dt
                }
            }
            state == SheepState.State.PLAYING || state == SheepState.State.HOPPING -> {
                if (Math.abs(sheepX - targetX) > 0.01f) {
                    val dir = Math.signum(targetX - sheepX)
                    sheepX += dir * 60f * dt
                    facingRight = dir > 0
                } else {
                    targetX = -0.8f + Math.random().toFloat() * 1.6f
                }
            }
        }
    }
    
    private fun brainMoveSpeed(): Float {
        return (sheepX + 0.5f) * 0.8f + 0.2f
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x / screenW * 2f - 1f
        val y = -(event.y / screenH * 2f - 1f)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (Math.abs(x - sheepX * 0.3f) < 0.2f && Math.abs(y - 0.1f) < 0.3f) {
                    isTouchingSheep = true
                    lastTouchX = event.x
                    lastTouchY = event.y
                    
                    val part = touchEngine.detectPart(event.x, event.y, screenW, screenH)
                    if (part != null && part != TouchEngine.BodyPart.NONE) {
                        val partName = mapPartName(part)
                        brain.onTouched(partName)
                        triggerEffect(partName)
                        
                        when (partName) {
                            "HEAD" -> audioEngine?.playBleat()
                            "TAIL" -> audioEngine?.playHappy()
                        }
                    }
                    
                    onDragStart?.invoke()
                    return true
                }
                isTouchingSheep = false
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isTouchingSheep) {
                    val dx = (event.x - lastTouchX) / screenW * 4f
                    val dy = (event.y - lastTouchY) / screenH * 4f
                    sheepX = Math.max(-0.8f, Math.min(0.8f, sheepX + dx))
                    onDragMove?.invoke(event.x - screenW / 2f, event.y - screenH / 2f)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isTouchingSheep) {
                    isTouchingSheep = false
                    onDragEnd?.invoke()
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun mapPartName(part: TouchEngine.BodyPart): String = when (part) {
        TouchEngine.BodyPart.HEAD -> "HEAD"
        TouchEngine.BodyPart.HORN_LEFT, TouchEngine.BodyPart.HORN_RIGHT -> "HORN"
        TouchEngine.BodyPart.BODY -> "BODY"
        TouchEngine.BodyPart.LEG -> "LEG"
        TouchEngine.BodyPart.TAIL -> "TAIL"
        TouchEngine.BodyPart.EAR_LEFT, TouchEngine.BodyPart.EAR_RIGHT -> "HEAD"
        TouchEngine.BodyPart.EYE -> "HEAD"
        else -> "NONE"
    }
    
    private fun triggerEffect(part: String) {
        when (part) {
            "HEAD" -> { renderer.fxHearts = 1.5f; renderer.addParticlesForEffect("hearts", 8) }
            "HORN" -> { renderer.fxStars = 2f; renderer.addParticlesForEffect("stars", 10) }
            "BODY" -> { renderer.fxBlush = 2f; renderer.addParticlesForEffect("blush", 6) }
            "LEG" -> { renderer.fxExclaim = 1f }
            "TAIL" -> { renderer.fxNotes = 1.5f; renderer.addParticlesForEffect("notes", 6) }
        }
    }
    
    fun cleanup() {
        isRunning = false
        renderer.cleanup()
    }
}

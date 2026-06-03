package com.chongwu.pet

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.chongwu.pet.audio.AudioEngine
import com.chongwu.pet.interaction.TouchEngine
import com.chongwu.pet.render.gl.PetRenderer
import com.chongwu.pet.state.FishState
import com.chongwu.pet.state.SheepState
import com.chongwu.pet.util.ShakeDetector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class GLPetView(context: Context, private var petType: PetType) : GLSurfaceView(context) {

    val renderer = PetRenderer()
    private val touchEngine = TouchEngine()
    private val shakeDetector = ShakeDetector(context)
    private var audioEngine: AudioEngine? = null

    private val fishState = FishState()
    private val sheepState = SheepState()
    private var overlayService: PetOverlayService? = null

    private var screenW = 1080f; private var screenH = 1920f

    private var petX = 0f; private var petZ = 0f
    private var targetPetX = 0f; private var targetPetZ = 0f

    private var isTouchingPet = false
    private var lastTouchX = 0f; private var lastTouchY = 0f
    private var touchPart = TouchEngine.BodyPart.NONE
    private var touchDownTime = 0L
    private var isLongPressHandled = false
    private val LONG_PRESS_MS = 600L

    private var interactionTimer = 0f
    private var isIdle = false
    private var idleFpsCounter = 0

    private var lastInteractionTime = 0f
    private var isSleeping = false
    private var sleepTimer = 0f

    private var currentAlpha = 1.0f

    var isRunning = false

    init {
        // 初始化音效
        try {
            audioEngine = AudioEngine(context)
            audioEngine?.init()
        } catch (_: Exception) {}

        setEGLContextClientVersion(2)
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                renderer.switchPet(petType)
                renderer.init(screenW, screenH)
            }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                screenW = width.toFloat(); screenH = height.toFloat()
                GLES20.glViewport(0, 0, width, height)
            }
            override fun onDrawFrame(gl: GL10?) {
                val dt = calcDt()
                val currentState = if (petType == PetType.FISH) fishState else sheepState
                val isTouched = isTouchingPet
                val hour = (System.currentTimeMillis() / 3600000 % 24).toInt()

                if (petType == PetType.FISH) fishState.update(dt, isTouched, hour)
                else sheepState.update(dt, isTouched, hour, "SUNNY")

                updatePetPosition(dt, currentState)
                renderer.petX += (petX - renderer.petX) * 0.05f
                renderer.petZ += (petZ - renderer.petZ) * 0.05f
                renderer.petScale = if (petType == PetType.FISH) 0.8f else 1.0f

                renderer.update(dt)
                renderer.render(screenW, screenH)

                if (isTouchingPet || touchPart != TouchEngine.BodyPart.NONE) interactionTimer = 0f
                else interactionTimer += dt

                triggerSounds(dt, currentState)
                updateSleep(dt, currentState)
                updateTransparency(dt)
                updateFrameRate()
            }
        })
        renderMode = RENDERMODE_CONTINUOUSLY
        isRunning = true

        shakeDetector.onShake = { onShakeDetected() }
        shakeDetector.start()
    }

    private var lastFrameTime = 0L
    private fun calcDt(): Float {
        val now = System.nanoTime()
        val dt = if (lastFrameTime == 0L) 0.016f else (now - lastFrameTime) / 1_000_000_000f
        lastFrameTime = now
        return minOf(dt, 0.05f)
    }

    private fun updateTransparency(dt: Float) {
        if (isTouchingPet || touchDownTime > 0) {
            currentAlpha = minOf(1f, currentAlpha + dt * 2f)
        } else if (interactionTimer > 8f) {
            currentAlpha = maxOf(0.4f, currentAlpha - dt * 0.3f)
        } else {
            currentAlpha = minOf(1f, currentAlpha + dt * 0.5f)
        }
        overlayService?.setOverlayAlpha(currentAlpha)
    }

    private fun updatePetPosition(dt: Float, state: Any) {
        when (state) {
            is FishState -> {
                when {
                    state.currentState == FishState.State.IDLE || state.currentState == FishState.State.FLOATING -> {
                        targetPetX += (r() - 0.5f) * 0.02f
                    }
                    state.isMoving() -> {
                        val dir = state.swimDirection
                        targetPetX += cos(dir) * state.swimSpeed * 0.3f * dt
                        targetPetZ += sin(dir) * state.swimSpeed * 0.3f * dt
                    }
                    state.currentState == FishState.State.FEEDING -> {
                        targetPetX += (r() - 0.5f) * 0.01f
                    }
                    state.currentState == FishState.State.STARTLED -> {
                        targetPetX += (r() - 0.5f) * 0.2f
                        targetPetZ += (r() - 0.5f) * 0.2f
                    }
                }
                targetPetX = maxOf(-0.8f, minOf(0.8f, targetPetX))
                targetPetZ = maxOf(-0.8f, minOf(0.8f, targetPetZ))
                petX += (targetPetX - petX) * minOf(1f, dt * 3f)
                petZ += (targetPetZ - petZ) * minOf(1f, dt * 3f)
            }
            is SheepState -> {
                when {
                    state.isMoving() || state.currentState == SheepState.State.EXPLORING -> {
                        if (abs(petX - targetPetX) < 0.05f) {
                            targetPetX = -0.8f + r() * 1.6f
                            targetPetZ = -0.3f + r() * 0.6f
                        }
                        val dir = sign(targetPetX - petX)
                        petX += dir * state.moveSpeed * 0.3f * dt
                        petZ += (targetPetZ - petZ) * 0.02f
                    }
                    state.currentState == SheepState.State.GRAZING -> {
                        if (r() < 0.01f) targetPetX = -0.8f + r() * 1.6f
                        petX += sign(targetPetX - petX) * 0.15f * dt
                    }
                    state.currentState == SheepState.State.HOPPING -> {
                        if (abs(petX - targetPetX) < 0.05f) targetPetX = -0.8f + r() * 1.6f
                        petX += sign(targetPetX - petX) * 0.8f * dt
                    }
                    state.currentState == SheepState.State.STARTLED -> {
                        val dir = -sign(petX)
                        petX += dir * 2f * dt
                    }
                }
                petX = maxOf(-0.8f, minOf(0.8f, petX))
                petZ = maxOf(-0.3f, minOf(0.3f, petZ))
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouchingPet = true; touchDownTime = System.currentTimeMillis()
                isLongPressHandled = false; lastTouchX = x; lastTouchY = y
                val part = touchEngine.detectPart(x, y, screenW, screenH)
                touchPart = part ?: TouchEngine.BodyPart.NONE
                if (part != null && part != TouchEngine.BodyPart.NONE) {
                    onTouched(part)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchPart != TouchEngine.BodyPart.NONE && !isLongPressHandled) {
                    val dx = (x - lastTouchX) / screenW * 2f
                    petX = maxOf(-0.8f, minOf(0.8f, petX + dx))
                    lastTouchX = x
                    return true
                }
                // 检测长按
                if (touchPart != TouchEngine.BodyPart.NONE && !isLongPressHandled &&
                    System.currentTimeMillis() - touchDownTime > LONG_PRESS_MS) {
                    isLongPressHandled = true
                    onLongPress(touchPart)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchingPet = false; touchPart = TouchEngine.BodyPart.NONE
                touchDownTime = 0L; isLongPressHandled = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onTouched(part: TouchEngine.BodyPart) {
        renderer.addParticlesForEffect("hearts", 5)
        if (petType == PetType.FISH) {
            fishState.changeState(FishState.State.PLAYING)
            when (part) {
                TouchEngine.BodyPart.HEAD -> { renderer.fxHearts = 1.5f; renderer.addParticlesForEffect("hearts", 8); audioEngine?.playBubble() }
                TouchEngine.BodyPart.TAIL -> { renderer.fxBubbles = 2f; renderer.addParticlesForEffect("bubbles", 10); audioEngine?.playBubble() }
                TouchEngine.BodyPart.FIN -> { renderer.addParticlesForEffect("bubbles", 4) }
                else -> {}
            }
        } else {
            val partName = when (part) {
                TouchEngine.BodyPart.HEAD -> "HEAD"; TouchEngine.BodyPart.BODY -> "BODY"
                TouchEngine.BodyPart.LEG -> "LEG"; TouchEngine.BodyPart.TAIL -> "TAIL"
                TouchEngine.BodyPart.HORN -> "HORN"; else -> "NONE"
            }
            sheepState.onTouched(partName)
            when (part) {
                TouchEngine.BodyPart.HEAD -> { renderer.fxHearts = 1.5f; renderer.addParticlesForEffect("hearts", 8); audioEngine?.playBleat() }
                TouchEngine.BodyPart.BODY -> { renderer.fxBlush = 2f; renderer.addParticlesForEffect("blush", 6) }
                TouchEngine.BodyPart.LEG -> { renderer.fxNotes = 1f; if (r() < 0.3f) audioEngine?.playStartle() }
                TouchEngine.BodyPart.TAIL -> { renderer.fxNotes = 1.5f; renderer.addParticlesForEffect("notes", 6); audioEngine?.playHop() }
                TouchEngine.BodyPart.HORN -> { renderer.fxStars = 2f; renderer.addParticlesForEffect("stars", 10) }
                else -> {}
            }
        }
    }

    private fun onLongPress(part: TouchEngine.BodyPart) {
        if (petType == PetType.FISH) {
            fishState.changeState(FishState.State.FEEDING)
            renderer.addParticlesForEffect("bubbles", 15)
        } else {
            sheepState.changeState(SheepState.State.CURIOUS)
            renderer.addParticlesForEffect("stars", 8)
            audioEngine?.playHappy()
        }
    }

    private fun onShakeDetected() {
        lastInteractionTime = 0f
        if (isSleeping) { isSleeping = false; sleepTimer = 0f }
        if (petType == PetType.FISH) {
            fishState.changeState(FishState.State.STARTLED)
            renderer.addParticlesForEffect("bubbles", 15); audioEngine?.playStartle()
        } else {
            sheepState.changeState(SheepState.State.STARTLED)
            renderer.addParticlesForEffect("stars", 12); audioEngine?.playStartle()
        }
    }

    private var soundTimer = 0f; private var lastSoundTime = 0f
    private fun triggerSounds(dt: Float, state: Any) {
        soundTimer += dt
        if (petType == PetType.FISH) {
            val fs = state as FishState
            when {
                (fs.currentState == FishState.State.FEEDING || fs.currentState == FishState.State.PLAYING) && soundTimer - lastSoundTime > 3f -> {
                    audioEngine?.playBubble(); lastSoundTime = soundTimer
                }
            }
        } else {
            val ss = state as SheepState
            when {
                ss.currentState == SheepState.State.SINGING && soundTimer - lastSoundTime > 3f -> { audioEngine?.playBleat(); lastSoundTime = soundTimer }
                ss.currentState == SheepState.State.STARTLED -> { audioEngine?.playStartle(); lastSoundTime = soundTimer }
                ss.currentState == SheepState.State.HOPPING && soundTimer - lastSoundTime > 2f -> { audioEngine?.playHop(); lastSoundTime = soundTimer }
                ss.currentState == SheepState.State.GRAZING && r() < 0.01f -> audioEngine?.playEat()
            }
        }
    }

    private fun updateSleep(dt: Float, state: Any) {
        if (isTouchingPet) { lastInteractionTime = 0f; isSleeping = false; sleepTimer = 0f; return }
        lastInteractionTime += dt
        if (lastInteractionTime > 30f && !isSleeping) {
            isSleeping = true
            if (state is FishState) state.changeState(FishState.State.SLEEPING)
            else if (state is SheepState) state.changeState(SheepState.State.SLEEPING)
        }
        if (isSleeping) {
            sleepTimer += dt
            if (state is FishState && state.hunger < 0.2f) { isSleeping = false; sleepTimer = 0f; state.changeState(FishState.State.FEEDING) }
            if (state is SheepState && state.hunger < 0.2f) { isSleeping = false; sleepTimer = 0f; state.changeState(SheepState.State.GRAZING) }
        }
    }

    private fun updateFrameRate() {
        if (isTouchingPet || interactionTimer < 3f) {
            if (isIdle) { renderMode = RENDERMODE_CONTINUOUSLY; isIdle = false }
        } else if (interactionTimer > 5f) {
            if (!isIdle) { renderMode = RENDERMODE_WHEN_DIRTY; isIdle = true }
            idleFpsCounter++
            if (idleFpsCounter % 4 == 0) requestRender()
        }
    }

    fun switchPet(newType: PetType) {
        petType = newType
        renderer.switchPet(newType)
        if (newType == PetType.FISH) fishState.changeState(FishState.State.IDLE)
        else sheepState.changeState(SheepState.State.IDLE)
        petX = 0f; targetPetX = 0f; petZ = 0f; targetPetZ = 0f
    }

    fun getPetType() = petType
    fun setAudioEngine(engine: AudioEngine?) { audioEngine = engine }
    fun setOverlayService(service: PetOverlayService?) { overlayService = service }

    fun cleanup() {
        isRunning = false; shakeDetector.stop(); renderer.cleanup()
        audioEngine?.release()
    }

    private fun r() = Math.random().toFloat()
}

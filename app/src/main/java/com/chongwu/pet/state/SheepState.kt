package com.chongwu.pet.state

import kotlin.math.*

class SheepState {

    enum class State(val displayName: String) {
        IDLE("发呆"), WALKING("散步"), GRAZING("吃草"),
        SLEEPING("睡觉"), HOPPING("蹦跳"), PLAYING("玩耍"),
        EXPLORING("探索"), STARTLED("惊吓"), CURIOUS("好奇"), SINGING("咩咩叫")
    }

    var currentState = State.IDLE
    var previousState = State.IDLE
    var stateTimer = 0f
    var hunger = 0.5f; var energy = 0.8f; var happiness = 0.7f
    var moveSpeed = 1.0f; var curiosityLevel = 0f
    var isBeingTouched = false; var touchPart = ""; var touchCount = 0
    var expression = "NEUTRAL"; var headTilt = 0f
    private var headTiltTarget = 0f

    fun update(dt: Float, isTouched: Boolean, hour: Int, weatherType: String) {
        isBeingTouched = isTouched; stateTimer += dt
        hunger = maxOf(0f, hunger - dt * 0.008f)
        energy = maxOf(0f, energy - dt * 0.005f)
        happiness = minOf(1f, hunger * 0.5f + energy * 0.5f)
        updateExpression()

        if (isTouched && currentState != State.PLAYING && currentState != State.CURIOUS) {
            if (stateTimer > 0.5f) { changeState(State.PLAYING); return }
        }
        when (currentState) {
            State.IDLE -> {
                moveSpeed *= 0.9f; headTiltTarget = 0f
                if (stateTimer > 2f + r() * 3f) {
                    val roll = r(); val isNight = hour < 5 || hour >= 20
                    when { isNight || energy < 0.3f -> changeState(State.SLEEPING)
                        roll < 0.25f && hunger < 0.4f -> changeState(State.GRAZING)
                        roll < 0.45f -> changeState(State.EXPLORING)
                        roll < 0.65f -> changeState(State.WALKING)
                        roll < 0.80f -> changeState(State.CURIOUS)
                        else -> changeState(State.SINGING) }
                }
            }
            State.WALKING -> {
                moveSpeed = 1.5f + r() * 0.5f
                if (stateTimer > 3f + r() * 4f) changeState(State.IDLE)
            }
            State.GRAZING -> {
                moveSpeed = 0.2f; headTiltTarget = 25f
                hunger = minOf(1f, hunger + dt * 0.03f)
                if (hunger > 0.75f || stateTimer > 5f + r() * 3f) { headTiltTarget = 0f; changeState(State.IDLE) }
            }
            State.SLEEPING -> {
                moveSpeed = 0f; headTiltTarget = 30f
                energy = minOf(1f, energy + dt * 0.02f)
                val isNight = hour < 5 || hour >= 20
                if ((energy > 0.85f && stateTimer > 3f) || (!isNight && energy > 0.7f)) {
                    headTiltTarget = 0f; changeState(State.IDLE)
                }
            }
            State.EXPLORING -> {
                moveSpeed = 1.2f + r() * 0.3f; curiosityLevel = 1f; headTiltTarget = 10f
                if (stateTimer > 4f + r() * 3f) { curiosityLevel = 0f; headTiltTarget = 0f; changeState(State.IDLE) }
            }
            State.HOPPING -> { moveSpeed = 3f; if (stateTimer > 1.5f + r() * 2f) changeState(State.IDLE) }
            State.PLAYING -> {
                moveSpeed = 2f; headTiltTarget = -5f
                if ((!isBeingTouched && stateTimer > 3f) || stateTimer > 8f) {
                    headTiltTarget = 0f; changeState(State.IDLE)
                }
            }
            State.STARTLED -> { headTiltTarget = -20f; if (stateTimer > 0.8f) { headTiltTarget = 0f; changeState(State.IDLE) } }
            State.SINGING -> {
                headTiltTarget = -15f + sin(stateTimer * 4f) * 10f
                if (stateTimer > 2f + r() * 2f) { headTiltTarget = 0f; changeState(State.IDLE) }
            }
            State.CURIOUS -> {
                curiosityLevel = 1f; headTiltTarget = 20f + sin(stateTimer * 3f) * 10f
                if (stateTimer > 3f + r() * 2f) { curiosityLevel = 0f; headTiltTarget = 0f; changeState(State.IDLE) }
            }
        }
        headTilt += (headTiltTarget - headTilt) * 0.08f
    }

    private fun updateExpression() {
        expression = when {
            currentState == State.SLEEPING -> "SLEEPY"
            currentState == State.HOPPING || currentState == State.PLAYING || currentState == State.SINGING -> "HAPPY"
            currentState == State.STARTLED || currentState == State.CURIOUS -> "SURPRISED"
            hunger < 0.2f -> "SAD"
            happiness > 0.8f -> "HAPPY"
            energy < 0.2f -> "SLEEPY"
            else -> "NEUTRAL"
        }
    }

    fun onTouched(part: String) {
        touchPart = part; touchCount++; happiness = minOf(1f, happiness + 0.1f)
        when (part) {
            "HEAD" -> changeState(State.PLAYING)
            "BODY" -> changeState(State.PLAYING)
            "LEG" -> if (r() < 0.3f) changeState(State.STARTLED) else changeState(State.HOPPING)
            "TAIL" -> changeState(State.HOPPING)
            else -> if (currentState != State.PLAYING) changeState(State.CURIOUS)
        }
    }

    fun changeState(newState: State) {
        if (currentState == newState) return
        previousState = currentState; currentState = newState; stateTimer = 0f
    }

    fun isMoving() = currentState in listOf(State.WALKING, State.EXPLORING, State.HOPPING)
    private fun r() = Math.random().toFloat()
}

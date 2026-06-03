package com.chongwu.pet.state

import kotlin.math.*

class FishState {

    enum class State(val displayName: String) {
        IDLE("发呆"), SWIMMING("游动"), EXPLORING("探索"),
        SLEEPING("休息"), STARTLED("惊吓"), CURIOUS("好奇"),
        PLAYING("玩耍"), FEEDING("进食"), FLOATING("漂浮")
    }

    var currentState = State.IDLE
    var previousState = State.IDLE
    var stateTimer = 0f
    var hunger = 0.6f; var energy = 0.8f; var happiness = 0.7f
    var swimSpeed = 1.0f; var swimDirection = 0f
    var curiosityLevel = 0f
    var isBeingTouched = false
    var touchCount = 0

    fun update(dt: Float, isTouched: Boolean, hour: Int) {
        isBeingTouched = isTouched; stateTimer += dt
        hunger = maxOf(0f, hunger - dt * 0.006f)
        energy = maxOf(0f, energy - dt * 0.004f)
        happiness = minOf(1f, hunger * 0.5f + energy * 0.5f)

        if (isTouched && currentState != State.PLAYING && currentState != State.CURIOUS) {
            if (stateTimer > 0.5f) { changeState(State.CURIOUS); return }
        }
        when (currentState) {
            State.IDLE -> {
                swimSpeed *= 0.9f
                if (stateTimer > 2f + random() * 4f) {
                    val roll = random(); val isNight = hour < 5 || hour >= 20
                    when { isNight || energy < 0.3f -> changeState(State.SLEEPING)
                        hunger < 0.4f -> changeState(State.FEEDING)
                        roll < 0.45f -> changeState(State.SWIMMING)
                        roll < 0.65f -> changeState(State.EXPLORING)
                        roll < 0.80f -> changeState(State.FLOATING)
                        else -> swimDirection = random() * PI.toFloat() * 2f }
                }
            }
            State.SWIMMING -> {
                swimSpeed = 1.2f + random() * 0.5f
                if (stateTimer > 3f + random() * 3f) changeState(State.IDLE)
                swimDirection += (random() - 0.5f) * 0.1f
            }
            State.EXPLORING -> {
                swimSpeed = 0.8f + random() * 0.3f; curiosityLevel = 1f
                if (stateTimer > 4f + random() * 3f) { curiosityLevel = 0f; changeState(State.IDLE) }
            }
            State.SLEEPING -> {
                swimSpeed = 0f; energy = minOf(1f, energy + dt * 0.015f)
                val isNight = hour < 5 || hour >= 20
                if ((energy > 0.85f && stateTimer > 3f) || (!isNight && energy > 0.7f)) changeState(State.IDLE)
            }
            State.STARTLED -> { swimSpeed = 3f; if (stateTimer > 0.6f) changeState(State.IDLE) }
            State.CURIOUS -> {
                swimSpeed = 0.5f; curiosityLevel = 1f
                if ((!isBeingTouched && stateTimer > 2f) || stateTimer > 5f) {
                    curiosityLevel = 0f; changeState(State.IDLE)
                }
            }
            State.PLAYING -> {
                swimSpeed = 2f; swimDirection += dt * 3f
                if ((!isBeingTouched && stateTimer > 3f) || stateTimer > 8f) changeState(State.IDLE)
            }
            State.FEEDING -> {
                swimSpeed = 0.3f; hunger = minOf(1f, hunger + dt * 0.03f)
                if (hunger > 0.75f || stateTimer > 4f + random() * 3f) changeState(State.IDLE)
            }
            State.FLOATING -> { swimSpeed = 0.1f; if (stateTimer > 2f + random() * 3f) changeState(State.IDLE) }
        }
    }

    fun onTouched() {
        touchCount++; happiness = minOf(1f, happiness + 0.08f)
        if (random() < 0.3f) changeState(State.STARTLED) else changeState(State.PLAYING)
    }

    fun changeState(newState: State) {
        if (currentState == newState) return
        previousState = currentState; currentState = newState; stateTimer = 0f
    }

    fun isMoving() = currentState in listOf(State.SWIMMING, State.EXPLORING, State.PLAYING)
    private fun random() = Math.random().toFloat()
}

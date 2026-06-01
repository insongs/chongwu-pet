package com.chongwu.pet.ai

import kotlin.math.*

/**
 * 小羊AI大脑 - 状态机 + 需求系统
 */
class SheepState {
    
    enum class State(val displayName: String) {
        IDLE("发呆"),
        WALKING("散步"),
        GRAZING("吃草"),
        SLEEPING("睡觉"),
        HOPPING("蹦跳"),
        PLAYING("玩耍"),
        EXPLORING("探索"),
        HEADBUTTING("顶角"),
        STARTLED("惊吓"),
        FOLLOWING("跟随"),
        DRINKING("喝水"),
        SINGING("咩咩叫"),
        CURIOUS("好奇"),
        HIDING("躲藏"),
        GREETING("打招呼")
    }
    
    var currentState = State.IDLE
    var previousState = State.IDLE
    var stateTimer = 0f
    
    // 需求系统
    var hunger = 0.5f       // 0=饿, 1=饱
    var energy = 0.8f       // 0=累, 1=精力充沛
    var happiness = 0.7f    // 0=不开心, 1=开心
    var thirst = 0.3f       // 0=渴, 1=不渴
    var socialNeed = 0.5f   // 社交需求
    
    // 情绪表达
    var expression = "NEUTRAL"  // NEUTRAL, HAPPY, SAD, ANGRY, SURPRISED, SLEEPY
    var earWag = 0f
    
    // 行为参数
    var moveSpeed = 1.0f
    var curiosityLevel = 0f
    var isDaytime = true
    private var weatherChangeTimer = 0f
    
    // 环境感知
    var nearbyObjects = mutableListOf<String>()
    var isBeingTouched = false
    var lastTouchPart = ""
    var touchCount = 0
    
    // 学习/记忆
    var memory = mutableMapOf<String, Float>()  // 事件 -> 记忆强度
    var favoriteSpotX = 0f
    var favoriteSpotY = 0f
    
    fun update(dt: Float, isTouched: Boolean, hour: Int, weatherType: String) {
        isDaytime = hour in 6..18
        isBeingTouched = isTouched
        stateTimer += dt
        weatherChangeTimer += dt
        
        // 更新需求（随时间变化）
        hunger = maxOf(0f, hunger - dt * 0.008f)
        energy = maxOf(0f, energy - dt * 0.005f)
        thirst = maxOf(0f, thirst - dt * 0.006f)
        socialNeed = maxOf(0f, socialNeed - dt * 0.003f)
        
        // 幸福感受各需求影响
        happiness = (hunger * 0.3f + energy * 0.3f + (1f - thirst) * 0.2f + socialNeed * 0.2f)
        happiness = minOf(1f, happiness)
        
        // 更新表情
        updateExpression()
        
        // 状态机逻辑
        if (isTouched && currentState != State.GREETING && currentState != State.PLAYING) {
            if (stateTimer > 1f) {
                changeState(State.PLAYING)
                return
            }
        }
        
        when (currentState) {
            State.IDLE -> {
                moveSpeed *= 0.9f
                if (stateTimer > 2f + Math.random().toFloat() * 3f) {
                    val roll = Math.random().toFloat()
                    when {
                        roll < 0.25f && hunger < 0.4f -> changeState(State.GRAZING)
                        roll < 0.40f && energy < 0.3f -> changeState(State.SLEEPING)
                        roll < 0.55f -> changeState(State.EXPLORING)
                        roll < 0.70f -> changeState(State.WALKING)
                        roll < 0.80f && isDaytime -> changeState(State.HOPPING)
                        roll < 0.90f -> changeState(State.CURIOUS)
                        else -> changeState(State.SINGING)
                    }
                }
                // idle时轻微摇头晃脑
                earWag = sin(stateTimer * 2f) * 5f
            }
            
            State.WALKING -> {
                moveSpeed = 1.5f + Math.random().toFloat() * 0.5f
                if (stateTimer > 3f + Math.random().toFloat() * 4f) {
                    changeState(State.IDLE)
                }
            }
            
            State.GRAZING -> {
                moveSpeed = 0.2f
                hunger = minOf(1f, hunger + dt * 0.03f)
                if (hunger > 0.75f || stateTimer > 5f + Math.random().toFloat() * 3f) {
                    changeState(State.IDLE)
                }
            }
            
            State.SLEEPING -> {
                moveSpeed = 0f
                energy = minOf(1f, energy + dt * 0.02f)
                if ((energy > 0.85f && stateTimer > 3f) || (!isDaytime && energy > 0.95f)) {
                    changeState(State.IDLE)
                }
                // 夜晚睡更久
                if (!isDaytime && stateTimer < 15f) {
                    // stay sleeping
                } else if (isDaytime && stateTimer > 5f) {
                    changeState(State.IDLE)
                }
            }
            
            State.EXPLORING -> {
                moveSpeed = 1.2f + Math.random().toFloat() * 0.3f
                curiosityLevel = 1f
                if (stateTimer > 4f + Math.random().toFloat() * 3f) {
                    curiosityLevel = 0f
                    changeState(State.IDLE)
                }
            }
            
            State.HOPPING -> {
                moveSpeed = 3f
                if (stateTimer > 1.5f + Math.random().toFloat() * 2f) {
                    changeState(State.IDLE)
                }
            }
            
            State.PLAYING -> {
                if (!isBeingTouched && stateTimer > 3f) {
                    changeState(State.IDLE)
                } else if (stateTimer > 8f) {
                    changeState(State.IDLE)
                }
            }
            
            State.HEADBUTTING -> {
                if (stateTimer > 1.5f) {
                    changeState(State.IDLE)
                }
            }
            
            State.STARTLED -> {
                if (stateTimer > 0.8f) {
                    changeState(State.IDLE)
                }
            }
            
            State.SINGING -> {
                if (stateTimer > 2f + Math.random().toFloat() * 2f) {
                    changeState(State.IDLE)
                }
            }
            
            State.CURIOUS -> {
                curiosityLevel = 1f
                headTiltTarget = 15f + sin(stateTimer * 3f) * 10f
                if (stateTimer > 3f + Math.random().toFloat() * 2f) {
                    curiosityLevel = 0f
                    headTiltTarget = 0f
                    changeState(State.IDLE)
                }
            }
            
            State.GREETING -> {
                if (stateTimer > 1.5f) {
                    changeState(State.IDLE)
                }
            }
            
            State.DRINKING -> {
                thirst = minOf(1f, thirst + dt * 0.04f)
                if (thirst > 0.85f || stateTimer > 4f) {
                    changeState(State.IDLE)
                }
            }
            
            State.HIDING -> {
                moveSpeed = 0f
                if (stateTimer > 3f) {
                    changeState(State.IDLE)
                }
            }
            
            State.FOLLOWING -> {
                moveSpeed = 2f
                if (stateTimer > 5f) {
                    changeState(State.IDLE)
                }
            }
        }
    }
    
    private var headTiltTarget = 0f
    var headTilt = 0f
    
    private fun updateExpression() {
        headTilt += (headTiltTarget - headTilt) * 0.1f
        
        expression = when {
            currentState == State.SLEEPING -> "SLEEPY"
            currentState == State.STARTLED -> "SURPRISED"
            currentState == State.HOPPING || currentState == State.PLAYING -> "HAPPY"
            currentState == State.GRAZING -> "NEUTRAL"
            hunger < 0.2f -> "SAD"
            happiness > 0.8f -> "HAPPY"
            energy < 0.2f -> "SLEEPY"
            currentState == State.HEADBUTTING -> "ANGRY"
            currentState == State.SINGING -> "HAPPY"
            currentState == State.CURIOUS -> "SURPRISED"
            else -> "NEUTRAL"
        }
    }
    
    fun onTouched(part: String) {
        lastTouchPart = part
        touchCount++
        
        when (part) {
            "HEAD" -> {
                happiness = minOf(1f, happiness + 0.1f)
                changeState(State.GREETING)
            }
            "HORN" -> {
                happiness = minOf(1f, happiness + 0.05f)
                changeState(State.HEADBUTTING)
            }
            "BODY" -> {
                happiness = minOf(1f, happiness + 0.08f)
                if (currentState != State.SLEEPING) changeState(State.PLAYING)
            }
            "LEG" -> {
                if (Math.random().toFloat() < 0.3f) {
                    changeState(State.STARTLED)
                }
            }
            "TAIL" -> {
                happiness = minOf(1f, happiness + 0.12f)
                changeState(State.HOPPING)
            }
        }
        
        // 记忆
        memory["touched_$part"] = (memory["touched_$part"] ?: 0f) + 0.2f
    }
    
    fun changeState(newState: State) {
        if (currentState == newState) return
        previousState = currentState
        currentState = newState
        stateTimer = 0f
    }
    
    fun isMoving(): Boolean {
        return currentState in listOf(State.WALKING, State.EXPLORING, State.HOPPING, State.FOLLOWING)
    }
    
    fun wantsToGrazing(): Boolean = hunger < 0.4f
    fun wantsToSleep(): Boolean = energy < 0.3f || (!isDaytime && energy < 0.6f)
}

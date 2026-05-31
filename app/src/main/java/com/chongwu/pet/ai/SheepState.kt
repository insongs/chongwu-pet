package com.chongwu.pet.ai

/**
 * 小羊 AI 大脑 - 状态机 + 需求系统 + 性格
 */
class SheepState {

    // ==================== 状态枚举 ====================
    enum class State {
        IDLE,           // 发呆
        WALKING,        // 走路
        HOPPING,        // 蹦跳
        GRAZING,        // 吃草
        SLEEPING,       // 睡觉
        HEADBUTTING,    // 顶人/顶东西
        CLIMBING,       // 攀爬
        EXPLORING,      // 探索
        SOCIAL,         // 社交蹭蹭
        PLAYING,        // 玩耍
        STRETCHING,     // 伸懒腰
        SCRATCHING,     // 挠痒
        STARTLED       // 被吓到
    }

    // ==================== 需求系统 ====================
    data class Needs(
        var hunger: Float = 100f,       // 饱食度 0-100
        var energy: Float = 100f,       // 精力 0-100
        var social: Float = 80f,        // 社交欲 0-100
        var curiosity: Float = 60f,     // 好奇心 0-100
        var affection: Float = 50f      // 亲密值 0-100
    )

    // ==================== 性格维度 ====================
    data class Personality(
        var liveliness: Float = 50f,    // 活泼 ← → 慵懒
        var bravery: Float = 50f,       // 勇敢 ← → 胆小
        var clinginess: Float = 50f,    // 粘人 ← → 独立
        var curiosity: Float = 50f      // 好奇 ← → 谨慎
    )

    // ==================== 情绪 ====================
    enum class Mood {
        HAPPY, CONTENT, PLAYFUL, TIRED, SCARED, GRUMPY, LONELY, CURIOUS
    }

    // ==================== 成长阶段 ====================
    enum class GrowthStage(val days: Int) {
        LAMB(0),        // 羊羔
        JUVENILE(3),    // 少年
        ADULT(7)        // 成年
    }

    // 当前状态
    var currentState: State = State.IDLE
    var previousState: State = State.IDLE
    var stateTime: Float = 0f           // 在当前状态停留的时间

    // 需求
    val needs = Needs()

    // 性格（初始随机偏中间）
    val personality = Personality(
        liveliness = 40f + Math.random().toFloat() * 20f,
        bravery = 40f + Math.random().toFloat() * 20f,
        clinginess = 40f + Math.random().toFloat() * 20f,
        curiosity = 40f + Math.random().toFloat() * 20f
    )

    // 成长阶段
    var growthStage: GrowthStage = GrowthStage.LAMB
    var ageDays: Int = 0

    // 情绪
    var mood: Mood = Mood.HAPPY
    var moodTimer: Float = 0f

    // 当前表情字符串 (给渲染器用)
    var expression: String = "HAPPY"

    // 小动作计数器
    var idleActionTimer: Float = 0f
    var nextIdleAction: Float = 2f + Math.random().toFloat() * 5f

    // ==================== 决策逻辑 ====================
    fun update(dt: Float, isTouching: Boolean, hour: Int, weather: String) {
        stateTime += dt
        idleActionTimer += dt
        moodTimer += dt

        // 需求自然变化
        needs.hunger = max(0f, needs.hunger - dt * 0.3f)
        needs.energy = max(0f, needs.energy - dt * 0.2f)
        needs.social = max(0f, needs.social - dt * 0.15f)
        needs.curiosity = min(100f, needs.curiosity + dt * 0.1f)

        // 夜晚更易困
        val nightFactor = if (hour in 22..23 || hour in 0..5) 3f else 1f
        needs.energy -= dt * 0.1f * nightFactor

        // 天气影响
        val weatherScale = when (weather) {
            "rainy" -> 0.7f    // 下雨不爱动
            "snowy" -> 1.3f    // 下雪超兴奋
            else -> 1.0f
        }

        // === 决策 ===
        if (currentState == State.IDLE && idleActionTimer >= nextIdleAction) {
            idleActionTimer = 0f
            nextIdleAction = 2f + Math.random().toFloat() * 8f
            changeState(pickIdleAction(weatherScale))
        }

        // 紧急需求优先
        when {
            needs.energy < 15f && currentState != State.SLEEPING ->
                changeState(State.SLEEPING)
            needs.hunger < 30f && currentState != State.GRAZING ->
                changeState(State.GRAZING)
            needs.social < 20f && !isTouching && currentState != State.SOCIAL ->
                changeState(State.SOCIAL)
        }

        // 状态持续时间过长自动切换
        when (currentState) {
            State.SLEEPING -> if (stateTime > 15f && needs.energy > 70f) changeState(State.STRETCHING)
            State.GRAZING -> if (stateTime > 8f) changeState(State.IDLE)
            State.HOPPING -> if (stateTime > 4f) changeState(State.IDLE)
            State.HEADBUTTING -> if (stateTime > 2f) changeState(State.HOPPING)
            State.STRETCHING -> if (stateTime > 2f) changeState(State.IDLE)
            State.STARTLED -> if (stateTime > 1.5f) changeState(State.IDLE)
            State.PLAYING -> if (stateTime > 6f) changeState(State.IDLE)
            else -> {}
        }

        // 更新表情
        updateExpression()
    }

    private fun pickIdleAction(weatherScale: Float): State {
        val r = Math.random().toFloat()
        val exploreWeight = personality.curiosity * 0.3f
        val lazyWeight = (100f - personality.liveliness) * 0.3f
        return when {
            r < 0.2f -> State.IDLE          // 继续发呆
            r < 0.35f -> State.SCRATCHING   // 挠痒
            r < 0.5f + exploreWeight * 0.01f -> State.EXPLORING
            r < 0.65f + lazyWeight * 0.01f -> State.STRETCHING
            r < 0.75f -> State.HOPPING
            r < 0.85f -> State.PLAYING
            else -> State.WALKING
        }
    }

    private fun changeState(newState: State) {
        previousState = currentState
        currentState = newState
        stateTime = 0f
    }

    private fun updateExpression() {
        expression = when (currentState) {
            State.SLEEPING -> "SLEEPING"
            State.GRAZING -> "CONTENT"
            State.HOPPING, State.PLAYING -> "LAUGHING"
            State.HEADBUTTING -> "ANGRY"
            State.STARTLED -> "SURPRISED"
            State.SCRATCHING -> "DIZZY"
            State.STRETCHING -> "CONTENT"
            State.IDLE -> if (mood == Mood.TIRED) "TIRED" else "HAPPY"
            else -> "HAPPY"
        }
    }

    fun onTouched(bodyPart: String) {
        when (bodyPart) {
            "HEAD" -> { needs.affection = min(100f, needs.affection + 5f); changeState(State.HOPPING) }
            "BODY" -> { needs.affection = min(100f, needs.affection + 8f); mood = Mood.CONTENT }
            "HORN" -> { changeState(State.HEADBUTTING) }
            "LEG" -> { changeState(State.STARTLED) }
            "TAIL" -> { mood = Mood.PLAYFUL; changeState(State.PLAYING) }
        }
    }

    fun feed(foodType: String) {
        needs.hunger = min(100f, needs.hunger + 30f)
        needs.affection = min(100f, needs.affection + 3f)
        mood = Mood.HAPPY
        when (foodType) {
            "grass" -> changeState(State.GRAZING)
            "fruit" -> changeState(State.HOPPING)
            else -> changeState(State.PLAYING)
        }
    }
}

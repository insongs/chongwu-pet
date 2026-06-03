package com.chongwu.pet.model

/**
 * 3D Q版有角绵羊 - 圆润可爱的写实风格
 */
class SheepModel3D {

    private var isBuilt = false
    val root = Model3D()

    val bodyPart = Model3D()
    val head = Model3D()
    val tail = Model3D()
    val leftFrontLeg = Model3D()
    val rightFrontLeg = Model3D()
    val leftBackLeg = Model3D()
    val rightBackLeg = Model3D()
    val leftEar = Model3D()
    val rightEar = Model3D()
    val leftEye = Model3D()
    val rightEye = Model3D()
    val snout = Model3D()
    val leftHorn = Model3D()
    val rightHorn = Model3D()
    val woolTop = Model3D()

    fun build(): Model3D {
        if (isBuilt) return root
        isBuilt = true

        // ===== 身体 (圆润椭球) =====
        val body = PrimitiveBuilder.createEllipsoid(0.55f, 0.42f, 0.35f, 16, 18)
        body.colorR = 0.88f; body.colorG = 0.92f; body.colorB = 0.95f
        bodyPart.children.add(body)

        // 肚皮毛
        val belly = PrimitiveBuilder.createEllipsoid(0.4f, 0.2f, 0.3f, 10, 12)
        belly.colorR = 0.92f; belly.colorG = 0.95f; belly.colorB = 1.0f
        belly.translateY = -0.2f
        bodyPart.children.add(belly)

        // 羊毛球装饰
        for (i in 0..6) {
            val wb = PrimitiveBuilder.createSphere(0.05f, 6, 6)
            wb.colorR = 0.9f; wb.colorG = 0.93f; wb.colorB = 0.98f
            val angle = i * 51.4f * kotlin.math.PI.toFloat() / 180f
            wb.translateX = kotlin.math.cos(angle) * 0.45f
            wb.translateZ = kotlin.math.sin(angle) * 0.25f
            wb.translateY = 0.1f
            bodyPart.children.add(wb)
        }

        // 头顶蓬松毛
        val woolM = PrimitiveBuilder.createEllipsoid(0.15f, 0.1f, 0.12f, 10, 10)
        woolM.colorR = 0.9f; woolM.colorG = 0.93f; woolM.colorB = 0.98f
        woolTop.children.add(woolM)
        woolTop.translateY = 0.42f; woolTop.translateZ = 0.15f

        // ===== 头部 =====
        val headM = PrimitiveBuilder.createSphere(0.2f, 12, 12)
        headM.colorR = 0.92f; headM.colorG = 0.94f; headM.colorB = 0.96f
        head.children.add(headM)
        head.translateY = 0.32f; head.translateZ = 0.42f
        head.scaleX = 0.85f; head.scaleY = 0.8f; head.scaleZ = 0.75f

        // ===== 鼻子 =====
        val snoutM = PrimitiveBuilder.createEllipsoid(0.07f, 0.04f, 0.06f, 8, 8)
        snoutM.colorR = 1.0f; snoutM.colorG = 0.85f; snoutM.colorB = 0.9f
        snout.children.add(snoutM)
        snout.translateY = 0.25f; snout.translateZ = 0.58f

        // ===== 眼睛 =====
        val eW = PrimitiveBuilder.createSphere(0.055f, 8, 8)
        eW.colorR = 1f; eW.colorG = 1f; eW.colorB = 1f
        leftEye.children.add(eW)
        val lP = PrimitiveBuilder.createSphere(0.035f, 8, 8)
        lP.colorR = 0.15f; lP.colorG = 0.2f; lP.colorB = 0.35f; lP.translateZ = 0.04f
        leftEye.children.add(lP)
        leftEye.translateX = -0.1f; leftEye.translateY = 0.37f; leftEye.translateZ = 0.5f

        rightEye.children.add(PrimitiveBuilder.createSphere(0.055f, 8, 8).apply {
            colorR = 1f; colorG = 1f; colorB = 1f
        })
        val rP = PrimitiveBuilder.createSphere(0.035f, 8, 8)
        rP.colorR = 0.15f; rP.colorG = 0.2f; rP.colorB = 0.35f; rP.translateZ = 0.04f
        rightEye.children.add(rP)
        rightEye.translateX = 0.1f; rightEye.translateY = 0.37f; rightEye.translateZ = 0.5f

        // ===== 耳朵 =====
        val ear = PrimitiveBuilder.createEllipsoid(0.07f, 0.025f, 0.1f, 8, 8)
        ear.colorR = 0.9f; ear.colorG = 0.85f; ear.colorB = 0.92f
        leftEar.children.add(ear)
        leftEar.translateX = -0.18f; leftEar.translateY = 0.38f; leftEar.translateZ = 0.3f
        leftEar.rotateZ = 25f; leftEar.rotateX = -20f

        rightEar.children.add(PrimitiveBuilder.createEllipsoid(0.07f, 0.025f, 0.1f, 8, 8).apply {
            colorR = 0.9f; colorG = 0.85f; colorB = 0.92f
        })
        rightEar.translateX = 0.18f; rightEar.translateY = 0.38f; rightEar.translateZ = 0.3f
        rightEar.rotateZ = -25f; rightEar.rotateX = -20f

        // ===== 弯角 (使用PrimitiveBuilder的弯曲管状体) =====
        val hornColorR = 0.55f; val hornColorG = 0.5f; val hornColorB = 0.45f
        val leftHornM = PrimitiveBuilder.createCurvedHorn(0.4f, 0.05f, 0.012f, 140f, 10, 8)
        leftHornM.colorR = hornColorR; leftHornM.colorG = hornColorG; leftHornM.colorB = hornColorB
        leftHorn.children.add(leftHornM)
        leftHorn.translateX = -0.08f; leftHorn.translateY = 0.45f; leftHorn.translateZ = 0.25f
        leftHorn.rotateX = -15f; leftHorn.rotateY = -20f; leftHorn.rotateZ = 5f

        val rightHornM = PrimitiveBuilder.createCurvedHorn(0.4f, 0.05f, 0.012f, 140f, 10, 8)
        rightHornM.colorR = hornColorR; rightHornM.colorG = hornColorG; rightHornM.colorB = hornColorB
        rightHorn.children.add(rightHornM)
        rightHorn.translateX = 0.08f; rightHorn.translateY = 0.45f; rightHorn.translateZ = 0.25f
        rightHorn.rotateX = -15f; rightHorn.rotateY = 20f; rightHorn.rotateZ = -5f

        // ===== 腿 =====
        val leg = PrimitiveBuilder.createCylinder(0.04f, 0.25f, 8, true)
        leg.colorR = 0.82f; leg.colorG = 0.85f; leg.colorB = 0.9f
        leftFrontLeg.children.add(leg)
        leftFrontLeg.translateX = -0.22f; leftFrontLeg.translateY = -0.52f; leftFrontLeg.translateZ = 0.15f

        rightFrontLeg.children.add(PrimitiveBuilder.createCylinder(0.04f, 0.25f, 8, true).apply {
            colorR = 0.82f; colorG = 0.85f; colorB = 0.9f
        })
        rightFrontLeg.translateX = 0.22f; rightFrontLeg.translateY = -0.52f; rightFrontLeg.translateZ = 0.15f

        leftBackLeg.children.add(PrimitiveBuilder.createCylinder(0.04f, 0.25f, 8, true).apply {
            colorR = 0.82f; colorG = 0.85f; colorB = 0.9f
        })
        leftBackLeg.translateX = -0.22f; leftBackLeg.translateY = -0.52f; leftBackLeg.translateZ = -0.2f

        rightBackLeg.children.add(PrimitiveBuilder.createCylinder(0.04f, 0.25f, 8, true).apply {
            colorR = 0.82f; colorG = 0.85f; colorB = 0.9f
        })
        rightBackLeg.translateX = 0.22f; rightBackLeg.translateY = -0.52f; rightBackLeg.translateZ = -0.2f

        // ===== 尾巴 =====
        val tailM = PrimitiveBuilder.createSphere(0.055f, 8, 8)
        tailM.colorR = 0.9f; tailM.colorG = 0.93f; tailM.colorB = 0.98f
        tail.children.add(tailM)
        tail.translateY = -0.08f; tail.translateZ = -0.38f

        // ===== 腮红 =====
        val bl1 = PrimitiveBuilder.createSphere(0.035f, 6, 6)
        bl1.colorR = 1.0f; bl1.colorG = 0.5f; bl1.colorB = 0.6f; bl1.colorA = 0.3f
        bl1.translateX = -0.13f; bl1.translateY = 0.28f; bl1.translateZ = 0.5f; head.children.add(bl1)
        val bl2 = PrimitiveBuilder.createSphere(0.035f, 6, 6)
        bl2.colorR = 1.0f; bl2.colorG = 0.5f; bl2.colorB = 0.6f; bl2.colorA = 0.3f
        bl2.translateX = 0.13f; bl2.translateY = 0.28f; bl2.translateZ = 0.5f; head.children.add(bl2)

        // 组装
        root.children.addAll(listOf(bodyPart, woolTop, head, snout,
            leftEye, rightEye, leftEar, rightEar, leftHorn, rightHorn,
            tail, leftFrontLeg, rightFrontLeg, leftBackLeg, rightBackLeg))
        return root
    }

    fun applyPose(pose: Pose) {
        bodyPart.rotateX = pose.bodyTilt
        bodyPart.rotateZ = pose.bodySway

        head.rotateX = pose.headTilt
        head.rotateZ = pose.headSway

        tail.rotateX = pose.tailWag
        tail.rotateZ = pose.tailSway

        leftFrontLeg.rotateX = pose.leftFrontLegAngle
        rightFrontLeg.rotateX = pose.rightFrontLegAngle
        leftBackLeg.rotateX = pose.leftBackLegAngle
        rightBackLeg.rotateX = pose.rightBackLegAngle

        leftFrontLeg.translateY = -0.52f + pose.bobOffset
        rightFrontLeg.translateY = -0.52f - pose.bobOffset
        leftBackLeg.translateY = -0.52f - pose.bobOffset
        rightBackLeg.translateY = -0.52f + pose.bobOffset

        bodyPart.scaleX = 1f + pose.squashX
        bodyPart.scaleY = 1f + pose.squashY
        bodyPart.scaleZ = 1f + pose.squashZ

        leftEye.scaleX = 1f + pose.blink
        rightEye.scaleX = 1f + pose.blink

        leftEar.rotateZ = 25f + pose.earFlap
        rightEar.rotateZ = -25f - pose.earFlap
        leftHorn.rotateZ = 5f + pose.hornTilt
        rightHorn.rotateZ = -5f - pose.hornTilt
    }

    data class Pose(
        var bodyTilt: Float = 0f, var bodySway: Float = 0f,
        var headTilt: Float = 0f, var headSway: Float = 0f,
        var tailWag: Float = 0f, var tailSway: Float = 0f,
        var leftFrontLegAngle: Float = 0f, var rightFrontLegAngle: Float = 0f,
        var leftBackLegAngle: Float = 0f, var rightBackLegAngle: Float = 0f,
        var bobOffset: Float = 0f,
        var squashX: Float = 0f, var squashY: Float = 0f, var squashZ: Float = 0f,
        var blink: Float = 0f, var earFlap: Float = 0f, var hornTilt: Float = 0f
    )
}

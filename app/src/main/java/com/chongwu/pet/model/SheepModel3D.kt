package com.chongwu.pet.model

import kotlin.math.*

/**
 * 3D Q版花影羚羊模型
 * 风格：Q版圆润，头身比1:2.5，蓝色系角，花影装饰
 */
class SheepModel3D {
    
    private var isBuilt = false
    private val root = Model3D()
    
    // 动画用引用
    val body = Model3D()
    val head = Model3D()
    val leftHorn = Model3D()
    val rightHorn = Model3D()
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
    
    // 身体装饰（花瓣）
    val flowerDecor1 = Model3D()
    val flowerDecor2 = Model3D()
    val flowerDecor3 = Model3D()
    
    // 花影纹样（身体上的花纹）
    val markings = mutableListOf<Model3D>()
    
    fun build(): Model3D {
        if (isBuilt) return root
        isBuilt = true
        
        val bodySphere = PrimitiveBuilder.createEllipsoid(0.6f, 0.5f, 0.4f, 20, 20)
        bodySphere.colorR = 0.85f; bodySphere.colorG = 0.90f; bodySphere.colorB = 1.0f // 淡蓝白
        body.children.add(bodySphere)
        body.translateY = 0.0f
        
        // ===== 头部 =====
        val headSphere = PrimitiveBuilder.createSphere(0.3f, 16, 16)
        headSphere.colorR = 0.85f; headSphere.colorG = 0.90f; headSphere.colorB = 1.0f
        head.children.add(headSphere)
        head.translateX = 0f; head.translateY = 0.45f; head.translateZ = 0.45f
        head.scaleX = 0.85f; head.scaleY = 0.85f; head.scaleZ = 0.7f
        
        // ===== 鼻子/口鼻部 =====
        val snoutSphere = PrimitiveBuilder.createEllipsoid(0.12f, 0.08f, 0.12f, 12, 12)
        snoutSphere.colorR = 1.0f; snoutSphere.colorG = 0.85f; snoutSphere.colorB = 0.9f // 粉嫩
        snout.children.add(snoutSphere)
        snout.translateX = 0f; snout.translateY = 0.35f; snout.translateZ = 0.7f
        
        // ===== 眼睛 =====
        val eyeSphere = PrimitiveBuilder.createSphere(0.06f, 12, 12)
        eyeSphere.colorR = 0.15f; eyeSphere.colorG = 0.2f; eyeSphere.colorB = 0.4f // 深蓝黑色瞳孔
        val eyeWhite = PrimitiveBuilder.createSphere(0.09f, 12, 12)
        eyeWhite.colorR = 1.0f; eyeWhite.colorG = 1.0f; eyeWhite.colorB = 1.0f
        
        leftEye.children.add(eyeWhite)
        val leftPupil = PrimitiveBuilder.createSphere(0.05f, 12, 12)
        leftPupil.colorR = 0.2f; leftPupil.colorG = 0.3f; leftPupil.colorB = 0.6f
        leftPupil.translateZ = 0.07f
        leftEye.children.add(leftPupil)
        leftEye.translateX = -0.15f; leftEye.translateY = 0.5f; leftEye.translateZ = 0.5f
        
        rightEye.children.add(PrimitiveBuilder.createSphere(0.09f, 12, 12).apply {
            colorR = 1f; colorG = 1f; colorB = 1f
        })
        val rightPupil = PrimitiveBuilder.createSphere(0.05f, 12, 12)
        rightPupil.colorR = 0.2f; rightPupil.colorG = 0.3f; rightPupil.colorB = 0.6f
        rightPupil.translateZ = 0.07f
        rightEye.children.add(rightPupil)
        rightEye.translateX = 0.15f; rightEye.translateY = 0.5f; rightEye.translateZ = 0.5f
        
        // ===== 耳朵 =====
        val ear = PrimitiveBuilder.createEllipsoid(0.1f, 0.04f, 0.15f, 12, 12)
        ear.colorR = 0.9f; ear.colorG = 0.85f; ear.colorB = 1.0f
        leftEar.children.add(ear)
        leftEar.translateX = -0.28f; leftEar.translateY = 0.55f; leftEar.translateZ = 0.3f
        leftEar.rotateZ = -20f
        
        rightEar.children.add(PrimitiveBuilder.createEllipsoid(0.1f, 0.04f, 0.15f, 12, 12).apply {
            colorR = 0.9f; colorG = 0.85f; colorB = 1.0f
        })
        rightEar.translateX = 0.28f; rightEar.translateY = 0.55f; rightEar.translateZ = 0.3f
        rightEar.rotateZ = 20f
        
        // ===== 角——弯曲的长角（蓝色系，花影羚羊特征） =====
        val hornColorR = 0.3f; val hornColorG = 0.5f; val hornColorB = 0.9f
        
        // 左角 - 使用弯曲管状体
        val leftHornMesh = PrimitiveBuilder.createCurvedHorn(0.5f, 0.06f, 0.015f, 120f, 10, 8)
        leftHornMesh.colorR = hornColorR; leftHornMesh.colorG = hornColorG; leftHornMesh.colorB = hornColorB
        leftHorn.children.add(leftHornMesh)
        leftHorn.translateX = -0.12f; leftHorn.translateY = 0.6f; leftHorn.translateZ = 0.25f
        leftHorn.rotateX = -10f; leftHorn.rotateY = -15f
        
        // 右角
        val rightHornMesh = PrimitiveBuilder.createCurvedHorn(0.5f, 0.06f, 0.015f, 120f, 10, 8)
        rightHornMesh.colorR = hornColorR; rightHornMesh.colorG = hornColorG; rightHornMesh.colorB = hornColorB
        rightHorn.children.add(rightHornMesh)
        rightHorn.translateX = 0.12f; rightHorn.translateY = 0.6f; rightHorn.translateZ = 0.25f
        rightHorn.rotateX = -10f; rightHorn.rotateY = 15f
        
        // 角上的小装饰（花芽）
        val hornDecor = PrimitiveBuilder.createSphere(0.025f, 8, 8)
        hornDecor.colorR = 0.8f; hornDecor.colorG = 0.4f; hornDecor.colorB = 0.8f
        
        // ===== 腿 =====
        val leg = PrimitiveBuilder.createCylinder(0.06f, 0.35f, 10)
        leg.colorR = 0.8f; leg.colorG = 0.85f; leg.colorB = 0.95f
        
        // 蹄子
        val hoof = PrimitiveBuilder.createCylinder(0.07f, 0.04f, 8)
        hoof.colorR = 0.4f; hoof.colorG = 0.5f; hoof.colorB = 0.6f
        hoof.translateY = -0.195f
        
        leftFrontLeg.children.add(Model3D().apply {
            children.add(leg); children.add(hoof)
        })
        leftFrontLeg.translateX = -0.25f; leftFrontLeg.translateY = -0.4f; leftFrontLeg.translateZ = 0.2f
        
        rightFrontLeg.children.add(Model3D().apply {
            children.add(PrimitiveBuilder.createCylinder(0.06f, 0.35f, 10).apply {
                colorR = 0.8f; colorG = 0.85f; colorB = 0.95f
            })
            children.add(PrimitiveBuilder.createCylinder(0.07f, 0.04f, 8).apply {
                colorR = 0.4f; colorG = 0.5f; colorB = 0.6f
                translateY = -0.195f
            })
        })
        rightFrontLeg.translateX = 0.25f; rightFrontLeg.translateY = -0.4f; rightFrontLeg.translateZ = 0.2f
        
        leftBackLeg.children.add(Model3D().apply {
            children.add(PrimitiveBuilder.createCylinder(0.06f, 0.35f, 10).apply {
                colorR = 0.8f; colorG = 0.85f; colorB = 0.95f
            })
            children.add(PrimitiveBuilder.createCylinder(0.07f, 0.04f, 8).apply {
                colorR = 0.4f; colorG = 0.5f; colorB = 0.6f
                translateY = -0.195f
            })
        })
        leftBackLeg.translateX = -0.25f; leftBackLeg.translateY = -0.4f; leftBackLeg.translateZ = -0.25f
        
        rightBackLeg.children.add(Model3D().apply {
            children.add(PrimitiveBuilder.createCylinder(0.06f, 0.35f, 10).apply {
                colorR = 0.8f; colorG = 0.85f; colorB = 0.95f
            })
            children.add(PrimitiveBuilder.createCylinder(0.07f, 0.04f, 8).apply {
                colorR = 0.4f; colorG = 0.5f; colorB = 0.6f
                translateY = -0.195f
            })
        })
        rightBackLeg.translateX = 0.25f; rightBackLeg.translateY = -0.4f; rightBackLeg.translateZ = -0.25f
        
        // ===== 尾巴 =====
        val tailSphere = PrimitiveBuilder.createSphere(0.08f, 12, 12)
        tailSphere.colorR = 0.85f; tailSphere.colorG = 0.9f; tailSphere.colorB = 1.0f
        tail.children.add(tailSphere)
        tail.translateX = 0f; tail.translateY = 0.1f; tail.translateZ = -0.5f
        
        // ===== 花瓣装饰（花影羚羊特征） =====
        val flowerColors = listOf(
            Triple(0.7f, 0.4f, 0.9f), // 紫
            Triple(0.4f, 0.6f, 1.0f), // 蓝
            Triple(0.9f, 0.5f, 0.7f)  // 粉
        )
        
        for (i in 0..5) {
            val petal = PrimitiveBuilder.createEllipsoid(0.06f, 0.02f, 0.03f, 8, 8)
            val (cr, cg, cb) = flowerColors[i % 3]
            petal.colorR = cr; petal.colorG = cg; petal.colorB = cb
            val angle = (i * 60f) * PI / 180f
            val petalModel = Model3D()
            petalModel.children.add(petal)
            petalModel.translateX = cos(angle.toFloat()) * 0.02f
            petalModel.translateZ = sin(angle.toFloat()) * 0.02f
            petalModel.rotateY = (i * 60f)
            flowerDecor1.children.add(petalModel)
        }
        flowerDecor1.translateX = 0.25f; flowerDecor1.translateY = 0.1f; flowerDecor1.translateZ = 0.3f
        flowerDecor1.scaleX = 1.5f; flowerDecor1.scaleY = 1.5f; flowerDecor1.scaleZ = 1.5f
        
        // 第二个花瓣装饰
        for (i in 0..4) {
            val petal = PrimitiveBuilder.createEllipsoid(0.04f, 0.015f, 0.02f, 8, 8)
            val (cr, cg, cb) = flowerColors[(i + 1) % 3]
            petal.colorR = cr; petal.colorG = cg; petal.colorB = cb
            val angle = (i * 72f) * PI / 180f
            val petalModel = Model3D()
            petalModel.children.add(petal)
            petalModel.translateX = cos(angle.toFloat()) * 0.015f
            petalModel.translateZ = sin(angle.toFloat()) * 0.015f
            petalModel.rotateY = (i * 72f)
            flowerDecor2.children.add(petalModel)
        }
        flowerDecor2.translateX = -0.2f; flowerDecor2.translateY = -0.05f; flowerDecor2.translateZ = -0.2f
        flowerDecor2.scaleX = 1.2f; flowerDecor2.scaleY = 1.2f; flowerDecor2.scaleZ = 1.2f
        
        // 花影纹样（身体上的发光花纹）
        for (i in 0..3) {
            val mark = PrimitiveBuilder.createSphere(0.03f, 8, 8)
            val (cr, cg, cb) = flowerColors[(i + 2) % 3]
            mark.colorR = cr * 0.7f; mark.colorG = cg * 0.7f; mark.colorB = cb * 0.7f
            mark.colorA = 0.4f
            val markModel = Model3D()
            markModel.children.add(mark)
            val angle = (i * 90f + 45f) * PI / 180f
            markModel.translateX = cos(angle.toFloat()) * 0.35f
            markModel.translateZ = sin(angle.toFloat()) * 0.25f
            markModel.translateY = -0.1f
            markings.add(markModel)
        }
        
        // ===== 组装 =====
        root.children.add(body)
        root.children.add(head)
        root.children.add(snout)
        root.children.add(leftEye)
        root.children.add(rightEye)
        root.children.add(leftEar)
        root.children.add(rightEar)
        root.children.add(leftHorn)
        root.children.add(rightHorn)
        root.children.add(tail)
        root.children.add(leftFrontLeg)
        root.children.add(rightFrontLeg)
        root.children.add(leftBackLeg)
        root.children.add(rightBackLeg)
        root.children.add(flowerDecor1)
        root.children.add(flowerDecor2)
        for (mark in markings) root.children.add(mark)
        
        return root
    }
    
    /** 应用动画姿势 */
    fun applyPose(pose: Pose) {
        body.rotateX = pose.bodyTilt
        body.rotateZ = pose.bodySway
        
        head.rotateX = pose.headTilt
        head.rotateZ = pose.headSway
        
        leftHorn.rotateZ = pose.hornAngle
        rightHorn.rotateZ = -pose.hornAngle
        
        tail.rotateX = pose.tailWag
        tail.rotateZ = pose.tailSway
        
        leftFrontLeg.rotateX = pose.leftFrontLegAngle
        rightFrontLeg.rotateX = pose.rightFrontLegAngle
        leftBackLeg.rotateX = pose.leftBackLegAngle
        rightBackLeg.rotateX = pose.rightBackLegAngle
        
        leftFrontLeg.translateY = -0.4f + pose.bobOffset
        rightFrontLeg.translateY = -0.4f - pose.bobOffset
        leftBackLeg.translateY = -0.4f - pose.bobOffset
        rightBackLeg.translateY = -0.4f + pose.bobOffset
        
        body.scaleX = 1f + pose.squashX
        body.scaleY = 1f + pose.squashY
        body.scaleZ = 1f + pose.squashZ
        
        leftEye.scaleX = 1f + pose.blink
        rightEye.scaleX = 1f + pose.blink
        
        flowerDecor1.rotateY = pose.flowerSpin
        flowerDecor2.rotateY = -pose.flowerSpin * 0.7f
    }
    
    data class Pose(
        var bodyTilt: Float = 0f,
        var bodySway: Float = 0f,
        var headTilt: Float = 0f,
        var headSway: Float = 0f,
        var hornAngle: Float = 0f,
        var tailWag: Float = 0f,
        var tailSway: Float = 0f,
        var leftFrontLegAngle: Float = 0f,
        var rightFrontLegAngle: Float = 0f,
        var leftBackLegAngle: Float = 0f,
        var rightBackLegAngle: Float = 0f,
        var bobOffset: Float = 0f,
        var squashX: Float = 0f,
        var squashY: Float = 0f,
        var squashZ: Float = 0f,
        var blink: Float = 0f,
        var flowerSpin: Float = 0f
    )
}

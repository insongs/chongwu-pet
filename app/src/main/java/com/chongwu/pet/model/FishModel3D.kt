package com.chongwu.pet.model

/**
 * 程序化3D金鱼模型 - 圆胖可爱风格
 * 身体朝向+X, 尾巴在-X, 头在+X
 */
class FishModel3D {

    private var isBuilt = false
    val root = Model3D()

    val bodyPart = Model3D()
    val tailFin = Model3D()
    val dorsalFin = Model3D()
    val leftPectoralFin = Model3D()
    val rightPectoralFin = Model3D()
    val leftEye = Model3D()
    val rightEye = Model3D()
    val mouth = Model3D()
    val tailLower = Model3D()

    fun build(): Model3D {
        if (isBuilt) return root
        isBuilt = true

        // ===== 圆胖身体 =====
        val bodyMesh = PrimitiveBuilder.createEllipsoid(0.32f, 0.22f, 0.18f, 16, 20)
        bodyMesh.colorR = 1.0f; bodyMesh.colorG = 0.55f; bodyMesh.colorB = 0.1f
        bodyPart.children.add(bodyMesh)

        // 腹部亮色
        val belly = PrimitiveBuilder.createEllipsoid(0.25f, 0.1f, 0.14f, 12, 14)
        belly.colorR = 1.0f; belly.colorG = 0.85f; belly.colorB = 0.55f
        belly.translateY = -0.1f
        bodyPart.children.add(belly)

        // ===== 大尾巴 (飘逸双尾) =====
        // 上尾叶
        tailFin.vertices = floatArrayOf(
            0f, 0.02f, 0f,      0f, 0.1f, 0f,      0.35f, 0.15f, 0.05f,
            0f, 0.02f, 0f,      0f, 0.1f, 0f,      0.35f, 0.15f, -0.05f,
            0f, 0.02f, -0.01f,  0f, 0.1f, -0.01f,  0.35f, 0.15f, 0.04f,
            0f, 0.02f, -0.01f,  0f, 0.1f, -0.01f,  0.35f, 0.15f, -0.06f
        )
        tailFin.normals = floatArrayOf(
            0f,0f,1f, 0f,0f,1f, 0f,0f,1f, 0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f,
            0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f, 0f,0f,1f, 0f,0f,1f, 0f,0f,1f
        )
        tailFin.indices = shortArrayOf(0,1,2, 3,4,5, 6,8,7, 9,10,11)
        tailFin.colorR = 1.0f; tailFin.colorG = 0.5f; tailFin.colorB = 0.05f
        tailFin.translateX = -0.32f

        // 下尾叶
        tailLower.vertices = tailFin.vertices.copyOf()
        tailLower.normals = tailFin.normals.copyOf()
        tailLower.indices = tailFin.indices.copyOf()
        tailLower.colorR = 1.0f; tailLower.colorG = 0.55f; tailLower.colorB = 0.1f
        tailLower.translateX = -0.32f
        tailLower.scaleY = -0.8f
        tailLower.translateY = -0.02f

        // ===== 背鳍 =====
        dorsalFin.vertices = floatArrayOf(
            -0.1f, 0.18f, 0f,  0.15f, 0.18f, 0f,  0.02f, 0.38f, 0f,
            -0.1f, 0.18f, -0.01f, 0.15f, 0.18f, -0.01f, 0.02f, 0.38f, -0.01f
        )
        dorsalFin.normals = floatArrayOf(
            0f,0f,1f, 0f,0f,1f, 0f,0f,1f, 0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f
        )
        dorsalFin.indices = shortArrayOf(0,1,2, 3,5,4)
        dorsalFin.colorR = 1.0f; dorsalFin.colorG = 0.45f; dorsalFin.colorB = 0.05f

        // ===== 胸鳍 =====
        leftPectoralFin.vertices = floatArrayOf(
            0f,-0.04f,0.12f, 0f,0.02f,0.08f, 0.15f,-0.01f,0.15f,
            0f,-0.04f,-0.11f, 0f,0.02f,-0.07f, 0.15f,-0.01f,-0.14f
        )
        leftPectoralFin.normals = floatArrayOf(
            0f,1f,0f, 0f,1f,0f, 0f,1f,0f, 0f,-1f,0f, 0f,-1f,0f, 0f,-1f,0f
        )
        leftPectoralFin.indices = shortArrayOf(0,1,2, 3,5,4)
        leftPectoralFin.colorR = 1.0f; leftPectoralFin.colorG = 0.6f; leftPectoralFin.colorB = 0.15f
        leftPectoralFin.translateX = 0.05f; leftPectoralFin.translateY = -0.06f

        rightPectoralFin.vertices = leftPectoralFin.vertices.copyOf()
        rightPectoralFin.normals = leftPectoralFin.normals.copyOf()
        rightPectoralFin.indices = leftPectoralFin.indices.copyOf()
        rightPectoralFin.colorR = 1.0f; rightPectoralFin.colorG = 0.6f; rightPectoralFin.colorB = 0.15f
        rightPectoralFin.translateX = 0.05f; rightPectoralFin.translateY = -0.06f; rightPectoralFin.scaleZ = -1f

        // ===== 大眼睛 =====
        val eyeW = PrimitiveBuilder.createSphere(0.06f, 10, 10)
        eyeW.colorR = 1f; eyeW.colorG = 1f; eyeW.colorB = 1f
        leftEye.children.add(eyeW)
        val leftP = PrimitiveBuilder.createSphere(0.035f, 8, 8)
        leftP.colorR = 0.1f; leftP.colorG = 0.1f; leftP.colorB = 0.3f; leftP.translateX = 0.04f
        leftEye.children.add(leftP)
        leftEye.translateX = 0.26f; leftEye.translateY = 0.08f; leftEye.translateZ = 0.12f

        rightEye.children.add(PrimitiveBuilder.createSphere(0.06f, 10, 10).apply {
            colorR = 1f; colorG = 1f; colorB = 1f
        })
        val rightP = PrimitiveBuilder.createSphere(0.035f, 8, 8)
        rightP.colorR = 0.1f; rightP.colorG = 0.1f; rightP.colorB = 0.3f; rightP.translateX = 0.04f
        rightEye.children.add(rightP)
        rightEye.translateX = 0.26f; rightEye.translateY = 0.08f; rightEye.translateZ = -0.12f

        // ===== 小嘴 =====
        val mouthMesh = PrimitiveBuilder.createEllipsoid(0.035f, 0.02f, 0.025f, 8, 8)
        mouthMesh.colorR = 1.0f; mouthMesh.colorG = 0.4f; mouthMesh.colorB = 0.4f
        mouth.children.add(mouthMesh)
        mouth.translateX = 0.33f; mouth.translateY = -0.02f

        // 组装
        root.children.addAll(listOf(bodyPart, tailFin, tailLower, dorsalFin,
            leftPectoralFin, rightPectoralFin, leftEye, rightEye, mouth))
        return root
    }

    fun applyPose(pose: Pose) {
        bodyPart.rotateZ = pose.bodySway
        bodyPart.rotateY = pose.bodyYaw * 0.5f
        bodyPart.translateY = pose.bodyBob

        tailFin.rotateZ = pose.tailSway
        tailLower.rotateZ = -pose.tailSway * 0.6f
        dorsalFin.rotateZ = pose.dorsalAngle

        leftPectoralFin.rotateX = pose.pectoralAngle
        rightPectoralFin.rotateX = -pose.pectoralAngle

        leftEye.scaleX = 1f + pose.blink
        rightEye.scaleX = 1f + pose.blink
        mouth.scaleX = 1f + pose.mouthOpen
        mouth.translateX = 0.33f + pose.mouthOpen * 0.015f
    }

    data class Pose(
        var bodySway: Float = 0f,
        var bodyYaw: Float = 0f,
        var bodyBob: Float = 0f,
        var tailSway: Float = 0f,
        var dorsalAngle: Float = 0f,
        var pectoralAngle: Float = 0f,
        var blink: Float = 0f,
        var mouthOpen: Float = 0f
    )
}

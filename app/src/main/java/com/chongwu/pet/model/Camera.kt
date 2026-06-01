package com.chongwu.pet.model

/**
 * 3D摄像机 - 透视投影 + 轨道控制
 */
class Camera {
    var eyeX = 0f; var eyeY = 2f; var eyeZ = 5f
    var centerX = 0f; var centerY = 0f; var centerZ = 0f
    var upX = 0f; var upY = 1f; var upZ = 0f
    
    var fov = 45f
    var near = 0.1f
    var far = 100f
    
    // 轨道角度
    var theta = 0f   // 水平角度
    var phi = 30f    // 垂直角度（度）
    var distance = 6f
    
    // 目标偏移
    var targetOffsetX = 0f
    var targetOffsetY = 0.5f
    
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val vpMatrix = FloatArray(16)
    
    fun updateOrbit() {
        val radTheta = Math.toRadians(theta.toDouble()).toFloat()
        val radPhi = Math.toRadians(phi.toDouble()).toFloat()
        
        eyeX = centerX + targetOffsetX + distance * cos(radPhi) * sin(radTheta)
        eyeY = centerY + targetOffsetY + distance * sin(radPhi)
        eyeZ = centerZ + distance * cos(radPhi) * cos(radTheta)
    }
    
    fun updateView() {
        updateOrbit()
        android.opengl.Matrix.setLookAtM(viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            centerX + targetOffsetX, centerY + targetOffsetY, centerZ,
            upX, upY, upZ)
    }
    
    fun updateProjection(width: Float, height: Float) {
        val aspect = if (height > 0f) width / height else 1f
        android.opengl.Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, near, far)
    }
    
    fun update(width: Float, height: Float) {
        updateView()
        updateProjection(width, height)
        android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }
    
    /** 摇晃效果 */
    fun shake(intensity: Float) {
        eyeX += (Math.random().toFloat() - 0.5f) * intensity
        eyeY += (Math.random().toFloat() - 0.5f) * intensity
    }
}

package com.chongwu.pet.render.gl

import android.opengl.GLES20

/**
 * 卡通渲染着色器 - 3段式光照 + 轮廓描边
 */
class ShaderProgram {
    var programId = 0
    
    // Attribute locations
    var aPositionLoc = -1
    var aNormalLoc = -1
    
    // Uniform locations
    var uMVPLoc = -1
    var uModelLoc = -1
    var uColorLoc = -1
    var uLightDirLoc = -1
    var uAmbientLoc = -1
    var uTimeLoc = -1
    
    fun create() {
        programId = ShaderHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        
        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        
        uMVPLoc = GLES20.glGetUniformLocation(programId, "uMVP")
        uModelLoc = GLES20.glGetUniformLocation(programId, "uModel")
        uColorLoc = GLES20.glGetUniformLocation(programId, "uColor")
        uLightDirLoc = GLES20.glGetUniformLocation(programId, "uLightDir")
        uAmbientLoc = GLES20.glGetUniformLocation(programId, "uAmbient")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "uTime")
    }
    
    fun use() {
        GLES20.glUseProgram(programId)
    }
    
    companion object {
        val VERTEX_SHADER = """
            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            uniform float uAmbient;
            
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec4 vColor;
            
            void main() {
                vPosition = (uModel * vec4(aPosition, 1.0)).xyz;
                vNormal = normalize((uModel * vec4(aNormal, 0.0)).xyz);
                vColor = uColor;
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """.trimIndent()
        
        val FRAGMENT_SHADER = """
            precision mediump float;
            
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec4 vColor;
            
            uniform vec3 uLightDir;
            uniform float uAmbient;
            uniform float uTime;
            
            void main() {
                vec3 normal = normalize(vNormal);
                vec3 lightDir = normalize(uLightDir);
                
                // 基础漫反射
                float diff = max(dot(normal, lightDir), 0.0);
                
                // Cel-shading 三段式
                float cel;
                if (diff > 0.7) cel = 1.0;
                else if (diff > 0.3) cel = 0.6;
                else cel = 0.3;
                
                // 环境光
                float finalLight = max(cel, uAmbient);
                
                // 边缘光（Fresnel）
                vec3 viewDir = normalize(-vPosition);
                float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.0);
                float rim = fresnel * 0.3;
                
                // 高光
                vec3 halfDir = normalize(lightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 20.0);
                float specCel = spec > 0.5 ? 1.0 : 0.0;
                
                vec3 finalColor = vColor.rgb * finalLight + vec3(rim) + vec3(specCel * 0.4);
                
                gl_FragColor = vec4(finalColor, vColor.a);
            }
        """.trimIndent()
    }
}

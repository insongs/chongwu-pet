package com.chongwu.pet.model

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 3D网格模型 - 支持顶点、法线、纹理坐标、索引绘制
 */
class Model3D {
    var vertices: FloatArray = FloatArray(0)
    var normals: FloatArray = FloatArray(0)
    var texCoords: FloatArray = FloatArray(0)
    var indices: ShortArray = ShortArray(0)
    
    val vertexCount get() = vertices.size / 3
    val indexCount get() = indices.size
    
    // VBO handles
    private var vboVertex = 0
    private var vboNormal = 0
    private var vboTexCoord = 0
    private var ibo = 0
    private var isUploaded = false
    
    // 变换（相对父级）
    var translateX = 0f; var translateY = 0f; var translateZ = 0f
    var rotateX = 0f; var rotateY = 0f; var rotateZ = 0f
    var scaleX = 1f; var scaleY = 1f; var scaleZ = 1f
    
    // 颜色
    var colorR = 1f; var colorG = 1f; var colorB = 1f; var colorA = 1f
    
    // 子模型
    val children = mutableListOf<Model3D>()
    
    fun addChild(child: Model3D): Model3D {
        children.add(child)
        return this
    }
    
    fun uploadToGPU() {
        if (isUploaded) return
        
        val vbo = IntArray(1)
        val iboArr = IntArray(1)
        
        // 顶点
        if (vertices.isNotEmpty()) {
            GLES20.glGenBuffers(1, vbo, 0)
            vboVertex = vbo[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertex)
            val buf = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(vertices).position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buf.capacity() * 4, buf, GLES20.GL_STATIC_DRAW)
        }
        
        // 法线
        if (normals.isNotEmpty()) {
            GLES20.glGenBuffers(1, vbo, 0)
            vboNormal = vbo[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboNormal)
            val buf = ByteBuffer.allocateDirect(normals.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(normals).position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buf.capacity() * 4, buf, GLES20.GL_STATIC_DRAW)
        }
        
        // 纹理坐标
        if (texCoords.isNotEmpty()) {
            GLES20.glGenBuffers(1, vbo, 0)
            vboTexCoord = vbo[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTexCoord)
            val buf = ByteBuffer.allocateDirect(texCoords.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(texCoords).position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buf.capacity() * 4, buf, GLES20.GL_STATIC_DRAW)
        }
        
        // 索引
        if (indices.isNotEmpty()) {
            GLES20.glGenBuffers(1, iboArr, 0)
            ibo = iboArr[0]
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
            val buf = ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer()
            buf.put(indices).position(0)
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, buf.capacity() * 2, buf, GLES20.GL_STATIC_DRAW)
        }
        
        // 上传子模型
        for (child in children) child.uploadToGPU()
        
        isUploaded = true
    }
    
    fun draw(modelMatrix: FloatArray, mvpMatrix: FloatArray, 
             aPositionLoc: Int, aNormalLoc: Int,
             uModelLoc: Int, uColorLoc: Int) {
        // 应用本地变换
        val localM = FloatArray(16)
        android.opengl.Matrix.setIdentityM(localM, 0)
        android.opengl.Matrix.translateM(localM, 0, translateX, translateY, translateZ)
        android.opengl.Matrix.rotateM(localM, 0, rotateX, 1f, 0f, 0f)
        android.opengl.Matrix.rotateM(localM, 0, rotateY, 0f, 1f, 0f)
        android.opengl.Matrix.rotateM(localM, 0, rotateZ, 0f, 0f, 1f)
        android.opengl.Matrix.scaleM(localM, 0, scaleX, scaleY, scaleZ)
        
        val finalM = FloatArray(16)
        android.opengl.Matrix.multiplyMM(finalM, 0, modelMatrix, 0, localM, 0)
        
        // 绘制自身
        if (isUploaded && vertices.isNotEmpty()) {
            GLES20.glUniformMatrix4fv(uModelLoc, 1, false, finalM, 0)
            GLES20.glUniform4f(uColorLoc, colorR, colorG, colorB, colorA)
            
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertex)
            GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glEnableVertexAttribArray(aPositionLoc)
            
            if (vboNormal != 0 && aNormalLoc >= 0) {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboNormal)
                GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, 0, 0)
                GLES20.glEnableVertexAttribArray(aNormalLoc)
            }
            
            if (ibo != 0) {
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
            }
        }
        
        // 绘制子模型
        for (child in children) {
            child.draw(finalM, mvpMatrix, aPositionLoc, aNormalLoc, uModelLoc, uColorLoc)
        }
    }
    
    companion object {
        fun createMerged(vararg models: Model3D): Model3D {
            val merged = Model3D()
            val vertList = mutableListOf<Float>()
            val normList = mutableListOf<Float>()
            val idxList = mutableListOf<Short>()
            var offset: Short = 0
            
            for (model in models) {
                for (i in model.vertices.indices) vertList.add(model.vertices[i])
                for (i in model.normals.indices) normList.add(model.normals[i])
                for (i in model.indices.indices) idxList.add((model.indices[i] + offset).toShort())
                offset = (offset + model.vertexCount).toShort()
            }
            
            merged.vertices = vertList.toFloatArray()
            merged.normals = normList.toFloatArray()
            merged.indices = idxList.toShortArray()
            return merged
        }
    }
}

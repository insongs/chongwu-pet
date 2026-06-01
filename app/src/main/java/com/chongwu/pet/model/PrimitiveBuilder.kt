package com.chongwu.pet.model

import kotlin.math.*

/**
 * 几何体构建器 - 程序化生成球体、圆柱体、锥体等
 */
object PrimitiveBuilder {
    
    /** 球体（经纬度网格） */
    fun createSphere(radius: Float, rings: Int = 24, sectors: Int = 24): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()
        
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val y = sin((-PI / 2 + PI * r * R).toFloat())
                val x = cos(2 * PI * s * S).toFloat() * cos(PI * r * R).toFloat()
                val z = sin(2 * PI * s * S).toFloat() * cos(PI * r * R).toFloat()
                
                verts.addAll(listOf(x * radius, y * radius, z * radius))
                norms.addAll(listOf(x, y, z))
            }
        }
        
        for (r in 0 until rings - 1) {
            for (s in 0 until sectors - 1) {
                val cur = (r * sectors + s).toShort()
                val next = (cur + sectors).toShort()
                idxs.addAll(listOf(cur, (cur + 1).toShort(), next))
                idxs.addAll(listOf(next, (cur + 1).toShort(), (next + 1).toShort()))
            }
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
    
    /** 拉伸球体（椭球） */
    fun createEllipsoid(rx: Float, ry: Float, rz: Float, rings: Int = 24, sectors: Int = 24): Model3D {
        val sphere = createSphere(1f, rings, sectors)
        val verts = FloatArray(sphere.vertices.size)
        for (i in 0 until sphere.vertexCount) {
            verts[i * 3] = sphere.vertices[i * 3] * rx
            verts[i * 3 + 1] = sphere.vertices[i * 3 + 1] * ry
            verts[i * 3 + 2] = sphere.vertices[i * 3 + 2] * rz
        }
        // 调整法线
        val norms = FloatArray(sphere.normals.size)
        for (i in 0 until sphere.vertexCount) {
            val nx = sphere.normals[i * 3] / rx
            val ny = sphere.normals[i * 3 + 1] / ry
            val nz = sphere.normals[i * 3 + 2] / rz
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            norms[i * 3] = nx / len
            norms[i * 3 + 1] = ny / len
            norms[i * 3 + 2] = nz / len
        }
        val model = Model3D()
        model.vertices = verts
        model.normals = norms
        model.indices = sphere.indices
        return model
    }
    
    /** 圆柱体 */
    fun createCylinder(radius: Float, height: Float, sectors: Int = 20, capped: Boolean = true): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        val hh = height / 2f
        
        // 侧面
        for (i in 0..sectors) {
            val angle = (2 * PI * i / sectors).toFloat()
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val nx = cos(angle)
            val nz = sin(angle)
            verts.addAll(listOf(x, -hh, z)); norms.addAll(listOf(nx, 0f, nz))
            verts.addAll(listOf(x, hh, z)); norms.addAll(listOf(nx, 0f, nz))
        }
        
        for (i in 0 until sectors) {
            val a = (i * 2).toShort(); val b = (a + 1).toShort()
            val c = (a + 2).toShort(); val d = (b + 2).toShort()
            idxs.addAll(listOf(a, b, c, c, b, d))
        }
        
        if (capped) {
            // 底部
            val baseIdx = verts.size / 3
            verts.addAll(listOf(0f, -hh, 0f)); norms.addAll(listOf(0f, -1f, 0f))
            for (i in 0 until sectors) {
                val angle = (2 * PI * i / sectors).toFloat()
                verts.addAll(listOf(cos(angle) * radius, -hh, sin(angle) * radius))
                norms.addAll(listOf(0f, -1f, 0f))
            }
            for (i in 0 until sectors) {
                idxs.addAll(listOf(
                    baseIdx.toShort(), (baseIdx + 1 + i).toShort(), 
                    (baseIdx + 1 + (i + 1) % sectors).toShort()
                ))
            }
            
            // 顶部
            val topIdx = verts.size / 3
            verts.addAll(listOf(0f, hh, 0f)); norms.addAll(listOf(0f, 1f, 0f))
            for (i in 0 until sectors) {
                val angle = (2 * PI * i / sectors).toFloat()
                verts.addAll(listOf(cos(angle) * radius, hh, sin(angle) * radius))
                norms.addAll(listOf(0f, 1f, 0f))
            }
            for (i in 0 until sectors) {
                idxs.addAll(listOf(
                    (topIdx + 1 + (i + 1) % sectors).toShort(), 
                    (topIdx + 1 + i).toShort(), topIdx.toShort()
                ))
            }
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
    
    /** 锥体 */
    fun createCone(radius: Float, height: Float, sectors: Int = 20): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        val hh = height / 2f
        
        // 底部
        val baseCenter = 0
        verts.addAll(listOf(0f, -hh, 0f)); norms.addAll(listOf(0f, -1f, 0f))
        for (i in 0 until sectors) {
            val angle = (2 * PI * i / sectors).toFloat()
            verts.addAll(listOf(cos(angle) * radius, -hh, sin(angle) * radius))
            norms.addAll(listOf(0f, -1f, 0f))
        }
        for (i in 0 until sectors) {
            idxs.addAll(listOf(0, (1 + i).toShort(), (1 + (i + 1) % sectors).toShort()))
        }
        
        // 侧面
        val topIdx = verts.size / 3
        verts.addAll(listOf(0f, hh, 0f)); norms.addAll(listOf(0f, 1f, 0f))
        val sideBase = verts.size / 3
        for (i in 0 until sectors) {
            val angle = (2 * PI * i / sectors).toFloat()
            val x = cos(angle) * radius; val z = sin(angle) * radius
            verts.addAll(listOf(x, -hh, z))
            val nx = x / radius; val ny = radius / height; val nz = z / radius
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            norms.addAll(listOf(nx / len, ny / len, nz / len))
        }
        for (i in 0 until sectors) {
            idxs.addAll(listOf(
                topIdx.toShort(), (sideBase + (i + 1) % sectors).toShort(), (sideBase + i).toShort()
            ))
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
    
    /** 圆环（用于角） */
    fun createTorus(majorRadius: Float, minorRadius: Float, majorSeg: Int = 16, minorSeg: Int = 8): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        for (i in 0..majorSeg) {
            val u = (2 * PI * i / majorSeg).toFloat()
            for (j in 0..minorSeg) {
                val v = (2 * PI * j / minorSeg).toFloat()
                val x = (majorRadius + minorRadius * cos(v)) * cos(u)
                val y = minorRadius * sin(v)
                val z = (majorRadius + minorRadius * cos(v)) * sin(u)
                verts.addAll(listOf(x, y, z))
                norms.addAll(listOf(cos(v) * cos(u), sin(v), cos(v) * sin(u)))
            }
        }
        
        for (i in 0 until majorSeg) {
            for (j in 0 until minorSeg) {
                val a = (i * (minorSeg + 1) + j).toShort()
                val b = (a + 1).toShort()
                val c = ((i + 1) * (minorSeg + 1) + j).toShort()
                val d = (c + 1).toShort()
                idxs.addAll(listOf(a, c, b, b, c, d))
            }
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
    
    /** 弯曲的管状体（用于弯曲的角） */
    fun createCurvedHorn(length: Float, startRadius: Float, endRadius: Float, 
                         curveAngle: Float = 90f, segments: Int = 12, sides: Int = 8): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val r = startRadius + (endRadius - startRadius) * t
            
            // 沿着弯曲路径
            val angle = t * curveAngle * PI / 180f
            val cx = sin(angle).toFloat() * length * 0.6f
            val cy = length * t - length * 0.3f
            val cz = (1 - cos(angle)).toFloat() * length * 0.3f
            
            // 路径切线方向
            val tx = cos(angle).toFloat() * length * 0.6f * (curveAngle * PI / 180f) / length
            val ty = 1f
            val tz = sin(angle).toFloat() * length * 0.3f * (curveAngle * PI / 180f) / length
            val tlen = sqrt(tx*tx + ty*ty + tz*tz)
            
            for (j in 0 until sides) {
                val u = (2 * PI * j / sides).toFloat()
                // 构建垂直于切线的坐标系
                val up = floatArrayOf(0f, 1f, 0f)
                val right = floatArrayOf(
                    up[1]*tz - up[2]*ty,
                    up[2]*tx - up[0]*tz,
                    up[0]*ty - up[1]*tx
                )
                val rlen = sqrt(right[0]*right[0] + right[1]*right[1] + right[2]*right[2])
                if (rlen > 0.001f) {
                    right[0] /= rlen; right[1] /= rlen; right[2] /= rlen
                }
                val newUp = floatArrayOf(
                    right[1]*tz/tlen - right[2]*ty/tlen,
                    right[2]*tx/tlen - right[0]*tz/tlen,
                    right[0]*ty/tlen - right[1]*tx/tlen
                )
                
                val px = cx + r * (cos(u).toFloat() * right[0] + sin(u).toFloat() * newUp[0])
                val py = cy + r * (cos(u).toFloat() * right[1] + sin(u).toFloat() * newUp[1])
                val pz = cz + r * (cos(u).toFloat() * right[2] + sin(u).toFloat() * newUp[2])
                verts.addAll(listOf(px, py, pz))
                
                val nx = cos(u).toFloat() * right[0] + sin(u).toFloat() * newUp[0]
                val ny = cos(u).toFloat() * right[1] + sin(u).toFloat() * newUp[1]
                val nz = cos(u).toFloat() * right[2] + sin(u).toFloat() * newUp[2]
                norms.addAll(listOf(nx, ny, nz))
            }
        }
        
        for (i in 0 until segments) {
            for (j in 0 until sides) {
                val a = (i * sides + j).toShort()
                val b = (a + 1).toShort()
                val c = ((i + 1) * sides + j).toShort()
                val d = (c + 1).toShort()
                if (j < sides - 1) {
                    idxs.addAll(listOf(a, b, c, c, b, d))
                } else {
                    idxs.addAll(listOf(a, (i * sides).toShort(), c, c, (i * sides).toShort(), ((i + 1) * sides).toShort()))
                }
            }
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
    
    /** 平面（用于地面） */
    fun createPlane(width: Float, depth: Float, segmentsX: Int = 1, segmentsZ: Int = 1): Model3D {
        val model = Model3D()
        val verts = mutableListOf<Float>()
        val norms = mutableListOf<Float>()
        val idxs = mutableListOf<Short>()
        
        val hw = width / 2f; val hd = depth / 2f
        
        for (z in 0..segmentsZ) {
            for (x in 0..segmentsX) {
                val px = -hw + x * (width / segmentsX)
                val pz = -hd + z * (depth / segmentsZ)
                verts.addAll(listOf(px, 0f, pz))
                norms.addAll(listOf(0f, 1f, 0f))
            }
        }
        
        for (z in 0 until segmentsZ) {
            for (x in 0 until segmentsX) {
                val a = (z * (segmentsX + 1) + x).toShort()
                val b = (a + 1).toShort()
                val c = ((z + 1) * (segmentsX + 1) + x).toShort()
                val d = (c + 1).toShort()
                idxs.addAll(listOf(a, c, b, b, c, d))
            }
        }
        
        model.vertices = verts.toFloatArray()
        model.normals = norms.toFloatArray()
        model.indices = idxs.toShortArray()
        return model
    }
}

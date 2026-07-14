package com.edlplan.andengine

import com.edlplan.framework.math.Vec3
import java.util.Arrays

class Triangle3DBuilder {

    private var ver = FloatArray(9)

    private var offset = 0

    fun reset() {
        offset = 0
    }

    fun add(x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float) {
        if (offset + 9 > ver.size) {
            ver = Arrays.copyOf(ver, ver.size * 3 / 2 + 9)
        }
        ver[offset++] = x1
        ver[offset++] = y1
        ver[offset++] = z1
        ver[offset++] = x2
        ver[offset++] = y2
        ver[offset++] = z2
        ver[offset++] = x3
        ver[offset++] = y3
        ver[offset++] = z3
    }

    fun add(p1: Vec3, p2: Vec3, p3: Vec3) {
        add(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z)
    }

    fun getVertex(): FloatArray {
        if (offset != ver.size) {
            ver = Arrays.copyOf(ver, offset)
        }
        return ver
    }

    fun getRawVertex(): FloatArray {
        return ver
    }

    fun getOffset(): Int {
        return offset
    }
}

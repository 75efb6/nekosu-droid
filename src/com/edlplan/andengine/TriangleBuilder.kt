package com.edlplan.andengine

import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.Vec3
import com.edlplan.framework.utils.FloatArraySlice
import java.util.Arrays

class TriangleBuilder : FloatArraySlice {

    constructor(cache: FloatArray) {
        ary = cache
    }

    constructor() : this(1)

    constructor(size: Int) {
        ary = FloatArray(6 * size)
    }

    fun add(p1: Vec3, p2: Vec3, p3: Vec3) {
        if (length + 6 > ary.size) {
            ary = Arrays.copyOf(ary, ary.size * 3 / 2 + 6)
        }
        ary[length++] = p1.x
        ary[length++] = p1.y
        ary[length++] = p2.x
        ary[length++] = p2.y
        ary[length++] = p3.x
        ary[length++] = p3.y
    }

    fun add(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        if (length + 6 > ary.size) {
            ary = Arrays.copyOf(ary, ary.size * 3 / 2 + 6)
        }
        ary[length++] = x1
        ary[length++] = y1
        ary[length++] = x2
        ary[length++] = y2
        ary[length++] = x3
        ary[length++] = y3
    }

    fun add(p1: Vec2, p2: Vec2, p3: Vec2) {
        add(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }

    fun copyVertex(): FloatArray {
        return Arrays.copyOf(ary, offset)
    }

    fun getVertex(slice: FloatArraySlice): FloatArraySlice {
        slice.offset = 0
        if (slice.ary.size < length) {
            slice.ary = FloatArray(length)
        }
        slice.length = length
        System.arraycopy(ary, 0, slice.ary, 0, length)
        return slice
    }
}

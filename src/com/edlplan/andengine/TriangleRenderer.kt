package com.edlplan.andengine

import com.edlplan.framework.utils.FloatArraySlice
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class TriangleRenderer private constructor() {

    var buffer: FloatBuffer? = null

    init {
        val bb = ByteBuffer.allocateDirect(INITIAL_FLOAT_CAPACITY * 4)
        bb.order(ByteOrder.nativeOrder())
        buffer = bb.asFloatBuffer()
    }

    fun renderTriangles(ver: FloatArraySlice, pGL: GL10) {
        val offset = ver.length
        if (buffer!!.capacity() < offset) {
            val bb = ByteBuffer.allocateDirect((offset + 12) * 4)
            bb.order(ByteOrder.nativeOrder())
            buffer = bb.asFloatBuffer()
        }
        buffer!!.position(0).limit(buffer!!.capacity())
        buffer!!.put(ver.ary, ver.offset, ver.length)
        buffer!!.position(0).limit(offset)

        pGL.glVertexPointer(2, GL10.GL_FLOAT, 0, buffer)
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.length / 2)
    }

    companion object {
        private const val INITIAL_FLOAT_CAPACITY = 200 * 28 * 6
        private val triangleRenderer = TriangleRenderer()

        @JvmStatic
        fun get(): TriangleRenderer {
            return triangleRenderer
        }
    }
}

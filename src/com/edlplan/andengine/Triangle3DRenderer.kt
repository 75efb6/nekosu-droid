package com.edlplan.andengine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class Triangle3DRenderer private constructor() {

    var buffer: FloatBuffer? = null

    fun renderTriangles(ver: FloatArray, pGL: GL10) {
        val offset = ver.size
        if (buffer == null || buffer!!.capacity() < offset) {
            val bb = ByteBuffer.allocateDirect((offset + 18).coerceAtLeast(900) * 4)
            bb.order(ByteOrder.nativeOrder())
            buffer = bb.asFloatBuffer()
        }
        buffer!!.position(0).limit(buffer!!.capacity())
        buffer!!.put(ver, 0, ver.size)
        buffer!!.position(0).limit(offset)

        pGL.glVertexPointer(3, GL10.GL_FLOAT, 0, buffer)
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.size / 3)
    }

    companion object {
        private val triangleRenderer = Triangle3DRenderer()

        @JvmStatic
        fun get(): Triangle3DRenderer {
            return triangleRenderer
        }
    }
}

package com.edlplan.andengine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class TriangleTexture3DRenderer private constructor() {

    var buffer: FloatBuffer? = null
    var coordBuffer: FloatBuffer? = null

    fun renderTriangles(ver: FloatArray, coord: FloatArray, pGL: GL10) {
        run {
            val offset = ver.size
            if (buffer == null || buffer!!.capacity() < offset) {
                val bb = ByteBuffer.allocateDirect((offset + 18) * 4)
                bb.order(ByteOrder.nativeOrder())
                buffer = bb.asFloatBuffer()
            }
            buffer!!.position(0).limit(buffer!!.capacity())
            buffer!!.put(ver, 0, ver.size)
            buffer!!.position(0).limit(offset)
        }
        run {
            val offset = coord.size
            if (coordBuffer == null || coordBuffer!!.capacity() < offset) {
                val bb = ByteBuffer.allocateDirect((offset + 12) * 4)
                bb.order(ByteOrder.nativeOrder())
                coordBuffer = bb.asFloatBuffer()
            }
            coordBuffer!!.position(0).limit(coordBuffer!!.capacity())
            coordBuffer!!.put(coord, 0, coord.size)
            coordBuffer!!.position(0).limit(offset)
        }

        pGL.glTexCoordPointer(2, GL10.GL_FLOAT, 0, coordBuffer)
        pGL.glVertexPointer(3, GL10.GL_FLOAT, 0, buffer)
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.size / 3)
    }

    companion object {
        private val triangleRenderer = TriangleTexture3DRenderer()

        @JvmStatic
        fun get(): TriangleTexture3DRenderer {
            return triangleRenderer
        }
    }
}

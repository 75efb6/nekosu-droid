package ru.nsu.ccfit.zuev.osu.polygon

import org.anddev.andengine.opengl.vertex.VertexBuffer

class PolygonVertexBuffer(pVerticesCount: Int, pDrawType: Int, managed: Boolean) :
    VertexBuffer(2 * pVerticesCount * BYTES_PER_FLOAT, pDrawType, managed) {

    @Synchronized
    fun update(pVertices: FloatArray) {
        val buffer = floatBuffer
        buffer.position(0)
        buffer.put(pVertices)
        buffer.position(0)
    }

    companion object {
        val BYTES_PER_FLOAT: Int = Float.SIZE_BITS / 8
    }
}

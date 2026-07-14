package com.edlplan.framework.support.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

class BufferUtil {

    class ReusedFloatBuffer {

        private var buffer: FloatBuffer? = null

        fun load(ary: FloatArray): FloatBuffer {
            if (buffer == null || buffer!!.capacity() < ary.size) {
                buffer = createFloatBuffer(ary.size * 3 / 2 + 20)
            }
            buffer!!.position(0).limit(ary.size)
            buffer!!.put(ary)
            buffer!!.position(0)
            return buffer!!
        }

        fun getBuffer(): FloatBuffer? {
            return buffer
        }

    }

    class ReusedShortBuffer {

        private var buffer: ShortBuffer? = null

        fun load(ary: ShortArray): ShortBuffer {
            if (buffer == null || buffer!!.capacity() < ary.size) {
                buffer = createShortBuffer(ary.size * 3 / 2 + 20)
            }
            buffer!!.position(0).limit(ary.size)
            buffer!!.put(ary)
            buffer!!.position(0)
            return buffer!!
        }

        fun getBuffer(): ShortBuffer? {
            return buffer
        }

    }

    companion object {

        @JvmStatic
        fun createFloatBuffer(floatCount: Int): FloatBuffer {
            val bb = ByteBuffer.allocateDirect(floatCount * 4)
            bb.order(ByteOrder.nativeOrder())
            return bb.asFloatBuffer()
        }

        @JvmStatic
        fun createShortBuffer(shortCount: Int): ShortBuffer {
            val bb = ByteBuffer.allocateDirect(shortCount * 2)
            bb.order(ByteOrder.nativeOrder())
            return bb.asShortBuffer()
        }

        @JvmStatic
        fun createIntBuffer(intCount: Int): IntBuffer {
            val bb = ByteBuffer.allocateDirect(intCount * 4)
            bb.order(ByteOrder.nativeOrder())
            return bb.asIntBuffer()
        }
    }

}

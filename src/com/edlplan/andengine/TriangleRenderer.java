package com.edlplan.andengine;

import com.edlplan.framework.utils.FloatArraySlice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class TriangleRenderer {

    // Pre-allocate enough for a complex slider (~200 path segments × 28 triangles × 6 floats).
    // Growing the direct ByteBuffer mid-game is expensive; resize only on rare very large sliders.
    private static final int INITIAL_FLOAT_CAPACITY = 200 * 28 * 6;

    private static TriangleRenderer triangleRenderer = new TriangleRenderer();
    FloatBuffer buffer;

    private TriangleRenderer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(INITIAL_FLOAT_CAPACITY * 4);
        bb.order(ByteOrder.nativeOrder());
        buffer = bb.asFloatBuffer();
    }

    public static TriangleRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(FloatArraySlice ver, GL10 pGL) {
        int offset = ver.length;
        if (buffer.capacity() < offset) {
            ByteBuffer bb = ByteBuffer.allocateDirect((offset + 12) * 4);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        }
        buffer.position(0).limit(buffer.capacity());
        buffer.put(ver.ary, ver.offset, ver.length);
        buffer.position(0).limit(offset);

        pGL.glVertexPointer(2, GL10.GL_FLOAT, 0, buffer);
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.length / 2);
    }

}

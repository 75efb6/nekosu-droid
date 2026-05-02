package com.edlplan.andengine;

import com.edlplan.framework.math.Vec3;

import java.util.Arrays;

public class Triangle3DBuilder {

    private float[] ver = new float[9];

    private int offset;

    public void reset() {
        offset = 0;
    }

    public void add(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3) {
        if (offset + 9 > ver.length) {
            ver = Arrays.copyOf(ver, ver.length * 3 / 2 + 9);
        }
        ver[offset++] = x1;
        ver[offset++] = y1;
        ver[offset++] = z1;
        ver[offset++] = x2;
        ver[offset++] = y2;
        ver[offset++] = z2;
        ver[offset++] = x3;
        ver[offset++] = y3;
        ver[offset++] = z3;
    }

    public void add(Vec3 p1, Vec3 p2, Vec3 p3) {
        add(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z);
    }

    public float[] getVertex() {
        if (offset != ver.length) {
            ver = Arrays.copyOf(ver, offset);
        }
        return ver;
    }

    /** Returns vertex data without trimming or copying — safe to use if offset is the true length. */
    public float[] getRawVertex() {
        return ver;
    }

    public int getOffset() {
        return offset;
    }

}

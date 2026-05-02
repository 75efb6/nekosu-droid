package com.edlplan.osu.support.slider;

import com.edlplan.andengine.Triangle3DBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;

public class Draw3DLinePath {
    private static final int MAXRES = 24;

    // Precomputed rotation constants for arc caps (same trick as DrawLinePath)
    private static final float CAP_STEP = FMath.Pi / MAXRES;
    private static final float CAP_STEP_COS = (float) Math.cos(CAP_STEP);
    private static final float CAP_STEP_SIN = (float) Math.sin(CAP_STEP);

    public float alpha;
    public float width;

    private float zEdge = -1, zCenter = 1;

    // Precomputed path structure — width-independent, computed once per path
    private float[] segTheta;
    private float[] segNormX;
    private float[] segNormY;
    private int structureSize;

    private Triangle3DBuilder triangles;
    private AbstractPath path;

    public Draw3DLinePath(AbstractPath p, float width, float zCenter, float zEdge) {
        this.zCenter = zCenter;
        this.zEdge = zEdge;
        alpha = 1;
        this.width = width;
        prepareForPath(p);
    }

    public Draw3DLinePath() {
        alpha = 1;
    }

    public void setZCenter(float zCenter) { this.zCenter = zCenter; }
    public void setZEdge(float zEdge) { this.zEdge = zEdge; }

    // -------------------------------------------------------------------------
    // Two-phase API (mirrors DrawLinePath)
    // -------------------------------------------------------------------------

    public Draw3DLinePath prepareForPath(AbstractPath p) {
        this.path = p;
        int n = p.size();
        if (n >= 2) {
            int segs = n - 1;
            if (segTheta == null || segTheta.length < segs) {
                segTheta = new float[segs];
                segNormX = new float[segs];
                segNormY = new float[segs];
            }
            for (int i = 0; i < segs; i++) {
                Vec2 a = p.get(i);
                Vec2 b = p.get(i + 1);
                segTheta[i] = Vec2.calTheta(a, b);
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 1e-6f) {
                    segNormX[i] = -dy / len;
                    segNormY[i] = dx / len;
                } else {
                    segNormX[i] = 0f;
                    segNormY[i] = 0f;
                }
            }
        }
        structureSize = n;
        return this;
    }

    public Triangle3DBuilder buildForWidth(float width, float zCenter, float zEdge,
                                           Triangle3DBuilder builder) {
        this.width = width;
        this.zCenter = zCenter;
        this.zEdge = zEdge;
        builder.reset();
        triangles = builder;
        init();
        return builder;
    }

    // -------------------------------------------------------------------------
    // Legacy single-use API (kept for applyToScene)
    // -------------------------------------------------------------------------

    public Triangle3DBuilder getTriangles() {
        if (triangles == null) {
            triangles = new Triangle3DBuilder();
        } else {
            triangles.reset();
        }
        init();
        return triangles;
    }

    // -------------------------------------------------------------------------
    // Geometry generation — all floats, zero allocation
    // -------------------------------------------------------------------------

    private void addLineCap(Vec2 org, float theta, float thetaDiff) {
        float dir = Math.signum(thetaDiff);
        if (dir == 0f) return;
        thetaDiff *= dir;
        int amountPoints = (int) Math.ceil(thetaDiff / CAP_STEP);
        if (amountPoints == 0) return;

        if (dir < 0) theta += FMath.Pi;

        float ux = (float) Math.cos(theta);
        float uy = (float) Math.sin(theta);
        float prevX = ux * width + org.x;
        float prevY = uy * width + org.y;

        final float cs = CAP_STEP_COS;
        final float ss = dir * CAP_STEP_SIN;

        float orgX = org.x, orgY = org.y;

        for (int i = 1; i <= amountPoints; i++) {
            float ux2, uy2;
            if (i == amountPoints && i * CAP_STEP > thetaDiff) {
                float finalAngle = theta + dir * thetaDiff;
                ux2 = (float) Math.cos(finalAngle);
                uy2 = (float) Math.sin(finalAngle);
            } else {
                ux2 = ux * cs - uy * ss;
                uy2 = uy * cs + ux * ss;
            }
            float x2 = ux2 * width + org.x;
            float y2 = uy2 * width + org.y;

            triangles.add(
                    orgX, orgY, zCenter,
                    prevX, prevY, zEdge,
                    x2, y2, zEdge
            );

            prevX = x2; prevY = y2;
            ux = ux2; uy = uy2;
        }
    }

    private void addLineQuads(int segIdx, Vec2 ps, Vec2 pe) {
        float nx = segNormX[segIdx] * width;
        float ny = segNormY[segIdx] * width;

        float slx = ps.x + nx, sly = ps.y + ny;
        float srx = ps.x - nx, sry = ps.y - ny;
        float elx = pe.x + nx, ely = pe.y + ny;
        float erx = pe.x - nx, ery = pe.y - ny;
        float psx = ps.x, psy = ps.y;
        float pex = pe.x, pey = pe.y;

        triangles.add(psx, psy, zCenter, pex, pey, zCenter, elx, ely, zEdge);
        triangles.add(psx, psy, zCenter, elx, ely, zEdge,   slx, sly, zEdge);
        triangles.add(psx, psy, zCenter, erx, ery, zEdge,   pex, pey, zCenter);
        triangles.add(psx, psy, zCenter, srx, sry, zEdge,   erx, ery, zEdge);
    }

    private void init() {
        int n = structureSize;
        if (n < 2) {
            if (n == 1) {
                addLineCap(path.get(0), FMath.Pi, FMath.Pi);
                addLineCap(path.get(0), 0, FMath.Pi);
            }
            return;
        }

        float theta = segTheta[0];
        addLineCap(path.get(0), theta + FMath.PiHalf, FMath.Pi);
        addLineQuads(0, path.get(0), path.get(1));

        if (n == 2) {
            addLineCap(path.get(1), theta - FMath.PiHalf, FMath.Pi);
            return;
        }

        float preTheta = theta;
        for (int i = 1; i < n - 1; i++) {
            float nextTheta = segTheta[i];
            addLineCap(path.get(i), preTheta - FMath.PiHalf, nextTheta - preTheta);
            addLineQuads(i, path.get(i), path.get(i + 1));
            preTheta = nextTheta;
        }
        addLineCap(path.get(n - 1), preTheta - FMath.PiHalf, FMath.Pi);
    }
}

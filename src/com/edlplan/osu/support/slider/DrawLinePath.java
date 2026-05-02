package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;

public class DrawLinePath {
    private static final int MAXRES = 24;

    // Precomputed rotation constants for arc caps (step = π/MAXRES).
    // Using incremental rotation replaces per-step sin/cos with 4 multiplications.
    private static final float CAP_STEP = FMath.Pi / MAXRES;
    private static final float CAP_STEP_COS = (float) Math.cos(CAP_STEP);
    private static final float CAP_STEP_SIN = (float) Math.sin(CAP_STEP);

    public float alpha;
    public float width;

    // Reusable working vector (avoids allocation in tight loops)
    private final Vec2 current = new Vec2();

    // Precomputed path structure — computed once per path, reused for every width pass.
    // segTheta[i]  = angle of segment from point[i] to point[i+1]
    // segNormX/Y[i] = perpendicular unit normal of that segment (CCW, matches lineOthNormal)
    private float[] segTheta;
    private float[] segNormX;
    private float[] segNormY;
    private int structureSize;

    private TriangleBuilder triangles;
    private AbstractPath path;

    public DrawLinePath(AbstractPath p, float width) {
        alpha = 1;
        prepareForPath(p);
        this.width = width;
    }

    public DrawLinePath() {
        alpha = 1;
    }

    // -------------------------------------------------------------------------
    // New two-phase API
    // -------------------------------------------------------------------------

    /**
     * Precomputes per-segment angles and normals for the given path.
     * Call this ONCE per onUpdate() frame before calling buildForWidth for each layer.
     */
    public DrawLinePath prepareForPath(AbstractPath p) {
        this.path = p;
        int n = p.size();
        if (n >= 2) {
            int segs = n - 1;
            if (segTheta == null || segTheta.length < segs) {
                segTheta = new float[segs];
                segNormX = new float[segs];
                segNormY = new float[segs];
            }
            Vec2 a = p.get(0);
            for (int i = 0; i < segs; i++) {
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
                a = b;
            }
        }
        structureSize = n;
        return this;
    }

    /**
     * Generates triangles for the given width using precomputed structure.
     * Call prepareForPath() before this method.
     */
    public TriangleBuilder buildForWidth(float width, TriangleBuilder builder) {
        this.width = width;
        builder.length = 0;
        triangles = builder;
        init();
        return builder;
    }

    // -------------------------------------------------------------------------
    // Backward-compatible API (kept for applyToScene and any other callers)
    // -------------------------------------------------------------------------

    public DrawLinePath reset(AbstractPath p, float width) {
        prepareForPath(p);
        this.width = width;
        if (triangles != null) {
            triangles.length = 0;
        }
        return this;
    }

    public DrawLinePath reset(AbstractPath p) {
        prepareForPath(p);
        if (triangles != null) {
            triangles.length = 0;
        }
        return this;
    }

    public TriangleBuilder getTriangles() {
        if (triangles == null) {
            triangles = new TriangleBuilder(path.size() * 6);
        } else {
            triangles.length = 0;
        }
        init();
        return triangles;
    }

    public TriangleBuilder getTriangles(TriangleBuilder builder) {
        builder.length = 0;
        triangles = builder;
        init();
        return builder;
    }

    // -------------------------------------------------------------------------
    // Geometry generation
    // -------------------------------------------------------------------------

    /**
     * Adds a semicircular or arc cap at org, starting at angle theta and sweeping thetaDiff.
     * Uses incremental rotation: each step costs 4 multiplications instead of sin+cos.
     */
    private void addLineCap(Vec2 org, float theta, float thetaDiff) {
        float dir = Math.signum(thetaDiff);
        if (dir == 0f) return;
        thetaDiff *= dir;
        int amountPoints = (int) Math.ceil(thetaDiff / CAP_STEP);
        if (amountPoints == 0) return;

        if (dir < 0) theta += FMath.Pi;

        // Initial unit vector at theta (1 sin + 1 cos for the whole cap)
        float ux = (float) Math.cos(theta);
        float uy = (float) Math.sin(theta);
        current.x = ux * width + org.x;
        current.y = uy * width + org.y;

        // Rotation delta: dir * CAP_STEP applied incrementally
        final float cs = CAP_STEP_COS;
        final float ss = dir * CAP_STEP_SIN;

        float prevX = current.x, prevY = current.y;

        for (int i = 1; i <= amountPoints; i++) {
            float ux2, uy2;
            if (i == amountPoints && i * CAP_STEP > thetaDiff) {
                // Exact final position to avoid overshoot from float accumulation
                float finalAngle = theta + dir * thetaDiff;
                ux2 = (float) Math.cos(finalAngle);
                uy2 = (float) Math.sin(finalAngle);
            } else {
                // Incremental rotation: 4 multiplications instead of sin/cos
                ux2 = ux * cs - uy * ss;
                uy2 = uy * cs + ux * ss;
            }
            float x2 = ux2 * width + org.x;
            float y2 = uy2 * width + org.y;

            triangles.add(org.x, org.y, prevX, prevY, x2, y2);

            prevX = x2;
            prevY = y2;
            ux = ux2;
            uy = uy2;
        }
    }

    /**
     * Adds a rectangular quad strip for segment segIdx using precomputed normals.
     * Avoids calling lineOthNormal (which involves a sqrt) on every width pass.
     */
    private void addLineQuads(int segIdx, Vec2 ps, Vec2 pe) {
        float nx = segNormX[segIdx] * width;
        float ny = segNormY[segIdx] * width;

        float slx = ps.x + nx, sly = ps.y + ny; // startL
        float srx = ps.x - nx, sry = ps.y - ny; // startR
        float elx = pe.x + nx, ely = pe.y + ny; // endL
        float erx = pe.x - nx, ery = pe.y - ny; // endR

        triangles.add(ps.x, ps.y, pe.x, pe.y, elx, ely);
        triangles.add(ps.x, ps.y, elx, ely, slx, sly);
        triangles.add(ps.x, ps.y, erx, ery, pe.x, pe.y);
        triangles.add(ps.x, ps.y, srx, sry, erx, ery);
    }

    private void init() {
        int n = structureSize;
        if (n < 2) {
            if (n == 1) {
                Vec2 p0 = path.get(0);
                addLineCap(p0, FMath.Pi, FMath.Pi);
                addLineCap(p0, 0, FMath.Pi);
            }
            return;
        }

        Vec2 prev = path.get(0);
        Vec2 next = path.get(1);
        float theta = segTheta[0];
        addLineCap(prev, theta + FMath.PiHalf, FMath.Pi);
        addLineQuads(0, prev, next);

        if (n == 2) {
            addLineCap(next, theta - FMath.PiHalf, FMath.Pi);
            return;
        }

        float preTheta = theta;
        for (int i = 1; i < n - 1; i++) {
            prev = next;
            next = path.get(i + 1);
            float nextTheta = segTheta[i];
            addLineCap(prev, preTheta - FMath.PiHalf, nextTheta - preTheta);
            addLineQuads(i, prev, next);
            preTheta = nextTheta;
        }
        addLineCap(next, preTheta - FMath.PiHalf, FMath.Pi);
    }
}

package com.edlplan.osu.support.slider;

import com.edlplan.andengine.Triangle3DBuilder;
import com.edlplan.andengine.Triangle3DPack;
import com.edlplan.framework.math.line.LinePath;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class SliderBody3D extends AbstractSliderBody {

    private static float zOff = 0.001f;

    private static float zStart = -1 + zOff;

    private static float zEnd = 1;

    private Triangle3DPack body = null, border = null, bodyMask = null, borderMask = null;

    private RGBColor bodyColor = new RGBColor(), borderColor = new RGBColor();

    private float bodyWidth, borderWidth;

    private float startLength = 0, endLength = 0;
    private boolean dirty = true;

    // Cached path builder — prepared once per path, reused for both body and border widths
    private final Draw3DLinePath pathBuilder = new Draw3DLinePath();
    // Shared triangle builder — reset and refilled each onUpdate
    private final Triangle3DBuilder sharedBuilder = new Triangle3DBuilder();

    public SliderBody3D(LinePath path) {
        super(path);
    }

    @Override
    public void onUpdate() {
        if (body == null || border == null || !dirty) {
            return;
        }
        dirty = false;

        LinePath sub = path.cutPath(startLength, endLength).fitToLinePath();

        float zBody = -bodyWidth / borderWidth + zOff;
        float alpha = endLength / path.getMeasurer().maxLength();

        // Compute segment structure once for both width passes
        pathBuilder.prepareForPath(sub);

        body.setVertices(
                pathBuilder.buildForWidth(bodyWidth, 1, 1, sharedBuilder).getVertex());
        body.setAlpha(0.7f * alpha);

        border.setVertices(
                pathBuilder.buildForWidth(borderWidth, -1, -1, sharedBuilder).getVertex());
        border.setAlpha(alpha);
    }

    @Override
    public void setBodyWidth(float width) {
        bodyWidth = width;
    }

    @Override
    public void setBorderWidth(float width) {
        borderWidth = width;
    }

    @Override
    public void setBodyColor(float r, float g, float b) {
        bodyColor.set(r, g, b);
        if (body != null) {
            body.setColor(r, g, b);
        }
    }

    @Override
    public void setBorderColor(float r, float g, float b) {
        borderColor.set(r, g, b);
        if (border != null) {
            border.setColor(r, g, b);
        }
    }

    @Override
    public void setStartLength(float length) {
        if (startLength != length) {
            startLength = length;
            dirty = true;
        }
    }

    @Override
    public void setEndLength(float length) {
        if (endLength != length) {
            endLength = length;
            dirty = true;
        }
    }

    @Override
    public void applyToScene(Scene scene, boolean emptyOnStart) {

        if (!emptyOnStart) {
            startLength = 0;
            endLength = path.getMeasurer().maxLength();
        }

        float zBody = -bodyWidth / borderWidth + zOff;

        body = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, bodyWidth, zEnd, zBody))
                                .getTriangles()
                                .getVertex()
        );

        body.setClearDepthOnStart(true);

        border = new Triangle3DPack(0, 0,
                emptyOnStart ?
                        new float[0] :
                        (new Draw3DLinePath(path, borderWidth, zEnd, zStart))
                                .getTriangles()
                                .getVertex()
        );

        body.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b());
        border.setColor(borderColor.r(), borderColor.g(), borderColor.b());

        scene.attachChild(border, 0);
        scene.attachChild(body, 0);

        dirty = false;
    }

    @Override
    public void removeFromScene(Scene scene) {
        if (body != null) {
            body.detachSelf();
        }
        if (border != null) {
            border.detachSelf();
        }
        if (bodyMask != null) {
            bodyMask.detachSelf();
        }
        if (borderMask != null) {
            borderMask.detachSelf();
        }
    }
}

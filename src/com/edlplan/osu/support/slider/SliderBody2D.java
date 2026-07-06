package com.edlplan.osu.support.slider;

import com.edlplan.andengine.SpriteCache;
import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.andengine.TrianglePack;
import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.line.LinePath;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class SliderBody2D extends AbstractSliderBody {

    private static final ThreadLocal<BuildCache> localCache = new ThreadLocal<BuildCache>() {
        @Override
        public BuildCache get() {
            BuildCache cache = super.get();
            if (cache == null) {
                cache = new BuildCache();
                set(cache);
            }
            return cache;
        }
    };
    private float sliderBodyBaseAlpha = 0.7f;
    private float hintAlpha = 0.3f;
    private TrianglePack body = null, border = null, hint = null;
    private RGBColor bodyColor = new RGBColor(), borderColor = new RGBColor(), hintColor = new RGBColor();
    private float bodyWidth, borderWidth, hintWidth;
    private float startLength = 0, endLength = 0;
    private boolean enableHint = false;
    private boolean dirty = true;

    public SliderBody2D(LinePath path) {
        super(path);
    }

    public boolean isEnableHint() {
        return enableHint;
    }

    public void setEnableHint(boolean enableHint) {
        this.enableHint = enableHint;
    }

    public float getSliderBodyBaseAlpha() {
        return sliderBodyBaseAlpha;
    }

    @Override
    public void setSliderBodyBaseAlpha(float sliderBodyBaseAlpha) {
        this.sliderBodyBaseAlpha = sliderBodyBaseAlpha;
    }

    public void setHintAlpha(float hintAlpha) {
        this.hintAlpha = hintAlpha;
    }

    public void setHintColor(float r, float g, float b) {
        this.hintColor.set(r, g, b);
        if (hint != null) {
            hint.setColor(r, g, b);
        }
    }

    public void setHintWidth(float hintWidth) {
        this.hintWidth = hintWidth;
    }

    public void applyFadeAdjustments(float fadeInDuration) {
        if (body != null) {
            body.registerEntityModifier(new AlphaModifier(fadeInDuration, 0, sliderBodyBaseAlpha));
        }

        if (border != null) {
            border.registerEntityModifier(new FadeInModifier(fadeInDuration));
        }

        if (hint != null) {
            hint.registerEntityModifier(new AlphaModifier(fadeInDuration, 0, hintAlpha));
        }
    }

    public void applyFadeAdjustments(float fadeInDuration, float fadeOutDuration) {
        final EaseQuadOut easing = EaseQuadOut.getInstance();

        if (body != null) {
            body.registerEntityModifier(new SequenceEntityModifier(
                    new AlphaModifier(fadeInDuration, 0, sliderBodyBaseAlpha),
                    new AlphaModifier(fadeOutDuration, sliderBodyBaseAlpha, 0, easing)
            ));
        }

        if (border != null) {
            border.registerEntityModifier(new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration, easing)
            ));
        }

        if (hint != null) {
            hint.registerEntityModifier(new SequenceEntityModifier(
                    new AlphaModifier(fadeInDuration, 0, hintAlpha),
                    new AlphaModifier(fadeOutDuration, hintAlpha, 0, easing)
            ));
        }
    }

    @Override
    public void onUpdate() {
        if (body == null || border == null || !dirty) {
            return;
        }
        dirty = false;

        BuildCache cache = localCache.get();
        LinePath sub = path.cutPath(startLength, endLength).fitToLinePath(cache.path);

        // Compute segment angles and normals once for all width passes
        cache.drawLinePath.prepareForPath(sub);

        if (hint != null) {
            cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder)
                    .getVertex(hint.getVertices());
        }
        cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder)
                .getVertex(body.getVertices());
        cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder)
                .getVertex(border.getVertices());
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
        BuildCache cache = localCache.get();
        body = SpriteCache.trianglePackCache.get();
        border = SpriteCache.trianglePackCache.get();

        body.clearEntityModifiers();
        border.clearEntityModifiers();

        if (enableHint) {
            hint = SpriteCache.trianglePackCache.get();
            hint.clearEntityModifiers();
            hint.setAlpha(0);
            hint.setDepthTest(true);
            hint.setClearDepthOnStart(true);
            hint.setColor(hintColor.r(), hintColor.g(), hintColor.b());
        }

        body.setAlpha(0);
        body.setDepthTest(true);
        body.setClearDepthOnStart(!enableHint);
        body.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b());

        border.setAlpha(0);
        border.setDepthTest(true);
        border.setClearDepthOnStart(false);
        border.setColor(borderColor.r(), borderColor.g(), borderColor.b());

        if (emptyOnStart) {
            if (hint != null) {
                hint.getVertices().length = 0;
            }
            body.getVertices().length = 0;
            border.getVertices().length = 0;
        } else {
            cache.drawLinePath.prepareForPath(path);
            if (hint != null) {
                cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder)
                        .getVertex(hint.getVertices());
            }
            cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder)
                    .getVertex(body.getVertices());
            cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder)
                    .getVertex(border.getVertices());
        }

        if (!emptyOnStart) {
            startLength = 0;
            endLength = path.getMeasurer().maxLength();
        }
        dirty = false;

        scene.attachChild(border, 0);
        scene.attachChild(body, 0);
        if (hint != null) {
            scene.attachChild(hint, 0);
        }
    }

    @Override
    public void removeFromScene(Scene scene) {
        if (hint != null) {
            hint.detachSelf();
            SpriteCache.trianglePackCache.save(hint);
            hint = null;
        }
        if (body != null) {
            body.detachSelf();
            SpriteCache.trianglePackCache.save(body);
            body = null;
        }
        if (border != null) {
            border.detachSelf();
            SpriteCache.trianglePackCache.save(border);
            border = null;
        }
    }

    private static class BuildCache {
        public LinePath path = new LinePath();
        public TriangleBuilder triangleBuilder = new TriangleBuilder();
        public DrawLinePath drawLinePath = new DrawLinePath();
    }

    public static class SliderProperty {

        public Color4 color = Color4.White.copyNew();

        public float width;

        public TrianglePack pack;

    }
}

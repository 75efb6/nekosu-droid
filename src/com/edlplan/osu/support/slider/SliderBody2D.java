package com.edlplan.osu.support.slider;

import com.edlplan.andengine.SpriteCache;
import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.andengine.TrianglePack;
import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.math.line.PathMeasurer;
import com.edlplan.framework.utils.FloatArraySlice;
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
    private float maxPathLength = 0;
    private boolean enableHint = false;
    private boolean dirty = true;

    private static final float SNAKE_LENGTH_THRESHOLD = 0.75f;

    // Maximum vertex count per cached layer (~2MB). Prevents OOM on extreme sliders
    // (e.g. aspire maps with 100K+ control points) while still caching normal sliders.
    private static final int MAX_CACHED_VERTICES = 512 * 1024;

    // Prefix reuse cache — stores full-path vertex data to avoid per-frame rebuild during snaking.
    private float[] cachedBodyVertices;
    private float[] cachedBorderVertices;
    private float[] cachedHintVertices;
    private int cachedBodyLength;
    private int cachedBorderLength;
    private int cachedHintLength;

    public SliderBody2D(LinePath path) {
        super(path);
        maxPathLength = path.getMeasurer().maxLength();
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

        float maxLength = path.getMeasurer().maxLength();

        // Fast path: when slider is fully visible, use source path directly.
        boolean fullPath = startLength <= 0.001f && endLength >= maxLength - 0.001f;

        if (fullPath) {
            // Full build — also cache vertices for prefix reuse during future snaking
            cache.drawLinePath.prepareForPath(path);

            if (hint != null) {
                cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder);
                cache.hintBuilderLength = cache.triangleBuilder.length;
                cache.triangleBuilder.getVertex(hint.getVertices());
            }
            cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder);
            cache.bodyBuilderLength = cache.triangleBuilder.length;
            cache.triangleBuilder.getVertex(body.getVertices());

            cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder);
            cache.borderBuilderLength = cache.triangleBuilder.length;
            cache.triangleBuilder.getVertex(border.getVertices());

            // Cache full-path vertices for prefix reuse
            cacheBodyVertices(body.getVertices());
            cacheBorderVertices(border.getVertices());
            if (hint != null) {
                cacheHintVertices(hint.getVertices());
            }
            cache.cachedMaxLength = maxLength;
            cache.cachedStartLength = 0;
            cache.cachedEndLength = maxLength;
            return;
        }

        // Snake-in prefix reuse: startLength ≈ 0, endLength increasing.
        if (startLength <= 0.001f && cachedBodyVertices != null
                && endLength > cache.cachedEndLength) {
            buildSnakingPrefix(cache, maxLength);
            return;
        }

        // Snake-out suffix reuse: endLength ≈ maxLength, startLength increasing.
        if (endLength >= maxLength - 0.001f && cachedBodyVertices != null
                && startLength > cache.cachedStartLength) {
            buildSnakingSuffix(cache, maxLength);
            return;
        }

        // Fallback: full rebuild for other cases
        AbstractPath sub = path.cutPathView(startLength, endLength);
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

    /**
     * Builds the snaking-in body by copying cached prefix vertices and rebuilding
     * only the boundary segment + end cap. Turns O(N) per-frame rebuild into
     * O(boundary) copy + O(segment) rebuild.
     */
    private void buildSnakingPrefix(BuildCache cache, float maxLength) {
        // Find boundary segment using the full path's measurer
        PathMeasurer measurer = path.getMeasurer();
        int boundarySeg = measurer.binarySearch(endLength);
        if (boundarySeg >= structureSize() - 1) {
            boundarySeg = structureSize() - 2;
        }

        // Interpolate boundary point along the segment
        float segStartLen = measurer.getLengthAt(boundarySeg);
        float segEndLen = measurer.getLengthAt(boundarySeg + 1);
        float segLen = segEndLen - segStartLen;
        float t = segLen > 0.001f ? (endLength - segStartLen) / segLen : 1f;
        t = Math.min(1f, Math.max(0f, t));

        Vec2 segStart = path.get(boundarySeg);
        Vec2 segEnd = path.get(boundarySeg + 1);
        Vec2 boundaryPoint = new Vec2(
                segStart.x + (segEnd.x - segStart.x) * t,
                segStart.y + (segEnd.y - segStart.y) * t
        );

        // Rebuild DrawLinePath structure for the full path (normals are the same)
        cache.drawLinePath.prepareForPath(path);

        // Get the vertex offset where the boundary segment's quads start
        int prefixLength = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg);

        // --- Body ---
        if (prefixLength <= cachedBodyLength) {
            System.arraycopy(cachedBodyVertices, 0,
                    body.getVertices().ary, 0, prefixLength);
            body.getVertices().length = prefixLength;
        } else {
            body.getVertices().length = 0;
        }
        cache.drawLinePath.buildBoundarySuffix(bodyWidth, cache.triangleBuilder,
                boundarySeg, boundaryPoint, segStart);
        cache.triangleBuilder.getVertex(body.getVertices());

        // --- Border ---
        if (prefixLength <= cachedBorderLength) {
            System.arraycopy(cachedBorderVertices, 0,
                    border.getVertices().ary, 0, prefixLength);
            border.getVertices().length = prefixLength;
        } else {
            border.getVertices().length = 0;
        }
        cache.drawLinePath.buildBoundarySuffix(borderWidth, cache.triangleBuilder,
                boundarySeg, boundaryPoint, segStart);
        cache.triangleBuilder.getVertex(border.getVertices());

        // --- Hint ---
        if (hint != null && cachedHintVertices != null) {
            if (prefixLength <= cachedHintLength) {
                System.arraycopy(cachedHintVertices, 0,
                        hint.getVertices().ary, 0, prefixLength);
                hint.getVertices().length = prefixLength;
            } else {
                hint.getVertices().length = 0;
            }
            cache.drawLinePath.buildBoundarySuffix(hintWidth, cache.triangleBuilder,
                    boundarySeg, boundaryPoint, segStart);
            cache.triangleBuilder.getVertex(hint.getVertices());
        }

        cache.cachedEndLength = endLength;
    }

    /**
     * Builds the snaking-out body by copying cached suffix vertices and rebuilding
     * only the start cap + boundary segment + joint cap. The inverse of buildSnakingPrefix.
     * The end cap of the full path is already included in the suffix copy.
     */
    private void buildSnakingSuffix(BuildCache cache, float maxLength) {
        // Find boundary segment using the full path's measurer
        PathMeasurer measurer = path.getMeasurer();
        int boundarySeg = measurer.binarySearch(startLength);
        if (boundarySeg >= structureSize() - 1) {
            boundarySeg = structureSize() - 2;
        }

        // Interpolate boundary point along the segment
        float segStartLen = measurer.getLengthAt(boundarySeg);
        float segEndLen = measurer.getLengthAt(boundarySeg + 1);
        float segLen = segEndLen - segStartLen;
        float t = segLen > 0.001f ? (startLength - segStartLen) / segLen : 0f;
        t = Math.min(1f, Math.max(0f, t));

        Vec2 segStart = path.get(boundarySeg);
        Vec2 segEnd = path.get(boundarySeg + 1);
        Vec2 boundaryPoint = new Vec2(
                segStart.x + (segEnd.x - segStart.x) * t,
                segStart.y + (segEnd.y - segStart.y) * t
        );

        // Rebuild DrawLinePath structure for the full path (normals are the same)
        cache.drawLinePath.prepareForPath(path);

        // Build start cap + partial quad + joint cap, get suffix offset
        // --- Body ---
        body.getVertices().length = 0;
        cache.drawLinePath.buildBoundaryPrefix(bodyWidth, cache.triangleBuilder,
                boundarySeg, boundaryPoint, segEnd);
        int suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1);
        // Copy suffix from cached full-path vertices
        if (suffixOffset > 0 && suffixOffset <= cachedBodyLength) {
            int suffixLength = cachedBodyLength - suffixOffset;
            System.arraycopy(cachedBodyVertices, suffixOffset,
                    cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength);
            cache.triangleBuilder.length += suffixLength;
        }
        cache.triangleBuilder.getVertex(body.getVertices());

        // --- Border ---
        border.getVertices().length = 0;
        cache.drawLinePath.buildBoundaryPrefix(borderWidth, cache.triangleBuilder,
                boundarySeg, boundaryPoint, segEnd);
        suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1);
        if (suffixOffset > 0 && suffixOffset <= cachedBorderLength) {
            int suffixLength = cachedBorderLength - suffixOffset;
            System.arraycopy(cachedBorderVertices, suffixOffset,
                    cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength);
            cache.triangleBuilder.length += suffixLength;
        }
        cache.triangleBuilder.getVertex(border.getVertices());

        // --- Hint ---
        if (hint != null && cachedHintVertices != null) {
            hint.getVertices().length = 0;
            cache.drawLinePath.buildBoundaryPrefix(hintWidth, cache.triangleBuilder,
                    boundarySeg, boundaryPoint, segEnd);
            suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1);
            if (suffixOffset > 0 && suffixOffset <= cachedHintLength) {
                int suffixLength = cachedHintLength - suffixOffset;
                System.arraycopy(cachedHintVertices, suffixOffset,
                        cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength);
                cache.triangleBuilder.length += suffixLength;
            }
            cache.triangleBuilder.getVertex(hint.getVertices());
        }

        cache.cachedStartLength = startLength;
    }

    private int structureSize() {
        return path.size();
    }

    private void cacheBodyVertices(FloatArraySlice vertices) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedBodyVertices = null;
            cachedBodyLength = 0;
            return;
        }
        if (cachedBodyVertices == null || cachedBodyVertices.length < vertices.length) {
            cachedBodyVertices = new float[vertices.length];
        }
        System.arraycopy(vertices.ary, 0, cachedBodyVertices, 0, vertices.length);
        cachedBodyLength = vertices.length;
    }

    private void cacheBorderVertices(FloatArraySlice vertices) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedBorderVertices = null;
            cachedBorderLength = 0;
            return;
        }
        if (cachedBorderVertices == null || cachedBorderVertices.length < vertices.length) {
            cachedBorderVertices = new float[vertices.length];
        }
        System.arraycopy(vertices.ary, 0, cachedBorderVertices, 0, vertices.length);
        cachedBorderLength = vertices.length;
    }

    private void cacheHintVertices(FloatArraySlice vertices) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedHintVertices = null;
            cachedHintLength = 0;
            return;
        }
        if (cachedHintVertices == null || cachedHintVertices.length < vertices.length) {
            cachedHintVertices = new float[vertices.length];
        }
        System.arraycopy(vertices.ary, 0, cachedHintVertices, 0, vertices.length);
        cachedHintLength = vertices.length;
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
        float clamped = Math.max(0, Math.min(length, maxPathLength));
        if (Math.abs(startLength - clamped) >= SNAKE_LENGTH_THRESHOLD) {
            startLength = clamped;
            dirty = true;
        }
    }

    @Override
    public void setEndLength(float length) {
        float clamped = Math.max(0, Math.min(length, maxPathLength));
        if (Math.abs(endLength - clamped) >= SNAKE_LENGTH_THRESHOLD) {
            endLength = clamped;
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

        // Invalidate prefix cache on new slider
        cachedBodyVertices = null;
        cachedBorderVertices = null;
        cachedHintVertices = null;

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

            // Cache full-path vertices for prefix reuse
            cacheBodyVertices(body.getVertices());
            cacheBorderVertices(border.getVertices());
            if (hint != null) {
                cacheHintVertices(hint.getVertices());
            }
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
        public float cachedMaxLength;
        public float cachedStartLength;
        public float cachedEndLength;
        public int bodyBuilderLength;
        public int borderBuilderLength;
        public int hintBuilderLength;
    }

    public static class SliderProperty {

        public Color4 color = Color4.White.copyNew();

        public float width;

        public TrianglePack pack;

    }
}

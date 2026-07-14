package com.edlplan.andengine

import com.edlplan.framework.utils.FloatArraySlice
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import ru.nsu.ccfit.zuev.osu.polygon.PolygonVertexBuffer

class TrianglePack : Shape {

    private var mVertices: FloatArraySlice
    private var mPolygonVertexBuffer: PolygonVertexBuffer
    private var clearDepthOnStart = false
    private var depthTest = false

    constructor() : super(0f, 0f) {
        mVertices = FloatArraySlice()
        mVertices.ary = FloatArray(0)
        mPolygonVertexBuffer = PolygonVertexBuffer(0, GL11.GL_STATIC_DRAW, true)
    }

    constructor(pX: Float, pY: Float, pVertices: FloatArray) : this(pX, pY, pVertices,
        PolygonVertexBuffer(pVertices.size, GL11.GL_STATIC_DRAW, true))

    constructor(pX: Float, pY: Float, pVertices: FloatArray,
                pPolygonVertexBuffer: PolygonVertexBuffer) : super(pX, pY) {
        mVertices = FloatArraySlice()
        mVertices.ary = pVertices
        mVertices.length = pVertices.size
        this.mPolygonVertexBuffer = pPolygonVertexBuffer
        updateVertexBuffer()
    }

    fun setClearDepthOnStart(clearDepthOnStart: Boolean) {
        this.clearDepthOnStart = clearDepthOnStart
    }

    fun setDepthTest(depthTest: Boolean) {
        this.depthTest = depthTest
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
        if (clearDepthOnStart) {
            pGL.glClear(GL10.GL_DEPTH_BUFFER_BIT)
        }
    }

    override fun getVertexBuffer(): VertexBuffer {
        return mPolygonVertexBuffer
    }

    override fun onUpdateVertexBuffer() {
    }

    override fun onApplyVertices(pGL: GL10) {
    }

    var vertices: FloatArraySlice
        get() = mVertices
        set(v) { mVertices = v }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        if (mVertices.length == 0) return
        val tmp = GLHelper.isEnableDepthTest()
        GLHelper.setDepthTest(pGL, depthTest)
        pGL.glColor4f(red, green, blue, alpha)
        TriangleRenderer.get().renderTriangles(mVertices, pGL)
        GLHelper.setDepthTest(pGL, tmp)
    }

    override fun isCulled(pCamera: Camera): Boolean {
        return false
    }

    override fun collidesWith(pOtherShape: IShape): Boolean {
        return false
    }

    override fun getBaseHeight(): Float {
        return 0f
    }

    override fun getBaseWidth(): Float {
        return 0f
    }

    override fun getHeight(): Float {
        return 0f
    }

    override fun getSceneCenterCoordinates(): FloatArray? {
        return null
    }

    override fun getWidth(): Float {
        return 0f
    }

    @Deprecated("")
    override fun contains(pX: Float, pY: Float): Boolean {
        return false
    }

    @Deprecated("")
    override fun convertLocalToSceneCoordinates(pX: Float, pY: Float): FloatArray? {
        return null
    }

    @Deprecated("")
    override fun convertSceneToLocalCoordinates(pX: Float, pY: Float): FloatArray? {
        return null
    }
}

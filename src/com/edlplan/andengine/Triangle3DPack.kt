package com.edlplan.andengine

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import ru.nsu.ccfit.zuev.osu.polygon.PolygonVertexBuffer

class Triangle3DPack(
    pX: Float,
    pY: Float,
    private var mVertices: FloatArray,
    private val mPolygonVertexBuffer: PolygonVertexBuffer
) : Shape(pX, pY) {

    private var clearDepthOnStart = false

    constructor(pX: Float, pY: Float, pVertices: FloatArray) : this(pX, pY, pVertices,
        PolygonVertexBuffer(pVertices.size, GL11.GL_STATIC_DRAW, true))

    fun setClearDepthOnStart(clearDepthOnStart: Boolean) {
        this.clearDepthOnStart = clearDepthOnStart
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
        if (clearDepthOnStart) pGL.glClear(GL10.GL_DEPTH_BUFFER_BIT)
    }

    override fun getVertexBuffer(): VertexBuffer {
        return mPolygonVertexBuffer
    }

    override fun onUpdateVertexBuffer() {
    }

    fun updateShape() {
        onUpdateVertexBuffer()
    }

    fun getVertices(): FloatArray {
        return mVertices
    }

    fun setVertices(v: FloatArray) {
        mVertices = v
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        if (mVertices.isEmpty()) {
            return
        }
        val isEnable = GLHelper.isEnableDepthTest()
        GLHelper.enableDepthTest(pGL)
        pGL.glColor4f(red, green, blue, alpha)
        Triangle3DRenderer.get().renderTriangles(mVertices, pGL)
        GLHelper.setDepthTest(pGL, isEnable)
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

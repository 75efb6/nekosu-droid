package com.edlplan.andengine

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import ru.nsu.ccfit.zuev.osu.polygon.PolygonVertexBuffer

class TriangleTexture3DPack(
    pX: Float,
    pY: Float,
    private var mVertices: FloatArray,
    private var mTextureCoord: FloatArray,
    private val mPolygonVertexBuffer: PolygonVertexBuffer
) : Shape(pX, pY) {

    var textureRegion: TextureRegion? = null

    constructor(pX: Float, pY: Float, pVertices: FloatArray, pTextureCoord: FloatArray) : this(pX, pY, pVertices, pTextureCoord,
        PolygonVertexBuffer(pVertices.size, GL11.GL_STATIC_DRAW, true))

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.disableCulling(pGL)
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
        if (mVertices.isEmpty() || mTextureCoord.isEmpty() || textureRegion == null) {
            return
        }
        val isEnable = GLHelper.isEnableDepthTest()
        GLHelper.enableDepthTest(pGL)
        GLHelper.enableTexCoordArray(pGL)
        GLHelper.enableTextures(pGL)
        textureRegion!!.texture.bind(pGL)
        TriangleTexture3DRenderer.get().renderTriangles(mVertices, mTextureCoord, pGL)
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

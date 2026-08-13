package ru.nsu.ccfit.zuev.osu.polygon

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

class Polygon : Shape {
    private val mVertices: FloatArray
    private val mPolygonVertexBuffer: PolygonVertexBuffer

    constructor(pX: Float, pY: Float, pVertices: FloatArray) : this(
        pX, pY, pVertices,
        PolygonVertexBuffer(pVertices.size, GL11.GL_STATIC_DRAW, true)
    )

    constructor(pX: Float, pY: Float, pVertices: FloatArray, pPolygonVertexBuffer: PolygonVertexBuffer) : super(pX, pY) {
        this.mVertices = pVertices
        this.mPolygonVertexBuffer = pPolygonVertexBuffer
        this.updateVertexBuffer()
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }

    override fun getVertexBuffer(): VertexBuffer = mPolygonVertexBuffer

    override fun onUpdateVertexBuffer() {
        mPolygonVertexBuffer.update(mVertices)
    }

    fun updateShape() {
        onUpdateVertexBuffer()
    }

    fun getVertices(): FloatArray = mVertices

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        pGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.size / 2)
    }

    override fun isCulled(pCamera: Camera): Boolean = false

    override fun collidesWith(pOtherShape: IShape): Boolean = false

    override fun getBaseHeight(): Float = 0f

    override fun getBaseWidth(): Float = 0f

    override fun getHeight(): Float = 0f

    override fun getSceneCenterCoordinates(): FloatArray? = null

    override fun getWidth(): Float = 0f

    @Deprecated("")
    override fun contains(pX: Float, pY: Float): Boolean = false

    @Deprecated("")
    override fun convertLocalToSceneCoordinates(pX: Float, pY: Float): FloatArray? = null

    @Deprecated("")
    override fun convertSceneToLocalCoordinates(pX: Float, pY: Float): FloatArray? = null
}

package com.edlplan.framework.support.graphics

import com.edlplan.framework.math.Mat4
import com.edlplan.framework.math.Vec2

class Camera {
    private val maskMatrix = Mat4()
    private val projectionMatrix = Mat4()

    private val finalMatrix = Mat4()
    private var hasChange = true

    constructor() {
        maskMatrix.setIden()
        projectionMatrix.setIden()
    }

    constructor(c: Camera) {
        maskMatrix.set(c.maskMatrix)
        projectionMatrix.set(c.projectionMatrix)
        hasChange = true
    }

    fun set(c: Camera) {
        maskMatrix.set(c.maskMatrix)
        projectionMatrix.set(c.projectionMatrix)
        finalMatrix.set(c.finalMatrix)
        hasChange = c.hasChange
    }

    fun toProjPostion(x: Float, y: Float): Vec2 {
        return maskMatrix.mapToProj(x, y)
    }

    fun getMaskMatrix(): Mat4 {
        return maskMatrix
    }

    fun setMaskMatrix(maskMatrix: Mat4) {
        this.maskMatrix.set(maskMatrix)
    }

    fun getProjectionMatrix(): Mat4 {
        return projectionMatrix
    }

    fun setProjectionMatrix(projectionMatrix: Mat4) {
        this.projectionMatrix.set(projectionMatrix)
    }

    fun refresh() {
        hasChange = true
    }

    fun getFinalMatrix(): Mat4 {
        if (hasChange) {
            finalMatrix.set(maskMatrix).post(projectionMatrix)
            hasChange = false
            return finalMatrix
        } else {
            return finalMatrix
        }
    }

    fun copy(): Camera {
        return Camera(this)
    }

}

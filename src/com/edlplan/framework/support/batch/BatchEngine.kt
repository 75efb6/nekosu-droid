package com.edlplan.framework.support.batch

import com.edlplan.framework.support.graphics.Camera
import com.edlplan.framework.support.graphics.ShaderGlobals
import javax.microedition.khronos.opengles.GL10

class BatchEngine {
    companion object {
        @JvmField
        var pGL: GL10? = null
        @JvmField
        var shaderGlobals: ShaderGlobals = ShaderGlobals()
        private var savedbatch: AbstractBatch<*>? = null
        private var flushing = false

        @JvmStatic
        fun getShaderGlobals(): ShaderGlobals {
            return shaderGlobals
        }

        @JvmStatic
        fun setGlobalAlpha(alpha: Float) {
            if (Math.abs(shaderGlobals.alpha - alpha) > 0.002f) {
                flush()
                shaderGlobals.alpha = alpha
            }
        }

        @JvmStatic
        fun setGlobalCamera(camera: Camera) {
            flush()
            shaderGlobals.camera.set(camera)
        }

        fun bind(batch: AbstractBatch<*>) {
            flush()
            savedbatch = batch
        }

        fun unbind(batch: AbstractBatch<*>) {
            if (savedbatch === batch) {
                savedbatch = null
            }
        }

        @JvmStatic
        fun flush() {
            if (flushing) return
            if (savedbatch != null) {
                flushing = true
                savedbatch!!.flush()
                flushing = false
            }
        }

        @JvmStatic
        fun currentBatch(): AbstractBatch<*>? {
            return savedbatch
        }
    }
}

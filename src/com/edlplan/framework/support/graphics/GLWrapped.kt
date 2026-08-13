package com.edlplan.framework.support.graphics

import android.opengl.GLES10
import android.util.Log
import com.edlplan.framework.math.Color4
import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.utils.advance.BooleanSetting
import com.edlplan.framework.utils.interfaces.Setter
import java.nio.Buffer
import java.util.Stack

class GLWrapped {

    companion object {
        @JvmField
        val depthTest: BooleanSetting = BooleanSetting(Setter<Boolean> { t ->
            BatchEngine.flush()
            if (t) {
                GLES10.glEnable(GLES10.GL_DEPTH_TEST)
            } else {
                GLES10.glDisable(GLES10.GL_DEPTH_TEST)
            }
        }, false).initial()

        @JvmField
        var GL_SHORT: Int = GLES10.GL_SHORT
        @JvmField
        var GL_UNSIGNED_SHORT: Int = GLES10.GL_UNSIGNED_SHORT
        @JvmField
        var GL_TRIANGLES: Int = GLES10.GL_TRIANGLES
        @JvmField
        var GL_MAX_TEXTURE_SIZE: Int = 0

        @JvmField
        var blend: BlendSetting = BlendSetting().setUp()

        private var enable = true
        private var drawCalls = 0
        private var fboCreate = 0
        private var px1: Int = 0
        private var pw: Int = 0
        private var py1: Int = 0
        private var ph: Int = 0
        private val canvasStack: Stack<BaseCanvas> = Stack()

        @JvmStatic
        fun isEnable(): Boolean {
            return enable
        }

        @JvmStatic
        fun setEnable(enable: Boolean) {
            GLWrapped.enable = enable
        }

        @JvmStatic
        fun onFrame() {
            drawCalls = 0
            fboCreate = 0
        }

        @JvmStatic
        fun drawArrays(mode: Int, offset: Int, count: Int) {
            if (enable) GLES10.glDrawArrays(mode, offset, count)
            drawCalls++
        }

        @JvmStatic
        fun drawElements(mode: Int, count: Int, type: Int, b: Buffer) {
            if (enable) GLES10.glDrawElements(mode, count, type, b)
            drawCalls++
        }

        @JvmStatic
        fun frameDrawCalls(): Int {
            return drawCalls
        }

        @JvmStatic
        fun setViewport(x1: Int, y1: Int, w: Int, h: Int) {
            GLES10.glViewport(x1, y1, w, h)
            px1 = x1
            pw = w
            py1 = y1
            ph = h
        }

        @JvmStatic
        fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
            GLES10.glClearColor(r, g, b, a)
        }

        @JvmStatic
        fun clearColorBuffer() {
            if (enable) GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT)
        }

        @JvmStatic
        fun clearDepthBuffer() {
            if (enable) GLES10.glClear(GLES10.GL_DEPTH_BUFFER_BIT)
        }

        @JvmStatic
        fun clearDepthAndColorBuffer() {
            if (enable) GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT or GLES10.GL_DEPTH_BUFFER_BIT)
        }

        @JvmStatic
        fun setClearColor(c: Color4) {
            setClearColor(c.r, c.g, c.b, c.a)
        }

        @JvmStatic
        fun getFboCreate(): Int {
            return fboCreate
        }

        @JvmStatic
        fun getIntegerValue(key: Int): Int {
            val b = IntArray(1)
            GLES10.glGetIntegerv(key, b, 0)
            return b[0]
        }

        @JvmStatic
        fun checkGlError(op: String) {
            var error: Int
            while (GLES10.glGetError().also { error = it } != GLES10.GL_NO_ERROR) {
                Log.e("ES20_ERROR", "$op: glError $error")
                throw GLException("$op: glError $error")
            }
        }

        @JvmStatic
        fun prepareCanvas(canvas: BaseCanvas) {
            if (!canvasStack.empty()) {
                val pre = canvasStack.peek()
                if (pre.isPrepared()) {
                    pre.onUnprepare()
                }
            }
            BatchEngine.flush()
            canvasStack.push(canvas)
            canvas.onPrepare()
            BatchEngine.setGlobalCamera(canvas.getCamera())
        }

        @JvmStatic
        fun unprepareCanvas(canvas: BaseCanvas) {
            if (canvasStack.empty() || canvasStack.peek() !== canvas) {
                throw GLException("错误的canvas释放顺序！")
            }
            BatchEngine.flush()
            canvas.onUnprepare()
            canvasStack.pop()
            if (!canvasStack.empty()) {
                canvasStack.peek().onPrepare()
            }
        }

        @JvmStatic
        fun getUsingCanvas(): BaseCanvas? {
            return if (canvasStack.empty()) null else canvasStack.peek()
        }
    }
}

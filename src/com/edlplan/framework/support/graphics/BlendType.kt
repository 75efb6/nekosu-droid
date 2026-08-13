package com.edlplan.framework.support.graphics

import android.opengl.GLES20

enum class BlendType(
    val srcType: Int,
    val dstType: Int,
    val srcTypePreM: Int,
    val dstTypePreM: Int
) {
    Normal(
        GLES20.GL_SRC_ALPHA,
        GLES20.GL_ONE_MINUS_SRC_ALPHA,
        GLES20.GL_ONE,
        GLES20.GL_ONE_MINUS_SRC_ALPHA
    ),
    Additive(
        GLES20.GL_SRC_ALPHA,
        GLES20.GL_ONE,
        GLES20.GL_ONE,
        GLES20.GL_ONE
    ),
    Delete(
        GLES20.GL_ZERO,
        GLES20.GL_ONE_MINUS_SRC_COLOR,
        GLES20.GL_ZERO,
        GLES20.GL_ONE_MINUS_SRC_COLOR
    ),
    Delete_Alpha(
        GLES20.GL_ZERO,
        GLES20.GL_ONE_MINUS_SRC_ALPHA,
        GLES20.GL_ZERO,
        GLES20.GL_ONE_MINUS_SRC_ALPHA
    ),
    DeleteRepeat(
        GLES20.GL_ONE_MINUS_DST_ALPHA,
        GLES20.GL_ONE_MINUS_SRC_ALPHA,
        GLES20.GL_ONE_MINUS_DST_ALPHA,
        GLES20.GL_ONE_MINUS_SRC_ALPHA
    );
}

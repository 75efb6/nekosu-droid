package com.edlplan.framework.support.batch.`object`

import org.anddev.andengine.opengl.texture.region.TextureRegion

abstract class ATextureQuad {
    @JvmField
    var texture: TextureRegion? = null

    abstract fun write(ary: FloatArray, offset: Int)
}

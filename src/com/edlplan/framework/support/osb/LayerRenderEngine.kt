package com.edlplan.framework.support.osb

import com.edlplan.edlosbsupport.elements.StoryboardSprite

class LayerRenderEngine(private var layer: StoryboardSprite.Layer) : DepthOrderRenderEngine() {

    fun getLayer(): StoryboardSprite.Layer {
        return layer
    }

    fun setLayer(layer: StoryboardSprite.Layer) {
        this.layer = layer
    }
}

package com.edlplan.framework.support

import com.edlplan.framework.support.batch.BatchEngine

class SupportState {
    companion object {
        private var usingSupportCamera = false

        @JvmStatic
        fun isUsingSupportCamera(): Boolean {
            return usingSupportCamera
        }

        @JvmStatic
        fun setUsingSupportCamera(usingSupportCamera: Boolean) {
            BatchEngine.flush()
            SupportState.usingSupportCamera = usingSupportCamera
        }
    }
}
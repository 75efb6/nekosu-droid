package com.edlplan.framework.support.graphics

import android.graphics.BitmapFactory
import com.edlplan.framework.math.Vec2Int
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class BitmapUtil {

    companion object {
        @JvmStatic
        @Throws(FileNotFoundException::class)
        fun parseBitmapSize(file: File): Vec2Int {
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inJustDecodeBounds = true
            val v = Vec2Int()
            BitmapFactory.decodeStream(FileInputStream(file), null, decodeOptions)
            v.x = decodeOptions.outWidth
            v.y = decodeOptions.outHeight
            return v
        }
    }

}

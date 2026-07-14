package com.edlplan.framework.support

import android.os.Environment
import android.os.SystemClock
import com.edlplan.framework.utils.FileUtils
import java.io.File

class Framework {
    companion object {
        @JvmField
        const val PLATFORM_WIN_PC = 1

        @JvmField
        const val PLATFORM_ANDROID = 2

        private const val frameworkVersion = 1

        private const val platform = PLATFORM_ANDROID

        @JvmStatic
        fun getFrameworkVersion(): Int {
            return frameworkVersion
        }

        @JvmStatic
        fun getPlatform(): Int {
            return platform
        }

        @JvmStatic
        fun getFrameworkDir(): File {
            val dir = File(Environment.getExternalStorageDirectory(), "EdFramework")
            FileUtils.checkExistDir(dir)
            return dir
        }

        /**
         * 获取相对的精确时间
         */
        @JvmStatic
        fun relativePreciseTimeMillion(): Double {
            return System.nanoTime() / 1000000.0
        }

        @JvmStatic
        fun msToNm(ms: Double): Int {
            return (ms * 1000000).toInt()
        }

        @JvmStatic
        fun absoluteTimeMillion(): Long {
            return System.currentTimeMillis()
        }

        /**
         * @return 框架的标准时间
         */
        @JvmStatic
        fun frameworkTime(): Double {
            return SystemClock.uptimeMillis().toDouble()
        }
    }
}
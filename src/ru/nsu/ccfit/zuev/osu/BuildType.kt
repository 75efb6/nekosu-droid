package ru.nsu.ccfit.zuev.osu

import ru.nsu.ccfit.zuev.osuplus.BuildConfig

object BuildType {
    @JvmStatic
    fun hasOnlineAccess(): Boolean {
        return BuildConfig.BUILD_TYPE.matches(Regex("(release|pre_release|debug)"))
    }

    @JvmStatic
    fun isDebugEditor(): Boolean {
        return BuildConfig.DEBUG
    }
}

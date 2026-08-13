package com.edlplan.replay

import ru.nsu.ccfit.zuev.osu.Config
import java.io.File

object OdrConfig {

    @JvmStatic
    fun getSongDir(): File = File(Config.getBeatmapPath())

    @JvmStatic
    fun getDatabaseDir(): File = File(Config.getCorePath() + "/databases")

    @JvmStatic
    fun getScoreDir(): File = File(Config.getScorePath())

    @JvmStatic
    fun getMainDatabase(): File = File(getDatabaseDir(), "nekosu_droid.db")
}

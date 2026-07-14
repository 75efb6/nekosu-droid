package com.edlplan.replay

import ru.nsu.ccfit.zuev.osu.helper.FileUtils

class SongsLibrary {

    private val osu2set = HashMap<String, String>()

    init {
        val songs = OdrConfig.getSongDir()
        val songsList = FileUtils.listFiles(songs, ".osu") ?: return
        for (set in songsList) {
            if (set.isDirectory) {
                set.list()?.forEach { osu ->
                    osu2set[osu] = set.name + "/" + osu
                }
            }
        }
    }

    fun toSetLocal(raw: String): String {
        val osu = raw.substring(raw.indexOf("/") + 1, raw.length)
        return osu2set[osu] ?: raw
    }

    companion object {
        private var library: SongsLibrary? = null

        @JvmStatic
        fun get(): SongsLibrary {
            if (library == null) {
                library = SongsLibrary()
            }
            return library!!
        }
    }
}

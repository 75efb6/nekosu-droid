package ru.nsu.ccfit.zuev.osu.scoring

import java.util.HashMap

enum class TouchType(internal val id: Byte) {
    DOWN(0),
    MOVE(1),
    UP(2);

    companion object {
        private val byID = HashMap<Byte, TouchType>()

        init {
            for (v in entries) {
                byID[v.id] = v
            }
        }

        @JvmStatic
        fun getByID(id: Byte): TouchType? = byID[id]
    }
}

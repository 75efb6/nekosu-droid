package ru.nsu.ccfit.zuev.osu

class Constants private constructor() {
    companion object {
        @JvmField
        val MAP_WIDTH = 512
        @JvmField
        val MAP_HEIGHT = 384
        @JvmField
        val MAP_ACTUAL_WIDTH_OLD = 820
        @JvmField
        val MAP_ACTUAL_HEIGHT_OLD = 570
        @JvmField
        val MAP_ACTUAL_HEIGHT = (Config.getRES_HEIGHT() * 0.85f).toInt()
        @JvmField
        val MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4
        @JvmField
        val SLIDER_STEP = 10
        @JvmField
        val HIGH_SLIDER_STEP = 14
        @JvmField
        val DDL_URL_HTTPS = "https://osu.yas-online.net"
        @JvmField
        val DDL_URL = "http://osu.yas-online.net"
        @JvmField
        val SAMPLE_PREFIX = arrayOf("", "normal", "soft", "drum")
        @JvmField
        val SERVICE_ENDPOINT = "http://ops.dgsrz.com/api/"
        @JvmField
        val SERVICE_IDL_VERSION = "29"
    }
}

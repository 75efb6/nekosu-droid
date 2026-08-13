package ru.nsu.ccfit.zuev.osu.game

import ru.nsu.ccfit.zuev.osu.Constants

class TimingPoint(data: Array<String>, prevData: TimingPoint?) {

    @JvmField var time: Double
    @JvmField var beatLength: Double
    @JvmField var signature = 4
    @JvmField var hitSound: String? = null
    @JvmField var customSound = 0
    @JvmField var volume = 0f
    @JvmField var inherited = false
    @JvmField var kiai = false
    private val speed: Double

    init {
        time = data[0].toDouble() / 1000.0
        beatLength = data[1].toDouble()
        if (beatLength < 0 && prevData != null) {
            inherited = true
            speed = -100.0f / beatLength
            beatLength = -prevData.getBeatLength() * (beatLength / 100.0f)
        } else {
            beatLength /= 1000.0f
            speed = 1.0
        }

        if (data.size > 2) {
            if ("4" == data[2]) signature = 4
            if ("3" == data[2]) signature = 3
        }

        if (data.size > 3) {
            hitSound = when (data[3]) {
                "1" -> Constants.SAMPLE_PREFIX[1]
                "3" -> Constants.SAMPLE_PREFIX[3]
                else -> Constants.SAMPLE_PREFIX[2]
            }
        } else {
            hitSound = getDefaultSound()
        }
        if (data.size > 4) customSound = data[4].toInt()
        volume = if (data.size > 5) data[5].toInt() / 100f else 1f
        kiai = if (data.size > 7) data[7] != "0" else false
    }

    companion object {
        @JvmStatic
        private var defaultSound = "normal"

        @JvmStatic
        fun getDefaultSound(): String = defaultSound

        @JvmStatic
        fun setDefaultSound(defaultSound: String) {
            this.defaultSound = defaultSound
        }
    }

    fun wasInderited(): Boolean = inherited
    fun getHitSound(): String? = hitSound
    fun getCustomSound(): Int = customSound
    fun getVolume(): Float = volume
    fun getBeatLength(): Double = beatLength
    fun getSignature(): Int = signature
    fun getTime(): Double = time
    fun isKiai(): Boolean = kiai
    fun getSpeed(): Double = speed
}

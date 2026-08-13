package ru.nsu.ccfit.zuev.osu

import java.io.Serializable

class BeatmapProperties : Serializable {
    @JvmField
    var offset: Int = 0
    @JvmField
    var favorite: Boolean = false

    fun getOffset(): Int {
        return (Math.signum(offset.toFloat()) * Math.min(250.0, Math.abs(offset.toDouble()))).toInt()
    }

    fun setOffset(offset: Int) {
        this.offset = offset
    }

    fun isFavorite(): Boolean = favorite

    fun setFavorite(favorite: Boolean) {
        this.favorite = favorite
    }

    companion object {
        private const val serialVersionUID = -7229486402310659139L
    }
}

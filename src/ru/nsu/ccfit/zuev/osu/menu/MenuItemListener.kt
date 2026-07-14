package ru.nsu.ccfit.zuev.osu.menu

import ru.nsu.ccfit.zuev.osu.TrackInfo

interface MenuItemListener {
    fun select(item: MenuItem)
    fun selectTrack(track: TrackInfo, reloadBG: Boolean)
    fun stopScroll(y: Float)
    fun setY(y: Float)
    fun openScore(id: Int, showOnline: Boolean, playerName: String)
    fun playMusic(filename: String, previewTime: Int)
    fun isSelectAllowed(): Boolean
    fun showPropertiesMenu(item: MenuItem?)
}

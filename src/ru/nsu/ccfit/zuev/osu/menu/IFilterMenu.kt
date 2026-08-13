package ru.nsu.ccfit.zuev.osu.menu

import android.content.Context
import org.anddev.andengine.entity.scene.Scene

interface IFilterMenu {
    fun getFilter(): String
    fun getOrder(): SongMenu.SortOrder
    fun isFavoritesOnly(): Boolean
    fun getFavoriteFolder(): String?
    fun loadConfig(context: Context)
    fun getScene(): Scene
    fun hideMenu()
    fun showMenu(parent: SongMenu)
}

package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.util.Debug
import java.util.LinkedList

object SongMenuPool {
    private val instance = SongMenuPoolInternal()
    private val backgrounds: LinkedList<MenuItemBackground> = LinkedList()
    private val tracks: LinkedList<MenuItemTrack> = LinkedList()
    private var count = 0

    @JvmStatic
    fun getInstance(): SongMenuPoolInternal = instance

    class SongMenuPoolInternal {
        fun init() {
            count = 0
            tracks.clear()
            backgrounds.clear()
            for (i in 0 until 15) {
                backgrounds.add(MenuItemBackground())
            }
            for (i in 0 until 5) {
                tracks.add(MenuItemTrack())
            }
            count = 20
        }

        fun newBackground(): MenuItemBackground {
            if (backgrounds.isNotEmpty()) {
                return backgrounds.poll()
            }
            count++
            Debug.i("Count = $count")
            return MenuItemBackground()
        }

        fun putBackground(background: MenuItemBackground) {
            backgrounds.add(background)
        }

        fun newTrack(): MenuItemTrack {
            if (tracks.isNotEmpty()) {
                return tracks.poll()
            }
            count++
            Debug.i("Count = $count")
            return MenuItemTrack()
        }

        fun putTrack(track: MenuItemTrack) {
            tracks.add(track)
        }
    }
}

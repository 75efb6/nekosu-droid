package com.reco1l.legacy.ui

import com.reco1l.framework.lang.async
import com.reco1l.legacy.ui.multiplayer.LobbyScene
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.beatmapdownloader.BeatmapListing
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.BuildType
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

class MainMenu(val main: MainScene)
{

    private val sound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false)

    /**
     * Level 1: Play / Level 2: Solo / Level 3: Solo Play
     */
    val first = object : AnimSprite(0f, 0f, 0f, "play", "solo", "play")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                sound?.play()
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)

                if (main.isOnExitAnim)
                    return true

                when (getFrame()) {
                    // Play (level 1) -> show level 2
                    0 -> showLevel(1)

                    // Solo (level 2) -> show level 3
                    1 -> showLevel(2)

                    // Solo Play (level 3) -> open song selection
                    2 -> {
                        GlobalManager.getInstance().songService?.setGaming(true)

                        async {
                            LoadingScreen().show()

                            GlobalManager.getInstance().getMainActivity()?.checkNewSkins()
                            GlobalManager.getInstance().getMainActivity()?.checkNewBeatmaps()
                            LibraryManager.INSTANCE.updateLibrary(true)

                            if (LibraryManager.INSTANCE.library.isEmpty())
                            {
                                GlobalManager.getInstance().songService?.setGaming(false)
                                GlobalManager.getInstance().engine?.scene = main.scene

                                BeatmapListing().show()
                            } else {
                                main.musicControl(MusicOption.PLAY)

                                GlobalManager.getInstance().songMenu?.reload()
                                GlobalManager.getInstance().songMenu?.show()
                                GlobalManager.getInstance().songMenu?.select()
                            }
                        }
                    }
                }
                return true
            }
            return false
        }
    }

    /**
     * Level 1: Settings / Level 2: Multi / Level 3: Editor
     */
    val second = object : AnimSprite(0f, 0f, 0f, "options", "multi", "editor")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                sound?.play()
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)

                when (getFrame()) {
                    // Settings (level 1)
                    0 -> {
                        if (main.isOnExitAnim) return true
                        GlobalManager.getInstance().songService?.setGaming(true)
                        GlobalManager.getInstance().getMainActivity()?.runOnUiThread { SettingsMenu().show() }
                    }

                    // Multi (level 2)
                    1 -> {
                        if (!OnlineManager.getInstance().isStayOnline) {
                            ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                            return true
                        }

                        if (main.isOnExitAnim) return true

                        GlobalManager.getInstance().songService?.setGaming(true)
                        Multiplayer.isMultiplayer = true

                        async {
                            LoadingScreen().show()

                            GlobalManager.getInstance().getMainActivity()?.checkNewSkins()
                            GlobalManager.getInstance().getMainActivity()?.checkNewBeatmaps()
                            LibraryManager.INSTANCE.updateLibrary(true)

                            GlobalManager.getInstance().songMenu?.reload()

                            RoomScene.load()
                            LobbyScene.load()
                            LobbyScene.show()
                        }
                    }

                    // Editor (level 3)
                    2 -> {
                        if (main.isOnExitAnim) return true

                        if (!BuildType.isDebugEditor()) {
                            ToastLogger.showText("Editor is not available in this build", false)
                            return true
                        }

                        GlobalManager.getInstance().songService?.setGaming(true)

                        async {
                            LoadingScreen().show()

                            GlobalManager.getInstance().getMainActivity()?.checkNewSkins()
                            GlobalManager.getInstance().getMainActivity()?.checkNewBeatmaps()
                            LibraryManager.INSTANCE.updateLibrary(true)

                            if (LibraryManager.INSTANCE.library.isEmpty())
                            {
                                GlobalManager.getInstance().songService?.setGaming(false)
                                GlobalManager.getInstance().engine?.scene = main.scene

                                BeatmapListing().show()
                            } else {
                                main.musicControl(MusicOption.PLAY)

                                GlobalManager.getInstance().songMenu?.isEditorMode = true
                                GlobalManager.getInstance().songMenu?.reload()
                                GlobalManager.getInstance().songMenu?.show()
                                GlobalManager.getInstance().songMenu?.select()
                            }
                        }
                    }
                }
                return true
            }
            return false
        }
    }

    /**
     * Level 1: Exit / Level 2: Back / Level 3: Back
     */
    val third = object : AnimSprite(0f, 0f, 0f, "exit", "back", "back")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                sound?.play()
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)

                when (getFrame()) {
                    // Exit (level 1)
                    0 -> main.showExitDialog()

                    // Back (level 2 or 3) -> go back one level
                    1, 2 -> showLevel(menuLevel - 1)
                }
                return true
            }
            return false
        }
    }

    /**
     * Current menu level: 0 = first, 1 = second, 2 = third (solo submenu)
     */
    var menuLevel = 0
        private set

    fun attachButtons()
    {
        main.scene?.attachChild(first)
        main.scene?.attachChild(second)
        main.scene?.attachChild(third)
    }

    private fun showLevel(level: Int)
    {
        menuLevel = level
        first.setFrame(level)
        second.setFrame(level)
        third.setFrame(level)
    }

    fun showFirstMenu()
    {
        showLevel(0)
    }
}

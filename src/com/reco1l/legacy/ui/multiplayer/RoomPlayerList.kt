package com.reco1l.legacy.ui.multiplayer

import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.PlayerStatus.*
import com.reco1l.api.ibancho.data.RoomTeam.*
import com.reco1l.framework.lang.updateThread
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.entity.ScrollableList
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

class RoomPlayerList(val room: Room) : ScrollableList(), IScrollDetectorListener
{

    val menu = RoomPlayerMenu()

    var isValid = false


    init
    {
        for (i in 0 until room.maxPlayers)
        {
            camY = -146f

            val item = PlayerItem()
            attachChild(item)
            RoomScene.registerTouchArea(item)

            itemHeight = item.height
        }
    }


    fun invalidate()
    {
        isValid = false
    }

    override fun detachSelf(): Boolean
    {
        for (i in 0 until childCount)
            RoomScene.unregisterTouchArea(getChild(i) as ITouchArea)

        return RoomScene.detachChild(this)
    }

    override fun onDetached()
    {
        for (i in 0 until childCount)
            (getChild(i) as? PlayerItem)?.onDetached()
        super.onDetached()
    }


    override fun onManagedUpdate(secondsElapsed: Float)
    {
        if (!isValid)
        {
            isValid = true
            room.players.forEachIndexed { i, player ->

                val item = getChild(i) as PlayerItem

                item.room = room
                item.player = player
                item.isHost = player != null && player.id == room.host

                item.load()
            }
        }

        super.onManagedUpdate(secondsElapsed)
    }


    inner class PlayerItem : Rectangle(40f, 0f, Config.getRES_WIDTH() * 0.4f, 80f)
    {

        var room: Room? = null

        var player: RoomPlayer? = null

        var isHost: Boolean = false


        private val state = Rectangle(0f, 0f, 5f, height)

        private val text = ChangeableText(20f, 16f, ResourceManager.getInstance().getFont("smallFont"), "", 64)

        private var hostIcon: Sprite? = null

        private var missingIcon: Sprite? = null

        private var avatarSprite: Sprite? = null

        private var bannerSprite: Sprite? = null

        private var avatarTexture: TextureRegion? = null

        private var bannerTexture: TextureRegion? = null

        private var loadedPlayerId: Long = -1L


        private var moved = false
        private var dx = 0f
        private var dy = 0f


        init
        {
            attachChild(state)
            attachChild(text)
        }


        fun load()
        {
            setColor(1f, 1f, 1f, 0.15f)

            hostIcon?.detachSelf()
            missingIcon?.detachSelf()
            hostIcon = null
            missingIcon = null

            text.text = ""
            text.isVisible = false
            state.isVisible = false

            if (room == null || player == null)
            {
                if (loadedPlayerId != -1L)
                    clearAvatarAssets()
                return
            }

            state.isVisible = true
            text.isVisible = true
            text.text = "${player!!.name}\n${player!!.mods}"
            text.setPosition(80f, 16f)

            if (room!!.teamMode == TeamMode.TEAM_VS_TEAM)
            {
                when (player!!.team)
                {
                    RED -> setColor(1f, 0.2f, 0.2f, 0.15f)
                    BLUE -> setColor(0.2f, 0.2f, 1f, 0.15f)
                    else -> setColor(1f, 1f, 1f, 0.15f)
                }
            }
            else setColor(1f, 1f, 1f, 0.15f)

            if (isHost)
            {
                val icon = ResourceManager.getInstance().getTexture("crown")

                hostIcon = Sprite(width - icon!!.width - 15f, (height - icon.height) / 2f, icon)
                attachChild(hostIcon)
            }

            when (player!!.status)
            {
                MISSING_BEATMAP ->
                {
                    val icon = ResourceManager.getInstance().getTexture("missing")

                    missingIcon = Sprite(width - icon!!.width - 15f - (hostIcon?.let { it.width + 10f } ?: 0f), (height - icon.height) / 2f, icon)
                    attachChild(missingIcon)

                    state.setColor(1f, 0.1f, 0.1f)
                }

                NOT_READY -> state.setColor(1f, 0.1f, 0.1f)
                READY -> state.setColor(0.1f, 1f, 0.1f)
                PLAYING -> state.setColor(0.1f, 0.1f, 1f)
            }

            if (player!!.id != loadedPlayerId)
            {
                clearAvatarAssets()
                loadedPlayerId = player!!.id
                loadAvatarAsync(player!!.id)
            }
        }

        private fun clearAvatarAssets()
        {
            loadedPlayerId = -1L
            avatarSprite?.detachSelf()
            bannerSprite?.detachSelf()
            avatarSprite = null
            bannerSprite = null
            val av = avatarTexture
            val bn = bannerTexture
            avatarTexture = null
            bannerTexture = null
            if (av != null) updateThread { ResourceManager.getInstance().unloadTexture(av) }
            if (bn != null) updateThread { ResourceManager.getInstance().unloadTexture(bn) }
        }

        private fun loadAvatarAsync(uid: Long)
        {
            val avatarUrl = "https://${OnlineManager.HOSTNAME}/avatars/$uid"
            val bannerUrl = "https://${OnlineManager.HOSTNAME}/banners/user/$uid"

            Thread {
                OnlineManager.getInstance().loadAvatarToTextureManager(avatarUrl)
                OnlineManager.getInstance().loadBannerToTextureManager(bannerUrl)

                val aRaw = ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarUrl)
                val bRaw = ResourceManager.getInstance().getBannerTextureIfLoaded(bannerUrl)
                val aTexture = aRaw ?: ResourceManager.getInstance().getTexture("emptyavatar")

                updateThread {
                    if (parent == null || loadedPlayerId != uid) return@updateThread

                    if (bRaw != null)
                    {
                        bannerTexture = bRaw
                        val banner = Sprite(0f, 0f, width, height, bRaw)
                        banner.setColor(0.25f, 0.25f, 0.25f)
                        attachChild(banner, 0)
                        bannerSprite = banner
                    }

                    avatarTexture = aRaw
                    val avatar = Sprite(8f, (height - 60f) / 2f, 60f, 60f, aTexture)
                    attachChild(avatar)
                    avatarSprite = avatar
                }
            }.start()
        }

        override fun onDetached()
        {
            clearAvatarAssets()
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
        {
            handleScrolling(event)

            if (event.isActionDown)
            {
                moved = false
                dx = localX
                dy = localY

                alpha = 0.25f
                return true
            }

            if (event.isActionUp)
            {
                velocityY = 0f
                alpha = 0.15f

                if (moved || isScroll)
                    return true

                ResourceManager.getInstance().getSound("menuclick")?.play()

                if (player != null && Multiplayer.player != player)
                {
                    menu.player = player
                    menu.show()
                }
                return true
            }

            if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10)
            {
                moved = true
                alpha = 0.15f
                return true
            }

            return false
        }
    }
}

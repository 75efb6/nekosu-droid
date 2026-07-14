package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osuplus.R
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.Room
import com.reco1l.api.ibancho.data.RoomStatus.*
import com.reco1l.api.ibancho.data.TeamMode.HEAD_TO_HEAD
import com.reco1l.api.ibancho.data.TeamMode.TEAM_VS_TEAM
import com.reco1l.api.ibancho.data.WinCondition.*
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.framework.lang.mainThread
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.entity.ScrollableList
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager


class RoomPasswordDialog(
    private val room: Room,
    private val onSubmit: (String) -> Unit,
) : BaseFragment()
{
    override val layoutID = R.layout.dialog_room_password

    init { isDismissOnBackgroundClick = true }

    override fun onLoadView()
    {
        findViewById<TextView>(R.id.room_name)!!.text = room.name

        val input = findViewById<EditText>(R.id.password_input)!!

        fun submit()
        {
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(input.windowToken, 0)
            dismiss()
            onSubmit(input.text.toString())
        }

        input.setOnEditorActionListener { _, _, _ -> submit(); true }
        findViewById<View>(R.id.join_button)!!.setOnClickListener { submit() }

        input.post {
            input.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        playOnLoadAnim()
    }

    private fun playOnLoadAnim()
    {
        val body = findViewById<View>(R.id.body)!!
        body.alpha = 0f
        body.translationY = -200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    override fun dismiss()
    {
        val body = findViewById<View>(R.id.body) ?: return super.dismiss()
        body.animate().cancel()
        body.animate()
            .translationYBy(-200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) = super@RoomPasswordDialog.dismiss()
            })
            .start()
        playBackgroundHideOutAnim(200)
    }
}


class LobbyRoomList : ScrollableList()
{

    fun setList(rooms: List<Room>)
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as Sprite
            LobbyScene.unregisterTouchArea(item)
        }
        detachChildren()
        rooms.iterator().forEach { addItem(it) }
    }

    private fun showPasswordPrompt(room: Room) = mainThread {
        RoomPasswordDialog(room) { password ->
            connectToRoom(room, password)
        }.show()
    }

    private fun connectToRoom(room: Room, password: String? = null)
    {
        Multiplayer.log("Trying to connect socket...")

        LobbyScene.search.dismiss()
        LoadingScreen().show();

        { RoomAPI.connectToRoom(room.id, OnlineManager.getInstance().userId, OnlineManager.getInstance().username, password) }.orAsyncCatch {

            ToastLogger.showText("Failed to connect to the room: ${it.className} - ${it.message}", true)
            Multiplayer.log(it)
            LobbyScene.show()
        }
    }

    private fun addItem(room: Room)
    {
        val texture = ResourceManager.getInstance().getTexture("menu-button-background")

        camY = -146f

        val sprite = object : Sprite(Config.getRES_WIDTH() - texture!!.width - 20f, 0f, texture)
        {
            private var moved = false
            private var dx = 0f
            private var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                handleScrolling(event)

                if (event.isActionDown)
                {
                    moved = false
                    dx = localX
                    dy = localY

                    alpha = 0.6f
                    return true
                }

                if (event.isActionUp)
                {
                    velocityY = 0f
                    alpha = 0.3f

                    if (moved || isScroll)
                        return false

                    ResourceManager.getInstance().getSound("menuclick")?.play()

                    if (room.isLocked)
                        showPasswordPrompt(room)
                    else
                        connectToRoom(room)

                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10)
                {
                    alpha = 0.3f
                    moved = true
                }
                return false
            }
        }

        sprite.setColor(0f, 0f, 0f, 0.3f)

        // Icon
        val texName = when (room.teamMode)
        {
            HEAD_TO_HEAD -> "head_head"
            TEAM_VS_TEAM -> "team_vs"
        }

        val icon = Sprite(10f, 0f, ResourceManager.getInstance().getTexture(texName)).also {

            it.setScale(0.5f)
            it.setPosition(10f, (sprite.height - it.height) / 2f)
            sprite.attachChild(it)
        }

        // Title
        val name = Text(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), room.name).also {

            it.setPosition(icon.x + icon.width, 24f)
            sprite.attachChild(it)
        }

        // Info

        val status = when (room.status)
        {
            CHANGING_BEATMAP -> "Changing beatmap"
            PLAYING -> "Playing a match"
            else -> "Idle"
        }

        val winCondition = when (room.winCondition)
        {
            SCORE_V1 -> "Score V1"
            ACCURACY -> "Accuracy"
            MAX_COMBO -> "Combo"
            SCORE_V2 -> "Score V2"
        }

        val infoText = """
            ${room.playerCount} / ${room.maxPlayers} - ${room.playerNames}
            $status - $winCondition - ${room.modsToReadableString()}
        """.trimIndent()

        Text(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), infoText).also {

            it.setPosition(icon.x + icon.width, name.y + name.height)
            it.setColor(0.8f, 0.8f, 0.8f)
            sprite.attachChild(it)
        }

        // Lock indicator
        if (room.isLocked)
        {
            Sprite(0f, 0f, ResourceManager.getInstance().getTexture("lock")).also {

                it.setPosition(sprite.width - it.width - 5f, sprite.height - it.height - 5f)
                sprite.attachChild(it)
            }
        }

        attachChild(sprite)
        LobbyScene.registerTouchArea(sprite)

        itemHeight = sprite.height
    }

    override fun detachChildren()
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as Sprite
            LobbyScene.unregisterTouchArea(item)
        }
        super.detachChildren()
    }
}

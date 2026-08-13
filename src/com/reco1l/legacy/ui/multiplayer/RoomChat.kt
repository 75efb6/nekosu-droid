package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.framework.lang.mainThread
import com.reco1l.legacy.Multiplayer
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs
import ru.nsu.ccfit.zuev.osu.GlobalManager

class RoomChat : BaseFragment(), OnEditorActionListener, OnKeyListener
{
    override val layoutID = R.layout.multiplayer_room_chat

    var field: EditText? = null

    var text: TextView? = null

    private var scrollDownButton: View? = null

    private var isAtBottom = true

    val log = SpannableStringBuilder()

    private val isExtended: Boolean
        get() = findViewById<View?>(R.id.fullLayout) != null && abs(findViewById<View>(R.id.fullLayout)!!.translationY) < 10


    init
    {
        isDismissOnBackPress = false
    }


    override fun onLoadView()
    {
        reload()

        field = findViewById(R.id.chat_field)!!
        field!!.setOnEditorActionListener(this)
        field!!.setOnKeyListener(this)

        text = findViewById(R.id.chat_text)!!
        text!!.movementMethod = ScrollingMovementMethod()
        text!!.setOnScrollChangeListener { _, _, scrollY, _, _ -> updateScrollState(scrollY) }

        scrollDownButton = findViewById(R.id.scrollDownButton)
        scrollDownButton?.setOnClickListener { scrollToBottom() }

        // Restoring the chat log in case there is.
        text!!.text = log
        scrollToBottom()

        findViewById<View>(R.id.frg_header)!!.animate().cancel()
        findViewById<View>(R.id.frg_header)!!.alpha = 0f
        findViewById<View>(R.id.frg_header)!!.translationY = -100f
        findViewById<View>(R.id.frg_header)!!.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .start()
    }

    private fun updateScrollState(scrollY: Int)
    {
        val tv = text ?: return
        val layout = tv.layout ?: return
        val lineCount = tv.lineCount
        if (lineCount == 0) return
        val contentBottom = layout.getLineBottom(lineCount - 1)
        val visibleHeight = tv.height - tv.paddingTop - tv.paddingBottom
        val maxScroll = maxOf(0, contentBottom - visibleHeight)
        isAtBottom = scrollY >= maxScroll - SCROLL_THRESHOLD
        scrollDownButton?.visibility = if (isAtBottom) View.GONE else View.VISIBLE
    }

    private fun scrollToBottom()
    {
        val tv = text ?: return
        tv.post {
            val layout = tv.layout ?: return@post
            val lineCount = tv.lineCount
            if (lineCount == 0) return@post
            val contentBottom = layout.getLineBottom(lineCount - 1)
            val visibleHeight = tv.height - tv.paddingTop - tv.paddingBottom
            tv.scrollTo(0, maxOf(0, contentBottom - visibleHeight))
        }
    }

    private fun appendText(spanned: Spanned)
    {
        // Only play chat sound when not in gameplay.
        if (GlobalManager.getInstance().engine?.scene != GlobalManager.getInstance().gameScene?.scene)
            ResourceManager.getInstance().getSound("heartbeat")?.play(0.75f)

        if (log.isNotEmpty())
        {
            log.appendLine()
        }
        log.append(spanned)

        text?.text = log
        if (isAtBottom) scrollToBottom()
    }

    fun onRoomChatMessage(player: RoomPlayer, message: String) = mainThread {

        val color = when(player.id)
        {
            Multiplayer.player!!.id -> "#5245F7"
            in DEV_UIDS -> "#9E00FF"
            else -> "#F8558C"
        }

        val html = "<font color=$color><b>${player.name}: </b></font> <font color=#FFFFFF>$message</font>"
        val spanned = HtmlCompat.fromHtml(html, FROM_HTML_MODE_LEGACY)

        appendText(spanned)
        showPreview(" $message", tag = "${player.name}:", tagColor = color)
    }

    fun onSystemChatMessage(message: String, color: String) = mainThread {

        Multiplayer.log("System message: $message")

        val htmlError = "<font color=$color>${message}</font>"
        val spanned = HtmlCompat.fromHtml(htmlError, FROM_HTML_MODE_LEGACY)

        appendText(spanned)
        showPreview(message, contentColor = color)
    }

    private fun showPreview(content: String, contentColor: String? = null, tag: String? = null, tagColor: String? = null)
    {
        RGBColor.hex2Rgb(tagColor ?: "#FFFFFF").apply(RoomScene.chatPreview.tag)
        RGBColor.hex2Rgb(contentColor ?: "#FFFFFF").apply(RoomScene.chatPreview.content)

        RoomScene.chatPreview.setTagText(tag ?: "")
        RoomScene.chatPreview.setContentText(content)
    }

    private fun hideKeyboard()
    {
        field?.clearFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(field?.windowToken, 0)
    }

    private fun sendMessage()
    {
        hideKeyboard()

        val message = field?.text.takeUnless { it.isNullOrEmpty() } ?: return
        field?.text = null

        { RoomAPI.sendMessage(message.toString()) }.orAsyncCatch {

            onSystemChatMessage("Error to send message: ${it.message}", "#FF0000")
            it.printStackTrace()

        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean
    {
        if (actionId == EditorInfo.IME_ACTION_SEND)
        {
            sendMessage()
            return true
        }
        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean
    {
        if (keyCode == KeyEvent.KEYCODE_ENTER && v is TextView)
        {
            onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
            return true
        }
        return false
    }

    private fun reload()
    {
        val showMoreButton = findViewById<View>(R.id.showMoreButton) ?: return
        showMoreButton.setOnTouchListener { v: View, event: MotionEvent ->
            if (event.action == TouchEvent.ACTION_DOWN)
            {
                v.animate().cancel()
                v.animate().scaleY(0.9f).scaleX(0.9f).translationY(v.height * 0.1f).setDuration(100).start()
                toggleVisibility()
                return@setOnTouchListener true
            }
            else if (event.action == TouchEvent.ACTION_UP)
            {
                v.animate().cancel()
                v.animate().scaleY(1f).scaleX(1f).setDuration(100).translationY(0f).start()
                return@setOnTouchListener true
            }
            false
        }
        findViewById<View>(R.id.frg_background)!!.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleVisibility()
    {
        hideKeyboard()

        if (isExtended)
        {
            playHidePanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener(null)
            findViewById<View>(R.id.frg_background)!!.isClickable = false
        }
        else
        {
            playShowPanelAnim()
            findViewById<View>(R.id.frg_background)!!.setOnTouchListener { _, event ->
                if (event.action == TouchEvent.ACTION_DOWN)
                {
                    if (isExtended)
                    {
                        toggleVisibility()
                        return@setOnTouchListener true
                    }
                }
                false
            }
            findViewById<View>(R.id.frg_background)!!.isClickable = true
        }
    }

    private fun playShowPanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = true
                                         findViewById<View>(R.id.frg_background)!!.setOnClickListener { playHidePanelAnim() }
                                     }
                                 })
                    .start()
        }
    }

    private fun playHidePanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        if (fullLayout != null)
        {
            fullLayout.animate().cancel()
            fullLayout.animate()
                    .translationY(-findViewById<View>(R.id.optionBody)!!.height.toFloat())
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(object : BaseAnimationListener()
                                 {
                                     override fun onAnimationEnd(animation: Animator)
                                     {
                                         super.onAnimationEnd(animation)
                                         findViewById<View>(R.id.frg_background)!!.isClickable = false
                                     }
                                 })
                    .start()
        }
    }

    override fun callDismissOnBackPress()
    {
        if (isExtended)
        {
            mainThread { toggleVisibility() }
            return
        }

        if (GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene)
        {
            GlobalManager.getInstance().gameScene?.pause()
            return
        }
        mainThread { RoomScene.leaveDialog.showForResult { RoomScene.back() } }
    }

    companion object
    {
        private const val SCROLL_THRESHOLD = 20

        val DEV_UIDS = arrayOf<Long>(
                1,
        )
    }
}

package com.edlplan.ui.fragment

import android.animation.Animator
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osu.BeatmapProperties
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary
import ru.nsu.ccfit.zuev.osu.menu.IPropsMenu
import ru.nsu.ccfit.zuev.osu.menu.MenuItem
import ru.nsu.ccfit.zuev.osu.menu.SongMenu
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osuplus.R

class PropsMenuFragment : BaseFragment(), IPropsMenu {

    internal var menu: SongMenu? = null
    internal var item: MenuItem? = null
    internal var props: BeatmapProperties? = null

    private var offset: EditText? = null
    private var isFav: CheckBox? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override val layoutID: Int
        get() = R.layout.fragment_props_menu

    override fun onLoadView() {
        offset = findViewById(R.id.offsetBox)
        isFav = findViewById(R.id.addToFav)

        offset!!.setText(props?.offset?.toString() ?: "0")
        isFav!!.isChecked = props?.isFavorite() == true

        isFav!!.setOnCheckedChangeListener { _, isChecked ->
            props?.setFavorite(isChecked)
            saveProp()
        }

        offset!!.addTextChangedListener(object : TextWatcher {
            private var needRest = false
            private var o = 0
            private var pos = 0

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                pos = start
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                try {
                    o = s.toString().toInt()
                    needRest = false
                    if (Math.abs(o) > 250) {
                        o = 250 * if (o > 0) 1 else -1
                        needRest = true
                    }
                    if (needRest) {
                        offset!!.removeTextChangedListener(this)
                        offset!!.setText(o.toString())
                        offset!!.setSelection(pos)
                        offset!!.addTextChangedListener(this)
                    }
                    props?.offset = o
                    saveProp()
                } catch (e: NumberFormatException) {
                    if (s.isEmpty()) {
                        props?.offset = 0
                        saveProp()
                    }
                    return
                }
            }
        })

        findViewById<View>(R.id.manageFavButton)!!.setOnClickListener {
            val dialog = FavoriteManagerFragment()
            //TODO : 铺面引用还是全局耦合的，需要分离
            dialog.showToAddToFolder(
                ScoreLibrary.getTrackDir(GlobalManager.getInstance().selectedTrack?.filename ?: "")
            )
        }

        findViewById<View>(R.id.deleteBeatmap)!!.setOnClickListener {
            val confirm = ConfirmDialogFragment()
            confirm.showForResult { isAccepted ->
                if (isAccepted) {
                    menu?.scene?.postRunnable { item?.delete() }
                    dismiss()
                }
            }
        }

        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.alpha = 0f
        body.translationY(500f)
        body.animate().cancel()
        body.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutCubic))
            .setDuration(280)
            .start()
        playBackgroundHideInAnim(220)
    }

    private fun playEndAnim(action: Runnable?) {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.animate().cancel()
        body.animate()
            .alpha(0f)
            .translationY(500f)
            .setDuration(180)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    action?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(180)
    }

    override fun dismiss() {
        playEndAnim { super.dismiss() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun show(menu: SongMenu, item: MenuItem) {
        this.menu = menu
        this.item = item
        props = PropertiesLibrary.instance.getProperties(item.beatmap.path ?: "")
        if (props == null) {
            props = BeatmapProperties()
        }
        show()
    }

    fun saveProp() {
        PropertiesLibrary.instance.setProperties(
            item?.beatmap?.path ?: return, props!!
        )
        item?.setFavorite(props!!.favorite)
        PropertiesLibrary.instance.save()
    }
}

package com.edlplan.ui.fragment

import android.animation.Animator
import android.content.Intent
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.edlplan.framework.easing.Easing
import com.edlplan.replay.OdrDatabase
import com.edlplan.replay.OsuDroidReplay
import com.edlplan.replay.OsuDroidReplayPack
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.google.android.material.snackbar.Snackbar
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.util.Locale

class ScoreMenuFragment : BaseFragment() {

    private var scoreId: Int = 0

    init {
        isDismissOnBackgroundClick = true
    }

    override val layoutID: Int
        get() = R.layout.fragment_score_menu

    override fun onLoadView() {
        findViewById<View>(R.id.exportReplay)!!.setOnClickListener { v ->
            val replays: List<OsuDroidReplay> = OdrDatabase.get().getReplayById(scoreId)
            if (replays.isEmpty()) {
                return@setOnClickListener
            }
            try {
                val replay = replays[0]
                val file = File(
                    File(Environment.getExternalStorageDirectory(), "osu!droid/export"),
                    String.format(
                        Locale.getDefault(), "%s [%s]-%d.edr",
                        replay.fileName?.subSequence(
                            replay.fileName!!.indexOf('/') + 1,
                            replay.fileName!!.lastIndexOf('.')
                        ),
                        replay.playerName,
                        replay.time
                    )
                )
                if (!file.parentFile!!.exists()) {
                    file.parentFile!!.mkdirs()
                }
                OsuDroidReplayPack.packTo(file, replay)

                Snackbar.make(
                    v,
                    String.format(
                        v.context.resources.getString(R.string.frg_score_menu_export_succeed),
                        file.absolutePath
                    ),
                    2750
                ).setAction("Share") {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(
                        FileProvider.getUriForFile(
                            GlobalManager.getInstance().getMainActivity()!!,
                            BuildConfig.APPLICATION_ID + ".fileProvider",
                            file
                        ), "*/*"
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    GlobalManager.getInstance().getMainActivity()!!
                        .startActivityForResult(intent, 0)
                }.show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(v.context, R.string.frg_score_menu_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.deleteReplay)!!.setOnClickListener { v ->
            val confirm = ConfirmDialogFragment()
            confirm.showForResult { isAccepted ->
                if (isAccepted) {
                    val replays: List<OsuDroidReplay> = OdrDatabase.get().getReplayById(scoreId)
                    if (replays.isEmpty()) {
                        return@showForResult
                    }
                    try {
                        if (OdrDatabase.get().deleteReplay(scoreId) == 0) {
                            Snackbar.make(v, "Failed to delete replay!", 1500).show()
                        } else {
                            Snackbar.make(v, R.string.menu_deletescore_delete_success, 1500).show()
                        }
                        dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(v.context, "Failed to delete replay!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        playOnLoadAnim()
    }

    override fun dismiss() {
        playEndAnim { super.dismiss() }
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.alpha = 0f
        body.translationY = 500f
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

    fun show(scoreId: Int) {
        this.scoreId = scoreId
        show()
    }
}

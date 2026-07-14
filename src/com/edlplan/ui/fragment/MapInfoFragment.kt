package com.edlplan.ui.fragment

import android.animation.Animator
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osuplus.R

class MapInfoFragment : BaseFragment() {
    private lateinit var text: TextView
    var info: String? = null

    override val layoutID: Int
        get() = R.layout.mapinfo_dialog

    override fun onLoadView() {
        isDismissOnBackgroundClick = true
        text = findViewById(R.id.mapinfo_text)!!
        text.text = info
        val exit = findViewById<Button>(R.id.mapinfo_exit)!!
        exit.setOnClickListener { dismiss() }
        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.alpha = 0f
        body.translationY = 500f
        body.animate().cancel()
        body.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutCubic))
            .start()
        playBackgroundHideInAnim(220)
    }

    private fun playEndAnim(action: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
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
        playBackgroundHideOutAnim(200)
    }

    override fun dismiss() {
        playEndAnim { super.dismiss() }
    }
    /*
    fun showWithMap(track: TrackInfo, speedMultiplier: Float) {
        val diffRecalculator = DifficultyReCalculator()
        if (!diffRecalculator.calculateMapInfo(track, speedMultiplier)) {
            return
        }
        val circleCount = track.hitCircleCount
        val sliderCount = track.sliderCount
        val spinnerCount = track.spinnerCount
        val objectCount = track.totalHitObjectCount
        val circlePercent = circleCount.toFloat() / objectCount * 100
        val sliderPercent = sliderCount.toFloat() / objectCount * 100
        val spinnerPercent = spinnerCount.toFloat() / objectCount * 100
        val singleCount = diffRecalculator.singleCount
        val fastSingleCount = diffRecalculator.fastSingleCount
        val streamCount = diffRecalculator.streamCount
        val jumpCount = diffRecalculator.jumpCount
        val multiCount = diffRecalculator.multiCount
        val switchCount = diffRecalculator.switchFingeringCount
        val singlePercent = singleCount.toFloat() / objectCount * 100
        val fastSinglePercent = fastSingleCount.toFloat() / objectCount * 100
        val streamPercent = streamCount.toFloat() / objectCount * 100
        val jumpPercent = jumpCount.toFloat() / objectCount * 100
        val multiPercent = multiCount.toFloat() / objectCount * 100
        val switchPercent = switchCount.toFloat() / objectCount * 100
        val longestStreamCount = diffRecalculator.longestStreamCount
        val realTime = diffRecalculator.realTime
        val objectPerMin = objectCount / realTime * 60

        val string = StringBuilder()
        //string.append(String.format(StringTable.get(R.string.binfoStr2),
        //    track.getHitCircleCount(), track.getSliderCount(), track.getSpinnerCount(), track.getBeatmapSetID()))
        //string.append("\n\r")
        string.append(String.format("圈数:%d[%.1f%%] 滑条数:%d[%.1f%%] 转盘数:%d[%.1f%%] 物件数:%d 实际时间:%.1fs %.1f物件/分",
            circleCount, circlePercent, sliderCount, sliderPercent, spinnerCount, spinnerPercent,
            objectCount, realTime, objectPerMin))
        string.append("\n\r")
        string.append(String.format("单点:%d[%.1f%%] 高速单点:%d[%.1f%%] 连打:%d[%.1f%%] 跳:%d[%.1f%%]",
            singleCount, singlePercent, fastSingleCount, fastSinglePercent,
            streamCount, streamPercent, jumpCount, jumpPercent))
        string.append("\n\r")
        string.append(String.format("多押:%d[%.1f%%] 切指:%d[%.1f%%] 最长连打:%d",
            multiCount, multiPercent,
            switchCount, switchPercent, longestStreamCount))
        info = string.toString()
        show()
    }
    */
}

package com.edlplan.ui.fragment

import android.animation.Animator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.CalibratorErrorBarView
import ru.nsu.ccfit.zuev.osuplus.R

class AudioCalibratorFragment : BaseFragment() {

    override val layoutID: Int = R.layout.fragment_audio_calibrator

    private enum class State {
        IDLE, COUNTDOWN, COLLECTING, RESULT
    }

    private var state = State.IDLE
    private var targetTapCount = 20
    private var tapCount = 0
    private val tapErrors = mutableListOf<Float>()

    private var beatIntervalMs = 500L
    private var countdownBeatsRemaining = 0
    private var lastBeatTimeMs = 0L
    private var lastTapTimeMs = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var metronomeClick: BassSoundProvider? = null

    private lateinit var stateText: TextView
    private lateinit var countText: TextView
    private lateinit var averageText: TextView
    private lateinit var errorBar: CalibratorErrorBarView
    private lateinit var resultCard: LinearLayout
    private lateinit var resultOffsetText: TextView
    private lateinit var startButton: Button
    private lateinit var applyButton: Button
    private lateinit var retryButton: Button
    private lateinit var sampleCountSpinner: Spinner
    private lateinit var bpmEditText: EditText

    private var recommendedOffset = 0

    private val beatRunnable = object : Runnable {
        override fun run() {
            if (state != State.COUNTDOWN && state != State.COLLECTING) return

            metronomeClick?.play()
            lastBeatTimeMs = SystemClock.elapsedRealtime()

            if (state == State.COUNTDOWN) {
                countdownBeatsRemaining--
                if (countdownBeatsRemaining > 0) {
                    stateText.text = "${countdownBeatsRemaining - 1}"
                }
                if (countdownBeatsRemaining <= 0) {
                    state = State.COLLECTING
                    stateText.text = "Tap now!"
                    countText.text = "0 / $targetTapCount"
                }
            }

            handler.postDelayed(this, beatIntervalMs)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onLoadView() {
        stateText = findViewById(R.id.cal_state_text)!!
        countText = findViewById(R.id.cal_count_text)!!
        averageText = findViewById(R.id.cal_average_text)!!
        errorBar = findViewById(R.id.cal_error_bar)!!
        resultCard = findViewById(R.id.cal_result_card)!!
        resultOffsetText = findViewById(R.id.cal_result_offset_text)!!
        startButton = findViewById(R.id.cal_start_button)!!
        applyButton = findViewById(R.id.cal_apply_button)!!
        retryButton = findViewById(R.id.cal_retry_button)!!
        sampleCountSpinner = findViewById(R.id.cal_sample_count_spinner)!!
        bpmEditText = findViewById(R.id.cal_bpm_edit)!!

        startButton.setOnClickListener { startCalibration() }
        applyButton.setOnClickListener { applyOffset() }
        retryButton.setOnClickListener { resetToIdle() }

        root?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onScreenTap()
            }
            false
        }

        metronomeClick = BassSoundProvider().apply {
            prepare(context!!.assets, "sfx/nightcore-hat.ogg")
        }

        playOnLoadAnim()
    }

    override fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        metronomeClick?.free()
        metronomeClick = null
        playOnDismissAnim(Runnable { super@AudioCalibratorFragment.dismiss() })
    }

    override fun callDismissOnBackPress() {
        if (state == State.COUNTDOWN || state == State.COLLECTING) {
            handler.removeCallbacksAndMessages(null)
            resetToIdle()
            return
        }
        super.callDismissOnBackPress()
    }

    private fun startCalibration() {
        val bpm = bpmEditText.text.toString().toIntOrNull()?.coerceIn(30, 300) ?: 120
        beatIntervalMs = 60_000L / bpm

        targetTapCount = sampleCountSpinner.selectedItem.toString().toIntOrNull() ?: 20
        tapCount = 0
        tapErrors.clear()
        errorBar.clear()

        state = State.COUNTDOWN
        countdownBeatsRemaining = 4

        startButton.visibility = View.GONE
        applyButton.visibility = View.GONE
        retryButton.visibility = View.GONE
        resultCard.visibility = View.GONE
        averageText.text = ""

        stateText.text = "3"
        countText.text = ""

        sampleCountSpinner.isEnabled = false
        bpmEditText.isEnabled = false

        handler.post(beatRunnable)
    }

    private fun onScreenTap() {
        if (state != State.COLLECTING) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTapTimeMs < 100) return
        lastTapTimeMs = now

        var errorMs = (now - lastBeatTimeMs).toFloat()
        val halfInterval = beatIntervalMs / 2f
        if (errorMs > halfInterval) errorMs -= beatIntervalMs.toFloat()

        tapErrors.add(errorMs)
        tapCount++

        errorBar.addError(errorMs)
        countText.text = "$tapCount / $targetTapCount"

        val avg = tapErrors.average().toFloat()
        averageText.text = "Avg: ${String.format("%+.1f", avg)} ms"

        if (tapCount >= targetTapCount) {
            finishCalibration()
        }
    }

    private fun finishCalibration() {
        handler.removeCallbacksAndMessages(null)
        state = State.RESULT

        val avg = tapErrors.average().toFloat()
        recommendedOffset = (-avg).toInt().coerceIn(-250, 250)

        stateText.text = "Calibration complete"
        countText.text = ""
        averageText.text = ""
        resultCard.visibility = View.VISIBLE
        resultOffsetText.text = "${if (recommendedOffset >= 0) "+" else ""}$recommendedOffset ms"

        applyButton.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE

        sampleCountSpinner.isEnabled = true
        bpmEditText.isEnabled = true
    }

    private fun applyOffset() {
        val ctx = context ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putInt("offset", recommendedOffset).apply()
        Config.setOffset(recommendedOffset.toFloat())

        Toast.makeText(ctx, "Offset set to $recommendedOffset ms", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun resetToIdle() {
        handler.removeCallbacksAndMessages(null)
        state = State.IDLE
        tapCount = 0
        tapErrors.clear()
        errorBar.clear()

        stateText.text = "Press Start to begin"
        countText.text = ""
        averageText.text = ""
        resultCard.visibility = View.GONE

        startButton.visibility = View.VISIBLE
        applyButton.visibility = View.GONE
        retryButton.visibility = View.GONE

        sampleCountSpinner.isEnabled = true
        bpmEditText.isEnabled = true
    }

    protected fun playOnLoadAnim() {
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
        playBackgroundHideInAnim(200)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.animate().cancel()
        body.animate()
            .alpha(0f)
            .translationY(500f)
            .setDuration(180)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    runnable?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(180)
    }
}

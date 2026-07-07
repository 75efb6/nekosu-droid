package com.reco1l.legacy.replay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.Locale

class ReplayOverlayFragment : BaseFragment(), ReplayOverlay.Listener {

    private lateinit var toggleButton: ImageView
    private lateinit var controlPanel: View
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var pauseBtn: ImageView
    private lateinit var seekBackBtn: ImageView
    private lateinit var seekFwdBtn: ImageView
    private lateinit var speedText: TextView

    private var isPaused = false

    override val layoutID: Int
        get() = R.layout.fragment_replay_overlay

    override fun onLoadView() {
        isDismissOnBackPress = false

        // Don't let the root layout consume touch events — the GLSurfaceView
        // underneath (AndEngine) needs them for PauseMenu buttons and gameplay.
        root?.isClickable = false
        root?.isFocusable = false
        root?.setOnTouchListener(null)
        root?.isHorizontalScrollBarEnabled = false
        root?.isVerticalScrollBarEnabled = false
        findViewById<View>(R.id.frg_background)?.apply {
            setOnClickListener(null)
            setOnTouchListener(null)
            isClickable = false
        }

        toggleButton = findViewById(R.id.replayToggleButton)!!
        controlPanel = findViewById(R.id.replayControlPanel)!!
        seekBar = findViewById(R.id.replaySeekBar)!!
        currentTimeText = findViewById(R.id.replayCurrentTime)!!
        totalTimeText = findViewById(R.id.replayTotalTime)!!
        pauseBtn = findViewById(R.id.replayPauseBtn)!!
        seekBackBtn = findViewById(R.id.replaySeekBackBtn)!!
        seekFwdBtn = findViewById(R.id.replaySeekFwdBtn)!!
        speedText = findViewById(R.id.replaySpeedText)!!

        toggleButton.setOnClickListener {
            if (controlPanel.visibility == View.VISIBLE) {
                controlPanel.visibility = View.GONE
            } else {
                controlPanel.visibility = View.VISIBLE
            }
        }

        controlPanel.visibility = View.GONE

        pauseBtn.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            if (game.isPaused) {
                game.resume()
                isPaused = false
                pauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                game.pause()
                isPaused = true
                pauseBtn.setImageResource(android.R.drawable.ic_media_play)
            }
        }

        seekBackBtn.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newPos = (ReplayOverlay.currentSeekPositionMs - ReplayOverlay.SEEK_STEP_MS).coerceAtLeast(0)
            game.replaySeekTo(newPos)
        }

        seekFwdBtn.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newPos = (ReplayOverlay.currentSeekPositionMs + ReplayOverlay.SEEK_STEP_MS)
                .coerceAtMost(ReplayOverlay.totalLengthMs)
            game.replaySeekTo(newPos)
        }

        findViewById<View>(R.id.replaySpeedDown)!!.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newSpeed = (ReplayOverlay.currentSpeed - ReplayOverlay.SPEED_FINE_STEP)
                .coerceAtLeast(ReplayOverlay.MIN_SPEED)
            game.replaySetSpeed(newSpeed)
        }

        findViewById<View>(R.id.replaySpeedUp)!!.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newSpeed = (ReplayOverlay.currentSpeed + ReplayOverlay.SPEED_FINE_STEP)
                .coerceAtMost(ReplayOverlay.MAX_SPEED)
            game.replaySetSpeed(newSpeed)
        }

        findViewById<View>(R.id.replaySpeedDownCoarse)!!.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newSpeed = (ReplayOverlay.currentSpeed - ReplayOverlay.SPEED_COARSE_STEP)
                .coerceAtLeast(ReplayOverlay.MIN_SPEED)
            game.replaySetSpeed(newSpeed)
        }

        findViewById<View>(R.id.replaySpeedUpCoarse)!!.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            val newSpeed = (ReplayOverlay.currentSpeed + ReplayOverlay.SPEED_COARSE_STEP)
                .coerceAtMost(ReplayOverlay.MAX_SPEED)
            game.replaySetSpeed(newSpeed)
        }

        speedText.setOnClickListener {
            val game = GlobalManager.getInstance().gameScene ?: return@setOnClickListener
            game.replaySetSpeed(ReplayOverlay.originalSpeed)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val totalMs = ReplayOverlay.totalLengthMs
                if (totalMs <= 0 || totalMs == Int.MAX_VALUE) return
                val targetMs = (progress.toLong() * totalMs / seekBar!!.max).toInt()
                val game = GlobalManager.getInstance().gameScene ?: return
                game.replaySeekTo(targetMs)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                ReplayOverlay.seeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ReplayOverlay.seeking = false
            }
        })

        ReplayOverlay.listener = this

        totalTimeText.text = formatTime(ReplayOverlay.totalLengthMs)
        onPositionUpdate(ReplayOverlay.currentSeekPositionMs)
        onSpeedUpdate(ReplayOverlay.currentSpeed)
    }

    override fun onPositionUpdate(positionMs: Int) {
        if (!isAdded) return
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            currentTimeText.text = formatTime(positionMs)
            if (!ReplayOverlay.seeking) {
                val totalMs = ReplayOverlay.totalLengthMs
                if (totalMs > 0 && totalMs != Int.MAX_VALUE) {
                    seekBar.progress = (positionMs.toLong() * seekBar.max / totalMs).toInt()
                }
            }
        }
    }

    override fun onSpeedUpdate(speed: Float) {
        if (!isAdded) return
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            speedText.text = String.format(Locale.US, "%.2fx", speed)
        }
    }

    fun updatePaused(paused: Boolean) {
        isPaused = paused
        if (!isAdded) return
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            pauseBtn.setImageResource(
                if (paused) android.R.drawable.ic_media_play
                else android.R.drawable.ic_media_pause
            )
        }
    }

    fun dismissOverlay() {
        if (isAdded) {
            ReplayOverlay.listener = null
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    companion object {
        fun formatTime(ms: Int): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}

package com.reco1l.legacy.ui.spectate

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.server.ServerAPI
import com.reco1l.api.server.SpectateAPI
import com.reco1l.legacy.ui.beatmapdownloader.BeatmapDownloader
import com.reco1l.legacy.ui.beatmapdownloader.BeatmapListing
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * Overlay shown when spectating another player.
 * Displays target name, live score/combo/accuracy, and a stop button.
 */
class SpectateScene : BaseFragment() {

    override val layoutID = R.layout.spectate_overlay

    private var targetName = ""
    private var targetId = 0L
    private var launchingGame = false
    private var pendingBeatmapMd5: String? = null

    private var targetNameView: TextView? = null
    private var scoreView: TextView? = null
    private var comboView: TextView? = null
    private var accView: TextView? = null
    private var spectatorCountView: TextView? = null
    private var waitingView: TextView? = null
    private var body: View? = null

    private val dataListener: (JSONObject) -> Unit = { data ->
        activity?.runOnUiThread {
            val score = data.optJSONObject("s")
            if (score != null) {
                scoreView?.text = "${score.optInt("score", 0)}"
                comboView?.text = "${score.optInt("combo", 0)}x"
                val acc = score.optDouble("acc", 0.0) * 100
                accView?.text = String.format("%.2f%%", acc)
            }
        }
    }

    private val stateListener: (JSONObject) -> Unit = { state ->
        val beatmapMd5 = state.optString("beatmapMd5", "")
        if (beatmapMd5.isNotEmpty()) {
            activity?.runOnUiThread {
                if (launchingGame) {
                    // Host restarted or changed map while we're already in game
                    restartSpectateGame(beatmapMd5)
                } else {
                    startSpectatingGame(beatmapMd5)
                }
            }
        }
    }

    private val pausedListener: (JSONObject) -> Unit = { data ->
        activity?.runOnUiThread {
            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene != null && gameScene.isSpectating) {
                gameScene.spectatePause()
            }
        }
    }

    private val finishedListener: (JSONObject) -> Unit = { data ->
        activity?.runOnUiThread {
            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene != null && gameScene.isSpectating) {
                // Map finished, wait for next state event
                ToastLogger.showText("Map finished", false)
            }
        }
    }

    private val endedListener: (Long) -> Unit = { uid ->
        if (uid == targetId) {
            activity?.runOnUiThread {
                val gameScene = GlobalManager.getInstance().gameScene
                if (gameScene != null && gameScene.isSpectating) {
                    gameScene.spectateStop()
                }
                launchingGame = false
                ToastLogger.showText("Host quit the map — waiting for next game...", false)
            }
        }
    }

    private val deathListener: (JSONObject) -> Unit = { data ->
        val uid = data.optLong("targetId", 0)
        if (uid == targetId) {
            activity?.runOnUiThread {
                val gameScene = GlobalManager.getInstance().gameScene
                if (gameScene != null && gameScene.isSpectating) {
                    gameScene.spectateStop()
                }
                launchingGame = false
                ToastLogger.showText("Host failed — waiting for retry...", false)
            }
        }
    }

    private val skipListener: (JSONObject) -> Unit = { data ->
        activity?.runOnUiThread {
            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene != null && gameScene.isSpectating) {
                gameScene.spectateSkip()
            }
        }
    }

    private val resumeListener: (JSONObject) -> Unit = { data ->
        activity?.runOnUiThread {
            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene != null && gameScene.isSpectating) {
                gameScene.spectateResume()
            }
        }
    }

    private val errorListener: (String) -> Unit = { error ->
        activity?.runOnUiThread {
            ToastLogger.showText("Spectate error: $error", true)
            dismiss()
        }
    }

    private fun startSpectatingGame(beatmapMd5: String) {

        // Find track by MD5 in library
        val track = LibraryManager.INSTANCE.findTrackByMD5(beatmapMd5)
        if (track == null) {
            promptDownloadBeatmap(beatmapMd5)
            return
        }

        launchSpectateGame(track)
    }

    private fun launchSpectateGame(track: ru.nsu.ccfit.zuev.osu.TrackInfo) {
        val gameScene = GlobalManager.getInstance().gameScene ?: return

        // No AUTO mod - spectator receives hit data from host and renders it passively
        // Set spectating state on GameScene
        gameScene.isSpectating = true
        gameScene.spectateTargetId = targetId

        // Register spectate data listeners IMMEDIATELY (before startGame which is async)
        gameScene.registerSpectateListeners()

        // Launch game - keep this fragment alive (don't dismiss) so the stop button stays visible
        launchingGame = true
        gameScene.startGame(track, null)
    }

    private fun restartSpectateGame(beatmapMd5: String) {
        val track = LibraryManager.INSTANCE.findTrackByMD5(beatmapMd5)
        if (track == null) {
            promptDownloadBeatmap(beatmapMd5)
            return
        }

        val gameScene = GlobalManager.getInstance().gameScene ?: return
        gameScene.isSpectating = true
        gameScene.spectateTargetId = targetId
        gameScene.registerSpectateListeners()
        gameScene.startGame(track, null)
    }

    private fun promptDownloadBeatmap(beatmapMd5: String) {
        val ctx = activity ?: return

        // Query server for beatmap info on background thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            com.reco1l.framework.lang.Execution.async {
                val result = ServerAPI.getBeatmap(md5 = beatmapMd5)
                val setTitle = result?.optString("title", "") ?: ""
                val setArtist = result?.optString("artist", "") ?: ""
                val setId = result?.optInt("set_id", -1) ?: -1

                ctx.runOnUiThread {
                    if (setId <= 0) {
                        ToastLogger.showText("Beatmap not found on server", true)
                        return@runOnUiThread
                    }

                    val displayName = if (setArtist.isNotEmpty() && setTitle.isNotEmpty()) {
                        "$setArtist - $setTitle"
                    } else {
                        "Unknown beatmap"
                    }

                    android.app.AlertDialog.Builder(ctx)
                        .setTitle("Beatmap not found locally")
                        .setMessage("Download $displayName to spectate?")
                        .setPositiveButton("Download") { _, _ ->
                            val url = BeatmapListing.mirror.downloadEndpoint(setId.toLong())
                            BeatmapDownloader.download(url, "$setId $displayName.osz") {
                                // After download completes, try to spectate again
                                val newTrack = LibraryManager.INSTANCE.findTrackByMD5(beatmapMd5)
                                if (newTrack != null) {
                                    activity?.runOnUiThread {
                                        launchSpectateGame(newTrack)
                                    }
                                } else {
                                    ToastLogger.showText("Beatmap import failed", true)
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    override fun onLoadView() {
        currentInstance = this
        targetNameView = findViewById(R.id.spectateTargetName)
        scoreView = findViewById(R.id.spectateScore)
        comboView = findViewById(R.id.spectateCombo)
        accView = findViewById(R.id.spectateAcc)
        spectatorCountView = findViewById(R.id.spectatorCountView)

        targetNameView?.text = targetName

        findViewById<View>(R.id.stopButton)?.setOnClickListener {
            SpectateAPI.stopSpectating(targetId)

            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene != null && gameScene.isSpectating) {
                gameScene.quit()
            }

            dismiss()
        }

        SpectateAPI.onCount { count ->
            activity?.runOnUiThread {
                if (count > 0) {
                    spectatorCountView?.text = "${count} watching"
                    spectatorCountView?.visibility = View.VISIBLE
                } else {
                    spectatorCountView?.visibility = View.GONE
                }
            }
        }

        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.spectateOverlay) ?: return
        body.alpha = 0f
        body.translationY = -50f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    override fun dismiss() {
        currentInstance = null
        SpectateAPI.clearListeners()
        val body = findViewById<View>(R.id.spectateOverlay)
        if (body == null) {
            super@SpectateScene.dismiss()
            return
        }
        body.animate().cancel()
        body.animate()
            .translationY(-50f)
            .alpha(0f)
            .setDuration(150)
            .setListener(null)
            .start()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isAdded) super@SpectateScene.dismiss()
        }, 150)
    }

    fun toggleOverlay() {
        val body = findViewById<View>(R.id.spectateOverlay) ?: return
        if (body.visibility == View.VISIBLE) {
            body.animate().cancel()
            body.animate().alpha(0f).setDuration(150).withEndAction { body.visibility = View.GONE }.start()
        } else {
            body.visibility = View.VISIBLE
            body.alpha = 0f
            body.animate().cancel()
            body.animate().alpha(1f).setDuration(150).start()
        }
    }

    companion object {
        var currentInstance: SpectateScene? = null
            private set
        fun newInstance(targetId: Long, targetName: String): SpectateScene {
            return SpectateScene().apply {
                arguments = Bundle().apply {
                    putLong("targetId", targetId)
                    putString("targetName", targetName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetId = arguments?.getLong("targetId", 0) ?: 0
        targetName = arguments?.getString("targetName", "") ?: ""

        // Register all SpectateAPI callbacks EARLY (before any socket events can arrive)
        SpectateAPI.onData(dataListener)
        SpectateAPI.onState(stateListener)
        SpectateAPI.onPaused(pausedListener)
        SpectateAPI.onFinished(finishedListener)
        SpectateAPI.onEnded(endedListener)
        SpectateAPI.onError(errorListener)
        SpectateAPI.onDeath(deathListener)
        SpectateAPI.onSkip(skipListener)
        SpectateAPI.onResume(resumeListener)

        // Register spectate data listeners on GameScene
        val gameScene = GlobalManager.getInstance().gameScene
        if (gameScene != null) {
            gameScene.isSpectating = true
            gameScene.spectateTargetId = targetId
            gameScene.registerSpectateListeners()
        }
    }
}

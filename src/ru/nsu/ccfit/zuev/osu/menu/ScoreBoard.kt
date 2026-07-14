package ru.nsu.ccfit.zuev.osu.menu

import android.database.Cursor
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.Multiplayer
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.input.touch.detector.ScrollDetector
import org.anddev.andengine.input.touch.detector.SurfaceScrollDetector
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

class ScoreBoard(scene: Scene, layer: Entity, listener: MenuItemListener) : Entity(), ScrollDetector.IScrollDetectorListener {

    private val mainScene: Scene
    private val listener: MenuItemListener
    private val loadingText: ChangeableText
    private var percentShow = -1f
    var isShowOnlineScores = false
        private set
    private var lastTrack: TrackInfo? = null
    private var wasOnline = false
    private var isScroll = false
    private val mScrollDetector: SurfaceScrollDetector
    private var maxY = 100500f
    private var pointerId = -1
    private var initialY = -1f
    private var touchY: Float? = null
    private var camY = -146f
    private var velocityY = 0f
    private var secPassed = 0f
    private var tapTime = 0f
    private var height = 0f
    private var downTime = -1f
    private var _scoreID = -1
    private var moved = false
    private var scoreItems: ArrayList<ScoreBoardItem>? = null
    private var currentTask: LoadTask? = null
    private var currentAvatarTask: Runnable? = null
    private val loadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        this.mainScene = scene
        layer.attachChild(this)
        this.loadingText = ChangeableText(5f, 230f, ResourceManager.getInstance().getFont("strokeFont"), "", 50)
        this.attachChild(this.loadingText)
        this.listener = listener
        this.mScrollDetector = SurfaceScrollDetector(this)
    }

    private fun initFromOnline(track: TrackInfo) {
        loadingText.setText("Loading scores...")
        currentTask = object : LoadTask(true) {
            override fun run() {
                val trackFile = File(track.filename)
                val scores: List<String>
                try {
                    scores = OnlineManager.getInstance().getTop(trackFile, track.md5!!)
                } catch (e: OnlineManager.OnlineManagerException) {
                    Debug.e("Cannot load scores ${e.message}")
                    if (isActive()) loadingText.setText("Cannot load scores")
                    return
                }
                if (!isActive()) return
                loadingText.setText(OnlineManager.getInstance().failMessage)
                val items = ArrayList<ScoreBoardItem>(scores.size)
                val sb = StringBuilder()
                var nextTotalScore = 0
                for (i in scores.indices) {
                    if (!isActive()) break
                    Debug.i(scores[i])
                    val data = scores[i].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (data.size < 8 || data.size > 10) continue
                    val scoreID = data[0].toInt()
                    val isInLeaderboard = data.size == 8
                    val isPersonalBest = data.size == 9 || data[1] == OnlineManager.getInstance().username
                    val playerName = if (isPersonalBest) OnlineManager.getInstance().username else data[1]
                    val currentTotalScore = data[2].toInt()
                    val combo = data[3].toInt()
                    val mark = data[4]
                    val modString = data[5]
                    val accuracy = GameHelper.Round(data[6].toInt() / 1000f, 2)
                    val avatarURL = "https://" + OnlineManager.HOSTNAME + "/avatars/" + data[7]
                    val bannerURL = "https://" + OnlineManager.HOSTNAME + "/banners/user/" + data[7]
                    val beatmapRank = if (isPersonalBest && !isInLeaderboard) data[8].toInt() else i + 1
                    val titleStr = "#$beatmapRank $playerName\n${StringTable.format(R.string.menu_score, formatScore(sb, currentTotalScore), combo)}"
                    if (i < scores.size - 1) {
                        val nextData = scores[i + 1].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (nextData.size == 8 || nextData.size == 9) {
                            nextTotalScore = nextData[2].toInt()
                        }
                    } else {
                        nextTotalScore = 0
                    }
                    val diffTotalScore = currentTotalScore - nextTotalScore
                    val accStr = convertModString(sb, modString) + "\n" + String.format(Locale.ENGLISH, "%.2f", accuracy) + "%" + "\n" + if (nextTotalScore == 0) "-" else (if (diffTotalScore != 0) "+" else "") + diffTotalScore
                    if (!isActive()) return
                    if (isPersonalBest) {
                        attachChild(ScoreItem(avatarExecutor, titleStr, accStr, mark, true, scoreID, avatarURL, bannerURL, playerName, true), 0)
                    }
                    if (isInLeaderboard) {
                        attachChild(ScoreItem(avatarExecutor, titleStr, accStr, mark, true, scoreID, avatarURL, bannerURL, playerName, false))
                        val item = ScoreBoardItem()
                        item.set(beatmapRank, playerName, combo, currentTotalScore, scoreID)
                        items.add(item)
                    }
                }
                scoreItems = items
                percentShow = 0f
            }
        }
        loadExecutor.submit(currentTask)
    }

    private fun initFromLocal(track: TrackInfo) {
        currentTask = object : LoadTask(false) {
            override fun run() {
                val columns = arrayOf("id", "playername", "score", "combo", "mark", "accuracy", "mode")
                val scoreSet: Cursor? = ScoreLibrary.getInstance().getMapScores(columns, track.filename!!)
                scoreSet?.use {
                    if (it.count == 0 || !isActive()) {
                        if (isActive()) scoreItems = ArrayList()
                        return
                    }
                    val items = ArrayList<ScoreBoardItem>(it.count)
                    val sb = StringBuilder()
                    var nextTotalScore: Int
                    for (i in 0 until it.count) {
                        if (!isActive()) break
                        it.moveToPosition(i)
                        val scoreID = it.getInt(0)
                        val currTotalScore = it.getInt(it.getColumnIndexOrThrow("score"))
                        val totalScore = formatScore(sb, currTotalScore)
                        val titleStr = "#${i + 1} ${it.getString(it.getColumnIndexOrThrow("playername"))}\n${StringTable.format(R.string.menu_score, totalScore, it.getInt(it.getColumnIndexOrThrow("combo")))}"
                        if (i < it.count - 1) {
                            it.moveToPosition(i + 1)
                            nextTotalScore = it.getInt(it.getColumnIndexOrThrow("score"))
                            it.moveToPosition(i)
                        } else {
                            nextTotalScore = 0
                        }
                        val diffTotalScore = currTotalScore - nextTotalScore
                        val accStr = convertModString(sb, it.getString(it.getColumnIndexOrThrow("mode"))) + "\n" + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(it.getFloat(it.getColumnIndexOrThrow("accuracy")) * 100, 2)) + "%" + "\n" + if (nextTotalScore == 0) "-" else (if (diffTotalScore != 0) "+" else "") + diffTotalScore
                        if (!isActive()) return
                        attachChild(ScoreItem(avatarExecutor, titleStr, accStr, it.getString(it.getColumnIndexOrThrow("mark")), false, scoreID, null, null, null, false))
                        val item = ScoreBoardItem()
                        item.set(i + 1, it.getString(it.getColumnIndexOrThrow("playername")), it.getInt(it.getColumnIndexOrThrow("combo")), it.getInt(it.getColumnIndexOrThrow("score")), scoreID)
                        items.add(item)
                    }
                    scoreItems = items
                    percentShow = 0f
                }
            }
        }
        loadExecutor.submit(currentTask)
    }

    @Synchronized
    fun init(track: TrackInfo?) {
        if (lastTrack === track && isShowOnlineScores == wasOnline && wasOnline) return
        currentTask?.avatarExecutor?.shutdownNow()
        loadingText.setText("")
        lastTrack = track
        wasOnline = isShowOnlineScores
        scoreItems = null
        Execution.updateThread {
            detachChildren()
            currentAvatarTask = null
            attachChild(loadingText)
            if (track == null) return@updateThread
            if (OnlineManager.getInstance().isStayOnline && isShowOnlineScores) {
                initFromOnline(track)
                return@updateThread
            }
            initFromLocal(track)
        }
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)
        secPassed += pSecondsElapsed
        if (childCount <= 1) return
        if (percentShow == -1f) {
            var y = -camY
            val count = childCount
            for (i in 0 until count) {
                val child = getChild(i)
                if (child !is Sprite) continue
                child.setPosition(child.x, y)
                y += 0.8f * (child.height - 32)
            }
            y += camY
            camY += velocityY * pSecondsElapsed
            maxY = y - 0.8f * (Config.getRES_HEIGHT() - 110 - (height - 32))
            if (camY <= -146 && velocityY < 0 || camY > maxY && velocityY > 0) {
                camY -= velocityY * pSecondsElapsed
                velocityY = 0f
                isScroll = false
            }
            if (Math.abs(velocityY) > 500 * pSecondsElapsed) {
                velocityY -= 10 * pSecondsElapsed * Math.signum(velocityY)
            } else {
                velocityY = 0f
                isScroll = false
            }
        } else {
            percentShow += pSecondsElapsed * 4
            if (percentShow > 1) percentShow = 1f
            val count = childCount
            for (i in 0 until count) {
                val child = getChild(i)
                if (child !is Sprite) continue
                child.setPosition(-160f, 146 + 0.8f * percentShow * i * (child.height - 32))
            }
            if (percentShow == 1f) percentShow = -1f
        }
        if (downTime >= 0) downTime += pSecondsElapsed
        if (downTime > 0.5f) {
            moved = true
            if (!Multiplayer.isMultiplayer && _scoreID != -1 && !isShowOnlineScores) {
                GlobalManager.getInstance().songMenu?.showDeleteScoreMenu(_scoreID)
            }
            downTime = -1f
        }
    }

    override fun onScroll(pScrollDetector: ScrollDetector, pTouchEvent: TouchEvent, pDistanceX: Float, pDistanceY: Float) {
        when (pTouchEvent.action) {
            TouchEvent.ACTION_DOWN -> {
                velocityY = 0f
                touchY = pTouchEvent.y
                pointerId = pTouchEvent.pointerID
                tapTime = secPassed
                initialY = touchY!!
                isScroll = true
            }
            TouchEvent.ACTION_MOVE -> {
                if (pointerId == -1 || pointerId == pTouchEvent.pointerID) {
                    isScroll = true
                    if (initialY == -1f) {
                        velocityY = 0f
                        touchY = pTouchEvent.y
                        pointerId = pTouchEvent.pointerID
                        tapTime = secPassed
                        initialY = touchY!!
                    }
                    val dy = pTouchEvent.y - touchY!!
                    camY -= dy
                    touchY = pTouchEvent.y
                    if (camY <= -146) {
                        camY = -146f
                        velocityY = 0f
                    } else if (camY >= maxY) {
                        camY = maxY
                        velocityY = 0f
                    }
                }
            }
            else -> {
                if (pointerId == -1 || pointerId == pTouchEvent.pointerID) {
                    touchY = null
                    if (secPassed - tapTime < 0.001f || initialY == -1f) {
                        velocityY = 0f
                        isScroll = false
                    } else {
                        velocityY = (initialY - pTouchEvent.y) / (secPassed - tapTime)
                        isScroll = true
                    }
                    pointerId = -1
                    initialY = -1f
                }
            }
        }
    }

    fun setShowOnlineScores(showOnlineScores: Boolean) {
        this.isShowOnlineScores = showOnlineScores
    }

    fun getScoreBoardItems(): ArrayList<ScoreBoardItem>? = scoreItems

    private abstract inner class LoadTask(fromOnline: Boolean) : Runnable {
        val avatarExecutor: ExecutorService? = if (fromOnline) Executors.newSingleThreadExecutor() else null
        fun isActive(): Boolean = currentTask === this
    }

    private inner class ScoreItem(
        avatarExecutor: ExecutorService?,
        title: String,
        acc: String,
        markStr: String,
        showOnline: Boolean,
        scoreID: Int,
        avaURL: String?,
        banURL: String?,
        username: String?,
        isPersonalBest: Boolean
    ) : Sprite(
        -150f, 40f,
        ResourceManager.getInstance().getTexture("menu-button-background")!!.deepCopy()
    ) {
        private var dx = 0f
        private var dy = 0f
        private var avatarTexture: TextureRegion? = null
        private var bannerTexture: TextureRegion? = null
        private val bannerLayer = Entity()
        private var avatarTask: Runnable? = null
        private val avatarExecutor: ExecutorService?
        private val username: String?
        private val scoreID: Int
        private val showOnline: Boolean

        init {
            this.avatarExecutor = avatarExecutor
            this.showOnline = showOnline
            this.username = username
            this.scoreID = scoreID
            val shouldLoadAvatar = isShowOnlineScores && avaURL != null && avatarExecutor != null
            val baseX = if (shouldLoadAvatar) 90 else 0
            var baseY = 0f
            if (isPersonalBest) {
                val topText = Text(getWidth() / 2f, 0f, ResourceManager.getInstance().getFont("strokeFont"), "Personal Best")
                attachChild(topText)
                baseY = topText.height + 5
                topText.setPosition((getWidth() + baseX - topText.width) / 2f, 20f)
                topText.setScale(0.8f)
                setHeight(baseY + 120)
            } else {
                setHeight(107f)
            }
            setScale(0.65f)
            setWidth(724 * 1.1f)
            camY = -146f
            setColor(0f, 0f, 0f)
            setAlpha(0.5f)
            attachChild(bannerLayer)
            val finalBaseY = baseY
            avatarTask = if (shouldLoadAvatar) Runnable {
                var atexture = ResourceManager.getInstance().getTexture("emptyavatar")
                var btexture: TextureRegion? = null
                var avatarLoaded = false
                var bannerLoaded = false
                if (!avatarExecutor!!.isShutdown) {
                    avatarLoaded = OnlineManager.getInstance().loadAvatarToTextureManager(avaURL!!)
                    bannerLoaded = OnlineManager.getInstance().loadBannerToTextureManager(banURL!!)
                }
                if (avatarLoaded || bannerLoaded) {
                    avatarTexture = ResourceManager.getInstance().getAvatarTextureIfLoaded(avaURL!!)
                    bannerTexture = ResourceManager.getInstance().getBannerTextureIfLoaded(banURL!!)
                    if (avatarTexture != null) atexture = avatarTexture
                    if (bannerTexture != null) btexture = bannerTexture
                }
                if (parent == null) {
                    onDetached()
                    return@Runnable
                }
                val finalBtexture = btexture
                val finalAtexture = atexture
                Execution.updateThread {
                    if (parent == null) return@updateThread
                    if (finalBtexture != null) {
                        val w = (getWidth() - 68).toInt()
                        val h = 90
                        if (bannerTexture != null) {
                            val bannerSprite = Sprite(55f, finalBaseY + 12, w.toFloat(), h.toFloat(), finalBtexture.deepCopy())
                            bannerSprite.setColor(0.5f, 0.5f, 0.5f)
                            bannerLayer.attachChild(bannerSprite)
                        }
                    }
                    attachChild(Sprite(55f, finalBaseY + 12, 90f, 90f, finalAtexture))
                }
                if (currentAvatarTask === this@ScoreItem as Runnable) currentAvatarTask = null
            } else null
            val text = Text(baseX + 160f, baseY + 20, ResourceManager.getInstance().getFont("font"), title)
            val accText = Text(670f, baseY + 12, ResourceManager.getInstance().getFont("smallFont"), acc)
            val mark = Sprite(baseX + 80f, baseY + 35, ResourceManager.getInstance().getTexture("ranking-$markStr-small")!!)
            text.setScale(1.2f)
            mark.setScale(1.5f)
            mark.setPosition(baseX + mark.width / 2 + 60, mark.y)
            attachChild(text)
            attachChild(accText)
            attachChild(mark)
            mainScene.registerTouchArea(this)
            height = getHeight()
        }

        override fun onDetached() {
            avatarTexture?.let {
                Execution.updateThread { ResourceManager.getInstance().unloadTexture(it) }
            }
            bannerTexture?.let {
                Execution.updateThread { ResourceManager.getInstance().unloadTexture(it) }
            }
            mainScene.unregisterTouchArea(this)
        }

        override fun onManagedUpdate(pSecondsElapsed: Float) {
            super.onManagedUpdate(pSecondsElapsed)
            if (avatarTask != null && currentAvatarTask == null) {
                val task = avatarTask
                avatarTask = null
                currentAvatarTask = task
                try {
                    avatarExecutor?.submit(task)
                } catch (e: RejectedExecutionException) {
                    if (currentAvatarTask === task) currentAvatarTask = null
                }
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            mScrollDetector.onTouchEvent(event)
            mScrollDetector.isEnabled = true
            if (event.isActionDown) {
                moved = false
                setAlpha(0.8f)
                listener.stopScroll(y + localY)
                dx = localX
                dy = localY
                downTime = 0f
                _scoreID = scoreID
                return true
            } else if (event.isActionUp && !moved && !isScroll) {
                downTime = -1f
                setAlpha(0.5f)
                if (Multiplayer.isMultiplayer) return true
                listener.openScore(scoreID, showOnline, username ?: "")
                GlobalManager.getInstance().scoring?.setReplayID(scoreID)
                return true
            } else if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10) {
                downTime = -1f
                setAlpha(0.5f)
                moved = true
            }
            return false
        }
    }

    companion object {
        @JvmStatic
        fun convertModString(sb: StringBuilder, s: String): String {
            val track = GlobalManager.getInstance().selectedTrack ?: return ""
            var cs = track.circleSize
            var hasLegacySC = false
            sb.setLength(0)
            val mods = s.split("\\|".toRegex(), limit = 2).toTypedArray()
            for (i in mods[0].indices) {
                when (mods[0][i]) {
                    'a' -> sb.append("Auto,")
                    'x' -> sb.append("Relax,")
                    'p' -> sb.append("AP,")
                    'e' -> { sb.append("EZ,"); cs-- }
                    'n' -> sb.append("NF,")
                    'r' -> { sb.append("HR,"); cs++ }
                    'h' -> sb.append("HD,")
                    'i' -> sb.append("FL,")
                    'd' -> sb.append("DT,")
                    'c' -> sb.append("NC,")
                    't' -> sb.append("HT,")
                    's' -> sb.append("PR,")
                    'l' -> { sb.append("REZ,"); cs-- }
                    'm' -> { hasLegacySC = true; cs += 4 }
                    'u' -> sb.append("SD,")
                    'f' -> sb.append("PF,")
                    'b' -> sb.append("SU,")
                    'v' -> sb.append("ScoreV2,")
                }
            }
            if (hasLegacySC) sb.append(String.format("CS%.1f,", cs.toFloat()))
            if (mods.size > 1) convertExtraModString(sb, mods[1])
            if (sb.isEmpty()) return "None"
            return sb.toString().substring(0, sb.length - 1)
        }

        private fun convertExtraModString(sb: StringBuilder, s: String) {
            val split = s.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (str in split) {
                if (str.isEmpty()) continue
                if (str[0] == 'x' && str.length == 5) {
                    sb.append(str.substring(1)).append("x,")
                } else if (str.startsWith("AR") || str.startsWith("OD") || str.startsWith("CS") || str.startsWith("HP")) {
                    sb.append(str).append(',')
                }
            }
        }

        private fun formatScore(sb: StringBuilder, score: Int): String {
            sb.setLength(0)
            sb.append(Math.abs(score.toLong()))
            var i = sb.length - 3
            while (i > 0) {
                sb.insert(i, ' ')
                i -= 3
            }
            return sb.toString()
        }
    }
}

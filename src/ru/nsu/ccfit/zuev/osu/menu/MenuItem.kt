package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import java.lang.ref.WeakReference
import java.util.*
import java.util.regex.Pattern
import ru.nsu.ccfit.zuev.osu.BeatmapInfo
import ru.nsu.ccfit.zuev.osu.BeatmapProperties
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osuplus.R

class MenuItem {
    private val trackSprites: Array<MenuItemTrack?>
    val beatmap: BeatmapInfo
    private val trackDir: String
    private val bgHeight: Int
    private val titleStr: String
    private val creatorStr: String
    @JvmField
    var percentAppeared = 0f
    internal var background: MenuItemBackground? = null
    internal var scene: Scene? = null
    internal var selected = false
    internal var listener: WeakReference<MenuItemListener>? = null
    private var selTrack: MenuItemTrack? = null
    private var visible = true
    private var favorite: Boolean
    private var deleted = false
    private var layer: Entity? = null
    private val trackId: Int

    constructor(listener: MenuItemListener, info: BeatmapInfo) {
        this.listener = WeakReference(listener)
        beatmap = info
        trackDir = ScoreLibrary.getTrackDir(beatmap.path!!)
        bgHeight = ResourceManager.getInstance()
            .getTexture("menu-button-background")!!.height - Utils.toRes(25)
        titleStr = beatmap.artist + " - " + beatmap.title
        creatorStr = StringTable.format(R.string.menu_creator, beatmap.creator)
        trackSprites = arrayOfNulls(info.getCount())
        val props: BeatmapProperties? = PropertiesLibrary.instance.getProperties(info.path!!)
        favorite = props != null && props.isFavorite()
        trackId = -1
    }

    constructor(listener: MenuItemListener, info: BeatmapInfo, id: Int) {
        this.listener = WeakReference(listener)
        beatmap = info
        trackDir = ScoreLibrary.getTrackDir(beatmap.path!!)
        bgHeight = ResourceManager.getInstance()
            .getTexture("menu-button-background")!!.height - Utils.toRes(25)
        titleStr = beatmap.artist + " - " + beatmap.title
        creatorStr = StringTable.format(R.string.menu_creator, beatmap.creator)
        trackSprites = arrayOfNulls(1)
        trackId = id
        val props: BeatmapProperties? = PropertiesLibrary.instance.getProperties(info.path!!)
        favorite = props != null && props.isFavorite()
    }

    fun getBeatmap(): BeatmapInfo = beatmap

    fun updateMarks() {
        for (tr in trackSprites) {
            tr?.updateMark()
        }
    }

    fun attachToScene(scene: Scene, layer: Entity) {
        this.scene = scene
        this.layer = layer
    }

    fun getHeight(): Float {
        if (!visible) return 0f
        return if (selected) {
            bgHeight + percentAppeared * bgHeight * (trackSprites.size - 1)
        } else {
            bgHeight - Utils.toRes(5).toFloat()
        }
    }

    fun getInitialHeight(): Float {
        if (!visible) return 0f
        return (bgHeight - Utils.toRes(5)).toFloat()
    }

    fun getTotalHeight(): Float = (bgHeight * trackSprites.size).toFloat()

    fun setPos(x: Float, y: Float) {
        if (background != null) {
            background!!.setPosition(x, y)
            if (y > Config.getRES_HEIGHT() || y < -background!!.height) {
                freeBackground()
            }
        }
        if (!selected) {
            if (visible && background == null && y < Config.getRES_HEIGHT() && y > -bgHeight) {
                initBackground()
                background!!.setPosition(x, y)
            }
            return
        }
        var oy = 0f
        for (s in trackSprites) {
            if (s == null) continue
            val cy = y + oy + Config.getRES_HEIGHT() / 2f + s.height / 2
            val ox = x + Utils.toRes((170 * Math.abs(Math.cos(cy * Math.PI / (Config.getRES_HEIGHT() * 2)))).toFloat())
            s.setPosition(ox - Utils.toRes(100), y + oy)
            oy += (s.height - Utils.toRes(25)) * percentAppeared
        }
    }

    fun select(reloadMusic: Boolean, reloadBG: Boolean) {
        val listener = this.listener?.get() ?: return
        if (!listener.isSelectAllowed() || scene == null) return
        freeBackground()
        selected = true
        listener.select(this)
        initTracks()
        percentAppeared = 0f
        val musicFileName = beatmap.getMusic()
        if (reloadMusic) {
            listener.playMusic(musicFileName!!, beatmap.getPreviewTime())
        }
        selectTrack(trackSprites[0]!!, reloadBG)
        trackSprites[0]!!.setSelectedColor()
    }

    fun deselect() {
        if (scene == null) return
        if (deleted) return
        initBackground()
        selected = false
        percentAppeared = 0f
        deselectTrack()
        freeTracks()
    }

    fun deselectTrack() {
        if (scene == null) return
        selTrack?.setDeselectColor()
        selTrack = null
    }

    fun applyFilter(filter: String, favs: Boolean, limit: Set<String>?) {
        if ((favs && !isFavorite()) || (limit != null && !limit.contains(trackDir))) {
            if (selected) deselect()
            freeBackground()
            visible = false
            return
        }
        val builder = StringBuilder()
        builder.append(beatmap.title).append(' ').append(beatmap.artist).append(' ')
            .append(beatmap.creator).append(' ').append(beatmap.tags).append(' ')
            .append(beatmap.source).append(' ').append(beatmap.getTracks()[0].beatmapSetID)
        for (track in beatmap.getTracks()) {
            builder.append(' ').append(track.mode)
        }
        var canVisible = true
        val lowerText = builder.toString().lowercase()
        val lowerFilterTexts = filter.lowercase().split(" ").toTypedArray()
        for (filterText in lowerFilterTexts) {
            val pattern = Pattern.compile("(ar|od|cs|hp|star)(=|<|>|<=|>=)(\\d+)")
            val matcher = pattern.matcher(filterText)
            if (matcher.find()) {
                val key = matcher.group(1)
                val opt = matcher.group(2)
                val value = matcher.group(3)
                var vis = false
                if (trackId < 0) {
                    for (track in beatmap.getTracks()) {
                        if (key != null) {
                            vis = vis || visibleTrack(track, key, opt!!, value!!)
                        }
                    }
                } else {
                    if (key != null) {
                        vis = visibleTrack(beatmap.getTrack(trackId), key, opt!!, value!!)
                    }
                }
                canVisible = canVisible && vis
            } else {
                if (!lowerText.contains(filterText)) {
                    canVisible = false
                    break
                }
            }
        }
        if (filter == "") canVisible = true
        if (canVisible) {
            if (!visible) {
                visible = true
                selected = false
                percentAppeared = 0f
            }
            return
        }
        if (selected) deselect()
        freeBackground()
        visible = false
    }

    private fun visibleTrack(track: TrackInfo, key: String, opt: String, value: String): Boolean {
        return when (key) {
            "ar" -> calOpt(track.approachRate, value.toFloat(), opt)
            "od" -> calOpt(track.overallDifficulty, value.toFloat(), opt)
            "cs" -> calOpt(track.circleSize, value.toFloat(), opt)
            "hp" -> calOpt(track.hpDrain, value.toFloat(), opt)
            "star" -> calOpt(track.difficulty, value.toFloat(), opt)
            else -> false
        }
    }

    private fun calOpt(val1: Float, val2: Float, opt: String): Boolean {
        return when (opt) {
            "=" -> val1 == val2
            "<" -> val1 < val2
            ">" -> val1 > val2
            "<=" -> val1 <= val2
            ">=" -> val1 >= val2
            else -> false
        }
    }

    fun delete() {
        if (selected) deselect()
        freeBackground()
        visible = false
        deleted = true
        LibraryManager.INSTANCE.deleteMap(beatmap)
    }

    fun isVisible(): Boolean = visible && !deleted

    fun isDeleted(): Boolean = deleted

    fun stopScroll(y: Float) {
        listener?.get()?.stopScroll(y)
    }

    fun selectTrack(track: MenuItemTrack, reloadBG: Boolean) {
        selTrack = track
        listener?.get()?.selectTrack(track.getTrack()!!, reloadBG)
    }

    fun isTrackSelected(track: MenuItemTrack): Boolean = selTrack === track

    private fun freeBackground() {
        if (background == null) return
        background!!.setVisible(false)
        SongMenuPool.getInstance().putBackground(background!!)
        background = null
    }

    @Synchronized
    private fun initBackground() {
        if (background == null) {
            background = SongMenuPool.getInstance().newBackground()
        }
        background!!.setItem(this)
        background!!.setTitle(titleStr)
        background!!.setAuthor(creatorStr)
        background!!.setVisible(true)
        if (!background!!.hasParent()) {
            layer!!.attachChild(background)
            scene!!.registerTouchArea(background)
        }
    }

    private fun freeTracks() {
        for (i in trackSprites.indices) {
            trackSprites[i]!!.setVisible(false)
            scene!!.unregisterTouchArea(trackSprites[i])
            trackSprites[i]!!.setVisible(false)
            SongMenuPool.getInstance().putTrack(trackSprites[i]!!)
            trackSprites[i] = null
        }
    }

    private fun initTracks() {
        if (trackId == -1) {
            for (i in trackSprites.indices) {
                trackSprites[i] = SongMenuPool.getInstance().newTrack()
                trackSprites[i]!!.setItem(this)
                trackSprites[i]!!.setTrack(beatmap.getTrack(i), beatmap)
                beatmap.getTrack(i).setBeatmap(beatmap)
                if (!trackSprites[i]!!.hasParent()) {
                    layer!!.attachChild(trackSprites[i])
                }
                scene!!.registerTouchArea(trackSprites[i])
                trackSprites[i]!!.setVisible(true)
            }
        } else {
            trackSprites[0] = SongMenuPool.getInstance().newTrack()
            trackSprites[0]!!.setItem(this)
            trackSprites[0]!!.setTrack(beatmap.getTrack(trackId), beatmap)
            beatmap.getTrack(trackId).setBeatmap(beatmap)
            if (!trackSprites[0]!!.hasParent()) {
                layer!!.attachChild(trackSprites[0])
            }
            scene!!.registerTouchArea(trackSprites[0])
            trackSprites[0]!!.setVisible(true)
        }
    }

    fun isFavorite(): Boolean = favorite

    fun setFavorite(favorite: Boolean) {
        this.favorite = favorite
    }

    fun showPropertiesMenu() {
        listener?.get()?.showPropertiesMenu(this)
    }

    fun update(dt: Float) {
        if (deleted) return
        for (tr in trackSprites) {
            tr?.update(dt)
        }
    }

    fun getFirstTrack(): TrackInfo = beatmap.getTrack(maxOf(trackId, 0))

    fun removeFromScene() {
        if (scene == null) return
        if (selected) deselect()
        freeBackground()
        visible = false
        scene = null
    }

    fun tryGetCorrespondingTrackId(oldTrackFileName: String): Int {
        if (trackId <= -1) {
            var i = 0
            for (track in beatmap.getTracks()) {
                if (track == null) continue
                if (track.filename == oldTrackFileName) return i
                i++
            }
        } else if (beatmap.getTrack(trackId).filename == oldTrackFileName) {
            return trackId
        }
        return -1
    }

    fun getTrackSpritesById(index: Int): MenuItemTrack? {
        if (index < 0 || index >= trackSprites.size) return null
        return trackSprites[index]
    }
}

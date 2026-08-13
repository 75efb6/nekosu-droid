package ru.nsu.ccfit.zuev.osu.storyboard

import android.opengl.GLES10
import android.util.Log
import com.dgsrz.bancho.ui.StoryBoardTestActivity
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.sprite.AnimatedSprite
import org.anddev.andengine.entity.sprite.BaseSprite
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory
import org.anddev.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureBuilder
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.EaseQuadIn
import org.anddev.andengine.util.modifier.ease.EaseQuadOut
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.io.File

class OsuSprite {
    var spriteStartTime: Long = 0
    var spriteEndTime: Long = 0
    private var fileName: String? = null
    private var debugLine: String? = null
    private var layer = 0
    private var zIndex = 0
    private var origin: Origin? = null
    private var sprite: BaseSprite? = null
    private var eventList: ArrayList<OsuEvent>? = null
    private var textureRegion: TextureRegion? = null
    private val activity = StoryBoardTestActivity.activity
    private var isValid = false
    private var parallelEntityModifier: ParallelEntityModifier? = null
    private var isAnimation = false
    private var anchorCenterX = 0f
    private var anchorCenterY = 0f

    constructor(
        x: Float, y: Float, layer: Int, origin: Origin, filePath: String,
        eventList: ArrayList<OsuEvent>, zIndex: Int
    ) {
        this.fileName = filePath.replace("\"", "").replace("\\\\", "/")
        textureRegion = ResourceManager.getInstance().getTexture(File(StoryBoardTestActivity.FOLDER, fileName).path)
        if (textureRegion == null) {
            isValid = false
        } else {
            isValid = true
            sprite = Sprite(x, y, textureRegion)
            this.layer = layer
            this.origin = origin
            this.eventList = eventList
            if (filePath == activity?.mBackground) {
                activity?.mBackground = null
            }
            if (eventList.isEmpty()) {
                isValid = false
                return
            }
            this.zIndex = zIndex
            setUpSprite()
        }
    }

    constructor(
        x: Float, y: Float, layer: Int, origin: Origin, filePath: String,
        eventList: ArrayList<OsuEvent>, zIndex: Int, count: Int, delay: Int, loopType: String
    ) {
        isAnimation = true
        val processedPath = filePath.replace("\"", "").replace("\\\\", "/")
        this.fileName = processedPath.substring(processedPath.lastIndexOf("/") + 1, processedPath.lastIndexOf("."))
        val fileExt = processedPath.substring(processedPath.lastIndexOf("."))
        val cSource = FileBitmapTextureAtlasSource(
            File(StoryBoardTestActivity.FOLDER, processedPath.substring(0, processedPath.lastIndexOf(".")) + "0" + fileExt)
        )
        var tw = 16
        var th = 16
        val width = cSource.width * count
        val height = cSource.height
        while (tw < width) { tw *= 2 }
        while (th < height) { th *= 2 }
        val mBitmapTextureAtlas = BuildableBitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)
        val textureRegions = ArrayList<TextureRegion>()
        for (i in 0 until count) {
            val temp = File(StoryBoardTestActivity.FOLDER, processedPath.substring(0, processedPath.lastIndexOf(".")) + i + fileExt)
            if (temp.exists()) {
                val cSource2 = FileBitmapTextureAtlasSource(temp)
                val iTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromSource(mBitmapTextureAtlas, cSource2)
                textureRegions.add(iTextureRegion)
            } else {
                break
            }
        }
        if (textureRegions.size > 0) {
            isValid = true
        } else {
            isValid = false
            return
        }
        try {
            mBitmapTextureAtlas.build(BlackPawnTextureBuilder(0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ResourceManager.getInstance().engine?.textureManager?.loadTexture(mBitmapTextureAtlas)

        isValid = true
        val tiledTextureRegion = TiledTextureRegion(mBitmapTextureAtlas, 0, 0, width, height, count, 1)
        val animatedSprite = AnimatedSprite(x, y, tiledTextureRegion)
        animatedSprite.animate(delay.toLong(), loopType == "LoopForever")
        this.sprite = animatedSprite
        this.layer = layer
        this.origin = origin
        this.eventList = eventList
        if (eventList.isEmpty()) {
            isValid = false
            return
        }
        this.zIndex = zIndex
        setUpSprite()
    }

    private fun setUpSprite() {
        for (osuEvent in eventList!!) {
            if (osuEvent.startTime + 1 >= osuEvent.endTime && osuEvent.command != Command.F) {
                continue
            }
            spriteStartTime = osuEvent.startTime
            break
        }

        for (osuEvent in eventList!!) {
            if (osuEvent.startTime + 1 >= osuEvent.endTime && osuEvent.command != Command.F) {
                continue
            }
            if (spriteStartTime > osuEvent.startTime) {
                spriteStartTime = osuEvent.startTime
            }
        }
        for (firstEvent in eventList!!) {
            if (firstEvent.startTime + 1 == firstEvent.endTime && firstEvent.command != Command.F) {
                firstEvent.startTime = spriteStartTime
                break
            }
        }
        spriteEndTime = eventList!![eventList!!.size - 1].endTime
        sprite?.isVisible = false
        sprite?.setZIndex(zIndex)
        val x = sprite!!.x
        val y = sprite!!.y

        when (origin) {
            Origin.TopLeft -> {
                sprite!!.setScaleCenter(0f, 0f)
                sprite!!.setRotationCenter(0f, 0f)
                sprite!!.setPosition(x, y)
                anchorCenterX = 0f
                anchorCenterY = 0f
            }
            Origin.TopCentre -> {
                sprite!!.setScaleCenter(sprite!!.width / 2f, 0f)
                sprite!!.setRotationCenter(sprite!!.width / 2f, 0f)
                sprite!!.setPosition(x - sprite!!.width / 2f, y)
                anchorCenterX = sprite!!.width / 2f
                anchorCenterY = 0f
            }
            Origin.TopRight -> {
                sprite!!.setScaleCenter(sprite!!.width, 0f)
                sprite!!.setRotationCenter(sprite!!.width, 0f)
                sprite!!.setPosition(x - sprite!!.width, y)
                anchorCenterX = sprite!!.width
                anchorCenterY = 0f
            }
            Origin.CentreLeft -> {
                sprite!!.setScaleCenter(0f, sprite!!.height / 2f)
                sprite!!.setRotationCenter(0f, sprite!!.height / 2f)
                sprite!!.setPosition(x, y - sprite!!.height / 2f)
                anchorCenterX = 0f
                anchorCenterY = sprite!!.height / 2f
            }
            Origin.Centre -> {
                sprite!!.setScaleCenter(sprite!!.width / 2f, sprite!!.height / 2f)
                sprite!!.setRotationCenter(sprite!!.width / 2f, sprite!!.height / 2f)
                sprite!!.setPosition(x - sprite!!.width / 2f, y - sprite!!.height / 2f)
                anchorCenterX = sprite!!.width / 2f
                anchorCenterY = sprite!!.height / 2f
            }
            Origin.CentreRight -> {
                sprite!!.setScaleCenter(sprite!!.width, sprite!!.height / 2f)
                sprite!!.setRotationCenter(sprite!!.width, sprite!!.height / 2f)
                sprite!!.setPosition(x - sprite!!.width, y - sprite!!.height / 2f)
                anchorCenterX = sprite!!.width
                anchorCenterY = sprite!!.height / 2f
            }
            Origin.BottomLeft -> {
                sprite!!.setScaleCenter(0f, sprite!!.height)
                sprite!!.setRotationCenter(0f, sprite!!.height)
                sprite!!.setPosition(x, y - sprite!!.height)
                anchorCenterX = 0f
                anchorCenterY = sprite!!.height
            }
            Origin.BottomCentre -> {
                sprite!!.setScaleCenter(sprite!!.width / 2f, sprite!!.height)
                sprite!!.setRotationCenter(sprite!!.width / 2f, sprite!!.height)
                sprite!!.setPosition(x - sprite!!.width / 2f, y - sprite!!.height)
                anchorCenterX = sprite!!.width / 2f
                anchorCenterY = sprite!!.height
            }
            Origin.BottomRight -> {
                sprite!!.setScaleCenter(sprite!!.width, sprite!!.height)
                sprite!!.setRotationCenter(sprite!!.width, sprite!!.height)
                sprite!!.setPosition(x - sprite!!.width, y - sprite!!.height)
                anchorCenterX = sprite!!.width
                anchorCenterY = sprite!!.height
            }
            else -> {}
        }
        val entityModifiers = arrayOfNulls<IEntityModifier>(eventList!!.size)
        for (i in eventList!!.indices) {
            val osuEvent = eventList!![i]
            if (osuEvent.startTime == spriteStartTime) {
                entityModifiers[i] = parseModifier(osuEvent)
            } else {
                entityModifiers[i] = SequenceEntityModifier(
                    DelayModifier((osuEvent.startTime - spriteStartTime) / 1000f),
                    parseModifier(osuEvent)
                )
            }
        }
        parallelEntityModifier = ParallelEntityModifier(*entityModifiers)
        parallelEntityModifier!!.addModifierListener(object : IModifier.IModifierListener<org.anddev.andengine.entity.IEntity> {
            override fun onModifierStarted(pModifier: IModifier<org.anddev.andengine.entity.IEntity>, pItem: org.anddev.andengine.entity.IEntity) {
                sprite!!.isVisible = true
            }

            override fun onModifierFinished(pModifier: IModifier<org.anddev.andengine.entity.IEntity>, pItem: org.anddev.andengine.entity.IEntity) {
                sprite!!.isVisible = false
                sprite!!.isIgnoreUpdate = true

                val total = StoryBoardTestActivity.activity?.onScreenDrawCalls?.decrementAndGet()
                Log.i("draw calls", "(detach) total draw calls: $total")
            }
        })
    }

    fun play() {
        if (isValid) {
            sprite!!.registerEntityModifier(parallelEntityModifier)
            if (!sprite!!.hasParent()) {
                val s = sprite!!
                when (layer) {
                    LAYER_BACKGROUND -> StoryBoardTestActivity.activity?.attachBackground(s)
                    LAYER_FOREGROUND -> StoryBoardTestActivity.activity?.attachForeground(s)
                    LAYER_PASS -> StoryBoardTestActivity.activity?.attachPass(s)
                    LAYER_FAIL -> {}
                }
            }
            val total = StoryBoardTestActivity.activity?.onScreenDrawCalls?.incrementAndGet()
            Log.i("draw calls", "total draw calls: $total")
        }
    }

    fun getDebugLine(): String? = debugLine

    fun setDebugLine(debugLine: String?) {
        this.debugLine = debugLine
    }

    private fun parseModifier(osuEvent: OsuEvent): IEntityModifier {
        var iEntityModifier: IEntityModifier? = null
        val ease = osuEvent.ease
        var iEaseFunction: org.anddev.andengine.util.modifier.ease.IEaseFunction? = null
        when (ease) {
            1 -> iEaseFunction = EaseQuadOut.getInstance()
            2 -> iEaseFunction = EaseQuadIn.getInstance()
        }
        val duration = (osuEvent.endTime - osuEvent.startTime) / 1000f
        when (osuEvent.command) {
            Command.F -> {
                iEntityModifier = if (iEaseFunction != null) {
                    AlphaModifier(duration, osuEvent.params!![0], osuEvent.params!![1], iEaseFunction)
                } else {
                    AlphaModifier(duration, osuEvent.params!![0], osuEvent.params!![1])
                }
            }
            Command.M -> {
                iEntityModifier = if (iEaseFunction != null) {
                    MoveModifier(duration, osuEvent.params!![0] - anchorCenterX, osuEvent.params!![2] - anchorCenterX, osuEvent.params!![1] - anchorCenterY, osuEvent.params!![3] - anchorCenterY, iEaseFunction)
                } else {
                    MoveModifier(duration, osuEvent.params!![0] - anchorCenterX, osuEvent.params!![2] - anchorCenterX, osuEvent.params!![1] - anchorCenterY, osuEvent.params!![3] - anchorCenterY)
                }
            }
            Command.MX -> {
                iEntityModifier = if (iEaseFunction != null) {
                    MoveXModifier(duration, osuEvent.params!![0] - anchorCenterX, osuEvent.params!![1] - anchorCenterX, iEaseFunction)
                } else {
                    MoveXModifier(duration, osuEvent.params!![0] - anchorCenterX, osuEvent.params!![1] - anchorCenterX)
                }
            }
            Command.MY -> {
                iEntityModifier = if (iEaseFunction != null) {
                    MoveYModifier(duration, osuEvent.params!![0] - anchorCenterY, osuEvent.params!![1] - anchorCenterY, iEaseFunction)
                } else {
                    MoveYModifier(duration, osuEvent.params!![0] - anchorCenterY, osuEvent.params!![1] - anchorCenterY)
                }
            }
            Command.S -> {
                iEntityModifier = if (iEaseFunction != null) {
                    ScaleModifier(duration, osuEvent.params!![0], osuEvent.params!![1], iEaseFunction)
                } else {
                    ScaleModifier(duration, osuEvent.params!![0], osuEvent.params!![1])
                }
            }
            Command.V -> {
                iEntityModifier = if (iEaseFunction != null) {
                    ScaleModifier(duration, osuEvent.params!![0], osuEvent.params!![2], osuEvent.params!![1], osuEvent.params!![3], iEaseFunction)
                } else {
                    ScaleModifier(duration, osuEvent.params!![0], osuEvent.params!![2], osuEvent.params!![1], osuEvent.params!![3])
                }
            }
            Command.R -> {
                iEntityModifier = if (iEaseFunction != null) {
                    RotationModifier(duration, osuEvent.params!![0] * TO_DEGREES, osuEvent.params!![1] * TO_DEGREES, iEaseFunction)
                } else {
                    RotationModifier(duration, osuEvent.params!![0] * TO_DEGREES, osuEvent.params!![1] * TO_DEGREES)
                }
            }
            Command.C -> {
                iEntityModifier = if (iEaseFunction != null) {
                    ColorModifier(duration, osuEvent.params!![0] / 255f, osuEvent.params!![1] / 255f, osuEvent.params!![2] / 255f,
                        osuEvent.params!![3] / 255f, osuEvent.params!![4] / 255f, osuEvent.params!![5] / 255f, iEaseFunction)
                } else {
                    ColorModifier(duration, osuEvent.params!![0] / 255f, osuEvent.params!![1] / 255f, osuEvent.params!![2] / 255f,
                        osuEvent.params!![3] / 255f, osuEvent.params!![4] / 255f, osuEvent.params!![5] / 255f)
                }
            }
            Command.P -> {
                iEntityModifier = DelayModifier(0f)
                when (osuEvent.P) {
                    "H" -> sprite!!.textureRegion.setFlippedHorizontal(true)
                    "V" -> sprite!!.textureRegion.setFlippedVertical(true)
                    "A" -> sprite!!.setBlendFunction(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE)
                }
            }
            Command.L -> {
                val subEventList = osuEvent.subEvents
                val subEntityModifiers = arrayOfNulls<IEntityModifier>(subEventList!!.size)
                var firstSubTime = 0f
                if (subEventList.size > 0) {
                    firstSubTime = subEventList[0].startTime.toFloat()
                }
                for (i in subEventList.indices) {
                    val subOsuEvent = subEventList[i]
                    if (subOsuEvent.startTime == 0L) {
                        subEntityModifiers[i] = parseModifier(subOsuEvent)
                    } else {
                        subEntityModifiers[i] = SequenceEntityModifier(
                            DelayModifier((subOsuEvent.startTime - firstSubTime) / 1000f),
                            parseModifier(subOsuEvent)
                        )
                    }
                }
                iEntityModifier = LoopEntityModifier(ParallelEntityModifier(*subEntityModifiers), osuEvent.loopCount)
            }
            Command.T -> {
                val subEventList = osuEvent.subEvents
                val hitSounds = OsbParser.instance.getHitSounds()
                val entityModifierList = ArrayList<IEntityModifier>()
                val soundType = when (osuEvent.triggerType) {
                    "HitSoundWhistle" -> 2
                    "HitSoundFinish" -> 4
                    "HitSoundClap" -> 8
                    else -> return@parseModifier DelayModifier(0f)
                }
                var firstSoundTime: Long = -1
                for (hitSound in hitSounds) {
                    if (hitSound.time >= osuEvent.startTime && hitSound.time <= osuEvent.endTime && hitSound.soundType and soundType == soundType) {
                        if (firstSoundTime < 0) {
                            firstSoundTime = hitSound.time
                        }
                        val subEntityModifiers = arrayOfNulls<IEntityModifier>(subEventList!!.size)
                        var firstSubTime: Long = 0
                        if (subEventList.size > 0) {
                            firstSubTime = subEventList[0].startTime
                        }
                        for (i in subEventList.indices) {
                            val subOsuEvent = subEventList[i]
                            if (subOsuEvent.startTime == 0L) {
                                subEntityModifiers[i] = parseModifier(subOsuEvent)
                            } else {
                                subEntityModifiers[i] = SequenceEntityModifier(
                                    DelayModifier((subOsuEvent.startTime - firstSubTime) / 1000f),
                                    parseModifier(subOsuEvent)
                                )
                            }
                        }
                        if (firstSoundTime == hitSound.time) {
                            entityModifierList.add(ParallelEntityModifier(*subEntityModifiers))
                        } else {
                            entityModifierList.add(SequenceEntityModifier(
                                DelayModifier((hitSound.time - firstSoundTime) / 1000f),
                                ParallelEntityModifier(*subEntityModifiers)
                            ))
                        }
                    }
                    if (hitSound.time > osuEvent.endTime) {
                        break
                    }
                }
                if (entityModifierList.isNotEmpty()) {
                    iEntityModifier = ParallelEntityModifier(*entityModifierList.toTypedArray())
                }
            }
            Command.NONE -> {}
            else -> {}
        }
        if (iEntityModifier == null) {
            iEntityModifier = DelayModifier(0f)
        }
        return iEntityModifier
    }

    enum class Origin {
        TopLeft, TopCentre, TopRight, CentreLeft, Centre, CentreRight, BottomLeft, BottomCentre, BottomRight, NONE;

        companion object {
            @JvmStatic
            fun getType(type: String): Origin {
                return try {
                    valueOf(type.uppercase())
                } catch (e: Exception) {
                    NONE
                }
            }
        }
    }

    companion object {
        const val LAYER_BACKGROUND = 0
        const val LAYER_FAIL = 1
        const val LAYER_PASS = 2
        const val LAYER_FOREGROUND = 3
        @JvmField
        var TO_RADIANS = (1 / 180.0f) * Math.PI.toFloat()
        @JvmField
        var TO_DEGREES = (1 / Math.PI.toFloat()) * 180
    }
}

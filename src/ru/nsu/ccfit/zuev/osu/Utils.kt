package ru.nsu.ccfit.zuev.osu

import android.content.Context
import android.graphics.PointF
import android.net.ConnectivityManager
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener

object Utils {
    private val FSReservedChars = "|\\?*<\":>+[]/"
    private var accSign = 0
    private var soundMask = 0

    @JvmStatic
    fun <T> oneObjectArray(`object`: T, ary: Array<T>): Array<T> {
        for (i in ary.indices) {
            ary[i] = `object`
        }
        return ary
    }

    @JvmStatic
    fun setAccelerometerSign(sign: Int) {
        accSign = sign
    }

    @JvmStatic
    fun getAccselerometerSign(): Int = accSign

    @JvmStatic
    fun sqr(x: Float): Float = x * x

    @JvmStatic
    fun inter(v1: PointF, v2: PointF, percent: Float): PointF {
        return PointF(
            v1.x * (1 - percent) + v2.x * percent,
            v1.y * (1 - percent) + v2.y * percent
        )
    }

    @JvmStatic
    fun putSpriteAnchorCenter(x: Float, y: Float, sprite: Sprite) {
        val tex: TextureRegion = sprite.textureRegion
        sprite.setPosition(x - tex.width / 2f, y - tex.height / 2f)
    }

    @JvmStatic
    fun putSpriteAnchorCenter(pos: PointF, sprite: Sprite) {
        val tex: TextureRegion = sprite.textureRegion
        sprite.setPosition(pos.x - tex.width / 2f, pos.y - tex.height / 2f)
    }

    @JvmStatic
    fun trackToRealCoords(coords: PointF): PointF {
        val pos = scaleToReal(coords)
        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT.toFloat())) / 2f
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH.toFloat())) / 2f
        if (GameHelper.isHardrock()) {
            pos.y -= Config.getRES_HEIGHT() / 2.toFloat()
            pos.y *= -1
            pos.y += Config.getRES_HEIGHT() / 2.toFloat()
        }
        return pos
    }

    @JvmStatic
    fun changeTrackToRealCoords(coords: PointF) {
        val pos = scaleToRealC(coords)
        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT.toFloat())) / 2f
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH.toFloat())) / 2f
        if (GameHelper.isHardrock()) {
            pos.y -= Config.getRES_HEIGHT() / 2.toFloat()
            pos.y *= -1
            pos.y += Config.getRES_HEIGHT() / 2.toFloat()
        }
    }

    @JvmStatic
    fun realToTrackCoords(coords: PointF): PointF {
        return realToTrackCoords(coords, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat(), false)
    }

    @JvmStatic
    fun realToTrackCoords(coords: PointF, width: Float, height: Float, isOld: Boolean): PointF {
        val pos = PointF(coords.x, coords.y)
        if (GameHelper.isHardrock()) {
            pos.y -= height / 2
            pos.y *= -1
            pos.y += height / 2
        }
        pos.y -= (height - toRes((if (isOld) Constants.MAP_ACTUAL_HEIGHT_OLD else Constants.MAP_ACTUAL_HEIGHT).toFloat())) / 2f
        pos.x -= (width - toRes((if (isOld) Constants.MAP_ACTUAL_WIDTH_OLD else Constants.MAP_ACTUAL_WIDTH).toFloat())) / 2f
        return scaleToTrack(pos, isOld)
    }

    @JvmStatic
    fun flipY(y: Short): Short {
        val height = Config.getRES_HEIGHT() / 2
        return ((y - height) * -1 + height).toShort()
    }

    @JvmStatic
    fun scaleToReal(v: PointF): PointF {
        val pos = PointF(v.x, v.y)
        pos.x *= toRes(Constants.MAP_ACTUAL_WIDTH.toFloat()) / Constants.MAP_WIDTH.toFloat()
        pos.y *= toRes(Constants.MAP_ACTUAL_HEIGHT.toFloat()) / Constants.MAP_HEIGHT.toFloat()
        return pos
    }

    @JvmStatic
    fun scaleToRealC(v: PointF): PointF {
        v.x *= toRes(Constants.MAP_ACTUAL_WIDTH.toFloat()) / Constants.MAP_WIDTH.toFloat()
        v.y *= toRes(Constants.MAP_ACTUAL_HEIGHT.toFloat()) / Constants.MAP_HEIGHT.toFloat()
        return v
    }

    @JvmStatic
    fun scaleToTrack(v: PointF, isOld: Boolean): PointF {
        val pos = PointF(v.x, v.y)
        pos.x *= Constants.MAP_WIDTH.toFloat() / toRes((if (isOld) Constants.MAP_ACTUAL_WIDTH_OLD else Constants.MAP_ACTUAL_WIDTH).toFloat())
        pos.y *= Constants.MAP_HEIGHT.toFloat() / toRes((if (isOld) Constants.MAP_ACTUAL_HEIGHT_OLD else Constants.MAP_ACTUAL_HEIGHT).toFloat())
        return pos
    }

    @JvmStatic
    fun direction(x: Float, y: Float): Float {
        var len = Math.sqrt((x * x + y * y).toDouble()).toFloat()
        if (Math.abs(len) < 0.0001f) {
            return 0f
        }
        len = if (x > 0) {
            Math.asin((y / len).toDouble()).toFloat()
        } else {
            (Math.PI - Math.asin((y / len).toDouble())).toFloat()
        }
        return len
    }

    @JvmStatic
    fun direction(vector: PointF): Float {
        return direction(vector.x, vector.y)
    }

    @JvmStatic
    fun direction(v1: PointF, v2: PointF): Float {
        return direction(v2.x - v1.x, v2.y - v1.y)
    }

    @JvmStatic
    fun length(vector: PointF): Float {
        return Math.sqrt((vector.x * vector.x + vector.y * vector.y).toDouble()).toFloat()
    }

    @JvmStatic
    fun normalize(vector: PointF): PointF {
        val len = length(vector)
        if (Math.abs(len) < 0.0001f) {
            return PointF(0f, 0f)
        }
        return PointF(vector.x / len, vector.y / len)
    }

    @JvmStatic
    fun distance(v1: PointF, v2: PointF): Float {
        return MathUtils.distance(v1.x, v1.y, v2.x, v2.y)
    }

    @JvmStatic
    fun squaredDistance(v1: PointF, v2: PointF): Float {
        return (v2.x - v1.x) * (v2.x - v1.x) + (v2.y - v1.y) * (v2.y - v1.y)
    }

    @JvmStatic
    fun squaredDistance(v1x: Float, v1y: Float, v2x: Float, v2y: Float): Float {
        return (v2x - v1x) * (v2x - v1x) + (v2y - v1y) * (v2y - v1y)
    }

    @JvmStatic
    fun clearSoundMask() {
        soundMask = 0
    }

    @JvmStatic
    fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    @JvmStatic
    fun playHitSound(listener: GameObjectListener, soundId: Int) {
        playHitSound(listener, soundId, 0, 0)
    }

    @JvmStatic
    fun playHitSound(listener: GameObjectListener, soundId: Int, sampleSet: Int, addition: Int) {
        if (soundId and 32 > 0) {
            return
        }

        if (soundId and 16 > 0 && soundMask and 16 == 0) {
            soundMask = soundMask or 16
            listener.playSound("slidertick", sampleSet, addition)
            return
        }

        if (soundMask and 1 == 0) {
            soundMask = soundMask or 1
            listener.playSound("hitnormal", sampleSet, addition)
        }
        if (soundId and 2 > 0 && soundMask and 2 == 0) {
            soundMask = soundMask or 2
            listener.playSound("hitwhistle", sampleSet, addition)
        }
        if (soundId and 4 > 0 && soundMask and 4 == 0) {
            soundMask = soundMask or 4
            listener.playSound("hitfinish", sampleSet, addition)
        }
        if (soundId and 8 > 0 && soundMask and 8 == 0) {
            soundMask = soundMask or 8
            listener.playSound("hitclap", sampleSet, addition)
        }
    }

    @JvmStatic
    fun toRes(i: Int): Int = i / Config.getTextureQuality()

    @JvmStatic
    fun toRes(i: Float): Float = i / Config.getTextureQuality()

    @JvmStatic
    fun toFSValidString(s: String): String {
        val nameBuilder = StringBuilder()
        for (i in s.indices) {
            if (FSReservedChars.indexOf(s[i]) == -1) {
                nameBuilder.append(s[i])
            }
        }
        return nameBuilder.toString()
    }

    @JvmStatic
    fun isWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetInfo = connectivityManager.activeNetworkInfo
        return activeNetInfo != null && activeNetInfo.type == ConnectivityManager.TYPE_WIFI
    }

    @JvmStatic
    fun tryParseFloat(str: String, defaultVal: Float): Float {
        return try {
            str.toFloat()
        } catch (ignored: NumberFormatException) {
            defaultVal
        }
    }

    @JvmStatic
    fun tryParseInt(str: String, defaultVal: Int): Int {
        return try {
            str.toInt()
        } catch (ignored: NumberFormatException) {
            defaultVal
        }
    }

    @JvmStatic
    fun tryParseDouble(str: String, defaultVal: Double): Double {
        return try {
            str.toDouble()
        } catch (ignored: NumberFormatException) {
            defaultVal
        }
    }
}

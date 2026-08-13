package ru.nsu.ccfit.zuev.osu.scoring

import android.graphics.PointF
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.*
import java.util.BitSet
import java.util.EnumSet
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Replay {
    var pointsSkipped = 0
    @JvmField
    val cursorMoves: ArrayList<MoveArray> = ArrayList()
    lateinit var cursorIndex: IntArray
    lateinit var lastMoveIndex: IntArray
    @JvmField
    var objectData: Array<ReplayObjectData?>? = null
    @JvmField
    var replayVersion = 0
    @JvmField
    var stat: StatisticV2? = null
    internal var md5 = ""
    internal var mapFile = ""
    private var mapName = ""
    private var isSaving = false

    init {
        cursorMoves.add(MoveArray(200))
        cursorMoves.add(MoveArray(50))
        for (i in 2 until GameScene.CursorCount) {
            cursorMoves.add(MoveArray(15))
        }
        cursorIndex = IntArray(GameScene.CursorCount)
        lastMoveIndex = IntArray(GameScene.CursorCount)
        for (i in 0 until GameScene.CursorCount) {
            cursorIndex[i] = 0
            lastMoveIndex[i] = -1
        }
    }

    fun setMap(mapName: String, file: String, md5: String) {
        this.mapName = mapName
        this.md5 = md5
        this.mapFile = file
    }

    fun setObjectCount(count: Int) {
        objectData = arrayOfNulls(count)
    }

    fun addObjectResult(id: Int, accuracy: Short, ticks: BitSet) {
        if (id < 0 || objectData == null || id >= objectData!!.size) return

        val data = objectData!![id] ?: ReplayObjectData()
        data.accuracy = accuracy
        data.tickSet = ticks
        objectData!![id] = data
    }

    fun addObjectScore(id: Int, score: ResultType) {
        if (id < 0 || objectData == null || id >= objectData!!.size) return

        if (objectData!![id] == null)
            objectData!![id] = ReplayObjectData()

        objectData!![id]!!.result = score.id
    }

    fun addPress(timeMs: Int, pos: PointF, pid: Int) {
        if (pid > GameScene.CursorCount || isSaving) return
        cursorMoves[pid].pushBack(this, timeMs, pos.x, pos.y, TouchType.DOWN)
    }

    fun addMove(timeMs: Int, pos: PointF, pid: Int) {
        if (pid > GameScene.CursorCount || isSaving) return
        cursorMoves[pid].pushBack(this, timeMs, pos.x, pos.y, TouchType.MOVE)
    }

    fun addUp(timeMs: Int, pid: Int) {
        if (pid > GameScene.CursorCount || isSaving) return
        cursorMoves[pid].pushBack(timeMs, TouchType.UP)
    }

    fun save(filename: String) {
        isSaving = true

        for (i in cursorMoves.indices)
            Debug.i("Replay contains ${cursorMoves[i].size} moves for finger $i")
        Debug.i("Skipped $pointsSkipped points")
        Debug.i("Replay contains ${objectData!!.size} objects")
        val os: ObjectOutputStream
        val zip: ZipOutputStream
        try {
            zip = ZipOutputStream(FileOutputStream(filename))
            zip.setMethod(ZipOutputStream.DEFLATED)
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)
            zip.putNextEntry(ZipEntry("data"))
            os = ObjectOutputStream(zip)
        } catch (e: FileNotFoundException) {
            Debug.e("File not found $filename", e)
            isSaving = false
            return
        } catch (e: IOException) {
            Debug.e("IOException: " + e.message, e)
            isSaving = false
            return
        }

        try {
            os.writeObject(ReplayVersion())
            os.writeObject(mapName)
            os.writeObject(mapFile)
            os.writeObject(md5)

            if (stat != null) {
                os.writeLong(stat!!.time)
                os.writeInt(stat!!.hit300k)
                os.writeInt(stat!!.hit300)
                os.writeInt(stat!!.hit100k)
                os.writeInt(stat!!.hit100)
                os.writeInt(stat!!.hit50)
                os.writeInt(stat!!.misses)
                os.writeInt(stat!!.totalScoreWithMultiplier)
                os.writeInt(stat!!.maxCombo)
                os.writeFloat(stat!!.accuracy)
                os.writeBoolean(stat!!.isPerfect)
                os.writeObject(stat!!.playerName)
                os.writeObject(stat!!.mod)
                os.writeObject(stat!!.extraModString)
            }

            os.writeInt(cursorMoves.size)
            for (move in cursorMoves) {
                move.writeTo(os)
            }
            os.writeInt(objectData!!.size)
            for (data in objectData!!) {
                val d = data ?: ReplayObjectData()
                os.writeShort(d.accuracy.toInt())
                if (d.tickSet == null || d.tickSet!!.length() == 0) {
                    os.writeByte(0)
                } else {
                    val bytes = ByteArray((d.tickSet!!.length() + 7) / 8)
                    for (i in 0 until d.tickSet!!.length()) {
                        if (d.tickSet!!.get(i)) {
                            bytes[bytes.size - i / 8 - 1] = (bytes[bytes.size - i / 8 - 1].toInt() or (1 shl (i % 8))).toByte()
                        }
                    }
                    os.writeByte(bytes.size)
                    os.write(bytes)
                }
                os.writeByte(d.result.toInt())
            }
        } catch (e: IOException) {
            Debug.e("IOException: " + e.message, e)
            isSaving = false
            return
        }

        try {
            os.flush()
            zip.flush()
            zip.closeEntry()
            zip.flush()
        } catch (e: IOException) {
            Debug.e("IOException: " + e.message, e)
        }

        isSaving = false
    }

    @Suppress("UNCHECKED_CAST")
    fun loadInfo(filename: String): Boolean {
        val os: ObjectInputStream
        try {
            val zip = ZipInputStream(FileInputStream(filename))
            zip.getNextEntry()
            os = ObjectInputStream(zip)
        } catch (e: Exception) {
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        Debug.i("Loading replay $filename")

        cursorMoves.clear()
        var version = 0
        try {
            val firstObject = os.readObject()
            Debug.i("Readed object: ${firstObject.javaClass.name}")
            if (firstObject.javaClass == ReplayVersion::class.java) {
                Debug.i("Other replay version")
                version = (firstObject as ReplayVersion).version
                replayVersion = version
                mapName = os.readObject() as String
            } else {
                mapName = firstObject as String
            }
            mapFile = os.readObject() as String
            md5 = os.readObject() as String

            Debug.i(mapName)
            Debug.i(mapFile)
            Debug.i(md5)

            if (version >= 3) {
                stat = StatisticV2()
                stat!!.time = os.readLong()
                stat!!.hit300k = os.readInt()
                stat!!.hit300 = os.readInt()
                stat!!.hit100k = os.readInt()
                stat!!.hit100 = os.readInt()
                stat!!.hit50 = os.readInt()
                stat!!.misses = os.readInt()
                stat!!.setForcedScore(os.readInt())
                stat!!.maxCombo = os.readInt()
                stat!!.accuracy = os.readFloat()
                stat!!.isPerfect = os.readBoolean()
                stat!!.playerName = os.readObject() as String
                stat!!.mod = os.readObject() as EnumSet<GameMod>
            }

            if (version >= 4) {
                stat!!.setExtraModFromString(os.readObject() as String)
            }
        } catch (e: EOFException) {
            Debug.e("O_o eof...")
            ToastLogger.showTextId(R.string.replay_corrupted, true)
            return false
        } catch (e: Exception) {
            ToastLogger.showTextId(R.string.replay_corrupted, true)
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    fun load(filename: String): Boolean {
        val os: ObjectInputStream
        try {
            val zip = ZipInputStream(FileInputStream(filename))
            zip.getNextEntry()
            os = ObjectInputStream(zip)
        } catch (e: Exception) {
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        Debug.i("Loading replay $filename")

        cursorMoves.clear()
        var version = 0
        try {
            val mName: String
            val firstObject = os.readObject()
            Debug.i("Read object: ${firstObject.javaClass.name}")
            if (firstObject.javaClass == ReplayVersion::class.java) {
                Debug.i("Other replay version")
                version = (firstObject as ReplayVersion).version
                replayVersion = version
                mName = os.readObject() as String
            } else {
                mName = firstObject as String
            }
            val mFile = os.readObject() as String
            val mmd5 = os.readObject() as String

            if (mName != mapName && mFile != mapFile) {
                Debug.i("Replay doesn't match the map!")
                Debug.i("$mapName ::: $mName")
                Debug.i("$mapFile ::: $mFile")
                Debug.i("$md5 ::: $mmd5")
                ToastLogger.showTextId(R.string.replay_wrongmap, true)
                os.close()
                return false
            }

            if (version >= 3) {
                stat = StatisticV2()
                stat!!.time = os.readLong()
                stat!!.hit300k = os.readInt()
                stat!!.hit300 = os.readInt()
                stat!!.hit100k = os.readInt()
                stat!!.hit100 = os.readInt()
                stat!!.hit50 = os.readInt()
                stat!!.misses = os.readInt()
                stat!!.setForcedScore(os.readInt())
                stat!!.maxCombo = os.readInt()
                stat!!.accuracy = os.readFloat()
                stat!!.isPerfect = os.readBoolean()
                stat!!.playerName = os.readObject() as String
                stat!!.mod = os.readObject() as EnumSet<GameMod>
            }

            if (version >= 4) {
                stat!!.setExtraModFromString(os.readObject() as String)
            }

            val msize = os.readInt()
            for (i in 0 until msize) {
                cursorMoves.add(MoveArray.readFrom(os, this))
            }

            os.readInt()
            for (i in objectData!!.indices) {
                val data = ReplayObjectData()
                data.accuracy = os.readShort()
                val len = os.readByte().toInt()
                if (len > 0) {
                    data.tickSet = BitSet()
                    val bytes = ByteArray(len)
                    if (os.read(bytes) > 0) {
                        println("Read $len bytes")
                    }
                    for (j in 0 until len * 8) {
                        data.tickSet!!.set(j, bytes[len - j / 8 - 1].toInt() and (1 shl j % 8) != 0)
                    }
                }
                if (version >= 1) {
                    data.result = os.readByte()
                }
                objectData!![i] = data
            }
        } catch (e: EOFException) {
            Debug.e("O_o eof...")
            Debug.e(e)
            ToastLogger.showTextId(R.string.replay_corrupted, true)
            return false
        } catch (e: Exception) {
            ToastLogger.showTextId(R.string.replay_corrupted, true)
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        for (i in cursorMoves.indices)
            Debug.i("Loaded ${cursorMoves[i].size} moves for finger $i")
        Debug.i("Loaded ${objectData!!.size} objects")
        return true
    }

    fun countMarks(difficulty: Float) {}

    fun getStat(): StatisticV2? = stat

    fun setStat(stat: StatisticV2?) {
        this.stat = stat
    }

    fun getMd5(): String = md5

    fun getMapFile(): String = mapFile

    fun getMapName(): String = mapName

    class ReplayVersion : Serializable {
        @JvmField
        var version = 5

        companion object {
            private const val serialVersionUID = 4643121693566795335L
        }
    }

    class ReplayObjectData {
        @JvmField
        var accuracy: Short = 0
        @JvmField
        var tickSet: BitSet? = null
        @JvmField
        var result: Byte = 0
    }

    open class ReplayMovement {
        @JvmField
        var time = 0
        @JvmField
        var point = PointF()
        @JvmField
        var touchType: TouchType? = null

        fun getTime(): Int = time
        fun getPoint(): PointF = point
        fun getTouchType(): TouchType? = touchType
    }

    class MoveArray(startSize: Int) {
        lateinit var movements: Array<ReplayMovement?>
        @JvmField
        var size = 0
        @JvmField
        var allocated = 0

        init {
            allocated = startSize
            size = 0
            movements = arrayOfNulls(allocated)
        }

        fun reallocate(newSize: Int) {
            if (newSize <= allocated) return
            val newMovements = arrayOfNulls<ReplayMovement>(newSize)
            System.arraycopy(movements, 0, newMovements, 0, size)
            movements = newMovements
            allocated = newSize
        }

        fun checkNewPoint(px: Float, py: Float): Boolean {
            if (size < 2) return false

            val minusTwoMovement = movements[size - 2]!!
            val previousMovement = movements[size - 1]!!

            val tx = (px + minusTwoMovement.point.x) * 0.5f
            val ty = (py + minusTwoMovement.point.y) * 0.5f

            return (Utils.sqr(previousMovement.point.x - tx) + Utils.sqr(previousMovement.point.y - ty)) <= 25
        }

        fun pushBack(replay: Replay, time: Int, x: Float, y: Float, touchType: TouchType) {
            var idx = size
            if (touchType == TouchType.MOVE && checkNewPoint(x, y)) {
                idx = size - 1
                replay.pointsSkipped++
            } else {
                if (size + 1 >= allocated) {
                    reallocate(allocated * 3 / 2)
                }
                size++
            }
            val movement = ReplayMovement()
            movements[idx] = movement
            movement.time = time
            movement.point.x = x
            movement.point.y = y
            movement.touchType = touchType
        }

        fun pushBack(time: Int, touchType: TouchType) {
            if (size >= allocated) {
                reallocate(allocated * 3 / 2)
            }
            movements[size] = ReplayMovement()
            val movement = movements[size]!!
            movement.time = time
            movement.touchType = touchType
            size++
        }

        @Throws(IOException::class)
        fun writeTo(os: ObjectOutputStream) {
            os.writeInt(size)
            for (i in 0 until size) {
                val movement = movements[i]!!
                os.writeInt((movement.time shl 2) + movement.touchType!!.id)
                if (movement.touchType != TouchType.UP) {
                    os.writeFloat(movement.point.x * Config.getTextureQuality())
                    os.writeFloat(movement.point.y * Config.getTextureQuality())
                }
            }
        }

        companion object {
            @Throws(IOException::class)
            private fun readTouchPoint(`is`: ObjectInputStream, replay: Replay): Float {
                return if (replay.replayVersion < 5) `is`.readShort().toFloat() else `is`.readFloat()
            }

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(`is`: ObjectInputStream, replay: Replay): MoveArray {
                val size = `is`.readInt()
                val array = MoveArray(size)
                array.size = size
                for (i in 0 until size) {
                    val movement = ReplayMovement()
                    array.movements[i] = movement
                    movement.time = `is`.readInt()
                    movement.touchType = TouchType.getByID((movement.time and 3).toByte())
                    movement.time = movement.time shr 2
                    if (movement.touchType != TouchType.UP) {
                        val baseX = readTouchPoint(`is`, replay)
                        val baseY = readTouchPoint(`is`, replay)
                        val gamePoint = PointF(
                            baseX / Config.getTextureQuality(),
                            baseY / Config.getTextureQuality()
                        )
                        val realPoint = if (replay.replayVersion > 1)
                            Utils.trackToRealCoords(gamePoint)
                        else
                            Utils.trackToRealCoords(
                                Utils.realToTrackCoords(gamePoint, 1024f, 600f, true)
                            )
                        movement.point.set(realPoint)
                    }
                }
                return array
            }
        }
    }

    companion object {
        @JvmField
        var mod: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
        @JvmField
        var oldMod: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
        @JvmField
        var oldChangeSpeed = 1.0f
        @JvmField
        var oldFLFollowDelay = FlashLightEntity.defaultMoveDelayS

        @JvmField
        var oldCustomAR: Float? = null
        @JvmField
        var oldCustomOD: Float? = null
        @JvmField
        var oldCustomCS: Float? = null
        @JvmField
        var oldCustomHP: Float? = null
    }
}

package ru.nsu.ccfit.zuev.osu.storyboard

import com.dgsrz.bancho.ui.StoryBoardTestActivity
import okio.buffer
import okio.source
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.regex.Pattern

class OsbParser {
    private val sprites: LinkedList<OsuSprite> = LinkedList()
    private val timingPoints: ArrayList<TimingPoint> = ArrayList()
    private val hitSounds: ArrayList<HitSound> = ArrayList()
    private val variablesMap: HashMap<String, String> = HashMap()
    private var line: String? = null
    private var info: Array<String>? = null
    private var sliderMultiplier = 0f
    private var zIndex = -900

    fun getTimingPoints(): ArrayList<TimingPoint> = timingPoints

    fun getVariablesMap(): HashMap<String, String> = variablesMap

    fun getSprites(): LinkedList<OsuSprite> = sprites

    fun getHitSounds(): ArrayList<HitSound> = hitSounds

    @Throws(IOException::class)
    fun parse(path: String) {
        val osuFile = File(path)
        loadBeatmap(osuFile)
        val files = FileUtils.listFiles(osuFile.parentFile, ".osb")
        if (files != null && files.isNotEmpty()) {
            val source = files[0].source().buffer()

            var line: String?
            while (source.readUtf8Line().also { line = it } != null) {
                val pattern = Pattern.compile("\\[(\\w+)]")
                val matcher = pattern.matcher(line!!.trim())

                if (matcher.find()) {
                    val title = matcher.group(1)
                    when (title) {
                        "Events" -> parseObjects(source)
                        "Variables" -> parseVariables(source)
                    }
                }
            }
            source.close()
        }
        Collections.sort(hitSounds) { lhs, rhs -> (lhs.time - rhs.time).toInt() }
        Collections.sort(sprites) { lhs, rhs -> (lhs.spriteStartTime - rhs.spriteStartTime).toInt() }
    }

    @Throws(IOException::class)
    private fun parseObjects(source: okio.BufferedSource) {
        line = source.readUtf8Line()
        while (line != null) {
            if (line == "") {
                break
            }

            if (line!!.startsWith("Sprite")) {
                for (s in variablesMap.keys) {
                    if (line!!.contains(s)) {
                        line = line!!.replace(s, variablesMap[s]!!)
                    }
                }
                info = line!!.split(",".toRegex()).toTypedArray()
                var layer = 0
                when (info!![1]) {
                    "Background" -> layer = 0
                    "Fail" -> layer = 1
                    "Pass" -> layer = 2
                    "Foreground" -> layer = 3
                }
                val origin = OsuSprite.Origin.valueOf(info!![2])
                var filePath = info!![3]
                filePath = filePath.replace("\"", "")
                val x = info!![4].toFloat()
                val y = info!![5].toFloat()
                val events = parseEvents(source)
                val sprite = OsuSprite(x, y, layer, origin, filePath, events, zIndex++)
                sprite.setDebugLine(line)
                sprites.add(sprite)
            } else if (line!!.startsWith("Animation")) {
                for (s in variablesMap.keys) {
                    if (line!!.contains(s)) {
                        line = line!!.replace(s, variablesMap[s]!!)
                    }
                }
                info = line!!.split(",".toRegex()).toTypedArray()
                var layer = 0
                when (info!![1]) {
                    "Background" -> layer = 0
                    "Fail" -> layer = 1
                    "Pass" -> layer = 2
                    "Foreground" -> layer = 3
                }
                val origin = OsuSprite.Origin.valueOf(info!![2])
                var filePath = info!![3]
                filePath = filePath.replace("\"", "")
                val x = info!![4].toFloat()
                val y = info!![5].toFloat()
                val count = info!![6].toInt()
                val delay = info!![7].toInt()
                var loopType = "LoopForever"
                if (info!!.size == 9) {
                    loopType = info!![8]
                }
                val events = parseEvents(source)
                sprites.add(OsuSprite(x, y, layer, origin, filePath, events, zIndex++, count, delay, loopType))
            } else {
                line = source.readUtf8Line()
            }
        }
    }

    @Throws(IOException::class)
    private fun parseEvents(source: okio.BufferedSource): ArrayList<OsuEvent> {
        val eventList = ArrayList<OsuEvent>()
        line = source.readUtf8Line()
        if (line!!.startsWith("_")) {
            line = line!!.replace("_", " ")
        }
        while (line != null && line!!.startsWith(" ")) {
            line = line!!.trim()
            if (line!!.isEmpty()) break
            for (s in variablesMap.keys) {
                if (line!!.contains(s)) {
                    line = line!!.replace(s, variablesMap[s]!!)
                }
            }
            val currentOsuEvent = OsuEvent()
            info = line!!.split(",".toRegex()).toTypedArray()
            val command = Command.valueOf(info!![0])
            currentOsuEvent.command = command
            when (command) {
                Command.L -> {
                    currentOsuEvent.startTime = info!![1].toLong()
                    currentOsuEvent.loopCount = info!![2].toInt()
                    currentOsuEvent.subEvents = parseSubEvents(source)
                    if (currentOsuEvent.subEvents!!.size > 0) {
                        currentOsuEvent.startTime =
                            currentOsuEvent.subEvents!![0].startTime + currentOsuEvent.startTime
                    }
                }
                Command.T -> {
                    if (info!!.size > 2) {
                        currentOsuEvent.startTime = info!![2].toLong()
                        currentOsuEvent.endTime = info!![3].toLong()
                    } else {
                        currentOsuEvent.startTime = 0
                        currentOsuEvent.endTime = 999999999
                    }
                    currentOsuEvent.triggerType = info!![1]
                    var soundType = -1
                    when (currentOsuEvent.triggerType) {
                        "HitSoundWhistle" -> soundType = 2
                        "HitSoundFinish" -> soundType = 4
                        "HitSoundClap" -> soundType = 8
                    }
                    currentOsuEvent.subEvents = parseSubEvents(source)
                    for (hitSound in hitSounds) {
                        if (hitSound.time >= currentOsuEvent.startTime && hitSound.soundType and soundType == soundType) {
                            currentOsuEvent.startTime = hitSound.time
                            break
                        }
                    }
                }
                else -> {
                    currentOsuEvent.ease = info!![1].toInt()
                    currentOsuEvent.startTime = info!![2].toLong()
                    currentOsuEvent.endTime = if (info!![3] == "") currentOsuEvent.startTime + 1 else info!![3].toLong()
                    var params: FloatArray? = null
                    when (command) {
                        Command.F, Command.MX, Command.MY, Command.S, Command.R -> {
                            params = FloatArray(2)
                            params[0] = info!![4].toFloat()
                            params[1] = if (info!!.size == 5) info!![4].toFloat() else info!![5].toFloat()
                        }
                        Command.M, Command.V -> {
                            params = FloatArray(4)
                            params[0] = info!![4].toFloat()
                            params[1] = info!![5].toFloat()
                            if (info!!.size == 6) {
                                params[2] = info!![4].toFloat()
                                params[3] = info!![5].toFloat()
                            } else {
                                params[2] = info!![6].toFloat()
                                params[3] = info!![7].toFloat()
                            }
                        }
                        Command.C -> {
                            params = FloatArray(6)
                            params[0] = info!![4].toFloat()
                            params[1] = info!![5].toFloat()
                            params[2] = info!![6].toFloat()
                            if (info!!.size == 7) {
                                params[3] = info!![4].toFloat()
                                params[4] = info!![5].toFloat()
                                params[5] = info!![6].toFloat()
                            } else {
                                params[3] = info!![7].toFloat()
                                params[4] = info!![8].toFloat()
                                params[5] = info!![9].toFloat()
                            }
                        }
                        Command.P -> currentOsuEvent.P = info!![4]
                        else -> {}
                    }
                    currentOsuEvent.params = params
                    line = source.readUtf8Line()
                    if (line!!.startsWith("_")) {
                        line = line!!.replace("_", " ")
                    }
                    for (s in variablesMap.keys) {
                        if (line!!.contains(s)) {
                            line = line!!.replace(s, variablesMap[s]!!)
                        }
                    }
                }
            }
            if (currentOsuEvent.triggerType == null || (currentOsuEvent.triggerType != "Passing" && currentOsuEvent.triggerType != "Failing")) {
                eventList.add(currentOsuEvent)
            }
        }
        return eventList
    }

    @Throws(IOException::class)
    private fun parseSubEvents(source: okio.BufferedSource): ArrayList<OsuEvent> {
        val subOsuEventList = ArrayList<OsuEvent>()
        while (source.readUtf8Line().also { line = it } != null && (line!!.startsWith("  ") || line!!.startsWith("__"))) {
            line = line!!.replace("_", " ").trim()
            for (s in variablesMap.keys) {
                if (line!!.contains(s)) {
                    line = line!!.replace(s, variablesMap[s]!!)
                }
            }
            val subEvent = OsuEvent()
            info = line!!.split(",".toRegex()).toTypedArray()
            val subCommand = Command.valueOf(info!![0])
            subEvent.command = subCommand
            subEvent.ease = info!![1].toInt()
            subEvent.startTime = info!![2].toLong()
            subEvent.endTime = if (info!![3] == "") subEvent.startTime + 1 else info!![3].toLong()
            var params: FloatArray? = null
            when (subCommand) {
                Command.F, Command.MX, Command.MY, Command.S, Command.R -> {
                    params = FloatArray(2)
                    params[0] = info!![4].toFloat()
                    params[1] = if (info!!.size == 5) info!![4].toFloat() else info!![5].toFloat()
                }
                Command.M, Command.V -> {
                    params = FloatArray(4)
                    params[0] = info!![4].toFloat()
                    params[1] = info!![5].toFloat()
                    if (info!!.size == 6) {
                        params[2] = info!![4].toFloat()
                        params[3] = info!![5].toFloat()
                    } else {
                        params[2] = info!![6].toFloat()
                        params[3] = info!![7].toFloat()
                    }
                }
                Command.C -> {
                    params = FloatArray(6)
                    params[0] = info!![4].toFloat()
                    params[1] = info!![5].toFloat()
                    params[2] = info!![6].toFloat()
                    if (info!!.size == 7) {
                        params[3] = info!![4].toFloat()
                        params[4] = info!![5].toFloat()
                        params[5] = info!![6].toFloat()
                    } else {
                        params[3] = info!![7].toFloat()
                        params[4] = info!![8].toFloat()
                        params[5] = info!![9].toFloat()
                    }
                }
                Command.P -> subEvent.P = info!![4]
                Command.T, Command.L -> parseSubEvents(source)
                else -> {}
            }
            subEvent.params = params
            subOsuEventList.add(subEvent)
        }
        return subOsuEventList
    }

    @Throws(IOException::class)
    fun loadBeatmap(file: File) {
        val source = file.source().buffer()

        source.readUtf8Line()?.trim()

        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val pattern = Pattern.compile("\\[(\\w+)]")
            val matcher = pattern.matcher(line!!.trim())

            if (matcher.find()) {
                val title = matcher.group(1)
                when (title) {
                    "General" -> parseGeneral(source)
                    "Difficulty" -> parseDifficulty(source)
                    "Events" -> parseEvent(source)
                    "TimingPoints" -> parseTimingPoints(source)
                    "HitObjects" -> parseHitObject(source)
                }
            }
        }
        source.close()
    }

    @Throws(IOException::class)
    private fun parseGeneral(source: okio.BufferedSource) {
        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return
            val values = trimmed.split(":".toRegex()).toTypedArray()
            val key = values[0]
            val value = values[1].trim { it <= ' ' }

            if (key == "AudioFilename") {
                StoryBoardTestActivity.activity?.mAudioFileName = value
                break
            }
        }
    }

    @Throws(IOException::class)
    private fun parseEvent(source: okio.BufferedSource) {
        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return

            if (trimmed.contains(",")) {
                val info = trimmed.split(",".toRegex()).toTypedArray()
                val pattern = Pattern.compile("[^\"]+\\.(jpg|png)", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(trimmed)
                if (info[0] == "0" && matcher.find()) {
                    StoryBoardTestActivity.activity?.mBackground = matcher.group(0)
                    parseObjects(source)
                    break
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun parseVariables(source: okio.BufferedSource) {
        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return
            val values = trimmed.split("=".toRegex()).toTypedArray()
            variablesMap[values[0]] = values[1].trim()
        }
    }

    @Throws(IOException::class)
    private fun parseDifficulty(source: okio.BufferedSource) {
        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return
            val values = trimmed.split(":".toRegex()).toTypedArray()
            if (values[0] == "SliderMultiplier") {
                sliderMultiplier = values[1].toFloat()
            }
        }
    }

    @Throws(IOException::class)
    private fun parseTimingPoints(source: okio.BufferedSource) {
        var line: String?
        var lastLengthPerBeat = -100f
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return
            val values = trimmed.split(",".toRegex()).toTypedArray()
            val timingPoint = TimingPoint()
            timingPoint.startTime = values[0].toFloat().toLong()
            timingPoint.lengthPerBeat = values[1].toFloat()
            if (timingPoint.lengthPerBeat < 0) {
                timingPoint.lengthPerBeat = lastLengthPerBeat
            } else {
                lastLengthPerBeat = timingPoint.lengthPerBeat
            }
            timingPoints.add(timingPoint)
        }
    }

    @Throws(IOException::class)
    private fun parseHitObject(source: okio.BufferedSource) {
        var line: String?
        while (source.readUtf8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed == "") return
            val values = trimmed.split(",".toRegex()).toTypedArray()
            val objectType = values[3].toInt()
            if (objectType and 1 == 1) {
                val hitSound = HitSound()
                hitSound.time = values[2].toLong()
                hitSound.soundType = values[4].toInt()
                hitSounds.add(hitSound)
            } else if (objectType and 2 == 2) {
                val startTime = values[2].toLong()
                val count = values[6].toInt() + 1
                val sliderLength = values[7].toFloat()
                var soundTypes: Array<String>? = null
                if (values.size > 8) {
                    soundTypes = values[8].split("\\|".toRegex()).toTypedArray()
                }
                var currentPoint = timingPoints[0]
                for (timingPoint in timingPoints) {
                    if (startTime > timingPoint.startTime) {
                        currentPoint = timingPoint
                        break
                    }
                }
                val sliderLengthTime = currentPoint.lengthPerBeat * (sliderMultiplier / sliderLength) / 100
                for (i in 0 until count) {
                    val hitSound = HitSound()
                    if (values.size > 8) {
                        hitSound.soundType = soundTypes!![i].toInt()
                    } else {
                        hitSound.soundType = values[4].toInt()
                    }

                    hitSound.time = (startTime + sliderLengthTime * i).toLong()
                    if (hitSound.soundType > 0)
                        hitSounds.add(hitSound)
                }
            } else if (objectType and 8 == 8) {
                val hitSound = HitSound()
                hitSound.time = values[5].toLong()
                hitSound.soundType = values[4].toInt()
                hitSounds.add(hitSound)
            }
        }
    }

    companion object {
        @JvmField
        val instance = OsbParser()
    }
}

package com.edlplan.replay

import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object OsuDroidReplayPack {

    @JvmStatic
    @Throws(Exception::class)
    fun packTo(file: File, replay: OsuDroidReplay) {
        if (!file.exists()) {
            file.createNewFile()
        }
        file.outputStream().use { outputStream ->
            outputStream.write(pack(replay))
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun pack(replay: OsuDroidReplay): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = ZipOutputStream(byteArrayOutputStream)
        outputStream.putNextEntry(ZipEntry("entry.json"))

        val entryJson = JSONObject().apply {
            put("version", 1)
            put("replaydata", replay.toJSON())
        }

        outputStream.write(entryJson.toString(2).toByteArray())

        outputStream.putNextEntry(ZipEntry(replay.replayFileName))

        val file = if (replay.isAbsoluteReplay) {
            File(replay.replayFile)
        } else {
            File(OdrConfig.getScoreDir(), replay.replayFileName)
        }
        val inputStream = FileInputStream(file)

        val buffer = ByteArray(1024)
        var l: Int
        while (inputStream.read(buffer).also { l = it } != -1) {
            outputStream.write(buffer, 0, l)
        }

        outputStream.finish()
        return byteArrayOutputStream.toByteArray()
    }

    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    fun unpack(raw: InputStream): ReplayEntry {
        val inputStream = ZipInputStream(raw)
        val entry = ReplayEntry()
        val zipEntryMap = HashMap<String, ByteArray>()
        var zipEntry: ZipEntry? = inputStream.nextEntry
        while (zipEntry != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var l: Int
            while (inputStream.read(buffer).also { l = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, l)
            }
            zipEntryMap[zipEntry.name] = byteArrayOutputStream.toByteArray()
            println("解压文件：" + zipEntry.name + " size: " + zipEntryMap[zipEntry.name]!!.size)
            zipEntry = inputStream.nextEntry
        }
        inputStream.close()

        entry.replay = OsuDroidReplay.parseJSON(
            JSONObject(String(zipEntryMap["entry.json"]!!)).getJSONObject("replaydata")
        )
        entry.replayFile = zipEntryMap[entry.replay!!.replayFileName]

        return entry
    }

    class ReplayEntry {
        @JvmField
        var replay: OsuDroidReplay? = null
        @JvmField
        var replayFile: ByteArray? = null
    }
}

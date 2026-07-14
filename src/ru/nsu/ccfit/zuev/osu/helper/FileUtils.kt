package ru.nsu.ccfit.zuev.osu.helper

import android.os.Build
import android.os.Environment
import net.lingala.zip4j.ZipFile
import okio.Okio
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*
import java.util.stream.Collectors

object FileUtils {

    @JvmStatic
    @Throws(FileNotFoundException::class, IOException::class)
    fun copy(from: File, to: File) {
        val source = from.source()
        val bufferedSink = to.sink().buffer()
        bufferedSink.use { sink ->
            source.use { src ->
                sink.writeAll(src)
            }
        }
    }

    @JvmStatic
    @Throws(FileNotFoundException::class, IOException::class)
    fun move(from: File, to: File) {
        copy(from, to)
        from.delete()
    }

    @JvmStatic
    fun extractZip(sourcePath: String, targetPath: String): Boolean {
        val file = File(sourcePath)
        if (!canUseSD()) return false
        ToastLogger.addToLog("Importing $sourcePath")
        val sourceFileName = file.name
        val folderName = sourceFileName.substring(0, sourceFileName.length - 4)
        val folderFile = File("$targetPath/$folderName")
        if (!folderFile.exists()) {
            folderFile.mkdirs()
        }
        try {
            val zip = ZipFile(file)
            if (!zip.isValidZipFile) {
                ToastLogger.showText(StringTable.format(R.string.message_error, "Invalid file"), false)
                Debug.e("FileUtils.extractZip: ${file.name} is invalid")
                file.renameTo(File(file.parentFile, "$sourceFileName.badzip"))
                LibraryManager.deleteDir(folderFile)
                return false
            }
            zip.extractAll(folderFile.absolutePath)
            promoteSingleSubfolder(folderFile)
            if (Config.isDELETE_OSZ() && sourceFileName.lowercase().endsWith(".osz") || sourceFileName.lowercase().endsWith(".osk")) {
                file.delete()
            }
        } catch (e: net.lingala.zip4j.exception.ZipException) {
            Debug.e("FileUtils.extractZip: ${e.message}", e)
            val extensionIndex = sourceFileName.lastIndexOf('.')
            file.renameTo(File(file.parentFile,
                sourceFileName.substring(0, extensionIndex) + ".bad" + sourceFileName.substring(extensionIndex + 1)))
            return false
        }
        return true
    }

    private fun promoteSingleSubfolder(folder: File) {
        if (!folder.exists() || !folder.isDirectory) return
        val children = folder.listFiles() ?: return
        if (children.size != 1) return
        val onlyChild = children[0]
        if (!onlyChild.isDirectory) return
        val subChildren = onlyChild.listFiles() ?: return
        for (child in subChildren) {
            val dest = File(folder, child.name)
            child.renameTo(dest)
        }
        onlyChild.delete()
    }

    @JvmStatic
    fun getFileChecksum(algorithm: String, file: File): String {
        val sb = StringBuilder()
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val inputStream = BufferedInputStream(FileInputStream(file))
            val byteArray = ByteArray(1024)
            var bytesCount: Int
            while (inputStream.read(byteArray).also { bytesCount = it } != -1) {
                digest.update(byteArray, 0, bytesCount)
            }
            inputStream.close()
            val bytes = digest.digest()
            for (i in bytes.indices) {
                sb.append(Integer.toString((bytes[i].toInt() and 0xff) + 0x100, 16).substring(1))
            }
        } catch (e: IOException) {
            Debug.e("getFileChecksum ${e.message}", e)
        } catch (e: java.security.NoSuchAlgorithmException) {
            Debug.e(e.message, e)
        }
        return sb.toString()
    }

    @JvmStatic
    fun canUseSD(): Boolean {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            return true
        } else {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY) {
                ToastLogger.showText(StringTable.get(R.string.message_error_sdcardread), false)
            } else {
                ToastLogger.showText(StringTable.get(R.string.message_error_sdcard), false)
            }
        }
        return false
    }

    @JvmStatic
    fun getMD5Checksum(file: File): String = getFileChecksum("MD5", file)

    @JvmStatic
    fun getSHA256Checksum(file: File): String = getFileChecksum("SHA-256", file)

    @JvmStatic
    fun listFiles(directory: File): Array<File>? = listFiles(directory) { true }

    @JvmStatic
    fun listFiles(directory: File, endsWith: String): Array<File>? =
        listFiles(directory) { file -> file.name.lowercase().endsWith(endsWith) }

    @JvmStatic
    fun listFiles(directory: File, endsWithExtensions: Array<String>): Array<File>? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            listFiles(directory) { file ->
                for (extension in endsWithExtensions) {
                    if (file.name.lowercase().endsWith(extension)) {
                        return@listFiles true
                    }
                }
                false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            listFiles(directory) { file ->
                val filename = file.name.lowercase()
                endsWithExtensions.any { filename.endsWith(it) }
            }
        } else {
            null
        }
    }

    @JvmStatic
    fun listFiles(directory: File, filter: FileFilter): Array<File>? {
        var filelist: Array<File>? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val cachedFiles = LinkedList<File>()
            try {
                val directoryFilter = java.nio.file.DirectoryStream.Filter<java.nio.file.Path> { entry ->
                    filter.accept(entry.toFile())
                }
                val stream = Files.newDirectoryStream(java.nio.file.Paths.get(directory.absolutePath), directoryFilter)
                stream.use { ds ->
                    for (path in ds) {
                        cachedFiles.add(path.toFile())
                    }
                }
            } catch (err: Exception) {
                Debug.e("FileUtils.listFiles: ${err.message}", err)
            }
            filelist = cachedFiles.toTypedArray()
        } else {
            filelist = directory.listFiles(filter)
        }
        return filelist
    }
}

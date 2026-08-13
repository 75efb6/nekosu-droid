package ru.nsu.ccfit.zuev.osu

import android.os.Build
import com.reco1l.legacy.engine.VideoTexture
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

enum class LibraryManager {
    INSTANCE;

    private var fileCount = 0
    private var currentIndex = 0

    fun getLibraryCacheFile(): File {
        return File(
            GlobalManager.getInstance().getMainActivity()!!.filesDir,
            String.format("library.%s.dat", VERSION)
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun loadLibraryCache(forceUpdate: Boolean): Boolean {
        synchronized(library) {
            library.clear()
        }

        ToastLogger.addToLog("Loading library...")
        if (!FileUtils.canUseSD()) {
            ToastLogger.addToLog("Can't use SD card!")
            return true
        }

        val replayDir = File(Config.getScorePath())
        if (!replayDir.exists()) {
            if (!replayDir.mkdir()) {
                ToastLogger.showText(
                    StringTable.format(
                        R.string.message_error_createdir, replayDir.path
                    ), true
                )
                return false
            }
            createNoMediaFile(replayDir)
        }

        val lib = getLibraryCacheFile()
        val dir = File(Config.getBeatmapPath())
        if (!dir.exists()) {
            return false
        }
        try {
            if (lib.createNewFile()) {
                Debug.i("LibraryManager: create library cache file")
            } else {
                Debug.i("LibraryManager: library cache file already exists")
            }
        } catch (e: IOException) {
            Debug.e("LibraryManager: ${e.message}", e)
        }

        try {
            ObjectInputStream(FileInputStream(lib)).use { istream ->
                var obj = istream.readObject()
                if (obj is String) {
                    if (obj != VERSION) {
                        return false
                    }

                    obj = istream.readObject()
                    if (obj is Int) {
                        fileCount = obj

                        obj = istream.readObject()
                        if (obj is Collection<*>) {
                            synchronized(library) {
                                library.addAll(obj as Collection<BeatmapInfo>)
                            }

                            ToastLogger.addToLog("Library loaded")
                            if (forceUpdate) {
                                checkLibrary()
                            }
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Debug.e("LibraryManager: ${e.message}", e)
        }
        ToastLogger.addToLog("Cannot load library!")
        return false
    }

    private fun checkLibrary() {
        val dir = File(Config.getBeatmapPath())
        val files = FileUtils.listFiles(dir) ?: emptyArray()
        if (files.size == fileCount) {
            return
        }

        ToastLogger.showText(StringTable.get(R.string.message_lib_update), true)

        val fileCountLocal = files.size
        val manager = LibraryCacheManager(fileCountLocal, files)
        manager.addUncachedBeatmaps()

        while (isCaching) {
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Debug.e("LibraryManager: ${e.message}", e)
            }
        }
        isCaching = true

        fileCount = files.size
        saveToCache()
    }

    @Synchronized
    fun scanLibrary() {
        ToastLogger.addToLog("Caching library...")
        library.clear()

        val dir = File(Config.getBeatmapPath())
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                ToastLogger.showText(
                    StringTable.format(
                        R.string.message_error_createdir, dir.path
                    ), true
                )
                return
            }
            createNoMediaFile(dir)
            return
        }
        val filelist = FileUtils.listFiles(dir) ?: emptyArray()

        fileCount = filelist.size

        Debug.i("LibraryManager: Operating in multithreaded mode")
        val manager = LibraryCacheManager(fileCount, filelist)
        manager.start()

        while (isCaching) {
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Debug.e("LibraryManager: ${e.message}", e)
            }
        }
        isCaching = true

        saveToCache()
        ToastLogger.showText(
            StringTable.format(R.string.message_lib_complete, manager.totalMaps),
            true
        )
    }

    private fun createNoMediaFile(dir: File) {
        val nomedia = File(dir.parentFile, ".nomedia")
        try {
            if (nomedia.createNewFile()) {
                Debug.i("LibraryManager: create .nomedia file")
            } else {
                Debug.i("LibraryManager: .nomedia file already exists")
            }
        } catch (e: IOException) {
            Debug.e("LibraryManager: ${e.message}", e)
        }
    }

    fun deleteMap(info: BeatmapInfo) {
        val dir = File(info.path)
        deleteDir(dir)

        synchronized(library) {
            library.remove(info)
        }
    }

    fun saveToCache() {
        if (library.isEmpty()) {
            return
        }
        val lib = getLibraryCacheFile()
        try {
            ObjectOutputStream(FileOutputStream(lib)).use { ostream ->
                lib.createNewFile()
                ostream.writeObject(VERSION)
                ostream.writeObject(fileCount)

                synchronized(library) {
                    ostream.writeObject(library)
                }
            }
        } catch (e: IOException) {
            ToastLogger.showText(
                StringTable.format(R.string.message_error, e.message),
                false
            )
            Debug.e("LibraryManager: ${e.message}", e)
        }
        shuffleLibrary()
        currentIndex = 0
    }

    fun clearCache() {
        val lib = getLibraryCacheFile()
        if (lib.exists()) {
            lib.delete()
            ToastLogger.showText(
                StringTable.get(R.string.message_lib_cleared),
                false
            )
        }
        currentIndex = 0
    }

    fun getLibrary(): MutableList<BeatmapInfo> {
        synchronized(library) {
            return library
        }
    }

    fun shuffleLibrary() {
        synchronized(library) {
            Collections.shuffle(library)
        }
    }

    fun getSizeOfBeatmaps(): Int {
        synchronized(library) {
            return library.size
        }
    }

    fun getBeatmap(): BeatmapInfo? = getBeatmapByIndex(currentIndex)

    fun getNextBeatmap(): BeatmapInfo? = getBeatmapByIndex(++currentIndex)

    fun getPrevBeatmap(): BeatmapInfo? = getBeatmapByIndex(--currentIndex)

    fun getBeatmapByIndex(index: Int): BeatmapInfo? {
        synchronized(library) {
            Debug.i("Music Changing Info: Require index :$index/${library.size}")
            if (library.size == 0) return null
            return if (index < 0 || index >= library.size) {
                shuffleLibrary()
                currentIndex = 0
                library[0]
            } else {
                currentIndex = index
                library[index]
            }
        }
    }

    fun findBeatmap(info: BeatmapInfo): Int {
        synchronized(library) {
            for (i in library.indices) {
                if (library[i] == info) {
                    return i.also { currentIndex = it }
                }
            }
        }
        return 0.also { currentIndex = it }
    }

    fun findTrackByMD5(md5: String?): TrackInfo? {
        if (md5 == null) return null

        synchronized(library) {
            var i = library.size - 1
            while (i >= 0) {
                val tracks = library[i].getTracks()
                var j = tracks.size - 1
                while (j >= 0) {
                    val track = tracks[j]
                    if (md5 == track.md5) return track
                    --j
                }
                --i
            }
        }
        return null
    }

    fun findBeatmapById(mapSetId: Int): Int {
        synchronized(library) {
            for (i in library.indices) {
                if (library[i].getTrack(0).beatmapSetID == mapSetId) {
                    return i.also { currentIndex = it }
                }
            }
        }
        return 0.also { currentIndex = it }
    }

    fun getCurrentIndex(): Int = currentIndex

    fun setCurrentIndex(index: Int) {
        currentIndex = index
    }

    fun findTrackByFileNameAndMD5(fileName: String, md5: String): TrackInfo? {
        synchronized(library) {
            for (info in library) {
                for (j in 0 until info.getCount()) {
                    val track = info.getTrack(j)
                    val trackFile = File(track.filename)
                    if (fileName == trackFile.name && md5 == track.md5) {
                        return track
                    }
                }
            }
        }
        return null
    }

    fun updateLibrary(force: Boolean) {
        if (!loadLibraryCache(force)) {
            scanLibrary()
        }
        saveToCache()
    }

    private inner class LibraryCacheManager(
        private val fileCount: Int,
        files: Array<File>
    ) {
        private val files: List<File> = files.toList()
        private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        @Volatile
        private var fileCached = 0

        @Volatile
        var totalMaps = 0
            private set

        fun start() {
            val optimalChunkSize = Math.ceil(fileCount.toDouble() / Runtime.getRuntime().availableProcessors()).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val subFiles = ArrayList(
                    this.files.stream()
                        .collect(Collectors.groupingBy<File, Int> { files.indexOf(it) / optimalChunkSize })
                        .values
                )
                subFiles.parallelStream().forEach { submitToExecutor(it) }
            } else {
                var i = 0
                while (i < this.files.size) {
                    submitToExecutor(this.files.subList(i, Math.min(i + optimalChunkSize, this.files.size)))
                    i += optimalChunkSize
                }
            }

            executors.shutdown()
            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {
                    Debug.i("Library Cache: $totalMaps maps loaded")
                    isCaching = false

                    synchronized(LibraryManager::class.java) {
                        (LibraryManager::class.java as Object).notify()
                    }
                } else {
                    Debug.e("Library Cache: Timeout")
                }
            } catch (e: InterruptedException) {
                Debug.e(e)
            }
        }

        fun addUncachedBeatmaps() {
            val optimalChunkSize = Math.ceil(fileCount.toDouble() / Runtime.getRuntime().availableProcessors()).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val subFiles = ArrayList(
                    this.files.stream()
                        .collect(Collectors.groupingBy<File, Int> { files.indexOf(it) / optimalChunkSize })
                        .values
                )
                subFiles.parallelStream().forEach { submitToExecutorCheckCached(it) }
            } else {
                var i = 0
                while (i < this.files.size) {
                    submitToExecutorCheckCached(this.files.subList(i, Math.min(i + optimalChunkSize, this.files.size)))
                    i += optimalChunkSize
                }
            }

            executors.shutdown()
            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {
                    Debug.i("Library Cache Updated")
                    isCaching = false

                    synchronized(LibraryManager::class.java) {
                        (LibraryManager::class.java as Object).notify()
                    }
                } else {
                    Debug.e("Library Cache: Timeout")
                }
            } catch (e: InterruptedException) {
                Debug.e(e)
            }

            synchronized(library) {
                val iterator = library.iterator()
                while (iterator.hasNext()) {
                    val beatmap = iterator.next()
                    if (!this.files.contains(File(beatmap.path))) {
                        iterator.remove()
                    }
                }
            }
        }

        private fun submitToExecutorCheckCached(files: List<File>) {
            executors.submit {
                for (file in files) {
                    GlobalManager.getInstance().loadingProgress = 50 + 50 * fileCached / fileCount
                    ToastLogger.setPercentage(fileCached * 100f / fileCount)

                    synchronized(this) {
                        fileCached++
                    }

                    if (!file.isDirectory) {
                        return@submit
                    }

                    val info = BeatmapInfo()
                    info.path = file.path

                    var alreadyExists = false
                    synchronized(library) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            alreadyExists = library.stream().anyMatch { i -> i.path == info.path }
                        } else {
                            for (i in library) {
                                if (i.path == info.path) {
                                    alreadyExists = true
                                    break
                                }
                            }
                        }
                    }
                    if (alreadyExists) {
                        return@submit
                    }

                    GlobalManager.getInstance().info = "Loading ${file.name} ..."

                    scanFolder(info)
                    if (info.getCount() < 1) {
                        return@submit
                    }

                    fillEmptyFields(info)

                    synchronized(library) {
                        library.add(info)
                    }
                }
            }
        }

        private fun submitToExecutor(files: List<File>) {
            executors.submit {
                for (file in files) {
                    GlobalManager.getInstance().loadingProgress = 50 + 50 * fileCached / fileCount
                    ToastLogger.setPercentage(fileCached * 100f / fileCount)

                    synchronized(this) {
                        fileCached++
                    }

                    if (!file.isDirectory) {
                        return@submit
                    }

                    GlobalManager.getInstance().info = "Loading ${file.name}..."
                    val info = BeatmapInfo()
                    info.path = file.path
                    scanFolder(info)
                    if (info.getCount() < 1) {
                        return@submit
                    }

                    fillEmptyFields(info)

                    synchronized(library) {
                        library.add(info)
                    }

                    synchronized(this) {
                        totalMaps += info.getCount()
                    }
                }
            }
        }
    }

    companion object {
        private const val VERSION = "library4.2"
        internal val library: MutableList<BeatmapInfo> = Collections.synchronizedList(ArrayList())
        private var isCaching = true

        @JvmStatic
        fun deleteDir(dir: File) {
            if (dir.exists() && dir.isDirectory) {
                val files = FileUtils.listFiles(dir) ?: return
                for (f in files) {
                    if (f.isDirectory) {
                        deleteDir(f)
                    } else if (f.delete()) {
                        Debug.i("${f.path} deleted")
                    }
                }
                if (dir.delete()) {
                    Debug.i("${dir.path} deleted")
                }
            }
        }

        private fun fillEmptyFields(info: BeatmapInfo) {
            info.creator = info.getTrack(0).creator
            if (info.title == "") {
                info.title = "unknown"
            }
            if (info.artist == "") {
                info.artist = "unknown"
            }
            if (info.creator == "") {
                info.creator = "unknown"
            }
        }

        private fun scanFolder(info: BeatmapInfo) {
            val dir = File(info.path)
            info.date = dir.lastModified()
            val filelist = FileUtils.listFiles(dir, ".osu")

            if (filelist == null) {
                return
            }
            for (file in filelist) {
                val parser = BeatmapParser(file).setCalculator(true)
                if (!parser.openFile()) {
                    if (Config.isDeleteUnimportedBeatmaps()) {
                        file.delete()
                    }
                    continue
                }

                val track = TrackInfo(info)
                track.filename = file.path
                track.creator = "unknown"

                val data = parser.parse(true)
                if (data == null || !data.populateMetadata(info) || !data.populateMetadata(track)) {
                    if (Config.isDeleteUnimportedBeatmaps()) {
                        file.delete()
                    }
                    continue
                }

                if (data.events.videoFilename != null && Config.isDeleteUnsupportedVideos()) {
                    try {
                        val videoFile = File(info.path, data.events.videoFilename)
                        if (!VideoTexture.Companion.isSupportedVideo(videoFile)) {
                            videoFile.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                info.addTrack(track)
            }

            if (Config.isDeleteUnimportedBeatmaps() && info.getCount() == 0) {
                deleteDir(dir)
            }

            Collections.sort(info.getTracks()) { object1, object2 ->
                object1.difficulty.compareTo(object2.difficulty)
            }
        }
    }
}

package ru.nsu.ccfit.zuev.osu.online

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import okhttp3.OkHttpClient
import okhttp3.Request
import org.anddev.andengine.util.Debug
import org.json.JSONException
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.RankedStatus
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException
import java.io.File
import java.io.IOException
import java.util.ArrayList

class OnlineManager private constructor() {

    internal var stayOnline = true
    private var ssid = ""
    internal var userId = -1L
    private var playID: String? = ""

    internal var username = ""
    private var password = ""
    private var deviceID = ""
    internal var rank: Long = 0
    internal var score: Long = 0
    internal var accuracy: Float = 0f
    internal var avatarURL = ""
    internal var profileBannerURL = ""
    internal var mapRank = 0
    private var replayID = 0

    internal var failMessage = ""
    private lateinit var context: Context

    fun init(context: Context) {
        this.stayOnline = Config.isStayOnline
        this.username = Config.getOnlineUsername()
        this.password = Config.getOnlinePassword() ?: ""
        this.deviceID = Config.getOnlineDeviceID()
        this.context = context
    }

    @Throws(OnlineManagerException::class)
    fun logIn(): Boolean {
        return logIn(username, password)
    }

    @Throws(OnlineManagerException::class)
    fun logIn(username: String): Boolean {
        return logIn(username, password)
    }

    @Synchronized
    @Throws(OnlineManagerException::class)
    fun logIn(username: String, password: String): Boolean {
        this.username = username
        this.password = password

        val post = PostBuilder()
        post.addParam("username", username)
        post.addParam(
            "password",
            MD5Calculator.getStringMD5(
                escapeHTMLSpecialCharacters(addSlashes(password.trim())) + "taikotaiko"
            )
        )
        post.addParam("version", ONLINE_VERSION)
        post.addParam("deviceID", deviceID)

        val response = sendRequest(post, ENDPOINT + "login.php")

        if (response == null) {
            if (failMessage.isNotEmpty()) return false
            failMessage = "Cannot connect to server"
            return false
        }
        if (response.size < 2) {
            failMessage = "Invalid server response"
            return false
        }

        val params = response[1].split("\\s+".toRegex()).toTypedArray()
        if (params.size < 6) {
            failMessage = "Invalid server response"
            return false
        }
        userId = params[0].toLong()
        ssid = params[1]
        rank = params[2].toLong()
        score = params[3].toLong()
        accuracy = params[4].toInt() / 100000f
        this.username = params[5]
        avatarURL = "https://$HOSTNAME/avatars/$userId"
        profileBannerURL = "https://$HOSTNAME/banners/user/$userId"

        val bParams = Bundle()
        bParams.putString(FirebaseAnalytics.Param.METHOD, "ingame")
        GlobalManager.getInstance().getMainActivity()?.getAnalytics()?.logEvent(
            FirebaseAnalytics.Event.LOGIN,
            bParams
        )

        return true
    }

    @Throws(OnlineManagerException::class)
    internal fun tryToLogIn(): Boolean {
        if (!logIn(username, password)) {
            stayOnline = false
            return false
        }
        return true
    }

    @Throws(OnlineManagerException::class)
    fun startPlay(track: TrackInfo, hash: String) {
        Debug.i("Starting play...")
        playID = null
        val beatmap = track.beatmap ?: return

        val trackfile = File(track.filename)
        trackfile.parentFile?.name
        var osuID = trackfile.parentFile?.name
        Debug.i("osuid = $osuID")
        if (osuID != null && osuID.matches("^[0-9]+ .*".toRegex()))
            osuID = osuID.substring(0, osuID.indexOf(' '))
        else
            osuID = null

        val post = PostBuilder()
        post.addParam("userID", userId.toString())
        post.addParam("ssid", ssid)
        post.addParam("filename", trackfile.name)
        post.addParam("hash", hash)
        post.addParam("songTitle", beatmap.title ?: "")
        post.addParam("songArtist", beatmap.artist ?: "")
        post.addParam("songCreator", beatmap.creator ?: "")
        if (osuID != null)
            post.addParam("songID", osuID)

        val response = sendRequest(post, ENDPOINT + "submit.php")

        if (response == null) {
            if (failMessage == "Cannot log in" && stayOnline) {
                if (tryToLogIn()) {
                    startPlay(track, hash)
                }
            }
            return
        }

        if (response.size < 2) {
            failMessage = "Invalid server response"
            return
        }

        val resp = response[1].split("\\s+".toRegex()).toTypedArray()
        if (resp.size < 2) {
            failMessage = "Invalid server response"
            return
        }

        if (resp[0] != "1") {
            return
        }

        playID = resp[1]
        Debug.i("Getting play ID = $playID")
    }

    @Throws(OnlineManagerException::class)
    fun sendRecord(data: String): Boolean {
        if (playID == null || playID!!.isEmpty()) {
            failMessage = "I don't have play ID"
            return false
        }

        Debug.i("Sending record...")

        val post = PostBuilder()
        post.addParam("userID", userId.toString())
        post.addParam("playID", playID!!)
        post.addParam("data", data)

        val response = sendRequest(post, ENDPOINT + "submit.php")

        if (response == null) {
            return false
        }

        if (failMessage == "Invalid record data")
            return false

        if (response.size < 2) {
            failMessage = "Invalid server response"
            return false
        }

        val resp = response[1].split("\\s+".toRegex()).toTypedArray()
        if (resp.size < 4) {
            failMessage = "Invalid server response"
            return false
        }

        rank = resp[0].toLong()
        score = resp[1].toLong()
        accuracy = resp[2].toInt() / 100000f
        mapRank = resp[3].toInt()

        replayID = if (resp.size >= 5 && resp[4].isNotEmpty()) {
            resp[4].toIntOrNull() ?: 0
        } else {
            0
        }

        return true
    }

    @Throws(OnlineManagerException::class)
    fun getTop(trackFile: File, hash: String): ArrayList<String> {
        val post = PostBuilder()
        post.addParam("filename", trackFile.name)
        post.addParam("hash", hash)
        post.addParam("uid", userId.toString())

        val response = sendRequest(post, ENDPOINT + "getrank.php")

        if (response == null) {
            return ArrayList()
        }

        response.removeAt(0)

        return response
    }

    @Throws(OnlineManagerException::class)
    fun getBeatmapStatus(md5: String): RankedStatus? {
        val builder = Request.Builder().url(ENDPOINT + "v2/md5/$md5")
        val request = builder.build()

        try {
            val response = OnlineManager.client.newCall(request).execute()
            response.use {
                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!.string())
                    return RankedStatus.valueOf(json.optInt("ranked"))
                }
            }
        } catch (e: IOException) {
            Debug.e("getBeatmapStatus IOException " + e.message, e)
        } catch (e: JSONException) {
            Debug.e("getBeatmapStatus JSONException " + e.message, e)
        } catch (e: IllegalArgumentException) {
            Debug.e("getBeatmapStatus IllegalArgumentException " + e.message, e)
        }

        return null
    }

    fun loadAvatarToTextureManager(): Boolean {
        return loadAvatarToTextureManager(avatarURL)
    }

    fun loadAvatarToTextureManager(avatarURL: String): Boolean {
        if (avatarURL.isEmpty()) return false

        val filename = MD5Calculator.getStringMD5(avatarURL)
        Debug.i("Loading avatar from $avatarURL")
        Debug.i("filename = $filename")
        val picfile = File(Config.getCachePath(), filename)
        OnlineFileOperator.downloadFile(avatarURL, picfile.absolutePath, true)

        val bitmap = loadAvatarToBitmap(picfile)
        var imageWidth = 0
        var imageHeight = 0

        if (bitmap != null) {
            imageWidth = bitmap.width
            imageHeight = bitmap.height
        }

        if (imageWidth * imageHeight > 0) {
            ResourceManager.getInstance().loadHighQualityFile(filename, picfile)
            if (ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarURL) != null) {
                return true
            }
        }

        Debug.i("Success!")
        return false
    }

    private fun loadAvatarToBitmap(avatarFile: File): Bitmap? {
        if (!avatarFile.exists()) {
            return null
        }

        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(avatarFile.path, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            }
            return null
        } catch (e: NullPointerException) {
            return null
        }
    }

    fun loadBannerToTextureManager(bannerUrl: String): Boolean {
        if (bannerUrl.isEmpty()) return false

        val filename = MD5Calculator.getStringMD5(bannerUrl)
        Debug.i("Loading banner from $bannerUrl")
        Debug.i("filename = $filename")
        val picfile = File(Config.getCachePath(), filename)
        OnlineFileOperator.downloadFile(bannerUrl, picfile.absolutePath, true)

        val bitmap = loadBannerToBitmap(picfile)
        var imageWidth = 0
        var imageHeight = 0

        if (bitmap != null) {
            imageWidth = bitmap.width
            imageHeight = bitmap.height
        }

        if (imageWidth * imageHeight > 0) {
            ResourceManager.getInstance().loadHighQualityFile(filename, picfile)
            if (ResourceManager.getInstance().getBannerTextureIfLoaded(bannerUrl) != null) {
                return true
            }
        }

        Debug.i("Success!")
        return false
    }

    private fun loadBannerToBitmap(bannerFile: File): Bitmap? {
        if (!bannerFile.exists()) {
            return null
        }

        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(bannerFile.path, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            }
            return null
        } catch (e: NullPointerException) {
            return null
        }
    }

    fun sendReplay(filename: String) {
        Debug.i("Sending replay '$filename' for id = $replayID")
        OnlineFileOperator.sendFile(ENDPOINT + "upload.php", filename, replayID.toString())
    }

    @Throws(OnlineManagerException::class)
    fun getScorePack(playid: Int): String {
        val post = PostBuilder()
        post.addParam("playID", playid.toString())

        val response = sendRequest(post, ENDPOINT + "gettop.php")

        if (response == null || response.size < 2) {
            return ""
        }

        return response[1]
    }

    fun getFailMessage(): String = failMessage

    fun getRank(): Long = rank

    fun getScore(): Long = score

    fun getAccuracy(): Float = accuracy

    fun getAvatarURL(): String = avatarURL

    fun getProfileBannerURL(): String = profileBannerURL

    fun getUsername(): String = username

    fun getUserId(): Long = userId

    fun getPassword(): String = password

    fun getDeviceID(): String = deviceID

    var isStayOnline: Boolean
        get() = stayOnline
        set(value) { stayOnline = value }

    val isReadyToSend: Boolean get() = playID != null

    fun getMapRank(): Int = mapRank

    class OnlineManagerException : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)

        companion object {
            private const val serialVersionUID = -5703212596292949401L
        }
    }

    private fun escapeHTMLSpecialCharacters(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun addSlashes(str: String): String {
        return str
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\\", "\\\\")
    }

    @Throws(OnlineManagerException::class)
    private fun sendRequest(post: PostBuilder, url: String): ArrayList<String>? {
        val response: ArrayList<String>
        try {
            response = post.requestWithAttempts(url, 3)
        } catch (e: RequestException) {
            Debug.e(e.message, e)
            failMessage = "Cannot connect to server"
            throw OnlineManagerException("Cannot connect to server", e)
        }
        failMessage = ""

        if (response.size == 0 || response[0].isEmpty()) {
            failMessage = "Got empty response"
            Debug.i("Received empty response!")
            return null
        }

        if (response[0] != "SUCCESS") {
            Debug.i("sendRequest response code:  ${response[0]}")
            failMessage = if (response.size >= 2) {
                response[1]
            } else {
                "Unknown server error"
            }
            Debug.i("Received fail: $failMessage")
            return null
        }

        return response
    }

    companion object {
        const val HOSTNAME = "droid.neko.org.es"
        const val ENDPOINT = "https://$HOSTNAME/api/"
        const val updateEndpoint = "${ENDPOINT}update.php?lang="
        private const val ONLINE_VERSION = "8"

        val client = OkHttpClient()

        private var instance: OnlineManager? = null

        @JvmStatic
        fun getInstance(): OnlineManager {
            if (instance == null) {
                instance = OnlineManager()
            }
            return instance!!
        }

        @JvmStatic
        fun getReplayURL(playID: Int): String {
            return "https://$HOSTNAME/replays/$playID.odr"
        }
    }
}

package ru.nsu.ccfit.zuev.audio.serviceAudio

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reco1l.legacy.discord.DiscordRPC
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import java.io.File

class SongService : Service() {

    private var audioFunc: BassAudioFunc? = null
    var isGaming = false
        private set
    private lateinit var notify: NotifyPlayer

    override fun onBind(intent: Intent): IBinder {
        if (!::notify.isInitialized) {
            notify = NotifyPlayer()
            notify.load(this)
        }
        if (audioFunc == null) {
            audioFunc = BassAudioFunc()
            ContextCompat.registerReceiver(this, notify.receiver!!, notify.filter!!, ContextCompat.RECEIVER_NOT_EXPORTED)
            setReceiverStuff(notify.receiver!!, notify.filter!!)
        }
        return ReturnBindObject()
    }

    override fun onUnbind(intent: Intent): Boolean {
        println("Service unbind")
        hideNotification()
        NotificationManagerCompat.from(applicationContext).cancelAll()
        DiscordRPC.disconnect()
        exit()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(applicationContext).cancelAll()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        NotificationManagerCompat.from(applicationContext).cancelAll()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        NotificationManagerCompat.from(applicationContext).cancelAll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        println("onReBind")
    }

    fun preLoad(filePath: String, mode: PlayMode, isLoop: Boolean): Boolean {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) return false
            if (isLoop) {
                audioFunc!!.setLoop(isLoop)
            }
            return audioFunc!!.preLoad(filePath, mode)
        }
        return false
    }

    fun preLoad(filePath: String): Boolean = preLoad(filePath, PlayMode.MODE_NONE, false)

    fun preLoad(filePath: String, mode: PlayMode): Boolean = preLoad(filePath, mode, false)

    fun preLoad(filePath: String, speed: Float, enableNC: Boolean): Boolean {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) return false
            audioFunc!!.setLoop(false)
            return audioFunc!!.preLoad(filePath, speed, enableNC)
        }
        return false
    }

    fun preLoadWithLoop(filePath: String): Boolean = preLoad(filePath, PlayMode.MODE_NONE, true)

    fun play() {
        if (audioFunc == null) return
        audioFunc!!.play()
        notify.updateState()
    }

    fun pause() {
        if (audioFunc == null) return
        audioFunc!!.pause()
        notify.updateState()
    }

    fun stop(): Boolean {
        if (audioFunc == null) return false
        notify.updateState()
        return audioFunc!!.stop()
    }

    fun stopWithoutNotify() {
        audioFunc?.stop()
    }

    fun exit(): Boolean {
        Log.w("SongService", "Hei Service is on EXIT()")
        if (audioFunc == null) return false
        audioFunc!!.stop()
        audioFunc!!.unregisterReceiverBM()
        audioFunc!!.freeALL()
        unregisterReceiver(notify.receiver!!)
        stopSelf()
        return true
    }

    fun seekTo(time: Int) {
        if (audioFunc == null) return
        println(audioFunc!!.jump(time))
    }

    fun isGaming(): Boolean = isGaming

    fun setGaming(isGaming: Boolean) {
        audioFunc?.setGaming(isGaming)
        if (!isGaming) {
            hideNotification()
        }
        Log.w("Gaming Mode", "In Gamming mode :$isGaming")
        this.isGaming = isGaming
    }

    val status: Status get() = audioFunc?.getStatus() ?: Status.STOPPED

    val position: Int get() = audioFunc?.getPosition() ?: 0

    val length: Int get() = audioFunc?.getLength() ?: 0

    val spectrum: FloatArray get() = audioFunc?.getSpectrum() ?: FloatArray(0)

    val volume: Float get() = audioFunc?.getVolume() ?: 0f

    fun setVolume(volume: Float) {
        audioFunc?.setVolume(volume)
    }

    fun preLoadPreview(filePath: String): Boolean {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) return false
            return audioFunc!!.preLoadPreview(filePath)
        }
        return false
    }

    fun applySpeed(speed: Float, enableNC: Boolean) {
        audioFunc?.applySpeed(speed, enableNC)
    }

    fun showNotification() {
        if (isGaming) {
            Log.w("SongService", "NOT SHOW THE NOTIFY CUZ IS GAMING")
            return
        }

        audioFunc?.onGamePause()

        notify.show()
        notify.updateSong(GlobalManager.getInstance().mainScene?.beatmapInfo)
        notify.updateState()
    }

    fun hideNotification(): Boolean {
        if (notify.isShowing && audioFunc != null) {
            audioFunc!!.onGameResume()
        }
        return notify.hide()
    }

    fun setReceiverStuff(receiver: BroadcastReceiver, filter: IntentFilter) {
        audioFunc?.setReciverStuff(receiver, filter, this)
    }

    fun checkFileExist(path: String?): Boolean {
        if (path == null) return false
        if (path.trim().isEmpty()) return false
        val songFile = File(path)
        if (!songFile.exists()) return false
        return true
    }

    fun isRunningForeground(): Boolean = MainActivity.isActivityVisible()

    inner class ReturnBindObject : Binder() {
        fun getObject(): SongService = this@SongService
    }
}

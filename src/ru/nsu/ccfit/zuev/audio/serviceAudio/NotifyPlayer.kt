package ru.nsu.ccfit.zuev.audio.serviceAudio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.BeatmapInfo
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osuplus.R

class NotifyPlayer {

    private val mActivity = GlobalManager.getInstance().getMainActivity()
    private var context: Context? = null

    private val actionPrev = "player_previous"
    private val actionPlay = "player_play"
    private val actionNext = "Notify_next"
    private val actionClose = "player_close"

    private var prev: PendingIntent? = null
    private var next: PendingIntent? = null
    private var play: PendingIntent? = null
    private var close: PendingIntent? = null
    private var builder: NotificationCompat.Builder? = null
    private var manager: NotificationManagerCompat? = null
    var receiver: BroadcastReceiver? = null
        private set
    var filter: IntentFilter? = null
        private set

    private var mediaSession: MediaSessionCompat? = null
    private var notification: Notification? = null
    private var currentLargeIcon: Bitmap? = null

    var isShowing = false
    private var defaultIcon: Bitmap? = null

    fun load(service: SongService) {
        this.context = service.applicationContext

        manager = NotificationManagerCompat.from(context!!)
        mediaSession = MediaSessionCompat(context, "osu!droid")

        filter = IntentFilter().apply {
            addAction(actionPrev)
            addAction(actionPlay)
            addAction(actionNext)
            addAction(actionClose)
        }

        defaultIcon = BitmapFactory.decodeResource(mActivity!!.resources, R.drawable.osut)

        val pendingFlags = PendingIntent.FLAG_IMMUTABLE
        prev = PendingIntent.getBroadcast(context, 0, Intent(actionPrev), pendingFlags)
        next = PendingIntent.getBroadcast(context, 0, Intent(actionNext), pendingFlags)
        play = PendingIntent.getBroadcast(context, 0, Intent(actionPlay), pendingFlags)
        close = PendingIntent.getBroadcast(context, 0, Intent(actionClose), pendingFlags)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (service.isRunningForeground()) return

                when (intent.action) {
                    actionPlay -> {
                        if (service.status == Status.PLAYING) service.pause() else service.play()
                    }
                    actionPrev -> {
                        service.stop()
                        val prevBeatmap = LibraryManager.INSTANCE.getPrevBeatmap() ?: return
                        service.preLoad(prevBeatmap.getMusic()!!)
                        updateSong(prevBeatmap)
                        service.play()
                    }
                    actionNext -> {
                        service.stop()
                        val nextBeatmap = LibraryManager.INSTANCE.getNextBeatmap() ?: return
                        service.preLoad(nextBeatmap.getMusic()!!)
                        updateSong(nextBeatmap)
                        service.play()
                    }
                    actionClose -> {
                        service.stop()
                        GlobalManager.getInstance().mainScene?.exit()
                    }
                }
            }
        }
        create()
    }

    @SuppressLint("RestrictedApi")
    fun updateState() {
        if (!isShowing) return
        val isPlaying = GlobalManager.getInstance().songService!!.status == Status.PLAYING
        val drawable = if (isPlaying) R.drawable.v_pause else R.drawable.v_play

        builder!!.mActions.set(1, NotificationCompat.Action(drawable, actionPlay, play))
        manager!!.notify(NOTIFICATION_ID, builder!!.build())
    }

    fun updateSong(beatmap: BeatmapInfo?) {
        if (!isShowing || beatmap == null) return

        if (notification == null) create()

        var bitmap: Bitmap? = null
        var title = " "
        var artist = " "

        if (beatmap.artistUnicode != null && beatmap.titleUnicode != null) {
            title = beatmap.titleUnicode!!
            artist = beatmap.artistUnicode!!
        } else if (beatmap.artist != null && beatmap.title != null) {
            title = beatmap.title!!
            artist = beatmap.artist!!
        }

        if (beatmap.getTrack(0).background != null) {
            bitmap = BitmapFactory.decodeFile(beatmap.getTrack(0).background)
        }

        val iconToUse = bitmap ?: defaultIcon
        builder!!.setContentTitle(title)
        builder!!.setContentText(artist)
        builder!!.setLargeIcon(iconToUse)

        notification = builder!!.build()
        manager!!.notify(NOTIFICATION_ID, notification!!)

        if (currentLargeIcon != null && currentLargeIcon != defaultIcon && !currentLargeIcon!!.isRecycled) {
            currentLargeIcon!!.recycle()
        }
        currentLargeIcon = bitmap
    }

    fun show() {
        if (isShowing) return
        if (notification == null) create()

        manager!!.notify(NOTIFICATION_ID, notification!!)
        isShowing = true
    }

    fun hide(): Boolean {
        if (!isShowing) return false
        manager!!.cancel(NOTIFICATION_ID)
        isShowing = false
        return true
    }

    fun create() {
        val channelId = "ru.nsu.ccfit.zuev.audio"

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "Beatmap music player for osu!droid",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "osu!droid music player"
            manager!!.createNotificationChannel(channel)
        }

        val metadata = MediaMetadataCompat.Builder()
            .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
            .build()
        mediaSession!!.setMetadata(metadata)

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        builder = NotificationCompat.Builder(context!!, channelId)
            .setSmallIcon(R.drawable.notify_inso)
            .setLargeIcon(defaultIcon)
            .setContentTitle("title")
            .setContentText("artist")
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setShowWhen(false)
            .setContentIntent(openApp)
            .addAction(R.drawable.v_prev, actionPrev, prev)
            .addAction(R.drawable.v_play, actionPlay, play)
            .addAction(R.drawable.v_next, actionNext, next)
            .addAction(R.drawable.v_close, actionClose, close)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession!!.sessionToken)
            )

        notification = builder!!.build()
    }

    companion object {
        @JvmField
        var NOTIFICATION_ID = 1
    }
}

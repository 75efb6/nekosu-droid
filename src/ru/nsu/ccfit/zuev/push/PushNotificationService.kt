package ru.nsu.ccfit.zuev.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator

class PushNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val data = HashMap(remoteMessage.data)
            val channelId = "ru.nsu.ccfit.zuev.push"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            var title = data["title"]
            if (title == null) title = "osu!droid"
            var message = data["message"]
            if (message == null) message = "error"
            val url = data["url"]
            val imageUrl = data["imageUrl"]

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notify_inso)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)

            if (!imageUrl.isNullOrEmpty()) {
                val filePath = cacheDir.path + "/" + MD5Calculator.getStringMD5("osuplus$imageUrl")
                val downloaded = OnlineFileOperator.downloadFile(imageUrl, filePath)
                if (downloaded) {
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    notificationBuilder.setLargeIcon(bitmap)
                        .setStyle(NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null))
                }
            }

            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                notificationBuilder.setContentIntent(pendingIntent)
            } else {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                notificationBuilder.setContentIntent(pendingIntent)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId,
                    "osu!droid Push Notfications",
                    NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = "osu!droid Push Notfications"
                notificationManager.createNotificationChannel(channel)
            }

            val notificationId = notificationCount++
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    companion object {
        @JvmField
        var notificationCount = 0
    }
}

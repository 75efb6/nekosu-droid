package ru.nsu.ccfit.zuev.audio.serviceAudio

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import ru.nsu.ccfit.zuev.osu.AppException
import ru.nsu.ccfit.zuev.osu.GlobalManager

class SaveServiceObject : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler())
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                NotificationManagerCompat.from(applicationContext).cancelAll()
            }
        })
    }

    fun getSongService(): SongService? = songService

    fun setSongService(`object`: SongService?) {
        songService = `object`
        if (songService != null) {
            println("SongService Created!")
        } else {
            println("SongService is NULL")
        }
    }

    companion object {
        private var songService: SongService? = null

        @JvmStatic
        fun finishAllActivities() {
            if (GlobalManager.getInstance().getMainActivity() != null) {
                GlobalManager.getInstance().getMainActivity()?.finish()
            }
        }
    }
}

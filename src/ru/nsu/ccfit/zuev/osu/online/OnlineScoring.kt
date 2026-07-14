package ru.nsu.ccfit.zuev.osu.online

import android.content.Intent
import android.net.Uri
import com.google.android.material.snackbar.Snackbar
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.ui.multiplayer.LobbyScene
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.io.File
import java.util.ArrayList

class OnlineScoring private constructor() {

    private val onlineMutex = Any()
    internal var panel: OnlinePanel? = null
    internal var secondPanel: OnlinePanel? = null
    private var avatarLoaded = false
    private var bannerLoaded = false
    private val snackbar by lazy {
        Snackbar.make(
            GlobalManager.getInstance().getMainActivity()!!.window.decorView,
            "", 10000
        )
    }

    fun createPanel() {
        panel = OnlinePanel()
    }

    fun getPanel(): OnlinePanel? = panel

    fun createSecondPanel(): OnlinePanel? {
        if (!OnlineManager.getInstance().isStayOnline)
            return null
        secondPanel = OnlinePanel()
        secondPanel!!.setInfo()
        val avatarURL = OnlineManager.getInstance().avatarURL
        val bannerUrl = OnlineManager.getInstance().profileBannerURL
        secondPanel!!.setAvatar(if (avatarLoaded && avatarURL.isNotEmpty()) avatarURL else null)
        secondPanel!!.setBanner(if (bannerLoaded && bannerUrl.isNotEmpty()) bannerUrl else null)
        return secondPanel
    }

    fun getSecondPanel(): OnlinePanel? = secondPanel

    fun setPanelMessage(message: String, submessage: String) {
        panel?.setMessage(message, submessage)
        secondPanel?.setMessage(message, submessage)
    }

    fun updatePanels() {
        panel?.setInfo()
        secondPanel?.setInfo()
        LobbyScene.updateOnlinePanel()
        RoomScene.updateOnlinePanel()
    }

    fun updatePanelAvatars() {
        val avatarUrl = OnlineManager.getInstance().avatarURL
        val texname = if (avatarLoaded && avatarUrl.isNotEmpty()) avatarUrl else null
        panel?.setAvatar(texname)
        secondPanel?.setAvatar(texname)
        LobbyScene.updateOnlinePanel()
        RoomScene.updateOnlinePanel()
    }

    fun updatePanelBanner() {
        val bannerUrl = OnlineManager.getInstance().profileBannerURL
        val texname = if (bannerLoaded && bannerUrl.isNotEmpty()) bannerUrl else null
        panel?.setBanner(texname)
        secondPanel?.setBanner(texname)
        LobbyScene.updateOnlinePanel()
        RoomScene.updateOnlinePanel()
    }

    fun login() {
        if (!OnlineManager.getInstance().isStayOnline) return
        avatarLoaded = false

        Execution.async {
            synchronized(onlineMutex) {
                var success = false

                for (i in 0 until 3) {
                    setPanelMessage("Logging in...", "")

                    try {
                        success = OnlineManager.getInstance().logIn()
                    } catch (e: OnlineManager.OnlineManagerException) {
                        Debug.e("Login error: ${e.message}")
                        setPanelMessage("Login failed", "Retrying in 5 sec")
                        try {
                            Thread.sleep(3000)
                        } catch (e1: InterruptedException) {
                            break
                        }
                        continue
                    }
                    break
                }
                if (success) {
                    updatePanels()
                    OnlineManager.getInstance().isStayOnline = true
                    loadAvatar(true)
                    loadBanner(true)
                } else {
                    setPanelMessage("Cannot log in", OnlineManager.getInstance().failMessage)
                    OnlineManager.getInstance().isStayOnline = false

                    if (OnlineManager.getInstance().failMessage == "Cannot connect to server") {
                        snackbar.setText("Cannot connect to server. Please check the following article for troubleshooting.")

                        snackbar.setAction("Check") {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://neroyuki.github.io/osudroid-guide/help/login_fail")
                            )
                            GlobalManager.getInstance().getMainActivity()?.startActivity(intent)
                        }

                        snackbar.show()
                    }
                }
            }
        }
    }

    fun startPlay(track: TrackInfo, hash: String) {
        if (!OnlineManager.getInstance().isStayOnline) return

        Execution.async {
            synchronized(onlineMutex) {
                for (i in 0 until ATTEMPT_COUNT) {
                    try {
                        OnlineManager.getInstance().startPlay(track, hash)
                    } catch (e: OnlineManager.OnlineManagerException) {
                        Debug.e("Login error: ${e.message}")
                        continue
                    }
                    break
                }

                if (OnlineManager.getInstance().failMessage.isNotEmpty()) {
                    ToastLogger.showText(OnlineManager.getInstance().failMessage, true)
                }
            }
        }
    }

    fun sendRecord(record: StatisticV2, panel: SendingPanel, replay: String) {
        if (!OnlineManager.getInstance().isStayOnline || !OnlineManager.getInstance().isReadyToSend)
            return

        Debug.i("Sending score")

        val recordData = record.compile()

        Execution.async {
            var success = false
            synchronized(onlineMutex) {
                for (i in 0 until ATTEMPT_COUNT) {
                    if (!record.isScoreValid) {
                        Debug.e("Detected illegal actions.")
                        break
                    }

                    try {
                        success = OnlineManager.getInstance().sendRecord(recordData)
                    } catch (e: OnlineManager.OnlineManagerException) {
                        Debug.e("Login error: ${e.message}")
                        success = false
                    }

                    if (OnlineManager.getInstance().failMessage.isNotEmpty()) {
                        ToastLogger.showText(OnlineManager.getInstance().failMessage, true)
                        if (OnlineManager.getInstance().failMessage == "Invalid record data")
                            break
                    } else if (success) {
                        OnlineManager.getInstance().sendReplay(replay)
                        updatePanels()
                        val mgr = OnlineManager.getInstance()
                        panel.show(mgr.mapRank.toLong(), mgr.score, mgr.rank, mgr.accuracy)
                        break
                    }

                    try {
                        Thread.sleep(5000)
                    } catch (ignored: InterruptedException) {
                    }
                }

                if (!success) {
                    panel.setFail()
                }
            }
        }
    }

    fun getTop(trackFile: File, hash: String): ArrayList<String> {
        synchronized(onlineMutex) {
            return try {
                OnlineManager.getInstance().getTop(trackFile, hash)
            } catch (e: OnlineManager.OnlineManagerException) {
                Debug.e("Cannot load scores ${e.message}")
                ArrayList()
            }
        }
    }

    fun loadAvatar(both: Boolean) {
        if (!OnlineManager.getInstance().isStayOnline) return
        val avatarUrl = OnlineManager.getInstance().avatarURL
        if (avatarUrl.isEmpty()) return

        Execution.async {
            synchronized(onlineMutex) {
                avatarLoaded = OnlineManager.getInstance().loadAvatarToTextureManager()
                if (both)
                    updatePanelAvatars()
                else
                    secondPanel?.setAvatar(if (avatarLoaded) avatarUrl else null)
            }
        }
    }

    fun loadBanner(both: Boolean) {
        if (!OnlineManager.getInstance().isStayOnline) return
        val bannerUrl = OnlineManager.getInstance().profileBannerURL
        if (bannerUrl.isEmpty()) return

        Execution.async {
            synchronized(onlineMutex) {
                bannerLoaded = OnlineManager.getInstance().loadBannerToTextureManager(bannerUrl)
                if (both)
                    updatePanelBanner()
                else
                    secondPanel?.setBanner(if (bannerLoaded) bannerUrl else null)
            }
        }
    }

    fun isAvatarLoaded(): Boolean = avatarLoaded

    fun isBannerLoaded(): Boolean = avatarLoaded

    companion object {
        private const val ATTEMPT_COUNT = 5
        private var instance: OnlineScoring? = null

        @JvmStatic
        fun getInstance(): OnlineScoring {
            if (instance == null)
                instance = OnlineScoring()
            return instance!!
        }
    }
}

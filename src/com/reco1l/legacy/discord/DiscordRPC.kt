package com.reco1l.legacy.discord

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.discord.socialsdk.DiscordSocialSdkInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import kotlin.time.Duration.Companion.milliseconds

object DiscordRPC {

    private const val TAG = "DiscordRPC"
    private const val APP_ID = 1518735555687612466L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var callbackJob: Job? = null
    private val callbackDelay = 16.milliseconds
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmField
    @Volatile
    var isConnected = false

    @Volatile
    var isPendingAuthorization = false
        private set

    val isConnecting
        get() = !isConnected && callbackJob?.isActive == true

    private var connectionStateListener: (() -> Unit)? = null
    private var initialized = false

    @JvmStatic
    fun init(activity: Activity) {
        DiscordSocialSdkInit.setEngineActivity(activity)
        DiscordNative.nativeCreate()
        initialized = true
        playStartTimestamp = System.currentTimeMillis() / 1000
        Log.d(TAG, "SDK created")
    }

    @JvmStatic
    fun connect(activity: Activity) {
        if (!initialized) {
            init(activity)
        }

        if (isPendingAuthorization) {
            Log.d(TAG, "connect() ignored: authorization already in progress.")
            return
        }

        if (isConnected) {
            Log.d(TAG, "Re-authorizing: disconnecting and clearing saved token.")
            disconnect()
            clearSavedToken()
        }

        if (callbackJob?.isActive == true) {
            Log.d(TAG, "connect() ignored: connection already in progress.")
            return
        }

        val savedToken = Config.getDiscordToken()
        if (!savedToken.isNullOrEmpty()) {
            Log.d(TAG, "Restoring saved token, skipping auth flow.")
            DiscordNative.nativeProvideTokens(savedToken)
            startCallbackLoop()
            return
        }

        Log.d(TAG, "Starting authorization flow.")
        DiscordNative.nativeAuthorize(APP_ID)
        isPendingAuthorization = true
        startCallbackLoop()
    }

    @JvmStatic
    fun setConnectionStateListener(listener: (() -> Unit)?) {
        connectionStateListener = listener
    }

    @JvmStatic
    fun restore(activity: Activity) {
        if (!Config.isDiscordRichPresence()) return

        val savedToken = Config.getDiscordToken()
        if (savedToken.isNullOrEmpty()) return

        connect(activity)
    }

    @JvmStatic
    fun disconnect() {
        if (!initialized) return

        clearActivity()
        stopCallbackLoop()
        isConnected = false
        Log.d(TAG, "Disconnected from Discord.")
    }

    @JvmStatic
    fun clear() {
        if (!initialized) return

        clearActivity()
        stopCallbackLoop()
        isConnected = false
        clearSavedToken()
        DiscordNative.nativeDestroy()
        initialized = false
        Log.d(TAG, "Destroyed.")
    }

    private fun saveToken() {
        val token = Config.getDiscordToken() ?: return
        try {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                DiscordSocialSdkInit.getEngineActivity()
            )
            prefs.edit().putString("discordToken", token).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save token", e)
        }
    }

    private fun clearSavedToken() {
        Config.setDiscordToken(null)
        try {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                DiscordSocialSdkInit.getEngineActivity()
            )
            prefs.edit().remove("discordToken").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear saved token", e)
        }
    }

    private fun startCallbackLoop() {
        callbackJob?.cancel()
        var wasReady = false

        callbackJob = scope.launch {
            while (isActive) {
                DiscordNative.nativeRunCallbacks()

                if (DiscordNative.nativeHasAuthorizationCode()) {
                    Log.d(TAG, "Authorization code obtained, exchanging token...")
                    launch { exchangeToken() }
                }

                if (DiscordNative.nativeHasAuthorizationFailed()) {
                    DiscordNative.nativeClearAuthorizationFailed()
                    isPendingAuthorization = false
                    Log.w(TAG, "Authorization cancelled or rejected by user.")
                    stopCallbackLoop()
                    mainHandler.post { connectionStateListener?.invoke() }
                    return@launch
                }

                if (DiscordNative.nativeHasToken()) {
                    val token = DiscordNative.nativeGetAccessToken()
                    DiscordNative.nativeClearToken()

                    Config.setDiscordToken(token)
                    saveToken()

                    DiscordNative.nativeProvideTokens(token)

                    isPendingAuthorization = false
                    Log.d(TAG, "Token provided, connecting...")
                }

                val isNowReady = DiscordNative.nativeIsReady()

                if (isNowReady && !wasReady) {
                    isConnected = true
                    Log.d(TAG, "Discord ready.")
                    mainHandler.post { connectionStateListener?.invoke() }
                    refreshActivity()
                } else if (!isNowReady && wasReady) {
                    isConnected = false
                    Log.d(TAG, "Discord disconnected.")
                    mainHandler.post { connectionStateListener?.invoke() }
                }

                wasReady = isNowReady

                delay(callbackDelay)
            }
        }
    }

    private fun exchangeToken() {
        DiscordNative.nativeExchangeToken(APP_ID)
    }

    @JvmStatic
    fun onActivityResume() {
        if (isPendingAuthorization && !DiscordNative.nativeHasAuthorizationCode()) {
            Log.d(TAG, "onActivityResume: aborting pending authorization.")
            DiscordNative.nativeAbortAuthorize()
        }
    }

    private fun stopCallbackLoop() {
        callbackJob?.cancel()
        callbackJob = null
    }

    private fun getOnlineUsername(): String {
        return if (OnlineManager.getInstance().isStayOnline) {
            OnlineManager.getInstance().username
        } else {
            "User is offline."
        }
    }

    private var playStartTimestamp: Long = 0

    private fun setActivity(
        details: String,
        state: String = "",
        partySize: Int = 0,
        partyMax: Int = 0
    ) {
        if (!Config.isDiscordRichPresence() || !isConnected) return

        val online = OnlineManager.getInstance()
        val username = online.username
        val largeText = if (username.isNotEmpty()) username else "User is offline."

        DiscordNative.nativeUpdateRichPresence(
            details = details,
            state = state,
            partySize = partySize,
            partyMax = partyMax,
            startTimestamp = playStartTimestamp,
            largeText = largeText,
            largeImage = "large_image"
        )
    }

    private fun refreshActivity() {
        setActivity(details = "In main menu...")
    }

    @JvmStatic
    fun updateForMainMenu() {
        setActivity(details = "In main menu...")
    }

    @JvmStatic
    fun updateForSongSelection() {
        setActivity(details = "Selecting a song...")
    }

    @JvmStatic
    fun updateForMultiLobby() {
        setActivity(details = "Searching for a room...")
    }

    @JvmStatic
    fun updateForMultiRoom(roomName: String, playerCount: Int, maxPlayers: Int) {
        setActivity(
            details = "Inside a room...",
            state = "$roomName",
            partySize = playerCount,
            partyMax = maxPlayers
        )
    }

    @JvmStatic
    fun updateForPlaying(
        isMultiplayer: Boolean,
        title: String,
        artist: String,
        difficulty: String
    ) {
        val details = if (isMultiplayer) {
            "Playing a beatmap in a multiplayer game..."
        } else {
            "Playing a beatmap..."
        }

        var partySize = 0
        var partyMax = 0

        if (isMultiplayer) {
            val room = com.reco1l.legacy.Multiplayer.room
            if (room != null) {
                partySize = room.activePlayers.size
                partyMax = room.maxPlayers
            }
        }

        setActivity(
            details = details,
            state = "$artist - $title [$difficulty]",
            partySize = partySize,
            partyMax = partyMax
        )
    }

    @JvmStatic
    fun updateForReplay(artist: String, title: String, difficulty: String) {
        setActivity(
            details = "Viewing a replay...",
            state = "$artist - $title [$difficulty]"
        )
    }

    @JvmStatic
    fun updateForResults() {
        updateForResults(false)
    }

    @JvmStatic
    fun updateForResults(isMultiplayer: Boolean) {
        var partySize = 0
        var partyMax = 0

        if (isMultiplayer) {
            val room = com.reco1l.legacy.Multiplayer.room
            if (room != null) {
                partySize = room.activePlayers.size
                partyMax = room.maxPlayers
            }
        }

        val details = if (isMultiplayer) {
            "On results screen in a multiplayer game..."
        } else {
            "On results screen..."
        }

        setActivity(
            details = details,
            partySize = partySize,
            partyMax = partyMax
        )
    }

    @JvmStatic
    fun clearActivity() {
        if (!isConnected) return
        DiscordNative.nativeClearRichPresence()
    }
}

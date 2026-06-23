package com.reco1l.legacy.discord

object DiscordNative {

    init {
        System.loadLibrary("discord_bridge")
    }

    @JvmStatic
    external fun nativeCreate()

    @JvmStatic
    external fun nativeIsReady(): Boolean

    @JvmStatic
    external fun nativeAuthorize(clientId: Long)

    @JvmStatic
    external fun nativeHasAuthorizationCode(): Boolean

    @JvmStatic
    external fun nativeGetAuthorizationCode(): String

    @JvmStatic
    external fun nativeGetVerifier(): String

    @JvmStatic
    external fun nativeGetRedirectUri(): String

    @JvmStatic
    external fun nativeClearAuthorizationCode()

    @JvmStatic
    external fun nativeHasAuthorizationFailed(): Boolean

    @JvmStatic
    external fun nativeClearAuthorizationFailed()

    @JvmStatic
    external fun nativeAbortAuthorize()

    @JvmStatic
    external fun nativeExchangeToken(clientId: Long)

    @JvmStatic
    external fun nativeHasToken(): Boolean

    @JvmStatic
    external fun nativeGetAccessToken(): String

    @JvmStatic
    external fun nativeClearToken()

    @JvmStatic
    external fun nativeProvideTokens(accessToken: String)

    @JvmStatic
    external fun nativeRunCallbacks()

    @JvmStatic
    external fun nativeUpdateRichPresence(
        details: String,
        state: String,
        partySize: Int,
        partyMax: Int,
        startTimestamp: Long,
        largeText: String,
        largeImage: String
    )

    @JvmStatic
    external fun nativeClearRichPresence()

    @JvmStatic
    external fun nativeDisconnect()

    @JvmStatic
    external fun nativeDestroy()
}

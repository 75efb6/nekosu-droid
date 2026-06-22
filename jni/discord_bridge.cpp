#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <memory>
#include <string>
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#define TAG "DiscordJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<discordpp::Client> g_client;

static std::atomic g_isReady{false};
static std::atomic g_authorizationFailed{false};
static std::atomic g_hasAuthorizationCode{false};
static std::atomic g_hasToken{false};

static std::string g_pendingCode;
static std::string g_pendingVerifier;
static std::string g_pendingRedirectUri;
static std::string g_pendingAccessToken;

static std::string clampLength(std::string str) {
    if (str.size() <= 128) return str;

    constexpr size_t ellipsis_bytes = 3;
    size_t pos = 128 - ellipsis_bytes;
    while (pos > 0 && (static_cast<unsigned char>(str[pos]) & 0xC0) == 0x80) {
        --pos;
    }

    str.resize(pos);
    str += "\xe2\x80\xa6";
    return str;
}

static void handleTokenExchange(std::string accessToken) {
    g_client->UpdateToken(
        discordpp::AuthorizationTokenType::Bearer, std::move(accessToken),
        [](const discordpp::ClientResult ur) {
            if (!ur.Successful()) {
                LOGE("UpdateToken failed: %s", ur.Error().c_str());
                return;
            }

            LOGI("Token updated, connecting");
            g_client->Connect();
        });
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeCreate(JNIEnv*, jclass) {
    if (g_client) {
        return;
    }

    g_client = std::make_unique<discordpp::Client>();

    g_client->SetStatusChangedCallback(
        [](discordpp::Client::Status status, const discordpp::Client::Error error, const int32_t errorDetail) {
            const bool ready = status == discordpp::Client::Status::Ready;
            g_isReady.store(ready);

            if (status == discordpp::Client::Status::Disconnected && errorDetail != 0) {
                LOGE("Disconnected: %s (code %d)", discordpp::Client::ErrorToString(error).c_str(), errorDetail);
            } else {
                LOGI("Status changed to %d, ready=%s", static_cast<int>(status), ready ? "true" : "false");
            }
        });

    LOGI("Client created");
}

JNIEXPORT jboolean JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeIsReady() {
    return g_isReady.load();
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeAuthorize(JNIEnv*, jclass, const jlong clientId) {
    if (!g_client) {
        LOGE("authorize: client not created");
        return;
    }

    auto verifier = g_client->CreateAuthorizationCodeVerifier();
    discordpp::AuthorizationArgs args{};
    args.SetClientId(static_cast<uint64_t>(clientId));
    args.SetScopes(discordpp::Client::GetDefaultPresenceScopes());
    args.SetCodeChallenge(verifier.Challenge());

    g_client->Authorize(
        std::move(args),
        [v = std::move(verifier)](
            const discordpp::ClientResult result, const std::string &code, const std::string &redirectUri) mutable {
            if (!result.Successful()) {
                LOGE("Authorize failed: %s", result.Error().c_str());
                g_authorizationFailed.store(true);
                return;
            }

            g_pendingCode = code;
            g_pendingVerifier = v.Verifier();
            g_pendingRedirectUri = redirectUri;
            g_hasAuthorizationCode.store(true);
            LOGI("Authorization code obtained");
        });
}

JNIEXPORT jboolean JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeHasAuthorizationCode() {
    return g_hasAuthorizationCode.load();
}

JNIEXPORT jstring JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeGetAuthorizationCode(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingCode.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeGetVerifier(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingVerifier.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeGetRedirectUri(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingRedirectUri.c_str());
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeClearAuthorizationCode() {
    g_hasAuthorizationCode.store(false);
    g_pendingCode.clear();
    g_pendingVerifier.clear();
    g_pendingRedirectUri.clear();
}

JNIEXPORT jboolean JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeHasAuthorizationFailed() {
    return g_authorizationFailed.load();
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeClearAuthorizationFailed() {
    g_authorizationFailed.store(false);
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeAbortAuthorize(JNIEnv*, jclass) {
    if (g_client) {
        g_client->AbortAuthorize();
    }
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeExchangeToken(
        JNIEnv* env, jclass, const jlong clientId) {
    if (!g_client || g_pendingCode.empty()) {
        LOGE("exchangeToken: no pending authorization code");
        return;
    }

    g_client->GetToken(
        static_cast<uint64_t>(clientId),
        g_pendingCode,
        g_pendingVerifier,
        g_pendingRedirectUri,
        [](const discordpp::ClientResult result,
           const std::string& accessToken,
           const std::string& refreshToken,
           const discordpp::AuthorizationTokenType tokenType,
           const int32_t expiresIn,
           const std::string& scopes) {
            if (!result.Successful()) {
                LOGE("GetToken failed: %s", result.Error().c_str());
                return;
            }

            LOGI("Token exchange successful");
            g_pendingAccessToken = accessToken;
            g_hasToken.store(true);
        });

    g_hasAuthorizationCode.store(false);
    g_pendingCode.clear();
    g_pendingVerifier.clear();
    g_pendingRedirectUri.clear();
}

JNIEXPORT jboolean JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeHasToken() {
    return g_hasToken.load();
}

JNIEXPORT jstring JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeGetAccessToken(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_pendingAccessToken.c_str());
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeClearToken() {
    g_hasToken.store(false);
    g_pendingAccessToken.clear();
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeProvideTokens(JNIEnv* env, jclass, const jstring jAccessToken) {
    if (!g_client) {
        LOGE("provideTokens: client not created");
        return;
    }

    const char* s = env->GetStringUTFChars(jAccessToken, nullptr);
    std::string accessToken(s);
    env->ReleaseStringUTFChars(jAccessToken, s);

    handleTokenExchange(std::move(accessToken));
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeRunCallbacks(JNIEnv*, jclass) {
    discordpp::RunCallbacks();
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeUpdateRichPresence(
        JNIEnv* env, jclass,
        const jstring jDetails, const jstring jState,
        const jint partySize, const jint partyMax, const jlong startTimestamp,
        const jstring jLargeText, const jstring jLargeImage) {
    if (!g_client || !g_isReady.load()) {
        return;
    }

    discordpp::Activity activity{};
    activity.SetType(discordpp::ActivityTypes::Playing);

    if (jDetails) {
        const char* s = env->GetStringUTFChars(jDetails, nullptr);
        std::string details(s);
        env->ReleaseStringUTFChars(jDetails, s);

        if (details.size() >= 2) {
            activity.SetDetails(clampLength(std::move(details)));
        }
    }

    if (jState) {
        const char* s = env->GetStringUTFChars(jState, nullptr);
        std::string state(s);
        env->ReleaseStringUTFChars(jState, s);
        if (state.size() >= 2) {
            activity.SetState(clampLength(std::move(state)));
        }
    }

    if (partySize > 0) {
        discordpp::ActivityParty party{};
        party.SetId("nekosu");
        party.SetCurrentSize(partySize);
        party.SetMaxSize(partyMax);
        activity.SetParty(std::move(party));
    }

    if (startTimestamp > 0) {
        discordpp::ActivityTimestamps ts{};
        ts.SetStart(static_cast<uint64_t>(startTimestamp));
        activity.SetTimestamps(std::move(ts));
    }

    {
        discordpp::ActivityAssets assets{};
        if (jLargeText) {
            const char* s = env->GetStringUTFChars(jLargeText, nullptr);
            std::string largeText(s);
            env->ReleaseStringUTFChars(jLargeText, s);

            if (largeText.size() >= 2) {
                assets.SetLargeText(clampLength(std::move(largeText)));
            }
        }

        activity.SetAssets(std::move(assets));
    }

    g_client->UpdateRichPresence(std::move(activity), [](const discordpp::ClientResult r) {
        if (!r.Successful()) {
            LOGE("UpdateRichPresence failed: %s", r.Error().c_str());
        }
    });
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeClearRichPresence(JNIEnv*, jclass) {
    if (!g_client || !g_isReady.load()) {
        return;
    }

    g_client->ClearRichPresence();
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeDisconnect(JNIEnv*, jclass) {
    if (!g_client) return;
    g_client->Disconnect();
    g_isReady.store(false);
    LOGI("Disconnected");
}

JNIEXPORT void JNICALL
Java_com_reco1l_legacy_discord_DiscordNative_nativeDestroy(JNIEnv*, jclass) {
    g_client.reset();
    g_isReady.store(false);
    g_authorizationFailed.store(false);
    g_hasAuthorizationCode.store(false);
    g_hasToken.store(false);
    g_pendingCode.clear();
    g_pendingVerifier.clear();
    g_pendingRedirectUri.clear();
    g_pendingAccessToken.clear();
    LOGI("Client destroyed");
}

}  // extern "C"

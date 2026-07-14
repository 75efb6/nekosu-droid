package ru.nsu.ccfit.zuev.audio.serviceAudio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import java.nio.ByteBuffer

class BassAudioFunc {

    private var channel = 0
    private var mode: PlayMode? = null
    private var skipPosition: Long = 0
    private var buffer: ByteBuffer? = null
    private var playflag = BASS.BASS_STREAM_PRESCAN
    private var isGaming = false
    private var currentFilePath: String? = null
    private var baseFreq = 44100f
    private var receiver: BroadcastReceiver? = null
    private var broadcastManager: LocalBroadcastManager? = null

    /**
     * Whether the game is currently on focus.
     */
    private var onFocus = false

    /**
     * The playback buffer length that is used when the game is on focus, in seconds.
     * This is pretty low to achieve the smallest latency possible without introducing CPU overhead.
     */
    private val onFocusBufferLength = 0.02f

    /**
     * The playback buffer length that is used when the game is not on focus, in seconds.
     * This is a lot higher than the value used in [onFocusBufferLength] to reduce CPU usage.
     */
    private val offFocusBufferLength = 0.5f

    private fun applyTempoOptions() {
        BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_OPTION_SEQUENCE_MS, 20f)
        BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_OPTION_SEEKWINDOW_MS, 10f)
        BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_OPTION_OVERLAP_MS, 4f)
    }

    fun onGameResume() {
        onFocus = true

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5)

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, onFocusBufferLength)
        }
    }

    fun onGamePause() {
        onFocus = false

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 100)

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, offFocusBufferLength)
        }
    }

    fun pause(): Boolean = BASS.BASS_ChannelPause(channel)

    fun resume(): Boolean {
        setEndSync()

        if (BASS.BASS_ChannelPlay(channel, false)) {
            setVolume(Config.getBgmVolume())
            return true
        }
        return false
    }

    fun preLoad(filePath: String, mode: PlayMode): Boolean {
        Log.w("BassAudioFunc", "preLoad File: $filePath")
        val fx = BASS.BASS_CHANNELINFO()
        doClear()
        this.mode = mode
        this.currentFilePath = filePath
        when (mode) {
            PlayMode.MODE_NONE -> {
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, playflag)
            }
            PlayMode.MODE_HT -> {
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE)
                applyTempoOptions()
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, -25.0f)
            }
            PlayMode.MODE_DT -> {
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE)
                applyTempoOptions()
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 50.0f)
            }
            PlayMode.MODE_NC -> {
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE)
                applyTempoOptions()

                BASS.BASS_ChannelGetInfo(channel, fx)
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (fx.freq * 1.5).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0.0f)
            }
            PlayMode.MODE_SU -> {
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE)
                applyTempoOptions()
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 25.0f)
            }
            else -> {}
        }

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, if (onFocus) onFocusBufferLength else offFocusBufferLength)
        }

        return channel != 0
    }

    fun preLoad(filePath: String, speed: Float, enableNC: Boolean): Boolean {
        Log.w("BassAudioFunc", "preLoad File: $filePath")
        val fx = BASS.BASS_CHANNELINFO()
        doClear()
        this.mode = PlayMode.MODE_SC
        this.currentFilePath = filePath
        channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
        channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE)
        applyTempoOptions()
        if (enableNC) {
            BASS.BASS_ChannelGetInfo(channel, fx)
            if (speed > 1.5f) {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (fx.freq * 1.5f).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 1.5f - 1.0f) * 100)
            } else if (speed < 0.75f) {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (fx.freq * 0.75f).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 0.75f - 1.0f) * 100)
            } else {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (fx.freq * speed).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0.0f)
            }
        } else {
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed - 1.0f) * 100)
        }

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, if (onFocus) onFocusBufferLength else offFocusBufferLength)
        }

        return channel != 0
    }

    fun play(): Boolean {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PAUSED) {
            return resume()
        } else if (channel != 0) {
            setEndSync()
            if (BASS.BASS_ChannelPlay(channel, true)) {
                setVolume(Config.getBgmVolume())
                return true
            }
        }
        return false
    }

    fun stop(): Boolean {
        if (channel != 0) {
            BASS.BASS_ChannelStop(channel)
            return BASS.BASS_StreamFree(channel)
        }
        return false
    }

    fun jump(ms: Int): Boolean {
        if (channel != 0 && ms > 0) {
            skipPosition = BASS.BASS_ChannelSeconds2Bytes(channel, ms / 1000.0)
            if (mode == PlayMode.MODE_NONE || mode == PlayMode.MODE_PREVIEW) {
                return BASS.BASS_ChannelSetPosition(channel, skipPosition, BASS.BASS_POS_BYTE)
            } else {
                return BASS.BASS_ChannelSetPosition(channel, skipPosition, BASS.BASS_POS_DECODE)
            }
        }
        return false
    }

    fun getStatus(): Status {
        if (channel == 0) return Status.STOPPED

        return when (BASS.BASS_ChannelIsActive(channel)) {
            BASS.BASS_ACTIVE_STOPPED -> Status.STOPPED
            BASS.BASS_ACTIVE_PAUSED -> Status.PAUSED
            BASS.BASS_ACTIVE_PLAYING -> Status.PLAYING
            else -> Status.STALLED
        }
    }

    fun getPosition(): Int {
        if (channel != 0) {
            val pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
            if (pos != -1L) {
                return (BASS.BASS_ChannelBytes2Seconds(channel, pos) * 1000).toInt()
            }
        }
        return 0
    }

    fun getLength(): Int {
        if (channel != 0) {
            val length = BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE)
            if (length != -1L) {
                return (BASS.BASS_ChannelBytes2Seconds(channel, length) * 1000).toInt()
            }
        }
        return 0
    }

    fun getSpectrum(): FloatArray? {
        if (BASS.BASS_ChannelIsActive(channel) != BASS.BASS_ACTIVE_PLAYING) {
            return null
        }
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(WINDOW_FFT shl 1)
            buffer!!.order(null)
        }
        BASS.BASS_ChannelGetData(channel, buffer, BASS.BASS_DATA_FFT1024)
        val resSize = WINDOW_FFT shr 1
        val spectrum = FloatArray(resSize)
        buffer!!.asFloatBuffer().get(spectrum)
        return spectrum
    }

    fun preLoadPreview(filePath: String): Boolean {
        Log.w("BassAudioFunc", "preLoadPreview File: $filePath")
        doClear()
        this.mode = PlayMode.MODE_PREVIEW
        this.currentFilePath = filePath
        val decodeChannel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
        channel = BASS_FX.BASS_FX_TempoCreate(decodeChannel, BASS.BASS_STREAM_AUTOFREE)
        applyTempoOptions()
        if (channel != 0) {
            val info = BASS.BASS_CHANNELINFO()
            BASS.BASS_ChannelGetInfo(channel, info)
            baseFreq = info.freq.toFloat()
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, if (onFocus) onFocusBufferLength else offFocusBufferLength)
        }
        return channel != 0
    }

    fun applySpeed(speed: Float, enableNC: Boolean) {
        if (channel == 0) return
        if (enableNC) {
            if (speed > 1.5f) {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (baseFreq * 1.5f).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 1.5f - 1.0f) * 100)
            } else if (speed < 0.75f) {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (baseFreq * 0.75f).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 0.75f - 1.0f) * 100)
            } else {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (baseFreq * speed).toInt().toFloat())
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0.0f)
            }
        } else {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, baseFreq)
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed - 1.0f) * 100)
        }
    }

    private fun doClear() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelStop(channel)
        }
        BASS.BASS_StreamFree(channel)
        skipPosition = 0
    }

    fun setLoop(isLoop: Boolean) {
        if (isLoop) {
            this.playflag = BASS.BASS_SAMPLE_LOOP or BASS.BASS_STREAM_PRESCAN
        } else {
            this.playflag = BASS.BASS_STREAM_PRESCAN
        }
    }

    fun getVolume(): Float {
        val volume = BASS.FloatValue()
        if (channel != 0) {
            BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume)
        }
        return volume.value
    }

    fun setVolume(volume: Float) {
        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume)
        }
    }

    fun setGaming(isGaming: Boolean) {
        println("Audio Service Running In Game: $isGaming")
        this.isGaming = isGaming
    }

    fun setReciverStuff(receiver: BroadcastReceiver, filter: IntentFilter, context: Context) {
        this.receiver = receiver
        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context)
            broadcastManager!!.registerReceiver(receiver, filter)
        }
    }

    fun unregisterReceiverBM() {
        if (broadcastManager != null) broadcastManager!!.unregisterReceiver(receiver!!)
    }

    fun freeALL() {
        BASS.BASS_Free()
    }

    private fun setEndSync() {
        BASS.BASS_ChannelSetSync(channel, BASS.BASS_SYNC_END, 0, { _, _, _, _ ->
            if (!isGaming) {
                broadcastManager?.sendBroadcast(Intent("Notify_next"))
            } else {
                stop()
            }
        }, 0)
    }

    companion object {
        const val WINDOW_FFT = 1024
    }
}

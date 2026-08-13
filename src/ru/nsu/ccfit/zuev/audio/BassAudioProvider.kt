package ru.nsu.ccfit.zuev.audio

import android.content.res.AssetManager
import android.util.Log
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import java.nio.ByteBuffer

class BassAudioProvider {

    private var channel = 0
    private val freq = BASS.FloatValue()
    private var fileFlag = 0
    private var decoder = 0
    private var multiplier = 0

    private var buffer: ByteBuffer? = null
    private var spectrumBuffer: FloatArray? = null

    init {
        freq.value = 1.0f

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 20)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5)

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 5)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 5)

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_NONSTOP, 1)

        BASS.BASS_Init(-1, DEFAULT_FREQUENCY, BASS.BASS_DEVICE_LATENCY)

        Log.i("BASS-Config", "BASS initialized")
        Log.i("BASS-Config", "Update period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_UPDATEPERIOD))
        Log.i("BASS-Config", "Device period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_PERIOD))
        Log.i("BASS-Config", "Device buffer length:   " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_BUFFER))
        Log.i("BASS-Config", "Playback buffer length: " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER))
        Log.i("BASS-Config", "Device nonstop:         " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_NONSTOP))
    }

    fun prepare(fileName: String): Boolean {
        free()
        if (fileName.isNotEmpty()) {
            channel = BASS.BASS_StreamCreateFile(fileName, 0, 0, fileFlag)
            if (decoder > 0) {
                channel = BASS_FX.BASS_FX_TempoCreate(channel, 0)
                BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq)

                if (decoder == DECODER_DOUBLE_TIME) {
                    val targetTempo = multiplier - 100.0f
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value)
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, targetTempo)
                } else if (decoder == DECODER_NIGHT_CORE) {
                    val targetFreq = multiplier / 100.0f
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value * targetFreq)
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 1.0f)
                }
            }
        }
        return channel != 0
    }

    fun prepare(manager: AssetManager, assetName: String): Boolean {
        free()
        if (assetName.isNotEmpty()) {
            val asset = BASS.Asset(manager, assetName)
            channel = BASS.BASS_StreamCreateFile(asset, 0, 0, fileFlag)
            if (decoder > 0) {
                channel = BASS_FX.BASS_FX_TempoCreate(channel, 0)
                BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq)

                if (decoder == DECODER_DOUBLE_TIME) {
                    val targetTempo = multiplier - 100.0f
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value)
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, targetTempo)
                } else if (decoder == DECODER_NIGHT_CORE) {
                    val targetFreq = multiplier / 100.0f
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value * targetFreq)
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 1.0f)
                }
            }
        }
        return channel != 0
    }

    fun play() {
        if (channel != 0) {
            if (BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PAUSED) {
                BASS.BASS_ChannelPlay(channel, false)
            } else {
                BASS.BASS_ChannelPlay(channel, true)
            }
        }
    }

    fun pause() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelPause(channel)
        }
    }

    fun stop() {
        if (channel != 0) {
            BASS.BASS_ChannelStop(channel)
        }
    }

    fun seek(sec: Double) {
        if (channel != 0) {
            val playPos = BASS.BASS_ChannelSeconds2Bytes(channel, sec)
            BASS.BASS_ChannelSetPosition(channel, playPos, BASS.BASS_POS_DECODETO)
        }
    }

    fun free() {
        if (isPlaying()) {
            stop()
        }
        BASS.BASS_StreamFree(channel)
        channel = 0
    }

    fun getSpectrum(): FloatArray? {
        if (!isPlaying()) {
            return null
        }
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(WINDOW_FFT shl 1)
            buffer!!.order(null)
        }
        BASS.BASS_ChannelGetData(channel, buffer, BASS.BASS_DATA_FFT1024)

        val resSize = WINDOW_FFT shr 1
        if (spectrumBuffer == null || spectrumBuffer!!.size != resSize) {
            spectrumBuffer = FloatArray(resSize)
        }
        buffer!!.asFloatBuffer().get(spectrumBuffer!!)
        return spectrumBuffer
    }

    fun getErrorCode(): Int = BASS.BASS_ErrorGetCode()

    fun getStatus(): Status {
        if (channel == 0) return Status.STOPPED

        val playerStatus = BASS.BASS_ChannelIsActive(channel)

        return when (playerStatus) {
            BASS.BASS_ACTIVE_STOPPED -> Status.STOPPED
            BASS.BASS_ACTIVE_PLAYING -> Status.PLAYING
            BASS.BASS_ACTIVE_PAUSED -> Status.PAUSED
            else -> Status.STALLED
        }
    }

    fun isPlaying(): Boolean = channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING

    fun getPosition(): Double {
        if (channel != 0) {
            val pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
            if (pos != -1L) {
                return BASS.BASS_ChannelBytes2Seconds(channel, pos)
            }
        }
        return 0.0
    }

    fun getLength(): Double {
        if (channel != 0) {
            val length = BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE)
            if (length != -1L) {
                return BASS.BASS_ChannelBytes2Seconds(channel, length)
            }
        }
        return 0.0
    }

    fun setLoop() {
        fileFlag = fileFlag or BASS.BASS_SAMPLE_LOOP
    }

    fun setUseSoftDecoder(decoder: Int) {
        if (decoder > 0) {
            this.fileFlag = this.fileFlag or BASS.BASS_STREAM_DECODE
        } else {
            this.fileFlag = 0
        }
        this.decoder = decoder
    }

    fun setDecoderMultiplier(multiplier: Int) {
        this.multiplier = multiplier
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

    companion object {
        const val DECODER_NORMAL = 0
        const val DECODER_DOUBLE_TIME = 1
        const val DECODER_NIGHT_CORE = 2
        const val WINDOW_FFT = 1024
        const val DEFAULT_FREQUENCY = 44100
    }
}

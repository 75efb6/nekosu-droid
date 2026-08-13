package ru.nsu.ccfit.zuev.audio

import android.content.res.AssetManager
import com.un4seen.bass.BASS
import ru.nsu.ccfit.zuev.osu.Config

class BassSoundProvider {

    private var sample = 0
    private var channel = 0

    fun prepare(fileName: String): Boolean {
        free()
        if (fileName.isNotEmpty()) {
            sample = BASS.BASS_SampleLoad(fileName, 0, 0, SIMULTANEOUS_PLAYBACKS, BASS.BASS_SAMPLE_OVER_POS)
        }
        return sample != 0
    }

    fun prepare(manager: AssetManager, assetName: String): Boolean {
        free()
        if (assetName.isNotEmpty()) {
            val asset = BASS.Asset(manager, assetName)
            sample = BASS.BASS_SampleLoad(asset, 0, 0, SIMULTANEOUS_PLAYBACKS, BASS.BASS_SAMPLE_OVER_POS)
        }
        return sample != 0
    }

    fun play() {
        play(Config.getSoundVolume())
    }

    fun play(volume: Float) {
        if (sample != 0) {
            channel = BASS.BASS_SampleGetChannel(sample, false)
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1f)
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume * Config.getSoundVolume())
            BASS.BASS_ChannelPlay(channel, false)
        }
    }

    fun stop() {
        if (sample != 0) {
            BASS.BASS_ChannelStop(channel)
        }
    }

    fun free() {
        stop()
        BASS.BASS_SampleFree(sample)
        sample = 0
    }

    fun setLooping(looping: Boolean) {
        // not impl
    }

    companion object {
        private const val SIMULTANEOUS_PLAYBACKS = 8
    }
}

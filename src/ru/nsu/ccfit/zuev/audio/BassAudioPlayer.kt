package ru.nsu.ccfit.zuev.audio

import android.content.res.AssetManager

class BassAudioPlayer : IMusicPlayer {

    private var loadMode = 0
    private var manager: AssetManager? = null
    private var path: String? = null

    constructor() {
        initDevice()
        provider!!.setUseSoftDecoder(0)
        provider!!.setDecoderMultiplier(100)
    }

    constructor(fileName: String) : this() {
        loadMode = 0
        path = fileName
    }

    constructor(manager: AssetManager, assetName: String) : this() {
        loadMode = 1
        this.manager = manager
        path = assetName
    }

    override fun prepare() {
        if (loadMode == 0) {
            provider!!.prepare(path!!)
        } else {
            provider!!.prepare(manager!!, path!!)
        }
    }

    fun prepare(fileName: String) {
        provider!!.prepare(fileName)
    }

    override fun play() {
        provider?.play()
    }

    override fun pause() {
        provider?.pause()
    }

    override fun stop() {
        provider?.stop()
    }

    override fun release() {
        provider?.let {
            it.stop()
            it.free()
        }
    }

    override fun getStatus(): Status {
        return provider?.getStatus() ?: Status.STALLED
    }

    override fun getPosition(): Int {
        return provider?.let { (it.getPosition() * 1000.0).toInt() } ?: 0
    }

    override fun getLength(): Int {
        return provider?.let { (it.getLength() * 1000.0).toInt() } ?: 0
    }

    fun getSpectrum(): FloatArray {
        return provider?.getSpectrum() ?: FloatArray(0)
    }

    override fun seekTo(ms: Int) {
        provider?.seek(ms / 1000.0)
    }

    override fun setUseSoftDecoder(decoder: Int) {
        provider?.setUseSoftDecoder(decoder)
    }

    override fun setDecoderMultiplier(multiplier: Int) {
        provider?.setDecoderMultiplier(multiplier)
    }

    fun setLoop() {
        provider?.setLoop()
    }

    override fun getVolume(): Float {
        return provider?.getVolume() ?: 0f
    }

    override fun setVolume(volume: Float) {
        provider?.setVolume(volume)
    }

    fun getErrorCode(): Int {
        return provider?.getErrorCode() ?: -1
    }

    companion object {
        private var provider: BassAudioProvider? = null

        @JvmStatic
        fun initDevice() {
            if (provider == null) {
                provider = BassAudioProvider()
            }
        }

        @JvmStatic
        fun getProvider(): BassAudioProvider? = provider
    }
}

package ru.nsu.ccfit.zuev.audio

interface IMusicPlayer {
    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun getStatus(): Status
    fun getPosition(): Int
    fun getLength(): Int
    fun seekTo(ms: Int)
    fun setUseSoftDecoder(decoder: Int)
    fun setDecoderMultiplier(multiplier: Int)
    fun getVolume(): Float
    fun setVolume(volume: Float)
}

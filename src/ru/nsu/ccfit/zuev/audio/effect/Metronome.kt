package ru.nsu.ccfit.zuev.audio.effect

import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class Metronome {

    private val resources = ResourceManager.getInstance()

    private val kickSound = resources.getSound("nightcore-kick")
    private val finishSound = resources.getSound("nightcore-finish")
    private val clapSound = resources.getSound("nightcore-clap")
    private val hatSound = resources.getSound("nightcore-hat")

    private val volume = 1.0f
    private var lastBeatIndex = -1

    fun update(elapsedTime: Float) {
        if (elapsedTime - GameHelper.timingOffset <= 0) {
            return
        }

        val playSeconds = (elapsedTime - GameHelper.timingOffset).toFloat()
        val beatIndex = (playSeconds * 2 / GameHelper.beatLength).toInt()

        if (beatIndex < 0) {
            return
        }
        if (beatIndex == lastBeatIndex) {
            return
        }
        lastBeatIndex = beatIndex

        val beatInBar = beatIndex % GameHelper.timeSignature

        // Every 8 bars, kick + finish on the 4th beat
        if (beatIndex % (8 * GameHelper.timeSignature) == 0) {
            kickSound.play(volume)
            if (beatIndex > 0) {
                finishSound.play(volume)
            }
            return
        }
        // Kick on the 4th beat of each bar
        if (beatInBar % 4 == 0) {
            kickSound.play(volume)
            return
        }
        // Clap on the 2nd beat of each bar
        if (beatInBar % 4 == 2) {
            clapSound.play(volume)
            return
        }
        // Hat on odd beats of each bar
        hatSound.play(volume)
    }
}

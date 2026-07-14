package ru.nsu.ccfit.zuev.osu

import java.io.Serializable

class TrackInfo(var beatmap: BeatmapInfo?) : Serializable {
    var filename: String? = null
    var publicName: String? = null
    var mode: String? = null
    var creator: String? = null
    var md5: String? = null
    var background: String? = null
    var beatmapID: Int = 0
    var beatmapSetID: Int = 0
    var difficulty: Float = 0f
    var hpDrain: Float = 0f
    var overallDifficulty: Float = 0f
    var approachRate: Float = 0f
    var circleSize: Float = 0f
    var bpmMax: Float = 0f
    var bpmMin: Float = Float.MAX_VALUE
    var musicLength: Long = 0
    var hitCircleCount: Int = 0
    var sliderCount: Int = 0
    var spinnerCount: Int = 0
    var totalHitObjectCount: Int = 0
    var maxCombo: Int = 0
    var audioFilename: String? = null
    var previewTime: Int = -1

    fun getFilename(): String? = filename

    fun setFilename(filename: String?) {
        this.filename = filename
    }

    fun getMode(): String? = mode

    fun setMode(mode: String?) {
        this.mode = mode
    }

    fun getCreator(): String? = creator

    fun setCreator(creator: String?) {
        this.creator = creator
    }

    fun getDifficulty(): Float = difficulty

    fun setDifficulty(difficulty: Float) {
        this.difficulty = difficulty
    }

    fun getBackground(): String? = background

    fun setBackground(background: String?) {
        this.background = background
    }

    fun getPublicName(): String? = publicName

    fun setPublicName(publicName: String?) {
        this.publicName = publicName
    }

    fun getBeatmap(): BeatmapInfo? = beatmap

    fun setBeatmap(beatmap: BeatmapInfo?) {
        this.beatmap = beatmap
    }

    fun getHpDrain(): Float = hpDrain

    fun setHpDrain(hpDrain: Float) {
        this.hpDrain = hpDrain
    }

    fun getOverallDifficulty(): Float = overallDifficulty

    fun setOverallDifficulty(overallDifficulty: Float) {
        this.overallDifficulty = overallDifficulty
    }

    fun getApproachRate(): Float = approachRate

    fun setApproachRate(approachRate: Float) {
        this.approachRate = approachRate
    }

    fun getCircleSize(): Float = circleSize

    fun setCircleSize(circleSize: Float) {
        this.circleSize = circleSize
    }

    fun getBpmMax(): Float = bpmMax

    fun setBpmMax(bpmMax: Float) {
        this.bpmMax = bpmMax
    }

    fun getBpmMin(): Float = bpmMin

    fun setBpmMin(bpmMin: Float) {
        this.bpmMin = bpmMin
    }

    fun getMusicLength(): Long = musicLength

    fun setMusicLength(musicLength: Long) {
        this.musicLength = musicLength
    }

    fun getHitCircleCount(): Int = hitCircleCount

    fun setHitCircleCount(hitCircleCount: Int) {
        this.hitCircleCount = hitCircleCount
    }

    fun getSliderCount(): Int = sliderCount

    fun setSliderCount(sliderCount: Int) {
        this.sliderCount = sliderCount
    }

    fun getSpinnerCount(): Int = spinnerCount

    fun setSpinnerCount(spinnerCount: Int) {
        this.spinnerCount = spinnerCount
    }

    fun getTotalHitObjectCount(): Int = totalHitObjectCount

    fun setTotalHitObjectCount(totalHitObjectCount: Int) {
        this.totalHitObjectCount = totalHitObjectCount
    }

    fun getBeatmapID(): Int = beatmapID

    fun setBeatmapID(beatmapID: Int) {
        this.beatmapID = beatmapID
    }

    fun getBeatmapSetID(): Int = beatmapSetID

    fun setBeatmapSetID(beatmapSetID: Int) {
        this.beatmapSetID = beatmapSetID
    }

    fun getMaxCombo(): Int = maxCombo

    fun setMaxCombo(maxCombo: Int) {
        this.maxCombo = maxCombo
    }

    fun setMD5(md5: String?) {
        this.md5 = md5
    }

    fun getMD5(): String? = md5

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o is TrackInfo) {
            val track = o
            return md5 != null && track.md5 != null && track.md5 == md5
        }
        return false
    }

    fun getAudioFilename(): String? = audioFilename

    fun setAudioFilename(audioFilename: String?) {
        this.audioFilename = audioFilename
    }

    fun getPreviewTime(): Int = previewTime

    fun setPreviewTime(previewTime: Int) {
        this.previewTime = previewTime
    }

    companion object {
        private const val serialVersionUID = 2049627581836712912L
    }
}

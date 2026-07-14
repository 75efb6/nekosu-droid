package ru.nsu.ccfit.zuev.osu.game

import java.util.LinkedList

class GameObjectPool private constructor() {

    @JvmField var circles = LinkedList<HitCircle>()
    @JvmField var numbers = HashMap<Int, LinkedList<CircleNumber>>()
    @JvmField var effects = HashMap<String, LinkedList<GameEffect>>()
    @JvmField var sliders = LinkedList<Slider>()
    @JvmField var tracks = LinkedList<FollowTrack>()
    @JvmField var spinners = LinkedList<Spinner>()
    private var objectsCreated = 0

    fun getCircle(): HitCircle {
        if (circles.isNotEmpty()) return circles.poll()
        objectsCreated++
        return HitCircle()
    }

    fun putCircle(circle: HitCircle) {
        circles.add(circle)
    }

    fun getSpinner(): Spinner {
        if (spinners.isNotEmpty()) return spinners.poll()
        objectsCreated++
        return Spinner()
    }

    fun putSpinner(spinner: Spinner) {
        spinners.add(spinner)
    }

    fun getNumber(num: Int): CircleNumber {
        val list = numbers[num]
        if (list != null && list.isNotEmpty()) return list.poll()
        objectsCreated++
        return CircleNumber(num)
    }

    fun putNumber(number: CircleNumber) {
        numbers.getOrPut(number.getNum()) { LinkedList() }.add(number)
    }

    fun getEffect(texname: String): GameEffect {
        val list = effects[texname]
        if (list != null && list.isNotEmpty()) return list.poll()
        objectsCreated++
        return GameEffect(texname)
    }

    fun putEffect(effect: GameEffect) {
        effects.getOrPut(effect.getTexname()) { LinkedList() }.add(effect)
    }

    fun getSlider(): Slider {
        if (sliders.isNotEmpty()) return sliders.poll()
        objectsCreated++
        return Slider()
    }

    fun putSlider(slider: Slider) {
        sliders.add(slider)
    }

    fun getTrack(): FollowTrack {
        if (tracks.isNotEmpty()) return tracks.poll()
        objectsCreated++
        return FollowTrack()
    }

    fun putTrac(track: FollowTrack) {
        tracks.add(track)
    }

    fun getObjectsCreated(): Int = objectsCreated

    fun purge() {
        effects.clear()
        circles.clear()
        numbers.clear()
        sliders.clear()
        tracks.clear()
        objectsCreated = 0
    }

    fun preload() {
        for (i in 0 until 10) {
            putCircle(HitCircle())
            putNumber(CircleNumber(i + 1))
        }
        for (i in 0 until 5) {
            putSlider(Slider())
            putTrac(FollowTrack())
        }
        Spinner()
        objectsCreated = 31
    }

    companion object {
        @JvmField
        var instance = GameObjectPool()

        @JvmStatic
        fun getInstance(): GameObjectPool = instance
    }
}

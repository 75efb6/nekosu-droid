package ru.nsu.ccfit.zuev.osu.menu

import androidx.annotation.NonNull
import com.reco1l.api.ibancho.data.WinCondition
import com.reco1l.legacy.Multiplayer
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*
import kotlin.Throws

class ScoreBoardItem : Cloneable {

    var userName: String? = null
    var playScore = 0
    var scoreId = 0
    var rank = -1
    var maxCombo = 0
    var accuracy = -1f
    var isAlive = true

    constructor()
    constructor(userName: String?, playScore: Int, maxCombo: Int, accuracy: Float, isAlive: Boolean) {
        this.userName = userName
        this.playScore = playScore
        this.maxCombo = maxCombo
        this.accuracy = accuracy
        this.isAlive = isAlive
    }

    fun set(rankPos: Int, name: String?, combo: Int, score: Int, id: Int) {
        rank = rankPos
        userName = name
        maxCombo = combo
        playScore = score
        scoreId = id
    }

    fun get(): String {
        var text = "$userName\n${NUMBER_FORMAT.format(playScore.toLong())}\n"
        if (Multiplayer.isConnected && Multiplayer.room?.winCondition == WinCondition.ACCURACY) {
            accSb.setLength(0)
            text += DECIMAL_FORMAT.format("%2.2f%%", accuracy * 100f)
        } else {
            text += NUMBER_FORMAT.format(maxCombo.toLong()) + "x"
        }
        return text
    }

    fun toJson(): JSONObject {
        return object : JSONObject() {
            init {
                try {
                    put("accuracy", accuracy.toDouble())
                    put("score", playScore)
                    put("combo", maxCombo)
                    put("isAlive", isAlive)
                } catch (e: Exception) {
                    Multiplayer.log(e)
                }
            }
        }
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is ScoreBoardItem) return false
        return o.userName == userName && o.playScore == playScore && o.maxCombo == maxCombo && o.accuracy == accuracy && o.isAlive == isAlive
    }

    @NonNull
    @Throws(CloneNotSupportedException::class)
    override fun clone(): ScoreBoardItem {
        return super.clone() as ScoreBoardItem
    }

    companion object {
        private val NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US)
        private val accSb = StringBuilder()
        private val DECIMAL_FORMAT = Formatter(accSb, Locale.ENGLISH)
    }
}

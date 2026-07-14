package com.edlplan.osu.support.`object`

import android.graphics.PointF
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.LinePath
import com.edlplan.framework.utils.advance.StringSplitter
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameHelper

object SupportSliderPath {

    private fun parseStdPath(startPoint: Vec2, s: String): StdPath {
        val p = StdPath()
        p.addControlPoint(startPoint)
        val spl = StringSplitter(s, "\\|")
        p.type = StdPath.Type.forName(spl.next())
        while (spl.hasNext()) {
            p.addControlPoint(parseVec2FF(spl.next()))
        }
        return p
    }

    private fun parseVec2FF(s: String): Vec2 {
        val sp = s.split(":")
        return Vec2(sp[0].toFloat(), sp[1].toFloat())
    }

    @JvmStatic
    fun parseToLinePath(s: Vec2, p: String): LinePath {
        val path = parseStdPath(s, p)
        return StdSliderPathMaker(path).calculatePath()
    }

    @JvmStatic
    fun parseDroidLinePath(s: PointF, p: String, l: Float): GameHelper.SliderPath {
        var path = parseToLinePath(Vec2(s.x, s.y), p)
        path.measure()
        path.bufferLength(l)
        path = path.cutPath(0f, path.measurer.maxLength()).fitToLinePath()
        path.measure()
        val points = ArrayList<PointF>(path.size())
        for (i in 0 until path.size()) {
            val v = path[i]
            points.add(Utils.realToTrackCoords(PointF(v.x, v.y)))
        }
        val keywords = p.split("\\|".toRegex()).toTypedArray()
        return GameHelper.SliderPath(s, keywords, l, 0f)
    }
}

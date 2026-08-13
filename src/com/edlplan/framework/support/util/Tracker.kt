package com.edlplan.framework.support.util

import com.edlplan.framework.support.Framework
import com.edlplan.framework.utils.advance.ConsumerContainer
import com.edlplan.framework.utils.interfaces.Consumer

class Tracker {

    class TrackNode(var id: Int, var name: String) {
        var totalTimeMS: Double = 0.0
        var trackedTimes: Long = 0
        var latestRecordTime: Double = 0.0

        private var stack: Int = 0

        fun watch() {
            if (stack == 0) {
                latestRecordTime = Framework.relativePreciseTimeMillion()
            } else {
                val time = Framework.relativePreciseTimeMillion()
                totalTimeMS += time - latestRecordTime
                latestRecordTime = time
            }
            stack++
        }

        fun end() {
            trackedTimes++
            stack--
            if (stack == 0) {
                totalTimeMS += Framework.relativePreciseTimeMillion() - latestRecordTime
            } else {
                val time = Framework.relativePreciseTimeMillion()
                totalTimeMS += time - latestRecordTime
                latestRecordTime = time
            }
        }

        fun clear() {
            totalTimeMS = 0.0
            trackedTimes = 0
            latestRecordTime = 0.0
            stack = 0
        }

        fun wrap(runnable: Runnable): ConsumerContainer<TrackNode> {
            watch()
            runnable.run()
            end()
            return ConsumerContainer(this)
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("------------------------------------\n")
            sb.append("name         : $name ($id)\n")
            sb.append("totalTime    : ${totalTimeMS}ms\n")
            sb.append("trackedTimes : $trackedTimes\n")
            sb.append("------------------------------------")
            return sb.toString()
        }
    }

    companion object {
        const val DRAW_ARRAY = "DRAW_ARRAY"
        const val PREPARE_VERTEX_DATA = "PREPARE_VERTEX_DATA"
        const val INJECT_DATA = "INJECT_DATA"
        const val MAIN_LOOPER = "MAIN_LOOPER"
        const val DRAW_UI = "DRAW_UI"
        const val INVALIDATE_MEASURE_AND_LAYOUT = "INVALIDATE_MEASURE"
        const val TOTAL_FRAME_TIME = "TOTAL_FRAME_TIME"

        @JvmField
        val DrawArray: TrackNode
        @JvmField
        val PrepareVertexData: TrackNode
        @JvmField
        val InjectData: TrackNode
        @JvmField
        val MainLooper: TrackNode
        @JvmField
        val DrawUI: TrackNode
        @JvmField
        val TotalFrameTime: TrackNode
        @JvmField
        val InvalidateMeasureAndLayout: TrackNode

        private var enable = true
        private val nodes = ArrayList<TrackNode>()
        private val namemap = HashMap<String, TrackNode>()

        init {
            DrawArray = register(DRAW_ARRAY)
            PrepareVertexData = register(PREPARE_VERTEX_DATA)
            InjectData = register(INJECT_DATA)
            MainLooper = register(MAIN_LOOPER)

            InvalidateMeasureAndLayout = register(INVALIDATE_MEASURE_AND_LAYOUT)
            DrawUI = register(DRAW_UI)
            TotalFrameTime = register(TOTAL_FRAME_TIME)
        }

        @JvmStatic
        fun register(name: String): TrackNode {
            val node = TrackNode(nodes.size, name)
            nodes.add(node)
            namemap[name] = node
            return node
        }

        @JvmStatic
        fun createTmpNode(name: String): TrackNode {
            return TrackNode(-1, name)
        }

        @JvmStatic
        fun reset() {
            for (n in nodes) {
                n.clear()
            }
        }

        @JvmStatic
        fun printlnAsTime(ms: Int) {
            println("${ms}ms")
        }

        @JvmStatic
        fun printByTag(tag: String): Consumer<Int> {
            return Consumer { t -> println(String.format("[%s] %dms", tag, t)) }
        }
    }
}

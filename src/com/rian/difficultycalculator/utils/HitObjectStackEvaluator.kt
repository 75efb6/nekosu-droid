package com.rian.difficultycalculator.utils

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.Spinner

object HitObjectStackEvaluator {
    private const val stackDistance = 3

    @JvmStatic
    fun applyStacking(formatVersion: Int, objects: List<HitObject>, ar: Float, stackLeniency: Float) {
        applyStacking(formatVersion, objects, ar, stackLeniency, 0, objects.size - 1)
    }

    @JvmStatic
    fun applyStacking(formatVersion: Int, objects: List<HitObject>, ar: Float, stackLeniency: Float, startIndex: Int, endIndex: Int) {
        if (objects.isEmpty()) {
            return
        }

        if (formatVersion < 6) {
            applyStackingOld(objects, ar.toDouble(), stackLeniency)
            return
        }

        val timePreempt = if (ar <= 5) (1800 - 120 * ar) else (1950 - 150 * ar)
        val stackThreshold = timePreempt * stackLeniency

        var extendedEndIndex = endIndex

        if (endIndex < objects.size - 1) {
            for (i in endIndex downTo startIndex) {
                var stackBaseIndex = i

                for (n in stackBaseIndex + 1 until objects.size) {
                    val stackBaseObject = objects[stackBaseIndex]
                    if (stackBaseObject is Spinner) {
                        break
                    }

                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    var endTime = stackBaseObject.getStartTime()
                    if (stackBaseObject is HitObjectWithDuration) {
                        endTime = stackBaseObject.getEndTime()
                    }

                    if (objectN.getStartTime() - endTime > stackThreshold) {
                        break
                    }

                    if (stackBaseObject.getPosition().getDistance(objectN.getPosition()) < stackDistance ||
                        (stackBaseObject is Slider && stackBaseObject.getEndPosition().getDistance(objectN.getPosition()) < stackDistance)
                    ) {
                        stackBaseIndex = n

                        objectN.setStackHeight(0)
                    }
                }

                if (stackBaseIndex > extendedEndIndex) {
                    extendedEndIndex = stackBaseIndex
                    if (extendedEndIndex == objects.size - 1)
                        break
                }
            }
        }

        var extendedStartIndex = startIndex

        for (i in extendedEndIndex downTo startIndex + 1) {
            var n = i

            var objectI = objects[i]
            if (objectI.getStackHeight() != 0 || objectI is Spinner) {
                continue
            }

            if (objectI is HitCircle) {
                while (--n >= 0) {
                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    var endTime = objectN.getStartTime()
                    if (objectN is HitObjectWithDuration) {
                        endTime = objectN.getEndTime()
                    }

                    if (objectI.getStartTime() - endTime > stackThreshold) {
                        break
                    }

                    if (n < extendedStartIndex) {
                        objectN.setStackHeight(0)
                        extendedStartIndex = n
                    }

                    if (objectN is Slider && objectN.getEndPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        val offset = objectI.getStackHeight() - objectN.getStackHeight() + 1

                        for (j in n + 1..i) {
                            val objectJ = objects[j]
                            if (objectN.getEndPosition().getDistance(objectJ.getPosition()) < stackDistance) {
                                objectJ.setStackHeight(objectJ.getStackHeight() - offset)
                            }
                        }

                        break
                    }

                    if (objectN.getPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        objectN.setStackHeight(objectI.getStackHeight() + 1)
                        objectI = objectN
                    }
                }
            } else if (objectI is Slider) {
                while (--n >= startIndex) {
                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    if (objectI.getStartTime() - objectN.getStartTime() > stackThreshold) {
                        break
                    }

                    if (objectN.getEndPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        objectN.setStackHeight(objectI.getStackHeight() + 1)
                        objectI = objectN
                    }
                }
            }
        }
    }

    private fun applyStackingOld(objects: List<HitObject>, ar: Double, stackLeniency: Float) {
        val timePreempt = if (ar <= 5) (1800 - 120 * ar) else (1950 - 150 * ar)
        val stackThreshold = timePreempt * stackLeniency

        for (i in objects.indices) {
            val currentObject = objects[i]

            if (currentObject.getStackHeight() != 0 && currentObject !is Slider) {
                continue
            }

            var sliderStack = 0
            var startTime = currentObject.getStartTime()
            if (currentObject is HitObjectWithDuration) {
                startTime = currentObject.getEndTime()
            }

            for (j in i + 1 until objects.size) {
                if (objects[j].getStartTime() - stackThreshold > startTime) {
                    break
                }

                if (objects[j].getPosition().getDistance(currentObject.getPosition()) < stackDistance) {
                    currentObject.setStackHeight(currentObject.getStackHeight() + 1)
                    startTime = objects[j].getStartTime()
                } else if (objects[j].getPosition().getDistance(currentObject.getEndPosition()) < stackDistance) {
                    ++sliderStack
                    objects[j].setStackHeight(objects[j].getStackHeight() - sliderStack)
                    startTime = objects[j].getStartTime()
                }
            }
        }
    }
}

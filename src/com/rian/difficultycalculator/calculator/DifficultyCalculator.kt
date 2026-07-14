package com.rian.difficultycalculator.calculator

import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.difficultycalculator.beatmap.BeatmapDifficultyManager
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap
import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.skills.Aim
import com.rian.difficultycalculator.skills.Flashlight
import com.rian.difficultycalculator.skills.Skill
import com.rian.difficultycalculator.skills.Speed
import com.rian.difficultycalculator.utils.HitObjectStackEvaluator
import com.rian.difficultycalculator.utils.HitWindowConverter
import java.util.ArrayList
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class DifficultyCalculator {
    @JvmField
    val difficultyAdjustmentMods: EnumSet<GameMod> = EnumSet.of(
        GameMod.MOD_DOUBLETIME, GameMod.MOD_HALFTIME, GameMod.MOD_NIGHTCORE,
        GameMod.MOD_RELAX, GameMod.MOD_EASY,
        GameMod.MOD_REALLYEASY, GameMod.MOD_HARDROCK, GameMod.MOD_HIDDEN,
        GameMod.MOD_FLASHLIGHT
    )

    constructor()

    fun calculate(beatmap: DifficultyBeatmap): DifficultyAttributes {
        return calculate(beatmap, null)
    }

    fun calculate(beatmap: DifficultyBeatmap, parameters: DifficultyCalculationParameters?): DifficultyAttributes {
        var beatmapToCalculate = beatmap

        if (parameters != null) {
            beatmapToCalculate = beatmap.deepClone()
            applyParameters(beatmapToCalculate, parameters)
        }

        val skills = createSkills(beatmapToCalculate, parameters)

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            for (skill in skills) {
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(beatmapToCalculate, skills, parameters)
    }

    fun calculateTimed(beatmap: DifficultyBeatmap): List<TimedDifficultyAttributes> {
        return calculateTimed(beatmap, null)
    }

    fun calculateTimed(beatmap: DifficultyBeatmap, parameters: DifficultyCalculationParameters?): List<TimedDifficultyAttributes> {
        var beatmapToCalculate = beatmap

        if (parameters != null) {
            beatmapToCalculate = beatmap.deepClone()
            applyParameters(beatmapToCalculate, parameters)
        }

        val skills = createSkills(beatmapToCalculate, parameters)
        val attributes = ArrayList<TimedDifficultyAttributes>()

        if (beatmapToCalculate.getHitObjectsManager().getObjects().isEmpty()) {
            return attributes
        }

        val progressiveBeatmap = DifficultyBeatmap(beatmapToCalculate.getDifficultyManager())

        progressiveBeatmap.getHitObjectsManager().add(beatmapToCalculate.getHitObjectsManager().getObjects()[0])

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            progressiveBeatmap.getHitObjectsManager().add(obj.`object`)

            for (skill in skills) {
                skill.process(obj)
            }

            attributes.add(
                TimedDifficultyAttributes(
                    obj.endTime * (parameters?.getTotalSpeedMultiplier() ?: 1f),
                    createDifficultyAttributes(progressiveBeatmap, skills, parameters)
                )
            )
        }

        return attributes
    }

    private fun createDifficultyAttributes(beatmap: DifficultyBeatmap, skills: Array<Skill>, parameters: DifficultyCalculationParameters?): DifficultyAttributes {
        val attributes = DifficultyAttributes()

        if (parameters != null) {
            attributes.mods = parameters.mods.clone()
        }

        attributes.aimDifficulty = calculateRating(skills[0])
        attributes.speedDifficulty = calculateRating(skills[2])
        attributes.speedNoteCount = (skills[2] as Speed).relevantNoteCount()
        attributes.flashlightDifficulty = calculateRating(skills[3])

        val aimRatingNoSliders = calculateRating(skills[1])
        attributes.aimSliderFactor = if (attributes.aimDifficulty > 0) aimRatingNoSliders / attributes.aimDifficulty else 1.0

        if (parameters != null && parameters.mods.contains(GameMod.MOD_RELAX)) {
            attributes.aimDifficulty *= 0.9
            attributes.speedDifficulty = 0.0
            attributes.flashlightDifficulty *= 0.7
        }

        val baseAimPerformance = Math.pow(5 * Math.max(1.0, attributes.aimDifficulty / 0.0675) - 4, 3.0) / 100000
        val baseSpeedPerformance = Math.pow(5 * Math.max(1.0, attributes.speedDifficulty / 0.0675) - 4, 3.0) / 100000
        var baseFlashlightPerformance = 0.0

        if (parameters != null && parameters.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            baseFlashlightPerformance = Math.pow(attributes.flashlightDifficulty, 2.0) * 25.0
        }

        val basePerformance = Math.pow(
            Math.pow(baseAimPerformance, 1.1) +
                    Math.pow(baseSpeedPerformance, 1.1) +
                    Math.pow(baseFlashlightPerformance, 1.1),
            1.0 / 1.1
        )

        attributes.starRating = if (basePerformance > 1e-5)
            Math.cbrt(PerformanceCalculator.finalMultiplier) * 0.027 * (Math.cbrt(100000 / Math.pow(2.0, 1 / 1.1) * basePerformance) + 4)
        else
            0.0

        val ar = beatmap.getDifficultyManager().getAR()
        val preempt = if (ar <= 5) (1800 - 120 * ar) else (1950 - 150 * ar)

        attributes.approachRate = if (preempt > 1200) (1800 - preempt) / 120.0 else (1200 - preempt) / 150.0 + 5.0

        val od = beatmap.getDifficultyManager().getOD()
        val odMS = HitWindowConverter.odToHitWindow300(od) / (parameters?.getTotalSpeedMultiplier() ?: 1f)

        attributes.overallDifficulty = HitWindowConverter.hitWindow300ToOD(odMS)

        attributes.maxCombo = beatmap.getMaxCombo()
        attributes.hitCircleCount = beatmap.getHitObjectsManager().getCircleCount()
        attributes.sliderCount = beatmap.getHitObjectsManager().getSliderCount()
        attributes.spinnerCount = beatmap.getHitObjectsManager().getSpinnerCount()

        return attributes
    }

    private fun applyParameters(beatmap: DifficultyBeatmap, parameters: DifficultyCalculationParameters) {
        val manager = beatmap.getDifficultyManager()
        val initialAR = manager.getAR()

        processCS(manager, parameters)
        processAR(manager, parameters)
        processOD(manager, parameters)
        processHP(manager, parameters)

        if (initialAR != manager.getAR()) {
            beatmap.getHitObjectsManager().resetStacking()

            HitObjectStackEvaluator.applyStacking(
                beatmap.getFormatVersion(),
                beatmap.getHitObjectsManager().getObjects(),
                manager.getAR(),
                beatmap.getStackLeniency()
            )
        }
    }

    private fun createSkills(beatmap: DifficultyBeatmap, parameters: DifficultyCalculationParameters?): Array<Skill> {
        var mods: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
        val od = beatmap.getDifficultyManager().getOD()
        var greatWindow = HitWindowConverter.odToHitWindow300(od)

        if (parameters != null) {
            mods = parameters.mods
            greatWindow /= parameters.getTotalSpeedMultiplier()
        }

        return arrayOf(
            Aim(mods, true),
            Aim(mods, false),
            Speed(mods, greatWindow),
            Flashlight(mods)
        )
    }

    private fun calculateRating(skill: Skill): Double {
        return Math.sqrt(skill.difficultyValue()) * difficultyMultiplier
    }

    private fun processCS(manager: BeatmapDifficultyManager, parameters: DifficultyCalculationParameters) {
        var cs = manager.getCS()

        val maxValue = 12.13f

        if (parameters == null) {
            manager.setCS(Math.min(cs, maxValue))
            return
        }

        if (parameters.isCustomCS()) {
            manager.setCS(Math.min(maxValue, parameters.customCS))
        } else {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                ++cs
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                --cs
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                --cs
            }

            manager.setCS(Math.min(cs, maxValue))
        }
    }

    private fun processAR(manager: BeatmapDifficultyManager, parameters: DifficultyCalculationParameters) {
        var ar = manager.getAR()
        val maxValue = 10f

        if (parameters == null) {
            manager.setAR(Math.min(ar, maxValue))
            return
        }

        if (parameters.isCustomAR()) {
            manager.setAR(parameters.customAR)
        } else {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                ar *= 1.4f
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                ar /= 2f
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                if (parameters.mods.contains(GameMod.MOD_EASY)) {
                    ar *= 2f
                    ar -= 0.5f
                }

                ar -= 0.5f
                ar -= parameters.customSpeedMultiplier - 1f
            }

            manager.setAR(Math.min(ar, maxValue))
        }
    }

    private fun processOD(manager: BeatmapDifficultyManager, parameters: DifficultyCalculationParameters) {
        var od = manager.getOD()
        val maxValue = 10f

        if (parameters == null) {
            manager.setOD(Math.min(od, maxValue))
            return
        }

        if (parameters.isCustomOD()) {
            manager.setOD(parameters.customOD)
        } else {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                od *= 1.4f
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                od /= 2f
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                od /= 2f
            }

            manager.setOD(Math.min(od, maxValue))
        }
    }

    private fun processHP(manager: BeatmapDifficultyManager, parameters: DifficultyCalculationParameters) {
        var hp = manager.getHP()

        if (parameters != null) {
            if (parameters.mods.contains(GameMod.MOD_HARDROCK)) {
                hp *= 1.4f
            }
            if (parameters.mods.contains(GameMod.MOD_EASY)) {
                hp /= 2f
            }
            if (parameters.mods.contains(GameMod.MOD_REALLYEASY)) {
                hp /= 2f
            }
        }

        manager.setHP(Math.min(hp, 10f))
    }

    private fun createDifficultyHitObjects(beatmap: DifficultyBeatmap, parameters: DifficultyCalculationParameters?): List<DifficultyHitObject> {
        val objects = ArrayList<DifficultyHitObject>()
        val rawObjects = beatmap.getHitObjectsManager().getObjects()

        val ar = beatmap.getDifficultyManager().getAR()
        val timePreempt = if (ar <= 5) (1800 - 120 * ar) else (1950 - 150 * ar)
        val objectScale = (1 - 0.7f * (beatmap.getDifficultyManager().getCS() - 5) / 5) / 2

        for (i in 1 until rawObjects.size) {
            rawObjects[i].setScale(objectScale)
            rawObjects[i - 1].setScale(objectScale)

            val lastLast = if (i > 1) rawObjects[i - 2] else null

            objects.add(
                DifficultyHitObject(
                    rawObjects[i],
                    rawObjects[i - 1],
                    lastLast,
                    parameters?.getTotalSpeedMultiplier()?.toDouble() ?: 1.0,
                    objects,
                    objects.size,
                    timePreempt.toDouble(),
                    parameters?.isCustomAR() == true
                )
            )
        }

        return objects
    }

    companion object {
        private const val difficultyMultiplier = 0.0675
    }
}

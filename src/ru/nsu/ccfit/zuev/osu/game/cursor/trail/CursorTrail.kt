package ru.nsu.ccfit.zuev.osu.game.cursor.trail

import org.anddev.andengine.entity.particle.ParticleSystem
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer
import org.anddev.andengine.entity.particle.modifier.AlphaModifier
import org.anddev.andengine.entity.particle.modifier.ExpireModifier
import org.anddev.andengine.opengl.texture.region.TextureRegion
import javax.microedition.khronos.opengles.GL10

class CursorTrail(
    emitter: PointParticleEmitter,
    spawnRate: Int,
    trailSize: Float,
    pTextureRegion: TextureRegion
) : ParticleSystem(emitter, spawnRate.toFloat(), spawnRate.toFloat(), spawnRate, pTextureRegion) {

    init {
        fadeOut()
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        addParticleInitializer(ScaleInitializer(trailSize))
        setParticlesSpawnEnabled(false)
    }

    private fun fadeOut() {
        addParticleModifier(ExpireModifier(0.10f))
        addParticleModifier(AlphaModifier(1.0f, 0.0f, 0f, 0.10f))
    }
}

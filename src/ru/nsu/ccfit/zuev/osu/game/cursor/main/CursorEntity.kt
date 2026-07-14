package ru.nsu.ccfit.zuev.osu.game.cursor.main

import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.particle.ParticleSystem
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail

open class CursorEntity : Entity() {

    protected val cursorSprite: CursorSprite
    private var particles: ParticleSystem? = null
    private var emitter: PointParticleEmitter? = null
    private var isShowing = false
    private var particleOffsetX = 0f
    private var particleOffsetY = 0f

    init {
        val cursorTex: TextureRegion = ResourceManager.getInstance().getTexture("cursor")!!
        cursorSprite = CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex)

        if (Config.isUseParticles()) {
            val trailTex: TextureRegion = ResourceManager.getInstance().getTexture("cursortrail")!!

            particleOffsetX = -trailTex.getWidth() / 2f
            particleOffsetY = -trailTex.getHeight() / 2f

            val spawnRate = ((GlobalManager.getInstance().getMainActivity()?.getRefreshRate() ?: 60f) * 2).toInt()

            emitter = PointParticleEmitter(particleOffsetX, particleOffsetY)
            particles = CursorTrail(emitter!!, spawnRate, cursorSprite.baseSize, trailTex)
            particles!!.setParticlesSpawnEnabled(false)
        }

        attachChild(cursorSprite)
        isVisible = false
        setIgnoreUpdate(true)
    }

    fun setShowing(showing: Boolean) {
        isShowing = showing
        isVisible = showing
        particles?.setParticlesSpawnEnabled(showing)
    }

    fun click() {
        cursorSprite.handleClick()
    }

    fun update(pSecondsElapsed: Float) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed)
        }
        super.onManagedUpdate(pSecondsElapsed)
    }

    fun attachToScene(fgScene: Scene) {
        particles?.let { fgScene.attachChild(it) }
        fgScene.attachChild(this)
    }

    override fun setPosition(pX: Float, pY: Float) {
        emitter?.setCenter(pX + particleOffsetX, pY + particleOffsetY)
        super.setPosition(pX, pY)
    }
}

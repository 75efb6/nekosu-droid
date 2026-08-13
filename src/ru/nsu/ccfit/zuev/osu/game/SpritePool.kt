package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import java.util.LinkedList

class SpritePool private constructor() {

    private val sprites = HashMap<String, LinkedList<Sprite>>()
    private val animsprites = HashMap<String, LinkedList<AnimSprite>>()
    var count = 0
        private set
    var spritesCreated = 0
        private set

    fun putSprite(name: String, sprite: Sprite) {
        if (count > CAPACITY) return
        if (sprite.hasParent()) return

        sprite.setAlpha(1f)
        sprite.setColor(1f, 1f, 1f)
        sprite.setScale(1f)
        sprite.clearEntityModifiers()
        sprite.clearUpdateHandlers()
        count++
        sprites.getOrPut(name) { LinkedList() }.add(sprite)
    }

    fun getSprite(name: String): Sprite {
        val list = sprites[name]
        if (list != null) {
            while (list.isNotEmpty() && list.peek()!!.hasParent()) {
                list.poll()
            }
            if (list.isNotEmpty()) {
                count--
                return list.poll()!!
            }
        }

        spritesCreated++
        return Sprite(0f, 0f, ResourceManager.getInstance().getTexture(name)!!)
    }

    fun getCenteredSprite(name: String, pos: PointF): Sprite {
        val list = sprites[name]
        if (list != null) {
            while (list.isNotEmpty() && list.peek()!!.hasParent()) {
                list.poll()
            }
            if (list.isNotEmpty()) {
                count--
                val sp = list.poll()!!
                sp.setPosition(pos.x - sp.getWidth() / 2, pos.y - sp.getHeight() / 2)
                return sp
            }
        }

        spritesCreated++
        return CentredSprite(pos.x, pos.y, ResourceManager.getInstance().getTexture(name)!!)
    }

    fun getAnimSprite(name: String, count: Int): AnimSprite {
        val list = animsprites[name]
        if (list != null) {
            while (list.isNotEmpty() && list.peek()!!.hasParent()) {
                list.poll()
            }
            if (list.isNotEmpty()) {
                this.count--
                return list.poll()!!
            }
        }

        spritesCreated++
        return AnimSprite(0f, 0f, name, count, count.toFloat())
    }

    fun putAnimSprite(name: String, sprite: AnimSprite) {
        if (count > CAPACITY) return
        if (sprite.hasParent()) return

        sprite.setAlpha(1f)
        sprite.setColor(1f, 1f, 1f)
        sprite.setScale(1f)
        sprite.clearEntityModifiers()
        sprite.clearUpdateHandlers()
        count++
        animsprites.getOrPut(name) { LinkedList() }.add(sprite)
    }

    fun purge() {
        count = 0
        spritesCreated = 0
        sprites.clear()
        animsprites.clear()
    }

    companion object {
        private val instance = SpritePool()
        private const val CAPACITY = 250

        @JvmStatic
        fun getInstance(): SpritePool = instance
    }
}

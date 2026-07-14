package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.engine.Engine
import org.anddev.andengine.entity.scene.menu.MenuScene
import org.anddev.andengine.entity.scene.menu.MenuScene.IOnMenuItemClickListener
import org.anddev.andengine.entity.scene.menu.item.IMenuItem
import org.anddev.andengine.entity.scene.menu.item.SpriteMenuItem
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osuplus.R

class PauseMenu(engine: Engine, game: GameScene, fail: Boolean) : IOnMenuItemClickListener {

    private val scene: MenuScene
    private val game: GameScene
    private val fail: Boolean
    private var replaySaved = false

    init {
        this.game = game
        this.fail = fail
        replaySaved = false
        scene = MenuScene(engine.camera)

        val saveFailedReplay = SpriteMenuItem(ITEM_SAVE_REPLAY,
            ResourceManager.getInstance().getTexture("pause-save-replay"))
        scene.addMenuItem(saveFailedReplay)
        val itemContinue = SpriteMenuItem(ITEM_CONTINUE,
            ResourceManager.getInstance().getTexture("pause-continue"))
        scene.addMenuItem(itemContinue)
        val itemRetry = SpriteMenuItem(ITEM_RETRY,
            ResourceManager.getInstance().getTexture("pause-retry"))
        scene.addMenuItem(itemRetry)
        val itemBack = SpriteMenuItem(ITEM_BACK,
            ResourceManager.getInstance().getTexture("pause-back"))
        scene.addMenuItem(itemBack)
        scene.setBackgroundEnabled(false)
        val tex = if (fail) {
            itemContinue.setVisible(false)
            if (game.getReplaying()) {
                saveFailedReplay.setVisible(false)
            }
            ResourceManager.getInstance().getTexture("fail-background")
        } else {
            saveFailedReplay.setVisible(false)
            ResourceManager.getInstance().getTexture("pause-overlay")
        }
        if (tex != null) {
            var height = tex.height.toFloat()
            height *= Config.getRES_WIDTH() / tex.width.toFloat()
            val bg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH().toFloat(), height, tex)
            scene.attachChild(bg, 0)
        }
        scene.buildAnimations()
        scene.setOnMenuItemClickListener(this)
    }

    fun getScene(): MenuScene = scene

    override fun onMenuItemClicked(pMenuScene: MenuScene, pMenuItem: IMenuItem, pMenuItemLocalX: Float, pMenuItemLocalY: Float): Boolean {
        if (pMenuItem.alpha < 0.75f) return false
        var playSnd: BassSoundProvider?
        when (pMenuItem.id) {
            ITEM_SAVE_REPLAY -> {
                if (fail && !replaySaved && !game.getReplaying() && game.saveFailedReplay()) {
                    ToastLogger.showTextId(R.string.message_save_replay_successful, true)
                    replaySaved = true
                }
                return true
            }
            ITEM_CONTINUE -> {
                if (fail) return false
                playSnd = ResourceManager.getInstance().getSound("menuback")
                playSnd?.play()
                game.resume()
                return true
            }
            ITEM_BACK -> {
                GlobalManager.getInstance().scoring?.setReplayID(-1)
                playSnd = ResourceManager.getInstance().getSound("menuback")
                playSnd?.play()
                game.quit()
                return true
            }
            ITEM_RETRY -> {
                ResourceManager.getInstance().getSound("failsound").stop()
                playSnd = ResourceManager.getInstance().getSound("menuhit")
                playSnd?.play()
                game.restartGame()
                return true
            }
        }
        return false
    }

    companion object {
        const val ITEM_SAVE_REPLAY = 0
        const val ITEM_CONTINUE = 1
        const val ITEM_RETRY = 2
        const val ITEM_BACK = 3
    }
}

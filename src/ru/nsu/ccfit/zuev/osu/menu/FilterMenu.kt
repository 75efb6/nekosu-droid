package ru.nsu.ccfit.zuev.osu.menu

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.InputManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.SongMenu.SortOrder
import ru.nsu.ccfit.zuev.osuplus.R

class FilterMenu private constructor() : IUpdateHandler, IFilterMenu {
    private var configContext: Context? = null
    private var scene: Scene? = null
    private var filterText: ChangeableText? = null
    internal var filter = ""
    private var sortText: ChangeableText? = null
    private var menu: SongMenu? = null
    private var order: SortOrder = SortOrder.Title
    internal var favoritesOnly = false
    internal var favoriteFolder: String? = null

    override fun getFavoriteFolder(): String? = favoriteFolder

    override fun loadConfig(context: Context) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val sortOrder = prefs.getInt("sortorder", 0)
        order = when (sortOrder) {
            1 -> SortOrder.Artist
            2 -> SortOrder.Creator
            3 -> SortOrder.Date
            4 -> SortOrder.Bpm
            5 -> SortOrder.Stars
            6 -> SortOrder.Length
            else -> SortOrder.Title
        }
        configContext = context
        setSortText()
    }

    fun saveConfig() {
        val ctx = configContext ?: return
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        val editor = prefs.edit()
        when (order) {
            SortOrder.Artist -> editor.putInt("sortorder", 1)
            SortOrder.Creator -> editor.putInt("sortorder", 2)
            SortOrder.Date -> editor.putInt("sortorder", 3)
            SortOrder.Bpm -> editor.putInt("sortorder", 4)
            SortOrder.Stars -> editor.putInt("sortorder", 5)
            SortOrder.Length -> editor.putInt("sortorder", 6)
            else -> editor.putInt("sortorder", 0)
        }
        editor.apply()
    }

    fun setSongMenu(menu: SongMenu) {
        this.menu = menu
    }

    fun reload() {
        init()
    }

    fun init() {
        scene = Scene()
        scene!!.setBackgroundEnabled(false)
        val bg = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        bg.setColor(0f, 0f, 0f, 0.7f)
        scene!!.attachChild(bg)
        val caption = Text(0f, Utils.toRes(60).toFloat(), ResourceManager.getInstance().getFont("CaptionFont"),
            StringTable.get(R.string.menu_search_title))
        caption.setPosition(Config.getRES_WIDTH() / 2f - caption.width / 2, caption.y)
        scene!!.attachChild(caption)
        val font: Font = ResourceManager.getInstance().getFont("font")
        val capt1 = Text(Utils.toRes(100).toFloat(), Utils.toRes(160).toFloat(), font,
            StringTable.get(R.string.menu_search_filter))
        capt1.setPosition(Config.getRES_WIDTH() / 4f - capt1.width, capt1.y)
        scene!!.attachChild(capt1)
        val filterBorder = Rectangle(capt1.x, Utils.toRes(195).toFloat(),
            Utils.toRes(330).toFloat(), capt1.height + Utils.toRes(30).toFloat())
        scene!!.attachChild(filterBorder)
        filterBorder.setColor(1f, 150f / 255, 0f)
        filterBorder.setVisible(false)
        val filterBg = object : Rectangle(filterBorder.x + 5, Utils.toRes(200).toFloat(),
            Utils.toRes(320).toFloat(), capt1.height + Utils.toRes(20).toFloat()) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    filterBorder.setVisible(true)
                    InputManager.getInstance().startInput(FilterMenu.getInstance().getFilter(), 20)
                    return true
                }
                return false
            }
        }
        scene!!.registerTouchArea(filterBg)
        scene!!.attachChild(filterBg)
        filterText = ChangeableText(capt1.x, Utils.toRes(210).toFloat(), font, filter, 21)
        filterText!!.setColor(0f, 0f, 0f)
        scene!!.attachChild(filterText)
        val capt2 = Text(Utils.toRes(700).toFloat(), Utils.toRes(160).toFloat(), font,
            StringTable.get(R.string.menu_search_sort))
        capt2.setPosition(Config.getRES_WIDTH() * 2f / 3 - capt2.width, capt2.y)
        scene!!.attachChild(capt2)
        val sortBg = object : Rectangle(capt2.x, Utils.toRes(200).toFloat(),
            Utils.toRes(200).toFloat(), capt2.height + Utils.toRes(20).toFloat()) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    val newOrder = when (order) {
                        SortOrder.Title -> SortOrder.Artist
                        SortOrder.Artist -> SortOrder.Creator
                        SortOrder.Creator -> SortOrder.Date
                        SortOrder.Date -> SortOrder.Bpm
                        SortOrder.Bpm -> SortOrder.Stars
                        SortOrder.Stars -> SortOrder.Length
                        else -> SortOrder.Title
                    }
                    order = newOrder
                    setSortText()
                    saveConfig()
                    return true
                }
                return false
            }
        }
        scene!!.registerTouchArea(sortBg)
        scene!!.attachChild(sortBg)
        sortText = ChangeableText(capt2.x + 5, Utils.toRes(210).toFloat(), font,
            StringTable.get(R.string.menu_search_sort_title), 10)
        sortText!!.setColor(0f, 0f, 0f)
        setSortText()
        sortText?.detachSelf()
        scene!!.attachChild(sortText)
        val back = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"),
            StringTable.get(R.string.menu_mod_back)) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionUp) {
                    hideMenu()
                    return true
                }
                return false
            }
        }
        back.setWidth(Utils.toRes(400).toFloat())
        back.setScale(1.2f)
        back.setPosition(Config.getRES_WIDTH() / 2f - back.getWidth() / 2, Config.getRES_HEIGHT() * 3f / 4 - back.getHeight() / 2)
        back.setColor(66 / 255f, 76 / 255f, 80 / 255f)
        scene!!.attachChild(back)
        scene!!.registerTouchArea(back)
        val favs = object : ChangeableText(capt1.x, Utils.toRes(300).toFloat(),
            ResourceManager.getInstance().getFont("CaptionFont"),
            StringTable.get(R.string.menu_search_favsdisabled)) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionUp) {
                    if (favoritesOnly) {
                        setText(StringTable.get(R.string.menu_search_favsdisabled))
                        setColor(1f, 1f, 1f)
                    } else {
                        setText(StringTable.get(R.string.menu_search_favsenabled))
                        setColor(0f, 1f, 0f)
                    }
                    favoritesOnly = !favoritesOnly
                    return true
                }
                return false
            }
        }
        if (favoritesOnly) {
            favs.setText(StringTable.get(R.string.menu_search_favsenabled))
            favs.setColor(0f, 1f, 0f)
        }
        favs.setPosition(capt1.x, favs.y)
        scene!!.attachChild(favs)
        scene!!.registerTouchArea(favs)
        val folder = object : ChangeableText(favs.x, favs.y + favs.height + Utils.toRes(20),
            ResourceManager.getInstance().getFont("CaptionFont"),
            StringTable.get(R.string.favorite_folder) + " " + (favoriteFolder ?: StringTable.get(R.string.favorite_default)), 40) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionUp) {
                    GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
                        val dialog = com.edlplan.ui.fragment.FavoriteManagerFragment()
                        dialog.showToSelectFolder { folder1 ->
                            favoriteFolder = folder1
                            setText(StringTable.get(R.string.favorite_folder) + " " + (favoriteFolder ?: StringTable.get(R.string.favorite_default)))
                        }
                    }
                    return true
                }
                return false
            }
        }
        folder.setPosition(favs.x, favs.y + favs.height + Utils.toRes(20))
        scene!!.attachChild(folder)
        scene!!.registerTouchArea(folder)
        scene!!.registerUpdateHandler(this)
        scene!!.setTouchAreaBindingEnabled(true)
    }

    override fun hideMenu() {
        if (menu != null) {
            menu!!.getScene().clearChildScene()
            menu!!.loadFilter(this)
            scene = null
        }
    }

    override fun showMenu(parent: SongMenu) {
        reload()
        setSongMenu(parent)
        parent.scene?.setChildScene(scene, false, true, true)
    }

    private fun setSortText() {
        if (sortText == null) return
        val s = when (order) {
            SortOrder.Title -> StringTable.get(R.string.menu_search_sort_title)
            SortOrder.Artist -> StringTable.get(R.string.menu_search_sort_artist)
            SortOrder.Date -> StringTable.get(R.string.menu_search_sort_date)
            SortOrder.Bpm -> StringTable.get(R.string.menu_search_sort_bpm)
            SortOrder.Stars -> StringTable.get(R.string.menu_search_sort_stars)
            SortOrder.Length -> StringTable.get(R.string.menu_search_sort_length)
            else -> StringTable.get(R.string.menu_search_sort_creator)
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post { sortText?.setText(s) }
    }

    override fun getScene(): Scene {
        if (scene == null) init()
        return scene!!
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        if (InputManager.getInstance().isChanged()) {
            filter = InputManager.getInstance().getText()
            filterText?.setText(filter)
        }
    }

    override fun reset() {}

    override fun getOrder(): SortOrder = order
    override fun getFilter(): String = filter
    override fun isFavoritesOnly(): Boolean = favoritesOnly

    companion object {
        private var instance: IFilterMenu? = null

        @JvmStatic
        fun getInstance(): IFilterMenu {
            if (instance == null) {
                instance = FilterMenu()
            }
            return instance!!
        }
    }
}

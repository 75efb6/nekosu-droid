package com.edlplan.ui.fragment

import android.animation.Animator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edlplan.favorite.FavoriteLibrary
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.InputDialog
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class FavoriteManagerFragment : BaseFragment() {

    private var selected: String? = null

    private var adapter: FMAdapter? = null

    private var onLoadViewFunc: Runnable? = null

    override val layoutID: Int
        get() = R.layout.favorite_manager_dialog

    override fun onLoadView() {
        isDismissOnBackgroundClick = true
        val layoutManager = GridLayoutManager(context, 2)
        findViewById<RecyclerView>(R.id.main_recycler_view)!!.layoutManager = layoutManager
        layoutManager.orientation = RecyclerView.VERTICAL

        val newFolder = findViewById<Button>(R.id.new_folder)!!
        newFolder.setOnClickListener {
            val dialog = InputDialog(context!!)
            dialog.showForResult { s: String ->
                if (s.isEmpty()) return@showForResult
                if (FavoriteLibrary.get().getMaps(s) == null && s != StringTable.get(R.string.favorite_default)) {
                    FavoriteLibrary.get().addFolder(s)
                    adapter?.add(s)
                }
            }
        }

        onLoadViewFunc?.run()

        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.alpha = 0f
        body.translationY = 500f
        body.animate().cancel()
        body.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutCubic))
            .start()
        playBackgroundHideInAnim(220)
    }

    private fun playEndAnim(action: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.animate().cancel()
        body.animate()
            .alpha(0f)
            .translationY(500f)
            .setDuration(180)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    action?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    override fun dismiss() {
        playEndAnim { super.dismiss() }
    }

    fun getSelected(): String? = selected

    fun showToSelectFolder(onSelectListener: OnSelectListener?) {
        onLoadViewFunc = Runnable {
            adapter = SelectAdapter(onSelectListener)
            findViewById<RecyclerView>(R.id.main_recycler_view)!!.adapter = adapter
        }
        show()
    }

    fun showToAddToFolder(track: String) {
        onLoadViewFunc = Runnable {
            adapter = AddAdapter(track)
            findViewById<RecyclerView>(R.id.main_recycler_view)!!.adapter = adapter
        }
        show()
    }

    fun interface OnSelectListener {
        fun onSelect(folder: String?)
    }

    internal class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var folderName: TextView = itemView.findViewById(R.id.folder_name)
        var button1: Button = itemView.findViewById(R.id.button)
        var button2: Button = itemView.findViewById(R.id.button2)
        var mainBody: View = itemView.findViewById(R.id.mainBody)
    }

    internal abstract inner class FMAdapter : RecyclerView.Adapter<VH>() {
        protected var folders: MutableList<String> = mutableListOf()

        init {
            load()
        }

        fun add(folder: String) {
            val tmp = ArrayList(folders)
            folders = mutableListOf()
            folders.add(tmp[0])
            folders.add(folder)
            tmp.removeAt(0)
            folders.addAll(tmp)
            notifyDataSetChanged()
        }

        protected fun load() {
            val tmp = ArrayList(FavoriteLibrary.get().getFolders())
            tmp.sort()
            folders = mutableListOf()
            folders.add(StringTable.get(R.string.favorite_default))
            folders.addAll(tmp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.favorite_folder_item, parent, false)
            )
        }

        override fun getItemCount(): Int = folders.size
    }

    inner class AddAdapter(private val track: String) : FMAdapter() {

        override fun onBindViewHolder(holder: VH, position: Int) {
            val f = folders[position]
            holder.folderName.text = String.format(
                "%s (%s)",
                f,
                FavoriteLibrary.get().getMaps(f)?.size?.toString() ?: "*"
            )
            holder.button1.setText(R.string.favorite_delete)
            if (position == 0) {
                holder.button2.text = "( • ̀ω•́ )✧"
            } else {
                holder.button2.setText(
                    if (FavoriteLibrary.get().`in`(f, track)) R.string.favorite_remove
                    else R.string.favorite_add
                )
            }

            var clicked = false
            var deleted = false

            holder.button1.setOnClickListener {
                if (clicked) {
                    FavoriteLibrary.get().remove(f)
                    deleted = true
                    holder.button1.setTextColor(Color.argb(255, 255, 255, 255))
                    load()
                    notifyDataSetChanged()
                } else {
                    clicked = true
                    holder.button1.setText(R.string.favorite_ensure)
                    holder.button1.setTextColor(Color.argb(255, 255, 0, 0))
                    findViewById<View>(R.id.main_recycler_view)!!.postDelayed({
                        if (!deleted) {
                            clicked = false
                            holder.button1.setTextColor(Color.argb(255, 0, 0, 0))
                            holder.button1.setText(R.string.favorite_delete)
                        }
                    }, 2000)
                }
            }

            holder.button2.setOnClickListener {
                if (FavoriteLibrary.get().`in`(f, track)) {
                    FavoriteLibrary.get().remove(f, track)
                    holder.folderName.text = String.format(
                        "%s (%s)",
                        f,
                        FavoriteLibrary.get().getMaps(f)?.size?.toString() ?: "*"
                    )
                    holder.button2.setText(R.string.favorite_add)
                } else {
                    FavoriteLibrary.get().add(f, track)
                    holder.folderName.text = String.format(
                        "%s (%s)",
                        f,
                        FavoriteLibrary.get().getMaps(f)?.size?.toString() ?: "*"
                    )
                    holder.button2.setText(R.string.favorite_remove)
                }
            }

            if (position == 0) {
                holder.button1.setOnClickListener {
                    ToastLogger.showText(StringTable.get(R.string.favorite_warning_on_delete_default), false)
                }
                holder.button2.setOnClickListener {
                }
            }
        }
    }

    inner class SelectAdapter(private val onSelectListener: OnSelectListener?) : FMAdapter() {

        override fun onBindViewHolder(holder: VH, position: Int) {
            val f = folders[position]
            holder.folderName.text = String.format(
                "%s (%s)",
                f,
                FavoriteLibrary.get().getMaps(f)?.size?.toString() ?: "*"
            )
            holder.button1.setText(R.string.favorite_delete)
            holder.button2.setText(R.string.favorite_select)

            val mainClick: View.OnClickListener
            if (position != 0) {
                mainClick = View.OnClickListener {
                    selected = folders[position]
                    dismiss()
                    onSelectListener?.onSelect(selected)
                }
            } else {
                mainClick = View.OnClickListener {
                    selected = folders[position]
                    dismiss()
                    onSelectListener?.onSelect(null)
                }
            }

            holder.button2.setOnClickListener(mainClick)
            holder.mainBody.setOnClickListener(mainClick)

            var clicked = false
            var deleted = false

            holder.button1.setOnClickListener {
                if (clicked) {
                    FavoriteLibrary.get().remove(f)
                    deleted = true
                    holder.button1.setTextColor(Color.argb(255, 255, 255, 255))
                    load()
                    notifyDataSetChanged()
                } else {
                    clicked = true
                    holder.button1.setText(R.string.favorite_ensure)
                    holder.button1.setTextColor(Color.argb(255, 255, 0, 0))
                    findViewById<View>(R.id.main_recycler_view)!!.postDelayed({
                        if (!deleted) {
                            clicked = false
                            holder.button1.setTextColor(Color.argb(255, 255, 255, 255))
                            holder.button1.setText(R.string.favorite_delete)
                        }
                    }, 2000)
                }
            }

            if (position == 0) {
                holder.button1.setOnClickListener {
                    ToastLogger.showText(StringTable.get(R.string.favorite_warning_on_delete_default), false)
                }
            }
        }
    }
}

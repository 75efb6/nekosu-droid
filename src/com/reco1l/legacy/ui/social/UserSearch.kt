package com.reco1l.legacy.ui.social

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reco1l.api.server.SocialAPI
import com.reco1l.framework.lang.async
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osuplus.R

class UserSearch : DialogFragment() {

    private lateinit var searchField: EditText
    private lateinit var resultsList: RecyclerView
    private val results = mutableListOf<JSONObject>()
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.user_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchField = view.findViewById(R.id.searchField)
        resultsList = view.findViewById(R.id.resultsList)

        resultsList.layoutManager = LinearLayoutManager(context)
        adapter = SearchResultsAdapter(results) { userId, name ->
            val profile = ProfilePopup.newInstance(userId)
            profile.show()
            dismiss()
        }
        resultsList.adapter = adapter

        searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        view.findViewById<View>(R.id.cancelButton)?.setOnClickListener { dismiss() }

        // Auto-focus search field
        searchField.requestFocus()
    }

    private fun performSearch() {
        val query = searchField.text?.toString()?.trim() ?: return
        if (query.length < 2) return

        async {
            val result = SocialAPI.searchUsers(query) ?: return@async
            if (result.optString("status") != "success") return@async

            results.clear()
            val arr = result.optJSONArray("users")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    results.add(arr.getJSONObject(i))
                }
            }

            activity?.runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }
}

class SearchResultsAdapter(
    private val results: List<JSONObject>,
    private val onUserClick: (Long, String) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    override fun getItemCount() = results.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position], onUserClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendStatus: TextView = itemView.findViewById(R.id.friendStatus)
        private val messageButton: TextView = itemView.findViewById(R.id.messageButton)
        private val acceptButton: TextView = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: TextView = itemView.findViewById(R.id.rejectButton)

        fun bind(item: JSONObject, onClick: (Long, String) -> Unit) {
            val id = item.getLong("id")
            val name = item.optString("username", "Unknown")
            val online = item.optBoolean("online", false)
            val stats = item.optJSONObject("stats")

            friendName.text = name
            if (stats != null) {
                val pp = stats.optInt("pp", 0)
                val rank = stats.optInt("rank", 0)
                friendStatus.text = "${pp}pp | #$rank"
            } else {
                friendStatus.text = "No plays yet"
            }

            onlineIndicator.setBackgroundColor(if (online) 0xFF4CAF50.toInt() else Color.GRAY)

            messageButton.visibility = View.GONE
            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.GONE

            itemView.setOnClickListener { onClick(id, name) }
        }
    }
}

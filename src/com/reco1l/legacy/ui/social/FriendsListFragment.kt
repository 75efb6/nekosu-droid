package com.reco1l.legacy.ui.social

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reco1l.api.server.SocialAPI
import com.reco1l.api.server.ServerAPI
import com.reco1l.framework.lang.async
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class FriendsListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pendingHeader: TextView
    private lateinit var pendingList: RecyclerView
    private lateinit var pendingDivider: View

    private val friends = mutableListOf<JSONObject>()
    private val pendingRequests = mutableListOf<JSONObject>()
    private val outgoingRequests = mutableListOf<JSONObject>()

    private lateinit var adapter: FriendsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.friends_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.friendsList)
        pendingHeader = view.findViewById(R.id.pendingHeader)
        pendingList = view.findViewById(R.id.pendingList)
        pendingDivider = view.findViewById(R.id.pendingDivider)

        recyclerView.layoutManager = LinearLayoutManager(context)
        pendingList.layoutManager = LinearLayoutManager(context)

        adapter = FriendsAdapter(
            friends = friends,
            pendingRequests = pendingRequests,
            outgoingRequests = outgoingRequests,
            onAccept = { friendId -> acceptFriend(friendId) },
            onReject = { friendId -> rejectFriend(friendId) },
            onMessage = { friendId, name -> openChat(friendId, name) },
            onProfile = { friendId -> openProfile(friendId) }
        )
        recyclerView.adapter = adapter

        loadFriends()
    }

    private fun loadFriends() {
        val userId = OnlineManager.getInstance().userId
        async {
            val result = SocialAPI.getFriends(userId) ?: return@async
            if (result.optString("status") != "success") return@async

            friends.clear()
            pendingRequests.clear()
            outgoingRequests.clear()

            val friendsArr = result.optJSONArray("friends")
            if (friendsArr != null) {
                for (i in 0 until friendsArr.length()) {
                    friends.add(friendsArr.getJSONObject(i))
                }
            }

            val pendingArr = result.optJSONArray("pending")
            if (pendingArr != null) {
                for (i in 0 until pendingArr.length()) {
                    pendingRequests.add(pendingArr.getJSONObject(i))
                }
            }

            val outgoingArr = result.optJSONArray("outgoing")
            if (outgoingArr != null) {
                for (i in 0 until outgoingArr.length()) {
                    outgoingRequests.add(outgoingArr.getJSONObject(i))
                }
            }

            activity?.runOnUiThread {
                pendingHeader.visibility = if (pendingRequests.isNotEmpty()) View.VISIBLE else View.GONE
                pendingList.visibility = if (pendingRequests.isNotEmpty()) View.VISIBLE else View.GONE
                pendingDivider.visibility = if (pendingRequests.isNotEmpty()) View.VISIBLE else View.GONE
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun acceptFriend(friendId: Long) {
        val userId = OnlineManager.getInstance().userId
        async {
            SocialAPI.acceptFriend(userId, friendId)
            loadFriends()
        }
    }

    private fun rejectFriend(friendId: Long) {
        val userId = OnlineManager.getInstance().userId
        async {
            SocialAPI.rejectFriend(userId, friendId)
            loadFriends()
        }
    }

    private fun openChat(friendId: Long, name: String) {
        val chatScene = ChatScene.newInstance(friendId, name)
        chatScene.show()
    }

    private fun openProfile(friendId: Long) {
        val profile = ProfilePopup.newInstance(friendId)
        profile.show()
    }

    companion object {
        fun newInstance() = FriendsListFragment()
    }
}

class FriendsAdapter(
    private val friends: List<JSONObject>,
    private val pendingRequests: List<JSONObject>,
    private val outgoingRequests: List<JSONObject>,
    private val onAccept: (Long) -> Unit,
    private val onReject: (Long) -> Unit,
    private val onMessage: (Long, String) -> Unit,
    private val onProfile: (Long) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    companion object {
        private const val TYPE_PENDING = 0
        private const val TYPE_FRIEND = 1
    }

    override fun getItemCount(): Int = pendingRequests.size + friends.size

    override fun getItemViewType(position: Int): Int {
        return if (position < pendingRequests.size) TYPE_PENDING else TYPE_FRIEND
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.friend_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_PENDING) {
            val item = pendingRequests[position]
            holder.bindPending(item, onAccept, onReject, onProfile)
        } else {
            val item = friends[position - pendingRequests.size]
            holder.bindFriend(item, onMessage, onProfile)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendStatus: TextView = itemView.findViewById(R.id.friendStatus)
        private val messageButton: TextView = itemView.findViewById(R.id.messageButton)
        private val acceptButton: TextView = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: TextView = itemView.findViewById(R.id.rejectButton)

        fun bindPending(item: JSONObject, onAccept: (Long) -> Unit, onReject: (Long) -> Unit, onProfile: (Long) -> Unit) {
            val id = item.getLong("id")
            friendName.text = item.optString("username", "Unknown")
            friendStatus.text = "Wants to be your friend"
            friendStatus.setTextColor(0xFFE63E8C.toInt())

            onlineIndicator.setBackgroundColor(Color.GRAY)
            messageButton.visibility = View.GONE
            acceptButton.visibility = View.VISIBLE
            rejectButton.visibility = View.VISIBLE

            acceptButton.setOnClickListener { onAccept(id) }
            rejectButton.setOnClickListener { onReject(id) }
            itemView.setOnClickListener { onProfile(id) }
        }

        fun bindFriend(item: JSONObject, onMessage: (Long, String) -> Unit, onProfile: (Long) -> Unit) {
            val id = item.getLong("id")
            val name = item.optString("username", "Unknown")
            val online = item.optBoolean("online", false)
            val status = item.optString("status", "offline")

            friendName.text = name
            friendStatus.text = if (online) status.replaceFirstChar { it.uppercase() } else "Offline"
            friendStatus.setTextColor(if (online) 0xFF4CAF50.toInt() else 0xFF9999BB.toInt())

            onlineIndicator.setBackgroundColor(if (online) 0xFF4CAF50.toInt() else Color.GRAY)
            messageButton.visibility = View.VISIBLE
            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.GONE

            messageButton.setOnClickListener { onMessage(id, name) }
            itemView.setOnClickListener { onProfile(id) }
        }
    }
}

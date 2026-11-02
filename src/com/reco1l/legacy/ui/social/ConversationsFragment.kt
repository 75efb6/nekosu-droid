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
import com.reco1l.framework.lang.async
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class ConversationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val conversations = mutableListOf<JSONObject>()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.conversations_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.conversationList)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ConversationsAdapter(conversations) { userId, name ->
            val chatScene = ChatScene.newInstance(userId, name)
            chatScene.show()
        }
        recyclerView.adapter = adapter

        loadConversations()
    }

    private fun loadConversations() {
        val userId = OnlineManager.getInstance().userId
        async {
            val result = SocialAPI.getConversations(userId) ?: return@async
            if (result.optString("status") != "success") return@async

            conversations.clear()
            val arr = result.optJSONArray("conversations")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    conversations.add(arr.getJSONObject(i))
                }
            }

            activity?.runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    companion object {
        fun newInstance() = ConversationsFragment()
    }
}

class ConversationsAdapter(
    private val conversations: List<JSONObject>,
    private val onConversationClick: (Long, String) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    override fun getItemCount() = conversations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.conversation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position], onConversationClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val conversationName: TextView = itemView.findViewById(R.id.conversationName)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)

        fun bind(item: JSONObject, onClick: (Long, String) -> Unit) {
            val userId = item.getLong("userId")
            val name = item.optString("username", "Unknown")
            val online = item.optBoolean("online", false)
            val msg = item.optString("lastMessage", "")
            val unread = item.optInt("unread", 0)

            conversationName.text = name
            lastMessage.text = msg
            onlineIndicator.setBackgroundColor(if (online) 0xFF4CAF50.toInt() else Color.GRAY)

            if (unread > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unread > 99) "99+" else unread.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(userId, name) }
        }
    }
}

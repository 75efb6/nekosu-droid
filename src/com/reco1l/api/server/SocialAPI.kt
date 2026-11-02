package com.reco1l.api.server

import org.json.JSONObject

/**
 * Social features API. All calls go through [ServerAPI]'s socket connection.
 */
object SocialAPI {

    // =========================================================
    // FRIENDS
    // =========================================================

    fun addFriend(userId: Long, targetId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("targetId", targetId.toString())
        }
        return ServerAPI.emitWithCallback("add_friend", "add_friend_result", data)
    }

    fun removeFriend(userId: Long, friendId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("friendId", friendId.toString())
        }
        return ServerAPI.emitWithCallback("remove_friend", "remove_friend_result", data)
    }

    fun acceptFriend(userId: Long, friendId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("friendId", friendId.toString())
        }
        return ServerAPI.emitWithCallback("accept_friend", "accept_friend_result", data)
    }

    fun rejectFriend(userId: Long, friendId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("friendId", friendId.toString())
        }
        return ServerAPI.emitWithCallback("reject_friend", "reject_friend_result", data)
    }

    fun getFriends(userId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
        }
        return ServerAPI.emitWithCallback("get_friends", "get_friends_result", data)
    }

    // =========================================================
    // BLOCKS
    // =========================================================

    fun blockUser(userId: Long, targetId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("targetId", targetId.toString())
        }
        return ServerAPI.emitWithCallback("block_user", "block_user_result", data)
    }

    fun unblockUser(userId: Long, targetId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("targetId", targetId.toString())
        }
        return ServerAPI.emitWithCallback("unblock_user", "unblock_user_result", data)
    }

    fun getBlocks(userId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
        }
        return ServerAPI.emitWithCallback("get_blocks", "get_blocks_result", data)
    }

    // =========================================================
    // MESSAGES
    // =========================================================

    fun sendMessage(userId: Long, receiverId: Long, content: String): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("receiverId", receiverId.toString())
            put("content", content)
        }
        return ServerAPI.emitWithCallback("send_message", "send_message_result", data)
    }

    fun getMessages(userId: Long, otherId: Long, before: Long? = null, limit: Int = 50): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("otherId", otherId.toString())
            put("limit", limit)
            if (before != null) put("before", before.toString())
        }
        return ServerAPI.emitWithCallback("get_messages", "get_messages_result", data)
    }

    fun getConversations(userId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
        }
        return ServerAPI.emitWithCallback("get_conversations", "get_conversations_result", data)
    }

    fun markRead(userId: Long, otherId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("otherId", otherId.toString())
        }
        return ServerAPI.emitWithCallback("mark_read", "mark_read_result", data)
    }

    // =========================================================
    // PRESENCE
    // =========================================================

    fun getPresence(userIds: List<Long>): JSONObject? {
        val data = JSONObject().apply {
            val arr = org.json.JSONArray()
            for (id in userIds) arr.put(id.toString())
            put("userIds", arr)
        }
        return ServerAPI.emitWithCallback("get_presence", "get_presence_result", data)
    }

    fun updateStatus(userId: Long, status: String) {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("status", status)
        }
        ServerAPI.emit("update_status", data)
    }

    // =========================================================
    // PROFILE
    // =========================================================

    fun getProfile(userId: Long, targetId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("targetId", targetId.toString())
        }
        return ServerAPI.emitWithCallback("get_profile", "get_profile_result", data)
    }

    fun setProfileNote(userId: Long, targetId: Long, note: String): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("targetId", targetId.toString())
            put("note", note)
        }
        return ServerAPI.emitWithCallback("set_profile_note", "set_profile_note_result", data)
    }

    // =========================================================
    // RECENT PLAYERS
    // =========================================================

    fun addRecentPlayer(userId: Long, metId: Long) {
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("metId", metId.toString())
        }
        ServerAPI.emit("add_recent_player", data)
    }

    fun getRecentPlayers(userId: Long): JSONObject? {
        val data = JSONObject().apply {
            put("userId", userId.toString())
        }
        return ServerAPI.emitWithCallback("get_recent_players", "get_recent_players_result", data)
    }

    // =========================================================
    // USER SEARCH
    // =========================================================

    fun searchUsers(query: String, limit: Int = 20): JSONObject? {
        val data = JSONObject().apply {
            put("query", query)
            put("limit", limit)
        }
        return ServerAPI.emitWithCallback("search_users", "search_users_result", data)
    }
}

// Create new file: utils/ChatStateManager.kt
package com.example.Smart_Chat.utils.others

object ChatStateManager {
    private var currentChatRoomID: String? = null
    private var currentGroupID: String? = null

    fun setCurrentChat(chatRoomID: String) {
        currentChatRoomID = chatRoomID
        currentGroupID = null
    }

    fun setCurrentGroup(groupID: String) {
        currentGroupID = groupID
        currentChatRoomID = null
    }

    fun clearCurrentChat() {
        currentChatRoomID = null
        currentGroupID = null
    }

    fun isInChat(chatRoomID: String): Boolean {
        return currentChatRoomID == chatRoomID
    }

    fun isInGroup(groupID: String): Boolean {
        return currentGroupID == groupID
    }
}
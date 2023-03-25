package com.example.chatmate

data class Message(
    val id: Long,
    val content: String,
    val viewType: Int
) {
    companion object {
        const val VIEW_TYPE_MESSAGE = 1
        const val VIEW_TYPE_LOADING = 2
    }
}

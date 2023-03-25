package com.example.chatmate

data class Message(
    val id: Long,
    var content: String,
    var viewType: Int
) {
    companion object {
        const val VIEW_TYPE_MESSAGE = 1
        const val VIEW_TYPE_LOADING = 2
    }
}

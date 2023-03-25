package com.example.chatmate

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatRole

data class Message @OptIn(BetaOpenAI::class) constructor(
    val id: Long,
    var content: String,
    var viewType: Int,
    val role: ChatRole,
) {
    companion object {
        const val VIEW_TYPE_MESSAGE = 1
        const val VIEW_TYPE_LOADING = 2
    }
}

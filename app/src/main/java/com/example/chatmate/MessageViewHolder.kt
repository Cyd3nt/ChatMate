package com.example.chatmate

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val messageTextView: TextView = view.findViewById(R.id.message_text_view)
}

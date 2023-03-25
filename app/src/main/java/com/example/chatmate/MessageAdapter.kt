package com.example.chatmate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import kotlin.math.min

data class CodeBlockPosition(val start: Int, val end: Int)

//class RoundedBackgroundSpan(
//    private val backgroundColor: Int,
//    private val cornerRadius: Float
//) : LineBackgroundSpan {
//    override fun drawBackground(
//        canvas: Canvas,
//        paint: Paint,
//        left: Int,
//        right: Int,
//        top: Int,
//        baseline: Int,
//        bottom: Int,
//        text: CharSequence,
//        start: Int,
//        end: Int,
//        lineNumber: Int
//    ) {
//        val oldColor = paint.color
//        paint.color = backgroundColor
//
//        val rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
//        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
//
//        paint.color = oldColor
//    }
//}

class CodeBlockSpan(
    private val backgroundColor: Int,
    private val cornerRadius: Float,
    private val padding: Float,
    private val leftPadding: Float,
    private val topPadding: Float
) : LeadingMarginSpan {
    private var lastTop = 0

    override fun getLeadingMargin(first: Boolean): Int = 0

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        if (lastTop != top) {
            val oldColor = paint.color
            paint.color = backgroundColor

//            val left = x + padding * dir
//            val right = (canvas.width - padding) * dir
            val left = x + leftPadding * dir
            val right = canvas.width * dir
            val topAdjusted = top.toFloat() - if (first) 0f else topPadding

            val rect = if (first) {
                RectF(left, top.toFloat(), right.toFloat(), (bottom + padding).toFloat())
            } else {
                RectF(left, top.toFloat() - padding, right.toFloat(), bottom.toFloat())
            }

            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.color = oldColor

            lastTop = top
        }
    }
}

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text_view)
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val loadingIndicator: ProgressBar = view.findViewById(R.id.loading_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            Message.VIEW_TYPE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_item, parent, false)
                MessageViewHolder(view)
            }
            Message.VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.loading_item, parent, false)
                LoadingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

//    fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
//        val regex = Regex("```.*?```", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
//        val result = mutableListOf<CodeBlockPosition>()
//        regex.findAll(text).forEach { matchResult ->
//            result.add(CodeBlockPosition(matchResult.range.first, matchResult.range.last))
//        }
//        return result
//    }
//    fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
//        val regex = Regex("```.*?(?:```|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
//        val result = mutableListOf<CodeBlockPosition>()
//        regex.findAll(text).forEach { matchResult ->
//            result.add(CodeBlockPosition(matchResult.range.first, matchResult.range.last))
//        }
//        return result
//    }
//    fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
//        val regex = Regex("(?=```).*?(?:```|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
//        val result = mutableListOf<CodeBlockPosition>()
//        regex.findAll(text).forEach { matchResult ->
//            result.add(CodeBlockPosition(matchResult.range.first, matchResult.range.last))
//        }
//        return result
//    }
    fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
        val regex = Regex("```")
        val result = mutableListOf<CodeBlockPosition>()

        val backtickPositions = regex.findAll(text).map { it.range.first }.toList()
        val numberOfPairs = backtickPositions.size / 2

        for (i in 0 until numberOfPairs) {
            val startPosition = backtickPositions[2 * i]
            val endPosition = backtickPositions[2 * i + 1]
            result.add(CodeBlockPosition(startPosition, endPosition))
        }

        // Handle open code blocks without an ending
        if (backtickPositions.size % 2 == 1) {
            result.add(CodeBlockPosition(backtickPositions.last(), text.length))
        }

        return result
    }

//    fun styleCodeBlocks(text: String, context: Context): SpannableString {
//        val spannable = SpannableString(text)
//        val codeBlockPositions = findCodeBlockPositions(text)
//
//        for (position in codeBlockPositions) {
//            // Set custom background color using BackgroundColorSpan
//            val backgroundColorSpan = BackgroundColorSpan(ContextCompat.getColor(context, R.color.black))
//            spannable.setSpan(backgroundColorSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom text color using ForegroundColorSpan
//            val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.white))
//            spannable.setSpan(foregroundColorSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom typeface using TypeFaceSpan
//            val typeFaceSpan = TypefaceSpan("monospace")
//            spannable.setSpan(typeFaceSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//
//        return spannable
//    }
//    fun styleCodeBlocks(text: String, context: Context): SpannableString {
//        val spannable = SpannableString(text)
//        val codeBlockPositions = findCodeBlockPositions(text)
//
//        for (position in codeBlockPositions) {
//            // Set custom background using RoundedBackgroundSpan
//            val backgroundColor = ContextCompat.getColor(context, R.color.black)
//            val cornerRadius = context.resources.getDimension(R.dimen.code_block_corner_radius)
//            val backgroundSpan = RoundedBackgroundSpan(backgroundColor, cornerRadius)
//            spannable.setSpan(backgroundSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom text color using ForegroundColorSpan
//            val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.white))
//            spannable.setSpan(foregroundColorSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom typeface using TypefaceSpan
//            val typeFaceSpan = TypefaceSpan("monospace")
//            spannable.setSpan(typeFaceSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//
//        return spannable
//    }
//    fun styleCodeBlocks(text: String, context: Context): SpannableString {
//        val spannable = SpannableString(text)
//        val codeBlockPositions = findCodeBlockPositions(text)
//
//        for (position in codeBlockPositions) {
//            // Set custom background using CodeBlockSpan
//            val backgroundColor = ContextCompat.getColor(context, R.color.black)
//            val cornerRadius = context.resources.getDimension(R.dimen.code_block_corner_radius)
//            val padding = context.resources.getDimension(R.dimen.code_block_padding)
//            val backgroundSpan = CodeBlockSpan(backgroundColor, cornerRadius, padding)
//            spannable.setSpan(backgroundSpan, position.start, position.end + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom text color using ForegroundColorSpan
//            val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.white))
//            spannable.setSpan(foregroundColorSpan, position.start, position.end + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//            // Set custom typeface using TypefaceSpan
//            val typeFaceSpan = TypefaceSpan("monospace")
//            spannable.setSpan(typeFaceSpan, position.start, position.end + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//
//        return spannable
//    }
    fun styleCodeBlocks(text: String, context: Context): SpannableString {
        val spannable = SpannableString(text)
        val codeBlockPositions = findCodeBlockPositions(text)

        for (position in codeBlockPositions) {
            val startPos = position.start
            val endPos = min(position.end + 3, text.length)

            // Set custom background using CodeBlockSpan
//            val backgroundColor = ContextCompat.getColor(context, R.color.black)
//            val cornerRadius = context.resources.getDimension(R.dimen.code_block_corner_radius)
//            val padding = context.resources.getDimension(R.dimen.code_block_padding)
//            val backgroundSpan = CodeBlockSpan(backgroundColor, cornerRadius, padding)
            val backgroundColor = ContextCompat.getColor(context, R.color.black)
            val cornerRadius = context.resources.getDimension(R.dimen.code_block_corner_radius)
            val padding = context.resources.getDimension(R.dimen.code_block_padding)
            val leftPadding = context.resources.getDimension(R.dimen.code_block_left_padding)
            val topPadding = context.resources.getDimension(R.dimen.code_block_top_padding)
            val backgroundSpan = CodeBlockSpan(backgroundColor, cornerRadius, padding, leftPadding, topPadding)
            spannable.setSpan(backgroundSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set custom text color using ForegroundColorSpan
            val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.white))
            spannable.setSpan(foregroundColorSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set custom typeface using TypefaceSpan
            val typeFaceSpan = TypefaceSpan("monospace")
            spannable.setSpan(typeFaceSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val currentItem = messages[position]) {
            is Message -> {
                if (holder is MessageViewHolder) {
                    val styledMessage = styleCodeBlocks(currentItem.content, holder.itemView.context)
//                    holder.messageTextView.text = currentItem.content
                    holder.messageTextView.text = styledMessage
//                    val messageTextView = holder.itemView.findViewById<TextView>(R.id.message_text_view)
//                    messageTextView.text = styledMessage

//                    if (isCodeBlock(currentItem.content)) {
//                        // Apply custom styling for code blocks
//                        messageTextView.setBackgroundResource(R.drawable.message_code_block_item_background)
//                        // Set any additional properties, e.g., typeface, text color, etc.
//                        messageTextView.typeface = Typeface.MONOSPACE
//                        messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
//                    } else {
//                        // Apply default styling for non-code block messages
//                        messageTextView.setBackgroundResource(R.drawable.message_item_background)
//                        // Reset any additional properties
//                        messageTextView.typeface = Typeface.DEFAULT
//                        messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
//                    }

//                    val messageTextView = holder.itemView.findViewById<TextView>(R.id.message_text_view)
//                    val message = messagesList[position] // Retrieve the message from your data set
//
//                    if (isCodeBlock(message)) {
//                        // Apply custom styling for code blocks
//                        messageTextView.setBackgroundResource(R.drawable.message_code_block_item_background)
//                        // Set any additional properties, e.g., typeface, text color, etc.
//                        messageTextView.typeface = Typeface.MONOSPACE
//                        messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.your_custom_color))
//                    } else {
//                        // Apply default styling for non-code block messages
//                        messageTextView.setBackgroundResource(R.drawable.message_item_background)
//                        // Reset any additional properties
//                        messageTextView.typeface = Typeface.DEFAULT
//                        messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.default_text_color))
//                    }
//
//                    messageTextView.text = message
                }
            }
        }
    }

    fun isCodeBlock(text: String): Boolean {
        // Add your logic to detect code blocks in your messages.
        // The following is a simple example using triple backticks.
        return text.contains("```")
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return messages[position].viewType
    }

    fun updateMessageContent(position: Int, content: String) {
        messages[position].content = content
        notifyItemChanged(position)
    }

    @OptIn(BetaOpenAI::class)
    fun getChatCompletionsList(): List<ChatMessage> {
        return messages.map { message: Message -> ChatMessage(message.role, message.content)  }
    }
}

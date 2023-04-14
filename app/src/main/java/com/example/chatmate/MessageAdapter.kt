package com.example.chatmate

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import kotlin.math.min

// if ("```" in response) {
//     val parser = Parser.builder().build()
//     val document = parser.parse(response)

//     val visitor = NodeVisitor(
//         VisitHandler(FencedCodeBlock::class.java, Visitor { node: FencedCodeBlock ->
//             val language = node.info
//             val codeBlock = node.contentChars
//             println("Language: $language\nCode Block: $codeBlock\n")
//         })
//     )

//     visitor.visit(document)
// }

/**
 * MessageAdapter is a RecyclerView.Adapter responsible for managing and rendering messages.
 */
class MessageAdapter(context: Context, private val messages: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeface: Typeface = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")

    /**
     * onCreateViewHolder is used to create a new view holder based on the provided view type.
     */
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

    /**
     * findCodeBlockPositions is a helper function that detects the positions of code blocks in a given text.
     */
    private fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
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

    /**
     * styleCodeBlocks styles the code blocks in the given text using customized span instances.
     */
    private fun styleCodeBlocks(text: String, context: Context): SpannableString {
        val spannable = SpannableString(text)
        val codeBlockPositions = findCodeBlockPositions(text)

        for (position in codeBlockPositions) {
            val startPos = position.start
            val endPos = min(position.end + 3, text.length)

            // Set custom background using CodeBlockSpan
            val backgroundColor = ContextCompat.getColor(context, R.color.black)
            val cornerRadius = context.resources.getDimension(R.dimen.code_block_corner_radius)
            val padding = context.resources.getDimension(R.dimen.code_block_padding)
            val leftPadding = context.resources.getDimension(R.dimen.code_block_left_padding)
//            val topPadding = context.resources.getDimension(R.dimen.code_block_top_padding)
            val backgroundSpan = CodeBlockSpan(backgroundColor, cornerRadius, padding, leftPadding)
            spannable.setSpan(backgroundSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set custom text color using ForegroundColorSpan
            val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context,
                R.color.white))
            spannable.setSpan(foregroundColorSpan, startPos, endPos,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val textSize = context.resources.getDimension(R.dimen.code_text_size)
            val indentation = context.resources.getDimension(R.dimen.code_indentation)

            val styledTextSpan = StyledTextSpan(typeface, textSize, indentation)
            spannable.setSpan(styledTextSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    @OptIn(BetaOpenAI::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = messages[position]
        if (holder is MessageViewHolder) {
            val message = currentItem.content

            // Set the background color based on the item's color property
            if (currentItem.role == ChatRole.User) {
                holder.messageTextView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            } else {
                holder.messageTextView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.grey))
            }

            // Set the layout height to wrap_content, to compress the item vertically according to the text size
            val layoutParams = holder.messageTextView.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.messageTextView.layoutParams = layoutParams

            if (isCodeBlock(message)) {
                holder.messageTextView.text =
                    styleCodeBlocks(currentItem.content, holder.itemView.context)
            } else {
                holder.messageTextView.text = message
            }
        }
    }

    /**
     * isCodeBlock checks if the given text contains a code block according to the required logic.
     */
    private fun isCodeBlock(text: String): Boolean {
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
        return messages.map { message: Message -> ChatMessage(message.role, message.content) }
    }
}

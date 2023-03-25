package com.example.chatmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.ui.AppBarConfiguration
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.chatmate.databinding.ActivityMainBinding
import com.example.chatmate.ui.login.LoginActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.util.ast.Visitor

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ORGANIZATION_KEY_ALIAS = "organization_alias"
        private const val API_KEY_ALIAS = "api_key_alias"
    }
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

//    private lateinit var job: Job
//
//    val coroutineContext: CoroutineContext
//        get() = Dispatchers.Main + job
    private lateinit var job: Job
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private suspend fun fetchModels(openAI: OpenAI): List<Model> {
        return openAI.models()
    }

    @OptIn(BetaOpenAI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        fun findCodeBlockPositions(text: String): List<CodeBlockPosition> {
//            val regex = Regex("(?=```).*?(?:```|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
//            val result = mutableListOf<CodeBlockPosition>()
//            regex.findAll(text).forEach { matchResult ->
//                result.add(CodeBlockPosition(matchResult.range.first, matchResult.range.last))
//            }
//            return result
//        }
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

        val code = """
            I apologize for the confusion. Let's change the approach to use `LineHeightSpan` and modify the `styleCodeBlocks` function. This method should style the complete code block, including the lines in between, while keeping the text properly aligned.

            1. Create a custom `LineHeightSpan` that adds a background with rounded corners:

            ```kotlin
            class RoundedBackgroundWithIncreasedHeightSpan(
                private val backgroundColor: Int,
                private val cornerRadius: Float,
                private val extraLineHeight: Int
            ) : LineHeightSpan {
                override fun chooseHeight(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    spanstartv: Int,
                    lineHeight: Int,
                    fm: Paint.FontMetricsInt
                ) {
                    fm.descent += extraLineHeight / 2
                    fm.ascent -= extraLineHeight / 2
                    fm.top = fm.ascent
                    fm.bottom = fm.descent
                }

                override fun draw(
                    canvas: Canvas,
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    top: Int,
                    y: Int,
                    bottom: Int,
                    paint: Paint
                ) {
                    val oldColor = paint.color
                    paint.color = backgroundColor

                    val width = paint.measureText(text, start, end)
                    val rect = RectF(x, top.toFloat(), x + width, bottom.toFloat())
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                    paint.color = oldColor
                }
            }
            ```

            2. Modify the `styleCodeBlocks` utility function:

            ```kotlin
            fun styleCodeBlocks(text: String, context: Context): SpannableString {
                val spannable = SpannableString(text)
                val codeBlockPositions = findCodeBlockPositions(text)

                for (position in codeBlockPositions) {
                    // Set custom background using RoundedBackgroundWithIncreasedHeightSpan
                    val backgroundColor = ContextCompat.getColor(context, R.color.your_custom_background_color)
                    val cornerRadius = context.resources.getDimension(R.dimen.your_custom_corner_radius)
                    val extraLineHeight = context.resources.getDimensionPixelSize(R.dimen.your_custom_extra_line_height)
                    val backgroundSpan = RoundedBackgroundWithIncreasedHeightSpan(backgroundColor, cornerRadius, extraLineHeight)
                    spannable.setSpan(backgroundSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Set custom text color using ForegroundColorSpan
                    val foregroundColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.your_custom_text_color))
                    spannable.setSpan(foregroundColorSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Set custom typeface using TypefaceSpan
                    val typeFaceSpan = TypefaceSpan("monospace")
                    spannable.setSpan(typeFaceSpan, position.start, position.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                return spannable
            }
            ```

            Make sure to define appropriate dimensions for custom corner radius and extra line height in your `dimens.xml`:

            ```xml
            <dimen name="your_custom_corner_radius">8dp</dimen>
            <dimen name="your_custom_extra_line_height">4dp</dimen>
            ```

            This implementation should ensure proper styling of the entire code block, including the content between the backticks while maintaining alignment and layout.
        """

        val blocks: List<CodeBlockPosition> = findCodeBlockPositions(code)
        println("********** Blocks: $blocks")

        val apiKey = getApiKey(this)
        if (apiKey.isNullOrEmpty()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Optional: Close the MainActivity after starting LoginActivity
            return // Skip the rest of the onCreate method
        }

        val openAI = OpenAI(apiKey)
        job = Job()
        coroutineScope.launch {
            val models = fetchModels(openAI)
            println("********** Models: ${models}")
            for (model in models) {
                val id = model.id
                val otherId = id.id
                println("    $otherId")
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)

        val messages: MutableList<Message> = mutableListOf()
        val messageAdapter = MessageAdapter(messages)

        val recyclerView = findViewById<RecyclerView>(R.id.conversation_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        val messageInput = findViewById<EditText>(R.id.message_input)
        val sendMessageButton = findViewById<ImageButton>(R.id.send_message_button)

        sendMessageButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()

            if (messageText.isNotEmpty()) {
                messageAdapter.addMessage(Message(0, messageText, Message.VIEW_TYPE_MESSAGE, ChatRole.User))
                messageAdapter.addMessage(Message(0, "", Message.VIEW_TYPE_LOADING, ChatRole.Assistant))

                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-4"),
                    messages = messageAdapter.getChatCompletionsList()
                )

                val completions: Flow<ChatCompletionChunk> = openAI.chatCompletions(chatCompletionRequest)
                coroutineScope.launch {
                    var response = ""
                    completions.collect { chatCompletionChunk ->
//                        println("********* Received chunk: $chatCompletionChunk")
                        val choices: List<ChatChunk> = chatCompletionChunk.choices
                        for (choice in choices) {
                            val delta: ChatDelta? = choice.delta
                            if (delta != null) {
                                val content = delta.content
                                if (content != null) {
                                    response = response.plus(content)

//                                    if ("```" in response) {
//                                        val parser = Parser.builder().build()
//                                        val document = parser.parse(response)
//
//                                        val visitor = NodeVisitor(
//                                            VisitHandler(FencedCodeBlock::class.java, Visitor { node: FencedCodeBlock ->
//                                                val language = node.info
//                                                val codeBlock = node.contentChars
////                                                println("Language: $language\nCode Block: $codeBlock\n")
//                                            })
//                                        )
//
//                                        visitor.visit(document)
//                                    }

                                    messages.takeLast(1)[0].viewType = Message.VIEW_TYPE_MESSAGE
                                    messageAdapter.updateMessageContent(messages.size - 1, response)
                                }
                            }
                        }
                    }
                }

                messageInput.setText("") // Clear the input field
                recyclerView.smoothScrollToPosition(messages.size - 1) // Scroll to the latest message
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        return true
    }

    private fun getApiKey(context: Context): String? {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val key = "openai_api_key"

        val defaultValue = ""

        return sharedPreferences.getString(key, defaultValue)
    }
}
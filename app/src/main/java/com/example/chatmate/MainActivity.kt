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

        val messages: MutableList<Message> = mutableListOf<Message>()
        val messageAdapter = MessageAdapter(messages)

        val recyclerView = findViewById<RecyclerView>(R.id.conversation_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        val messageInput = findViewById<EditText>(R.id.message_input)
        val sendMessageButton = findViewById<ImageButton>(R.id.send_message_button)

        sendMessageButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()

            if (messageText.isNotEmpty()) {
                messageAdapter.addMessage(Message(0, messageText, Message.VIEW_TYPE_MESSAGE))
                messageAdapter.addMessage(Message(0, "", Message.VIEW_TYPE_LOADING))

//                GlobalScope.launch {
//                    delay(2000)
//
//                    // Update the loading indicator item with the result
//                    withContext(Dispatchers.Main) {
//                        val result = "API response" // Get the actual API response here
//                        messageAdapter.updateMessageContent(messages.size - 1, result)
//                    }
//                }

                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-4"),
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.User,
                            content = "Hello!"
                        )
                    )
                )

                val completions: Flow<ChatCompletionChunk> = openAI.chatCompletions(chatCompletionRequest)
                coroutineScope.launch {
                    var response = ""
                    completions.collect { chatCompletionChunk ->
                        println("********* Received chunk: $chatCompletionChunk")
                        val choices: List<ChatChunk> = chatCompletionChunk.choices
                        for (choice in choices) {
                            val delta: ChatDelta? = choice.delta
                            if (delta != null) {
                                val content = delta.content
                                if (content != null) {
                                    response = response.plus(content)
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
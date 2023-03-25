package com.example.chatmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.chatmate.databinding.ActivityMainBinding
import com.example.chatmate.ui.login.LoginActivity
import java.security.KeyStore
import javax.crypto.Cipher

/*
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class GenericSecretsKeyStore(private val alias: String, private val context: Context) {

    private val androidKeyStore = "AndroidKeyStore"

    init {
        generateSecretKey()
    }

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply {
            load(null)
        }

        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, androidKeyStore
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply {
            load(null)
        }
        return keyStore.getKey(alias, null) as SecretKey
    }

    fun encrypt(data: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(Cipher.ENCRYPT_MODE, getSecretKey())
        }
        return cipher.doFinal(data.toByteArray(Charset.forName("UTF-8")))
    }

    fun decrypt(encryptedData: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(Cipher.DECRYPT_MODE, getSecretKey())
        }
        return String(cipher.doFinal(encryptedData), Charset.forName("UTF-8"))
    }
}
```

Using this class, you can store and retrieve generic secrets in the Android KeyStore. A unique `alias` should be provided for each distinct instance of the `GenericSecretsKeyStore` class.

To use this class in your app, you can create an instance and call the `encrypt()` and `decrypt()` functions, like so:

```kotlin
val genericSecretsKeyStore = GenericSecretsKeyStore("sampleAlias", context)

// Encrypt
val plainText = "mySecretData"
val encryptedData = genericSecretsKeyStore.encrypt(plainText)

// Decrypt
val decryptedData = genericSecretsKeyStore.decrypt(encryptedData)
 */

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ORGANIZATION_KEY_ALIAS = "organization_alias"
        private const val API_KEY_ALIAS = "api_key_alias"
    }
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasLoginCredentials(this)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Optional: Close the MainActivity after starting LoginActivity
            return // Skip the rest of the onCreate method
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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun hasLoginCredentials(context: Context): Boolean {
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

        val value = sharedPreferences.getString(key, defaultValue)

        println("********** Loaded key = ${value}")

        if (value.isNullOrEmpty())
            return false;
        return true
    }
}
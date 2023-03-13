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
import com.example.chatmate.databinding.ActivityMainBinding
import com.example.chatmate.ui.login.LoginActivity
import java.security.KeyStore
import javax.crypto.Cipher

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

        if (!hasLoginCredentials()) {
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

    private fun hasLoginCredentials(): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val organizationAlias = "organization_alias"
        val apiKeyAlias = "api_key_alias"

        val organizationEntry = keyStore.getEntry(organizationAlias, null)// as KeyStore.SecretKeyEntry
        val apiKeyEntry = keyStore.getEntry(apiKeyAlias, null)// as KeyStore.SecretKeyEntry
        if (organizationEntry == null && apiKeyEntry == null)
            return false

        val organizationSecretKeyEntry: KeyStore.SecretKeyEntry = organizationEntry as KeyStore.SecretKeyEntry
        val apiKeySecretKeyEntry: KeyStore.SecretKeyEntry = apiKeyEntry as KeyStore.SecretKeyEntry

        val organizationCipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(Cipher.DECRYPT_MODE, organizationSecretKeyEntry.secretKey)
        }
        val apiKeyCipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(Cipher.DECRYPT_MODE, apiKeySecretKeyEntry.secretKey)
        }

        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val organizationEncryptedString = sharedPreferences.getString(organizationAlias, null)
        val apiKeyEncryptedString = sharedPreferences.getString(apiKeyAlias, null)

        if (organizationEncryptedString != null && apiKeyEncryptedString != null) {
            val organizationEncryptedBytes = Base64.decode(organizationEncryptedString, Base64.DEFAULT)
            val apiKeyEncryptedBytes = Base64.decode(apiKeyEncryptedString, Base64.DEFAULT)

            val organizationBytes = organizationCipher.doFinal(organizationEncryptedBytes)
            val apiKeyBytes = apiKeyCipher.doFinal(apiKeyEncryptedBytes)

            val organization = String(organizationBytes, Charsets.UTF_8)
            val apiKey = String(apiKeyBytes, Charsets.UTF_8)

            // Use the organization and apiKey here
            println("****** organization = $organization")
            println("****** apiKey = $apiKey")
        } else {
            // No organization and apiKey were found
        }

        return false
    }
}
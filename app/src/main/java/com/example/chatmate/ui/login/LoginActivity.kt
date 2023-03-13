package com.example.chatmate.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.example.chatmate.MainActivity
import com.example.chatmate.databinding.ActivityLoginBinding
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val organization = binding.organization
        val apiKey = binding.apiKey
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                organization.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                apiKey.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success, organization.text.toString(), apiKey.text.toString())
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        organization.afterTextChanged {
            loginViewModel.loginDataChanged(
                organization.text.toString(),
                apiKey.text.toString()
            )
        }

        apiKey.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    organization.text.toString(),
                    apiKey.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            organization.text.toString(),
                            apiKey.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(organization.text.toString(), apiKey.text.toString())
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView, organization: String, apiKey: String) {
        // Store the organization and apiKey in the Android Keystore
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val organizationAlias = "organization_alias"
        val apiKeyAlias = "api_key_alias"

        val organizationKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        organizationAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setKeySize(256)
                        .build()
                )
            }
            .generateKey()

        val apiKeyKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        apiKeyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setKeySize(256)
                        .build()
                )
            }
            .generateKey()

        val organizationCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            .apply {
                init(Cipher.ENCRYPT_MODE, organizationKey)
            }
        val organizationEncryptedBytes = organizationCipher.doFinal(organization.toByteArray(Charsets.UTF_8))

        val apiKeyCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            .apply {
                init(Cipher.ENCRYPT_MODE, apiKeyKey)
            }
        val apiKeyEncryptedBytes = apiKeyCipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        val organizationEncryptedString = Base64.encodeToString(organizationEncryptedBytes, Base64.DEFAULT)
        val apiKeyEncryptedString = Base64.encodeToString(apiKeyEncryptedBytes, Base64.DEFAULT)

        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(organizationAlias, organizationEncryptedString)
            .putString(apiKeyAlias, apiKeyEncryptedString)
            .apply()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
package com.cyouliao.goaleval

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

const val PREFERENCES_DATASTORE_NAME = "LOGIN_DATA"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_DATASTORE_NAME)

class LoginActivity : AppCompatActivity() {

    companion object {
        val LOGIN_DATA_KEY_EXP_ID = stringPreferencesKey("EXP_ID")
        val LOGIN_DATA_KEY_TOKEN = stringPreferencesKey("TOKEN")
    }

    @Serializable
    data class ResponseBodyHeaders(val status: String, val errorMsg: String)
    @Serializable
    data class ResponseBodyContentsTokenLogin(val isLogin: Boolean)
    @Serializable
    data class ResponseBodyContentsPasswordLogin(val isLogin: Boolean, val expID: String, val token: String)
    @Serializable
    data class ResponseBodyTokenLogin(val headers: ResponseBodyHeaders, val contents: ResponseBodyContentsTokenLogin)
    @Serializable
    data class ResponseBodyPasswordLogin(val headers: ResponseBodyHeaders, val contents: ResponseBodyContentsPasswordLogin)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("LoginActivity", "LoginActivity has been created.")

        // Check local storage login data
        val loginDataExpID: Flow<String> = this.dataStore.data.map {
            preferences ->
            preferences[LOGIN_DATA_KEY_EXP_ID]?: ""
        }
        val loginDataToken: Flow<String> = this.dataStore.data.map {
            preferences ->
            preferences[LOGIN_DATA_KEY_TOKEN]?: ""
        }

        // Views code
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // disable login form entries
        disableLoginForm()
        Toast.makeText(this, "正在嘗試使用內部儲存資料登入...", Toast.LENGTH_SHORT).show()

        // check local login data
        checkLoginDataExpID(loginDataExpID, loginDataToken)

        localDataLogin("", "")
    }

    private fun localDataLogin(expID: String, token: String) {
        Log.d("LoginActivity", "Call: localDataLogin")
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
        lifecycleScope.launch {
            val response: HttpResponse = client.post("https://goal-eval-test.cyouliao.com/login_android.php") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("exp_id", expID)
                    append("token", token)
                }))
            }
            Log.d("LoginActivity", "StatusCode: ${response.status}")
            if (response.status.isSuccess()) {
                val responseBody: ResponseBodyTokenLogin = response.body()
                Log.d("LoginActivity", "headers: ${responseBody.headers}")
                Log.d("LoginActivity", "status:\t${responseBody.headers.status}\nerrorMsg:\t${responseBody.headers.errorMsg}")
//                Log.d("LoginActivity", "ResponseBody: ${response.body<String>()}")
            } else {
                Log.d("LoginActivity", "ResponseCode: Bad")
            }
        }
    }

    private fun checkLoginDataToken(expID: String, loginDataToken: Flow<String>) {
        Log.d("LoginActivity", "Call: checkLoginDataToken")
        Log.d("LoginActivity", "Collecting flow: Token")
        lifecycleScope.launch {
            loginDataToken.collect { token ->
                Log.d("LoginActivity", "Token: $token")
                if (token.isEmpty()) {
                    Log.d("LoginActivity", "Token: Bad")
                    enableLoginForm()
                } else {
                    Log.d("LoginActivity", "Token: Good")
                    localDataLogin(expID, token)
                }
            }
        }
    }
    private fun checkLoginDataExpID(loginDataExpID: Flow<String>, loginDataToken: Flow<String>) {
        Log.d("LoginActivity", "Call: checkLoginDataExpID")
        Log.d("LoginActivity", "Collecting flow: ExpID")
        lifecycleScope.launch {
            loginDataExpID.collect { expID ->
                Log.d("LoginActivity", "ExpID: $expID")
                if (expID.isEmpty()) {
                    Log.d("LoginActivity", "ExpID: Bad")
                    enableLoginForm()
                } else {
                    Log.d("LoginActivity", "ExpID: Good")
                    checkLoginDataToken(expID, loginDataToken)
                }
            }
        }
    }

    private fun enableLoginForm() {
        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        etStdID.isEnabled = true
        etPassword.isEnabled = true
        btnLogin.isEnabled = true
    }
    private fun disableLoginForm() {
        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        etStdID.isEnabled = false
        etPassword.isEnabled = false
        btnLogin.isEnabled = false
    }

    private fun startWebActivity() {
        Log.d("LoginActivity", "Starting WebActivity...")
    }
}
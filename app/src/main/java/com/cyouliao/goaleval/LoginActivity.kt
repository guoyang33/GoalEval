package com.cyouliao.goaleval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import kotlinx.serialization.json.Json

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
    data class ResponseBodyContentsTokenLogin(val isLogin: Boolean, val expID: String, val token: String)
    @Serializable
    data class ResponseBodyContentsPasswordLogin(val isLogin: Boolean, val expID: String, val token: String)
    @Serializable
    data class ResponseBodyTokenLogin(val headers: ResponseBodyHeaders, val contents: ResponseBodyContentsTokenLogin)
    @Serializable
    data class ResponseBodyPasswordLogin(val headers: ResponseBodyHeaders, val contents: ResponseBodyContentsPasswordLogin)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // check local login data
        checkLoginDataExpID(loginDataExpID, loginDataToken)

    }

    private fun startWebActivity(expID: String, token: String) {
        Toast.makeText(this, "登入成功，正在載入網頁內容", Toast.LENGTH_LONG).show()

        val intent: Intent = Intent(this, WebActivity::class.java).apply {
            putExtra("EXTRA_EXP_ID", expID)
            putExtra("EXTRA_TOKEN", token)
        }
        startActivity(intent)
        finish()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun localDataLogin(expID: String, token: String) {
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
            if (response.status.isSuccess()) {
                val responseBody: ResponseBodyTokenLogin = response.body()

                if (responseBody.contents.isLogin) {
                    startWebActivity(expID, token)
                } else {
                    Toast.makeText(this@LoginActivity, "手機登入已過期，請重新登入", Toast.LENGTH_SHORT).show()
                    enableLoginForm()
                }
            } else {
                Toast.makeText(this@LoginActivity, "伺服器連線失敗，請確認網路連線狀態", Toast.LENGTH_LONG).show()
                startMainActivity()
            }

        }
    }

    private fun checkLoginDataToken(expID: String, loginDataToken: Flow<String>) {
        lifecycleScope.launch {
            loginDataToken.collect { token ->
                if (token.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "內部資料不存在，請輸入學號與密碼登入", Toast.LENGTH_SHORT).show()
                    enableLoginForm()
                } else {
                    localDataLogin(expID, token)
                }
            }
        }
    }
    private fun checkLoginDataExpID(loginDataExpID: Flow<String>, loginDataToken: Flow<String>) {
        Toast.makeText(this, "正在嘗試使用內部儲存資料登入...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            loginDataExpID.collect { expID ->
                if (expID.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "內部資料不存在，請輸入學號與密碼登入", Toast.LENGTH_SHORT).show()
                    enableLoginForm()
                } else {
                    checkLoginDataToken(expID, loginDataToken)
                }
            }
        }
    }

    private fun updateLocalData(expID: String, token: String) {
        lifecycleScope.launch {
            this@LoginActivity.dataStore.edit {
                it[LOGIN_DATA_KEY_EXP_ID] = expID
                it[LOGIN_DATA_KEY_TOKEN] = token
            }
        }
        startWebActivity(expID, token)
    }

    private fun startRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }

    private fun formDataLogin() {

        disableLoginForm()

        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)

        val stdID = etStdID.text.toString()
        val password = etPassword.text.toString()
        if (stdID.isEmpty() or password.isEmpty()) {
            Toast.makeText(this, "學號和密碼不得留空", Toast.LENGTH_SHORT).show()
            enableLoginForm()
            return
        }
        if (!stdID.isDigitsOnly()) {
            Toast.makeText(this, "學號格式不正確", Toast.LENGTH_SHORT).show()
            enableLoginForm()
            return
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
        lifecycleScope.launch {
            val response: HttpResponse = client.post("https://goal-eval-test.cyouliao.com/login_android.php") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("std_id", stdID)
                    append("password", password)
                }))
            }
            if (response.status.isSuccess()) {
                val responseBody: ResponseBodyTokenLogin = response.body()

                if (responseBody.headers.status == "OK") {
                    if (responseBody.contents.isLogin) {
                        updateLocalData(responseBody.contents.expID, responseBody.contents.token)
                    } else {
                        Toast.makeText(this@LoginActivity,"登入失敗，學號或密碼不正確", Toast.LENGTH_SHORT).show()
                        enableLoginForm()
                    }
                } else if (responseBody.headers.status == "ERROR_USER_NOT_REGISTERED") {
                    Toast.makeText(this@LoginActivity, "您尚未完成帳號註冊，請先完成註冊程序", Toast.LENGTH_LONG).show()
                    startRegisterActivity()
                } else {
                    Toast.makeText(this@LoginActivity, "未知錯誤，請聯絡管理人員\n錯誤訊息：${responseBody.headers.errorMsg}", Toast.LENGTH_LONG).show()
                    enableLoginForm()
                }

            } else {
                Toast.makeText(this@LoginActivity, "伺服器連線問題，請檢查您的網路連線狀態", Toast.LENGTH_LONG).show()
                startMainActivity()
            }

        }

    }

    private fun enableLoginForm() {

        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            formDataLogin()
        }

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

}
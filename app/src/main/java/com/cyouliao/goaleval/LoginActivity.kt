package com.cyouliao.goaleval

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlinx.coroutines.cancel

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check local storage login data
        val loginDataExpID: Flow<String> = this.dataStore.data.map {
            preferences ->
            preferences[Config.LOGIN_DATA_KEY_EXP_ID]?: ""
        }
        val loginDataToken: Flow<String> = this.dataStore.data.map {
            preferences ->
            preferences[Config.LOGIN_DATA_KEY_TOKEN]?: ""
        }

        // Views code
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLoginSubmit = findViewById<Button>(R.id.btnLoginSubmit)
        btnLoginSubmit.setOnClickListener {
            formDataLogin()
        }

        val btnLoginRegister = findViewById<Button>(R.id.btnLoginRegister)
        btnLoginRegister.setOnClickListener {
            startRegisterActivity()
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
        if (!isNetworkConnected(this)) {
            Toast.makeText(this, "伺服器連線失敗，請確認網路連線狀態", Toast.LENGTH_LONG).show()

            startMainActivity()

            return
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
        lifecycleScope.launch {
            val response: HttpResponse = client.post(Config.SERVER_DOMAIN + "/login_android.php") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("exp_id", expID)
                    append("token", token)
                }))
            }
            if (response.status.isSuccess()) {
                val responseJsonBody: Config.Companion.ResponseJsonBodyLogin = response.body()

                if (responseJsonBody.contents.isLogin) {
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

                cancel()
            }
        }
    }
    private fun checkLoginDataExpID(loginDataExpID: Flow<String>, loginDataToken: Flow<String>) {
        lifecycleScope.launch {
            loginDataExpID.collect { expID ->
                if (expID.isEmpty()) {
                    enableLoginForm()
                } else {
                    checkLoginDataToken(expID, loginDataToken)
                }

                cancel()
            }
        }
    }

    private fun startRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))

        finish()
    }

    private fun formDataLogin() {
        disableLoginForm()

        if (!isNetworkConnected(this)) {
            Toast.makeText(this, "伺服器連線失敗，請確認網路連線狀態", Toast.LENGTH_LONG).show()

            startMainActivity()

            return
        }

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
            val response: HttpResponse = client.post(Config.SERVER_DOMAIN + "/login_android.php") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("std_id", stdID)
                    append("password", password)
                }))
            }
            if (response.status.isSuccess()) {
                val responseJsonBody: Config.Companion.ResponseJsonBodyLogin = response.body()

                if (responseJsonBody.headers.status == "OK") {
                    if (responseJsonBody.contents.isLogin) {
                        updateLocalData(this@LoginActivity, responseJsonBody.contents.expID, responseJsonBody.contents.token)

                        startWebActivity(responseJsonBody.contents.expID, responseJsonBody.contents.token)
                    } else {
                        Toast.makeText(this@LoginActivity,"登入失敗，學號或密碼不正確", Toast.LENGTH_SHORT).show()
                    }
                } else if (responseJsonBody.headers.status == "ERROR_USER_NOT_REGISTERED") {
                    Toast.makeText(this@LoginActivity, "您尚未完成帳號註冊，請先完成註冊程序", Toast.LENGTH_LONG).show()
                    startRegisterActivity()
                } else if (responseJsonBody.headers.status == "ERROR_USER_NOT_FOUND") {
                    showAlertDialog(this@LoginActivity, "登入", "登入失敗，系統沒有您的資料，請詢問管理人員。\n學號:\t$stdID")
                } else if (responseJsonBody.headers.status == "ERROR_USER_NOT_ANDROID") {
                    showAlertDialog(this@LoginActivity, "登入", "登入失敗，蘋果手機組的參與者無法透過此APP登入。\n如有疑問，請洽詢管理人員。")
                } else {
                    showAlertDialog(this@LoginActivity, "登入", "登入失敗，未知錯誤，請向管理人員回報。\n學號:\t$stdID\nResponse:${response.bodyAsText()}")
                }

            } else {
                Toast.makeText(this@LoginActivity, "伺服器連線問題，請檢查您的網路連線狀態", Toast.LENGTH_LONG).show()
                startMainActivity()
            }

            enableLoginForm()

        }

    }

    private fun enableLoginForm() {
        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLoginSubmit: Button = findViewById(R.id.btnLoginSubmit)
        val btnLoginRegister = findViewById<Button>(R.id.btnLoginRegister)

        etStdID.isEnabled = true
        etPassword.isEnabled = true
        btnLoginSubmit.isEnabled = true
        btnLoginRegister.isEnabled = true
    }
    private fun disableLoginForm() {
        val etStdID: EditText = findViewById(R.id.etStdId)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLoginSubmit: Button = findViewById(R.id.btnLoginSubmit)
        val btnLoginRegister: Button = findViewById(R.id.btnLoginRegister)

        etStdID.isEnabled = false
        etPassword.isEnabled = false
        btnLoginSubmit.isEnabled = false
        btnLoginRegister.isEnabled = false
    }

}
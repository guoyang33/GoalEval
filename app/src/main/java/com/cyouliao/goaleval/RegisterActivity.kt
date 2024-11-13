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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private fun disableFormEntry() {
        val etExpID = findViewById<EditText>(R.id.etRegisterExpID)
        val etStdID = findViewById<EditText>(R.id.etRegisterStdID)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etRegisterPasswordConfirm)
        val btnSubmit = findViewById<Button>(R.id.btnRegisterSubmit)

        etExpID.isEnabled = false
        etStdID.isEnabled = false
        etPassword.isEnabled = false
        etPasswordConfirm.isEnabled = false
        btnSubmit.isEnabled = false
    }

    private fun enableFormEntry() {
        val etExpID = findViewById<EditText>(R.id.etRegisterExpID)
        val etStdID = findViewById<EditText>(R.id.etRegisterStdID)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etRegisterPasswordConfirm)
        val btnSubmit = findViewById<Button>(R.id.btnRegisterSubmit)

        etPassword.setText("")
        etPasswordConfirm.setText("")

        etExpID.isEnabled = true
        etStdID.isEnabled = true
        etPassword.isEnabled = true
        etPasswordConfirm.isEnabled = true
        btnSubmit.isEnabled = true
    }

    private fun checkRegisterData(): Boolean {
        val etExpID = findViewById<EditText>(R.id.etRegisterExpID)
        if (etExpID.text.toString().isEmpty()) {
            Toast.makeText(this, "研究編號不得為空", Toast.LENGTH_SHORT).show()
            return false
        }

        val etStdID = findViewById<EditText>(R.id.etRegisterStdID)
        if (etStdID.text.toString().isEmpty()) {
            Toast.makeText(this, "學號不得為空", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!etStdID.text.isDigitsOnly()) {
            Toast.makeText(this, "學號格式有誤", Toast.LENGTH_SHORT).show()
            return false
        }
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            Toast.makeText(this, "設定密碼不得為空", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(this, "密碼請設定至少8個字元", Toast.LENGTH_SHORT).show()
            return false
        }
        val etPasswordConfirm = findViewById<EditText>(R.id.etRegisterPasswordConfirm)
        val passwordConfirm = etPasswordConfirm.text.toString()
        if (passwordConfirm != password) {
            Toast.makeText(this, "兩次輸入的密碼不相同", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))

        finish()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))

        finish()
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

    private fun submitDataToServer() {
        if (!isNetworkConnected(this)) {
            Toast.makeText(this@RegisterActivity, "伺服器連線失敗，請確認網路連線狀態", Toast.LENGTH_LONG).show()

            startMainActivity()

            return
        }

        val expID = findViewById<EditText>(R.id.etRegisterExpID).text.toString()
        val stdID = findViewById<EditText>(R.id.etRegisterStdID).text.toString()
        val password = findViewById<EditText>(R.id.etRegisterPassword).text.toString()

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        lifecycleScope.launch {
            val response: HttpResponse = client.post(Config.SERVER_DOMAIN + "/register_android.php") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("exp_id", expID)
                    append("std_id", stdID)
                    append("password", password)
                }))
            }

            if (response.status.isSuccess()) {
                val responseJsonBody: Config.Companion.ResponseJsonBodyRegister = response.body()
                if (responseJsonBody.headers.status == "OK") {
                    if (responseJsonBody.contents.isSuccess) {
                        Toast.makeText(this@RegisterActivity, "註冊完成", Toast.LENGTH_SHORT).show()

                        updateLocalData(this@RegisterActivity, responseJsonBody.contents.expID, responseJsonBody.contents.token)

                        startWebActivity(responseJsonBody.contents.expID, responseJsonBody.contents.token)
                    } else {
                        showAlertDialog(this@RegisterActivity, "註冊","註冊失敗，請詢問管理人員。\n研究編號:\t$expID\n學號:\t$stdID\nResponse:${response.bodyAsText()}")
                    }
                } else if (responseJsonBody.headers.status == "ERROR_USER_ALREADY_REGISTERED") {
                    Toast.makeText(this@RegisterActivity, "您已經註冊過囉！\n請嘗試重新登入，若仍無法登入，請向管理人員回報。", Toast.LENGTH_LONG).show()

                    startLoginActivity()
                } else if (responseJsonBody.headers.status == "ERROR_USER_NOT_FOUND") {
                    showAlertDialog(this@RegisterActivity, "註冊", "註冊失敗，系統沒有您的資料，請詢問管理人員。\n研究編號:\t$expID\n學號:\t$stdID")
                } else if (responseJsonBody.headers.status == "ERROR_USER_NOT_ANDROID") {
                    showAlertDialog(this@RegisterActivity, "註冊", "註冊失敗，蘋果手機組的參與者無法透過此APP註冊。\n如有疑問，請洽詢管理人員。")
                } else if (responseJsonBody.headers.status == "STATUS_ERROR_EXP_ID_NOT_MATCH") {
                    showAlertDialog(this@RegisterActivity, "註冊", "註冊失敗，研究編號錯誤。\n如有疑問，請洽詢管理人員。\n研究編號:\t$expID\n學號:\t$stdID")
                } else {
                    showAlertDialog(this@RegisterActivity, "註冊", "註冊失敗，未知錯誤，請向管理人員回報。\n研究編號:\t$expID\n學號:\t$stdID\nResponse:${response.bodyAsText()}")
                }
            } else {
                Toast.makeText(this@RegisterActivity, "伺服器連線失敗，請確認網路連線狀態", Toast.LENGTH_LONG).show()

                startMainActivity()
            }

            enableFormEntry()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnRegisterSubmit: Button = findViewById(R.id.btnRegisterSubmit)
        btnRegisterSubmit.setOnClickListener {
            disableFormEntry()
            if (checkRegisterData()) {
                submitDataToServer()
            } else {
                enableFormEntry()
            }
        }
    }
}
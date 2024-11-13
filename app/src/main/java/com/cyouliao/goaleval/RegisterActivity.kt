package com.cyouliao.goaleval

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

    private fun submitDataToServer() {
        val etExpID = findViewById<EditText>(R.id.etRegisterExpID)
        val etStdID = findViewById<EditText>(R.id.etRegisterStdID)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etRegisterPasswordConfirm)
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
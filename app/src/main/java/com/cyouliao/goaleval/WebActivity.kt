package com.cyouliao.goaleval

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class WebActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_web)

        val expID: String = intent.getStringExtra("EXTRA_EXP_ID").toString()
        val token: String = intent.getStringExtra("EXTRA_TOKEN").toString()

        val wvMain: WebView = findViewById(R.id.wvMain)
        wvMain.webViewClient = WebViewClient()
        wvMain.settings.javaScriptEnabled = true
        wvMain.loadUrl("https://goal-eval-test.cyouliao.com/redirect_token.php?exp_id=$expID&token=$token")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
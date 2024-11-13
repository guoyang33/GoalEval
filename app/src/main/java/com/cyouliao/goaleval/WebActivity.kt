package com.cyouliao.goaleval

import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
        wvMain.settings.apply {
            javaScriptEnabled = true
        }
        wvMain.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

                Toast.makeText(this@WebActivity, "網頁載入發生錯誤，嘗試重新載入", Toast.LENGTH_SHORT).show()
                view?.reload()
            }

        }

        wvMain.loadUrl(Config.SERVER_DOMAIN + "/redirect_token.php?exp_id=$expID&token=$token")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
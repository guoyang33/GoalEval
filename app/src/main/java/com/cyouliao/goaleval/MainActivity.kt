package com.cyouliao.goaleval

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check network connection
        if (!isNetworkConnected(this)) {
            // Network not connected

            // Display message and button to retry
            // View code
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val btnNetworkConnectRetry: Button = findViewById(R.id.btnNetworkConnectRetry)
            btnNetworkConnectRetry.setOnClickListener {
                // Disable button
                btnNetworkConnectRetry.isEnabled = false

                // Show toast
                Toast.makeText(this, "嘗試連線中...", Toast.LENGTH_SHORT).show()

                // Recheck network connection
                if (isNetworkConnected(this)) {
                    // Network connected

                    // Show toast
                    Toast.makeText(this, "連線成功", Toast.LENGTH_SHORT).show()

                    // Start activity for processing user login
                    startLoginActivity()

                } else {
                    // Network not connected

                    // Show toast
                    Toast.makeText(this, "連線失敗", Toast.LENGTH_SHORT).show()
                }

                // Enable button
                btnNetworkConnectRetry.isEnabled = true
            }
        } else {
            // Network connected

            // Start activity for processing user login
            startLoginActivity()

        }
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))

        finish()
    }
}

fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}

package com.cyouliao.goaleval

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyouliao.goaleval.Config.Companion.LOGIN_DATA_KEY_EXP_ID
import com.cyouliao.goaleval.Config.Companion.LOGIN_DATA_KEY_TOKEN
import kotlinx.serialization.Serializable

const val PREFERENCES_DATASTORE_NAME = "LOGIN_DATA"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_DATASTORE_NAME)

class Config {
    companion object {
        const val SERVER_DOMAIN = "https://goal-eval.cyouliao.com"

        val LOGIN_DATA_KEY_EXP_ID = stringPreferencesKey("EXP_ID")
        val LOGIN_DATA_KEY_TOKEN = stringPreferencesKey("TOKEN")

        @Serializable
        data class ResponseHeaders(val status: String, val errorMsg: String)
        @Serializable
        data class ResponseContentsLogin(val isLogin: Boolean, val expID: String, val token: String)
        @Serializable
        data class ResponseContentsRegister(val isSuccess: Boolean, val expID: String, val token: String)
        @Serializable
        data class ResponseJsonBodyLogin(val headers: ResponseHeaders, val contents: ResponseContentsLogin)
        @Serializable
        data class ResponseJsonBodyRegister(val headers: ResponseHeaders, val contents: ResponseContentsRegister)

    }
}

fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
    return networkCapabilities != null
}

fun showAlertDialog(context: Context, title: String, message: String) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton("確定") { dialog, _ ->
            dialog.dismiss()
        }
    builder.create().show()
}

suspend fun updateLocalData(context: Context, expID: String, token: String) {
    context.dataStore.edit {
        it[LOGIN_DATA_KEY_EXP_ID] = expID
        it[LOGIN_DATA_KEY_TOKEN] = token
    }
}
package ir.moamelo.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Session {
    private var prefs: SharedPreferences? = null
    var token: String? = null
    var loggedIn by mutableStateOf(false)

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("moamelo", Context.MODE_PRIVATE)
        token = prefs?.getString("token", null)
        loggedIn = token != null
    }

    fun save(t: String) {
        token = t
        prefs?.edit()?.putString("token", t)?.apply()
        loggedIn = true
    }

    fun clear() {
        token = null
        prefs?.edit()?.remove("token")?.apply()
        loggedIn = false
    }
}

package com.yesmin.ai
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** API key + settings, stored encrypted on-device (never hardcoded). */
class Prefs(c: Context) {
    private val sp = EncryptedSharedPreferences.create(c, "yesmin_secure",
        MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    var apiKey: String get() = sp.getString("k", "")!! ; set(v) = sp.edit().putString("k", v).apply()
    var model: String get() = sp.getString("m", "gemini-2.5-flash")!! ; set(v) = sp.edit().putString("m", v).apply()
    var lang: String get() = sp.getString("l", "en-US")!! ; set(v) = sp.edit().putString("l", v).apply()
    var memory: String get() = sp.getString("mem", "")!! ; set(v) = sp.edit().putString("mem", v).apply()
    var temp: Float get() = sp.getFloat("t", 0.8f) ; set(v) = sp.edit().putFloat("t", v).apply()
    var speed: Float get() = sp.getFloat("s", 1f) ; set(v) = sp.edit().putFloat("s", v).apply()
    var enabled: Boolean get() = sp.getBoolean("e", true) ; set(v) = sp.edit().putBoolean("e", v).apply()
}

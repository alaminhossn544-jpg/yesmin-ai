package com.yesmin.ai
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

/** Device control through official APIs only. Returns a reply string, or null if not a local command. */
object Device {
    private var torch = false

    fun apps(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }.sortedBy { it.first.lowercase() }
    }

    fun launchPkg(c: Context, pkg: String): Boolean = try {
        c.startActivity(c.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) { false }

    private fun go(c: Context, i: Intent) = try { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e: Exception) { false }

    fun handle(c: Context, t: String): String? {
        val s = t.lowercase().trim()
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when {
            s.contains("battery") ->
                "Battery ${(c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}% ache."
            s == "time" || s.contains("what time") || s.contains("date today") || s == "date" || s.contains("samay") ->
                SimpleDateFormat("EEEE, dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            s.contains("flashlight") || s.contains("torch") -> try {
                val cm = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                torch = !s.contains("off"); cm.setTorchMode(cm.cameraIdList[0], torch)
                if (torch) "Flashlight on." else "Flashlight off."
            } catch (e: Exception) { "Flashlight control hoy nai: ${e.message}" }
            s.contains("volume up") -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); "Volume barlo." }
            s.contains("volume down") -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); "Volume komlo." }
            s.contains("pause") && s.contains("music") || s == "pause" || s == "play music" || s == "next song" -> {
                val k = if (s.contains("next")) android.view.KeyEvent.KEYCODE_MEDIA_NEXT else android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, k))
                am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, k)); "Media control pathano holo."
            }
            s.contains("wifi settings") -> if (go(c, Intent(Settings.ACTION_WIFI_SETTINGS))) "Wi-Fi settings khulchhe." else "Khola gelo na."
            s.contains("bluetooth settings") -> if (go(c, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) "Bluetooth settings khulchhe." else "Khola gelo na."
            s.startsWith("play ") && s.contains("youtube") -> {
                val q = s.removePrefix("play ").replace("on youtube", "").replace("youtube", "").trim()
                if (go(c, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(q, "UTF-8")))))
                    "YouTube e '$q' search kholchhi." else "YouTube khola gelo na."
            }
            s.startsWith("open ") || s.startsWith("kholo ") || s.endsWith(" kholo") -> {
                val n = s.removePrefix("open ").removePrefix("kholo ").removeSuffix(" kholo").trim()
                val a = apps(c).firstOrNull { it.first.lowercase() == n } ?: apps(c).firstOrNull { it.first.lowercase().contains(n) }
                if (a != null && launchPkg(c, a.second)) "${a.first} khulchhe." else "'$n' naam er app pelam na."
            }
            else -> null
        }
    }
}

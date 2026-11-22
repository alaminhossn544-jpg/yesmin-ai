package com.yesmin.ai
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class Vm(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val ai: AiProvider = GeminiProvider(prefs)
    val voice = Voice(app)
    val msgs = mutableStateListOf<Msg>()
    val models = mutableStateListOf<String>()
    var status by mutableStateOf("Idle")          // Idle | Listening | Thinking | Speaking
    var tokens by mutableStateOf(0)
    var conn by mutableStateOf("Not tested")
    var continuous by mutableStateOf(false)
    private var viaVoice = false
    private val main = Handler(Looper.getMainLooper())

    fun system() = "You are YESMIN, a friendly, caring, intelligent female AI assistant (JARVIS-like but warm). " +
        "Reply in the user's language (Bangla, English or Hindi). Keep answers short and natural for voice. " +
        "Never pretend to be human. User-approved memory: ${prefs.memory}"

    fun send(t: String) {
        if (t.isBlank()) return
        msgs.add(Msg("user", t))
        Device.handle(getApplication(), t)?.let { reply(it); return }
        if (!prefs.enabled || prefs.apiKey.isBlank()) { reply("Gemini chalu nai ba API key nai. Settings > AI BRAIN SETTINGS e jao."); return }
        status = "Thinking"
        viewModelScope.launch {
            try { reply(ai.chat(msgs.toList(), system(), prefs.temp)); tokens = ai.tokens }
            catch (e: Exception) { viaVoice = false; reply("Error: ${e.message}") }
        }
    }

    private fun reply(t: String) {
        msgs.add(Msg("model", t))
        if (viaVoice) {
            status = "Speaking"
            voice.speak(t, prefs.lang, prefs.speed) {
                main.post { status = "Idle"; if (continuous) mic() }
            }
        } else status = "Idle"
    }

    fun mic() {
        voice.stop(); status = "Listening"; viaVoice = true
        voice.listen(prefs.lang, { send(it) }, { e ->
            status = "Idle"; continuous = false
            Toast.makeText(getApplication(), e, Toast.LENGTH_SHORT).show()
        })
    }

    fun stopAll() { voice.stop(); voice.cancelListen(); continuous = false; viaVoice = false; status = "Idle" }

    fun run(sys: String, p: String, out: (String) -> Unit) {
        if (p.isBlank()) return
        viewModelScope.launch {
            try { out(ai.chat(listOf(Msg("user", p)), sys, prefs.temp)); tokens = ai.tokens }
            catch (e: Exception) { out("Error: ${e.message}") }
        }
    }

    fun test() {
        conn = "Testing…"
        viewModelScope.launch {
            try { val m = ai.models(); models.clear(); models.addAll(m); conn = "Connected ✓ (${m.size} models)" }
            catch (e: Exception) { conn = "Failed: ${e.message}" }
        }
    }
}

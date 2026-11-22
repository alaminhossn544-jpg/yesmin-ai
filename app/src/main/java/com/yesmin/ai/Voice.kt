package com.yesmin.ai
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class Voice(private val c: Context) {
    private var ready = false
    private val tts = TextToSpeech(c) { ready = it == TextToSpeech.SUCCESS }
    private var sr: SpeechRecognizer? = null

    fun speak(text: String, lang: String, speed: Float, done: () -> Unit) {
        if (!ready) { done(); return }
        val loc = Locale.forLanguageTag(lang)
        tts.language = loc
        // Prefer a female-labelled voice if the engine exposes one; else raise pitch slightly.
        val v = tts.voices?.firstOrNull { it.locale.language == loc.language && it.name.contains("female", true) }
        if (v != null) { tts.voice = v; tts.setPitch(1f) } else tts.setPitch(1.25f)
        tts.setSpeechRate(speed)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(u: String?) {}
            override fun onDone(u: String?) { done() }
            @Deprecated("x") override fun onError(u: String?) { done() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "y")
    }

    fun stop() { tts.stop() }
    fun cancelListen() { sr?.cancel() }

    fun listen(lang: String, onText: (String) -> Unit, onErr: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(c)) { onErr("Speech recognition available nai"); return }
        sr?.destroy(); sr = SpeechRecognizer.createSpeechRecognizer(c)
        sr!!.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t != null) onText(t) else onErr("Kuch suna nai")
            }
            override fun onError(e: Int) { onErr("Mic error $e") }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(r: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang))
    }
}

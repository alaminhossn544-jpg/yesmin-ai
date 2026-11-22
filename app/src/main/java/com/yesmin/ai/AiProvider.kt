package com.yesmin.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Msg(val role: String, val text: String) // role: "user" | "model"

/** Modular brain: add OpenAI/Claude/etc. by implementing this interface. */
interface AiProvider {
    val tokens: Int
    suspend fun chat(history: List<Msg>, system: String, temp: Float): String
    suspend fun models(): List<String>
}

class GeminiProvider(private val p: Prefs) : AiProvider {
    override var tokens = 0
    private val base = "https://generativelanguage.googleapis.com/v1beta"

    private fun http(url: String, body: String?): String {
        if (p.apiKey.isBlank()) throw Exception("API key set nai")
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("x-goog-api-key", p.apiKey)
        c.connectTimeout = 20000; c.readTimeout = 60000
        if (body != null) {
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val s = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code !in 200..299)
            throw Exception(try { JSONObject(s).getJSONObject("error").getString("message") } catch (e: Exception) { "HTTP $code" })
        return s
    }

    override suspend fun chat(history: List<Msg>, system: String, temp: Float) = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.takeLast(20).forEach {
            contents.put(JSONObject().put("role", it.role).put("parts", JSONArray().put(JSONObject().put("text", it.text))))
        }
        val body = JSONObject().put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("generationConfig", JSONObject().put("temperature", temp.toDouble()))
        val r = JSONObject(http("$base/models/${p.model}:generateContent", body.toString()))
        tokens += r.optJSONObject("usageMetadata")?.optInt("totalTokenCount") ?: 0
        r.getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0).getString("text")
    }

    override suspend fun models() = withContext(Dispatchers.IO) {
        val a = JSONObject(http("$base/models", null)).getJSONArray("models")
        (0 until a.length()).map { a.getJSONObject(it) }
            .filter { it.optJSONArray("supportedGenerationMethods")?.toString()?.contains("generateContent") == true }
            .map { it.getString("name").removePrefix("models/") }
    }
}

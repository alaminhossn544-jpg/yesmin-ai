package com.yesmin.ai
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

val BG = Color(0xFF070B1A); val Blue = Color(0xFF3D8BFF); val Purple = Color(0xFF9B5CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, secondary = Purple, background = BG, surface = Color(0xFF111833))) { Root() }
        }
    }
}

@Composable fun Root(vm: Vm = viewModel()) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "AI Chat", "Voice Assistant", "App Control", "Coding Studio", "AI Tools", "History", "Settings")
    Scaffold(containerColor = BG, topBar = {
        Column(Modifier.statusBarsPadding()) {
            Text("YESMIN AI", Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Blue)
            ScrollableTabRow(tab, containerColor = BG, edgePadding = 8.dp) {
                tabs.forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().imePadding()) {
            when (tab) {
                0 -> Home(vm); 1 -> Column { ChatList(vm, Modifier.weight(1f)); InputBar(vm) }
                2 -> VoiceScreen(vm); 3 -> Apps(); 4 -> AskPane(vm, codeModes); 5 -> AskPane(vm, toolModes)
                6 -> HistoryScreen(vm); else -> Settings(vm)
            }
        }
    }
}

@Composable fun Orb(status: String, sz: Dp = 180.dp) {
    val t = rememberInfiniteTransition(label = "o")
    val d = when (status) { "Listening" -> 600; "Thinking" -> 300; "Speaking" -> 450; else -> 1800 }
    val s by t.animateFloat(0.88f, 1.1f, infiniteRepeatable(tween(d, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Canvas(Modifier.size(sz)) {
        val r = this.size.minDimension / 2 * s
        drawCircle(Brush.radialGradient(listOf(Blue.copy(.9f), Purple.copy(.6f), Color.Transparent), radius = r * 1.3f), radius = r * 1.3f)
        drawCircle(Brush.radialGradient(listOf(Color.White, Blue, Purple), radius = r * .6f), radius = r * .6f)
    }
}

@Composable fun InputBar(vm: Vm) {
    var t by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(t, { t = it }, Modifier.weight(1f), placeholder = { Text("YESMIN ke likho…") }, maxLines = 3)
        Spacer(Modifier.width(6.dp)); Button({ vm.send(t); t = "" }) { Text("Send") }
    }
}

@Composable fun MicButtons(vm: Vm) {
    val ctx = LocalContext.current
    val l = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.mic() else Toast.makeText(ctx, "Mic permission dorkar", Toast.LENGTH_SHORT).show()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.mic() else l.launch(Manifest.permission.RECORD_AUDIO) }) { Text("🎤 Speak") }
        OutlinedButton({ vm.stopAll() }) { Text("⏹ Stop") }
    }
}

@Composable fun ChatList(vm: Vm, mod: Modifier) {
    val st = rememberLazyListState()
    LaunchedEffect(vm.msgs.size) { if (vm.msgs.isNotEmpty()) st.animateScrollToItem(vm.msgs.lastIndex) }
    LazyColumn(mod, st, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vm.msgs) { m ->
            val u = m.role == "user"
            Box(Modifier.fillMaxWidth(), contentAlignment = if (u) Alignment.CenterEnd else Alignment.CenterStart) {
                Surface(shape = RoundedCornerShape(16.dp), color = if (u) Blue.copy(.35f) else Purple.copy(.25f)) {
                    SelectionContainer { Text(m.text, Modifier.padding(12.dp)) }
                }
            }
        }
    }
}

@Composable fun Home(vm: Vm) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp)); Orb(vm.status)
        Text(vm.status, color = Purple, fontSize = 16.sp)
        Text(vm.msgs.lastOrNull { it.role == "model" }?.text ?: "Hi, ami YESMIN. Ki korte pari?",
            Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()))
        MicButtons(vm); InputBar(vm)
    }
}

@Composable fun VoiceScreen(vm: Vm) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Orb(vm.status, 260.dp); Text(vm.status, fontSize = 20.sp, color = Purple); Spacer(Modifier.height(16.dp))
        MicButtons(vm)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(vm.continuous, { vm.continuous = it }); Text("  Continuous conversation")
        }
        Text("Mic permission tomar approval e chole", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable fun Apps() {
    val c = LocalContext.current
    val all = remember { Device.apps(c) }
    var q by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(8.dp), label = { Text("Search apps") }, singleLine = true)
        LazyColumn { items(all.filter { it.first.contains(q, true) }) { a ->
            TextButton({ Device.launchPkg(c, a.second) }, Modifier.fillMaxWidth()) { Text(a.first, Modifier.fillMaxWidth()) }
        } }
    }
}

val codeModes = mapOf(
    "Generate" to "You are an expert programmer. Write complete working code from the request in fenced code blocks with brief notes.",
    "Explain" to "Explain the given code clearly, step by step, for the user's level.",
    "Debug" to "Find bugs in the given code/error, explain the cause, and give the fixed code.",
    "Project" to "Generate a complete project folder structure plus every file's code for the request.")
val toolModes = mapOf(
    "Translate" to "Translate the text between Bangla, English and Hindi (to English if source is Bangla/Hindi, else to Bangla). Output translation only.",
    "Summarize" to "Summarize the given document text in clear bullet points.",
    "Write" to "You are a writing assistant. Improve or write the text as requested.",
    "Math" to "Solve the math problem step by step and state the final answer.",
    "Search" to "Answer as a research assistant with clear, factual points. Say if unsure.",
    "Study" to "You are a patient tutor. Explain simply with an example and 3 quiz questions.",
    "Email" to "Draft a polished email (subject + body) from the request.",
    "Reminder" to "Turn the request into a clear reminder/to-do list with suggested times.")

@Composable fun AskPane(vm: Vm, modes: Map<String, String>) {
    var mode by remember { mutableStateOf(modes.keys.first()) }
    var inp by remember { mutableStateOf("") }; var out by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current; val ctx = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.keys.forEach { FilterChip(mode == it, { mode = it }, { Text(it) }) }
        }
        OutlinedTextField(inp, { inp = it }, Modifier.fillMaxWidth().heightIn(min = 120.dp), label = { Text("Input") })
        Button({ out = "Thinking…"; vm.run(modes[mode]!!, inp) { out = it } }) { Text("Run") }
        Surface(color = Color(0xFF0D1330), shape = RoundedCornerShape(12.dp)) {
            SelectionContainer { Text(out, Modifier.fillMaxWidth().padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ clip.setText(AnnotatedString(out)) }) { Text("Copy") }
            OutlinedButton({ ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, out), "Export")) }) { Text("Export") }
        }
    }
}

@Composable fun HistoryScreen(vm: Vm) {
    Column {
        TextButton({ vm.msgs.clear() }, Modifier.padding(8.dp)) { Text("Clear history") }
        ChatList(vm, Modifier.weight(1f))
    }
}

@Composable fun Settings(vm: Vm) {
    val p = vm.prefs
    var key by remember { mutableStateOf(p.apiKey) }; var model by remember { mutableStateOf(p.model) }
    var en by remember { mutableStateOf(p.enabled) }; var temp by remember { mutableStateOf(p.temp) }
    var spd by remember { mutableStateOf(p.speed) }; var lang by remember { mutableStateOf(p.lang) }
    var mem by remember { mutableStateOf(p.memory) }; var menu by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI BRAIN SETTINGS", fontWeight = FontWeight.Bold, color = Blue)
        OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("Gemini API Key") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ p.apiKey = key.trim() }) { Text("Save key") }
            OutlinedButton({ p.apiKey = key.trim(); vm.test() }) { Text("Test connection") }
        }
        Text("Status: ${vm.conn}\nTokens used (this session): ${vm.tokens}", fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(en, { en = it; p.enabled = it }); Text("  Enable Gemini") }
        Box {
            OutlinedButton({ menu = true }) { Text("Model: $model") }
            DropdownMenu(menu, { menu = false }) {
                vm.models.ifEmpty { listOf(model) }.forEach { m -> DropdownMenuItem({ Text(m) }, { model = m; p.model = m; menu = false }) }
            }
        }
        Text("Creativity: ${"%.1f".format(temp)}"); Slider(temp, { temp = it; p.temp = it }, valueRange = 0f..2f)
        Text("Language / Voice"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("en-US" to "English", "bn-BD" to "বাংলা", "hi-IN" to "हिन्दी").forEach { (c, n) -> FilterChip(lang == c, { lang = c; p.lang = c }, { Text(n) }) }
        }
        Text("Speech speed: ${"%.1f".format(spd)}"); Slider(spd, { spd = it; p.speed = it }, valueRange = 0.5f..1.8f)
        OutlinedTextField(mem, { mem = it; p.memory = it }, Modifier.fillMaxWidth(), label = { Text("Memory (tumi approve kora preferences)") })
        OutlinedButton({ mem = ""; p.memory = ""; vm.msgs.clear() }) { Text("Clear memory & history") }
        Text("Privacy: API key encrypted on-device. Chat sudhu Gemini API te jay.\nAbout: YESMIN AI v1.0 — AI, manush na.", fontSize = 12.sp, color = Color.Gray)
    }
}

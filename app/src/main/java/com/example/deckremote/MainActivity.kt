package com.example.deckremote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

data class DeckButton(val id: String, val label: String, val icon: String)

/**
 * Connects to the PC through `adb reverse` (USB): localhost:8765 on the phone == the PC's localhost:8765.
 * Protocol: newline-delimited JSON.
 */
class DeckClient(private val scope: CoroutineScope) {
    var status by mutableStateOf("Waiting for USB…")
        private set
    var buttons by mutableStateOf(listOf<DeckButton>())
        private set

    @Volatile private var writer: PrintWriter? = null

    fun start() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    Socket().use { s ->
                        s.connect(InetSocketAddress("127.0.0.1", 8765), 2000)
                        writer = PrintWriter(s.getOutputStream(), true)
                        status = "Connected"
                        val reader = s.getInputStream().bufferedReader()
                        while (true) {
                            val line = reader.readLine() ?: break
                            handle(line)
                        }
                    }
                } catch (_: Exception) {
                }
                writer = null
                status = "Waiting for USB…"
                delay(2000)
            }
        }
    }

    private fun handle(line: String) {
        val obj = JSONObject(line)
        if (obj.optString("type") == "buttons") {
            val arr = obj.getJSONArray("buttons")
            buttons = (0 until arr.length()).map {
                val b = arr.getJSONObject(it)
                DeckButton(b.getString("id"), b.getString("label"), b.optString("icon"))
            }
        }
    }

    fun press(id: String) {
        scope.launch(Dispatchers.IO) {
            writer?.println(JSONObject().put("type", "press").put("id", id).toString())
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            writer?.println(JSONObject().put("type", "refresh").toString())
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { DeckScreen() }
            }
        }
    }
}

@Composable
fun DeckScreen() {
    val scope = rememberCoroutineScope()
    val client = remember { DeckClient(scope).also { it.start() } }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val ok = client.status == "Connected"
            Text(
                text = client.status,
                color = if (ok) Color(0xFF4CAF50) else Color(0xFFFFB300),
                fontSize = 16.sp
            )
            TextButton(onClick = { client.refresh() }) { Text("Refresh") }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(client.buttons, key = { it.id }) { b ->
                Button(
                    onClick = { client.press(b.id) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.icon, fontSize = 32.sp)
                        Text(b.label, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

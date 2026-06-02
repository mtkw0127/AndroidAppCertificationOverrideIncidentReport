package io.github.mtkw0127.app2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.mtkw0127.app2.feature.sync.AuthBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val sentMessage = "Hello from App2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sendBroadcast(Intent("io.github.mtkw0127.ACTION_UPDATE").apply {
            setPackage("io.github.mtkw0127.app1")
            putExtra("message", sentMessage)
        })

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val broadcastReceived by AuthBroadcastReceiver.message.collectAsState()
                    var providerResponse by remember { mutableStateOf("querying...") }

                    LaunchedEffect(Unit) {
                        providerResponse = withContext(Dispatchers.IO) { queryApp1() }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        Text("App2", fontSize = 32.sp)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("[BroadcastReceiver]", fontSize = 12.sp)
                        Text("App2 → App1: $sentMessage", fontSize = 16.sp)
                        Text(
                            "App1 → App2: ${broadcastReceived ?: "(waiting...)"}",
                            fontSize = 16.sp
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("[ContentProvider]", fontSize = 12.sp)
                        Text("App2 → App1: (query)", fontSize = 16.sp)
                        Text("App1 → App2: $providerResponse", fontSize = 16.sp)
                    }
                }
            }
        }
    }

    private fun queryApp1(): String = try {
        "content://io.github.mtkw0127.app1.auth_provider/".toUri().let { uri ->
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "No data"
            } ?: "Null cursor"
        }
    } catch (e: SecurityException) {
        "SecurityException: ${e.message}"
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

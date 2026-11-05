package com.akaswising.conshite

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ConshiteGameHub()
            }
        }
    }
}

private data class GameEntry(
    val name: String,
    val uri: String,
    val format: String
)

@Composable
private fun ConshiteGameHub() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val games = remember {
        mutableStateListOf<GameEntry>().apply {
            addAll(loadSavedGames(context))
        }
    }
    var message by remember { mutableStateOf("Add a PSP ISO or CSO file to your library.") }
    val runtimeStatus = remember { NativeRuntime.status() }

    val gamePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri)
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

            if (extension == "iso" || extension == "cso") {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                val game = GameEntry(
                    name = name,
                    uri = uri.toString(),
                    format = extension.uppercase(Locale.ROOT)
                )

                if (games.none { it.uri == game.uri }) {
                    games.add(game)
                    saveGames(context, games)
                }
                message = "Added " + name + " to the library."
            } else {
                message = "Unsupported file. Choose a .iso or .cso file."
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CONSHITE",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "CONSOLE GAME HUB  /  PSP FIRST",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Your local game library",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Native runtime", fontWeight = FontWeight.Bold)
                    Text(runtimeStatus, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "The C++ runtime is included in the Android build. The PSP emulation core is a separate integration step.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = { gamePicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋  Add PSP game file")
            }

            Text(
                text = "GAME LIBRARY  (" + games.size + ")",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (games.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No games added yet", fontWeight = FontWeight.Bold)
                        Text(
                            "Import a PSP .iso or .cso file that you are entitled to use, or a homebrew game.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(games, key = { it.uri }) { game ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = game.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "PlayStation Portable  •  " + game.format,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Play unavailable: PSP core not connected")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "No game files are bundled with Conshite.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        val index = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
        if (cursor != null && cursor.moveToFirst() && index >= 0) {
            cursor.getString(index) ?: "Unknown game"
        } else {
            uri.lastPathSegment ?: "Unknown game"
        }
    } catch (_: Exception) {
        uri.lastPathSegment ?: "Unknown game"
    } finally {
        cursor?.close()
    }
}

private fun loadSavedGames(context: Context): List<GameEntry> = runCatching {
    val raw = context.getSharedPreferences("conshite_library", Context.MODE_PRIVATE)
        .getString("games", "[]") ?: "[]"
    val json = JSONArray(raw)

    buildList {
        for (index in 0 until json.length()) {
            val item = json.optJSONObject(index) ?: continue
            val uri = item.optString("uri")
            if (uri.isNotBlank()) {
                add(
                    GameEntry(
                        name = item.optString("name", "Unknown game"),
                        uri = uri,
                        format = item.optString("format", "ISO")
                    )
                )
            }
        }
    }
}.getOrDefault(emptyList())

private fun saveGames(context: Context, games: List<GameEntry>) {
    val json = JSONArray()
    games.forEach { game ->
        json.put(
            JSONObject()
                .put("name", game.name)
                .put("uri", game.uri)
                .put("format", game.format)
        )
    }

    context.getSharedPreferences("conshite_library", Context.MODE_PRIVATE)
        .edit()
        .putString("games", json.toString())
        .apply()
}

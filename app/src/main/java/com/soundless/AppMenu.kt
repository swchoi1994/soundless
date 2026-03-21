package com.soundless

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

const val APP_VERSION = "1.0.0"
private const val GITHUB_API = "https://api.github.com/repos/swchoi1994/soundless/releases/latest"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.soundless"

@Composable
fun AppMenuButton(
    onShowInstructions: () -> Unit,
    onRemoveAds: () -> Unit,
    adsRemoved: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2630", fontSize = 22.sp, color = Color(0xFF8E8E93))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF2A2A2A),
            offset = DpOffset(0.dp, 4.dp),
        ) {
            DropdownMenuItem(
                text = { Text(strings.menuInstructions, color = Color.White, fontSize = 15.sp) },
                onClick = { expanded = false; onShowInstructions() },
                leadingIcon = { Text("\uD83D\uDCD6", fontSize = 16.sp) },
            )
            DropdownMenuItem(
                text = { Text(strings.menuCheckUpdate, color = Color.White, fontSize = 15.sp) },
                onClick = { expanded = false; showUpdate = true },
                leadingIcon = { Text("\uD83D\uDD04", fontSize = 16.sp) },
            )
            if (!adsRemoved) {
                DropdownMenuItem(
                    text = { Text(strings.removeAds, color = Color(0xFF34C759), fontSize = 15.sp) },
                    onClick = { expanded = false; onRemoveAds() },
                    leadingIcon = { Text("\u2728", fontSize = 16.sp) },
                )
            }
            HorizontalDivider(color = Color(0xFF3A3A3C), thickness = 0.5.dp)
            DropdownMenuItem(
                text = {
                    Column {
                        Text("${strings.menuVersion}: $APP_VERSION", color = Color(0xFF8E8E93), fontSize = 13.sp)
                        Text("${strings.menuDeveloper}: swc94", color = Color(0xFF8E8E93), fontSize = 13.sp)
                        Text("${strings.menuContact}: swchoi94@seas.upenn.edu", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    }
                },
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:swchoi94@seas.upenn.edu")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                    expanded = false
                },
            )
        }
    }

    if (showUpdate) {
        UpdateDialog(onDismiss = { showUpdate = false })
    }
}

@Composable
private fun UpdateDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var checking by remember { mutableStateOf(true) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var isPlayStore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            // Detect install source
            val installerName = try {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } catch (_: Exception) { null }
            isPlayStore = installerName == "com.android.vending"

            if (isPlayStore) {
                // For Play Store installs, just link to Play Store
                latestVersion = null
                downloadUrl = PLAY_STORE_URL
                checking = false
            } else {
                // For sideloaded APKs, check GitHub releases
                try {
                    val (version, url) = withContext(Dispatchers.IO) { fetchLatestRelease() }
                    latestVersion = version
                    downloadUrl = url
                } catch (_: Exception) {
                    error = true
                }
                checking = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(strings.menuCheckUpdate, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(16.dp))

                when {
                    checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF4A90D9), strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                    }
                    isPlayStore -> {
                        // Play Store: direct link to check
                        Text("\uD83C\uDFEA", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Google Play", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("v$APP_VERSION", color = Color(0xFF8E8E93), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try { context.startActivity(intent) } catch (_: Exception) {}
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9)),
                        ) {
                            Text(strings.menuCheckUpdate, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    error -> {
                        Text(strings.updateCheckFailed, color = Color(0xFFE54D42), fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    latestVersion != null && isNewerVersion(latestVersion!!, APP_VERSION) -> {
                        Text("\uD83C\uDD95", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(strings.updateAvailable, color = Color(0xFF34C759), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("$APP_VERSION \u2192 ${latestVersion?.removePrefix("v")}", color = Color(0xFF8E8E93), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        if (downloadUrl != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9)),
                            ) {
                                Text(strings.updateDownload, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    else -> {
                        Text("\u2705", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(strings.updateLatest, color = Color(0xFF8E8E93), fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("v$APP_VERSION", color = Color(0xFF4A90D9), fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(strings.close, color = Color(0xFF8E8E93))
                }
            }
        }
    }
}

private fun fetchLatestRelease(): Pair<String?, String?> {
    val url = URL(GITHUB_API)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
    conn.connectTimeout = 10_000
    conn.readTimeout = 10_000

    val response = conn.inputStream.bufferedReader().readText()
    conn.disconnect()

    val tagName = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(response)?.groupValues?.get(1)
    val browserUrl = Regex(""""html_url"\s*:\s*"([^"]+)"""").find(response)?.groupValues?.get(1)

    return Pair(tagName, browserUrl)
}

private fun isNewerVersion(remote: String, current: String): Boolean {
    val r = remote.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val c = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(r.size, c.size)) {
        val rv = r.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (rv > cv) return true
        if (rv < cv) return false
    }
    return false
}

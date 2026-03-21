package com.soundless

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val langManager = LanguageManager(applicationContext)
        enableEdgeToEdge()
        setContent {
            val language by langManager.language.collectAsState()
            val strings = remember(language) { language.toStrings() }
            val direction = if (language == Language.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLanguage provides language,
                LocalLayoutDirection provides direction,
            ) {
                SoundlessTheme {
                    val prefs = remember {
                        applicationContext.getSharedPreferences("soundless", Context.MODE_PRIVATE)
                    }
                    var showOnboarding by remember {
                        mutableStateOf(!prefs.getBoolean("onboarding_done", false))
                    }

                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinish = {
                                prefs.edit().putBoolean("onboarding_done", true).apply()
                                showOnboarding = false
                            },
                            langManager = langManager,
                        )
                    } else {
                        val vm: MainViewModel = viewModel()
                        val state by vm.state.collectAsState()
                        SoundlessApp(
                            state, vm, langManager,
                            onShowHelp = { showOnboarding = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoundlessTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        background = Color(0xFF0F0F0F),
        surface = Color(0xFF1A1A1A),
        primary = Color(0xFF4A90D9),
        secondary = Color(0xFF34C759),
        error = Color(0xFFE54D42),
        onBackground = Color.White,
        onSurface = Color.White,
        onPrimary = Color.White,
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun SoundlessApp(
    state: UiState,
    vm: MainViewModel,
    langManager: LanguageManager,
    onShowHelp: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current
    val showAd = !state.adsRemoved && state.screen == AppScreen.MAIN
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onShowInstructions = {
                    scope.launch { drawerState.close() }
                    onShowHelp()
                },
                onRemoveAds = {
                    scope.launch { drawerState.close() }
                    (context as? Activity)?.let { vm.billing.launchPurchaseFlow(it) }
                },
                adsRemoved = state.adsRemoved,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
        gesturesEnabled = true,
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showAd) {
                BannerAd(modifier = Modifier.padding(bottom = 8.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Header with hamburger (left) and language toggle (right)
            Box(modifier = Modifier.fillMaxWidth()) {
                // Hamburger icon — opens sidebar drawer
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { scope.launch { drawerState.open() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u2630", fontSize = 22.sp, color = Color(0xFF8E8E93))
                }
                // Centered title
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            "Soundless",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                    Text(
                        strings.subtitle,
                        fontSize = 16.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                    )
                }
                // Language toggle — top end
                LanguageToggleButton(
                    currentLanguage = language,
                    onLanguageSelected = langManager::setLanguage,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                )
            }

            when (state.screen) {
                AppScreen.CONNECTING -> ConnectingScreen(state)
                AppScreen.PAIRING -> PairingScreen(state, vm)
                AppScreen.MAIN -> MainScreen(state, vm)
                AppScreen.ERROR -> ErrorScreen(state, vm)
            }
        }
    }
    } // ModalNavigationDrawer
}

@Composable
fun ConnectingScreen(state: UiState) {
    val strings = LocalStrings.current
    Spacer(Modifier.height(48.dp))
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    val msg = state.message.resolve(strings)
    Text(
        msg.ifEmpty { strings.connectingChecking },
        color = Color(0xFF8E8E93),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun PairingScreen(state: UiState, vm: MainViewModel) {
    val strings = LocalStrings.current
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0xFF3A3A3C),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(strings.wirelessDebugSetup, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(strings.splitScreenHint, color = Color(0xFF8E8E93), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)

            Spacer(Modifier.height(20.dp))
            Text(strings.step1Label, color = Color(0xFF4A90D9), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = state.connectPort, onValueChange = vm::updateConnectPort, label = { Text(strings.connectPortLabel) }, placeholder = { Text(strings.connectPortPlaceholder) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)

            Spacer(Modifier.height(16.dp))
            Text(strings.step2Label, color = Color(0xFF4A90D9), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = state.pairingPort, onValueChange = vm::updatePairingPort, label = { Text(strings.pairingPortLabel) }, placeholder = { Text(strings.pairingPortPlaceholder) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = state.pairingCode, onValueChange = vm::updatePairingCode, label = { Text(strings.pairingCodeLabel) }, placeholder = { Text(strings.pairingCodePlaceholder) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = vm::pair,
                enabled = !state.isPairing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = Color(0xFF3A3A3C)),
            ) {
                if (state.isPairing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(strings.pairAndConnect, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    val msg = state.message.resolve(strings)
    if (msg.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            msg,
            color = if (state.message.isError()) MaterialTheme.colorScheme.error else Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }

    Spacer(Modifier.height(24.dp))
    Text(strings.pairingFooter, color = Color(0xFF636366), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
}

@Composable
fun MainScreen(state: UiState, vm: MainViewModel) {
    val strings = LocalStrings.current
    val isOff = state.shutterSoundOn == false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, if (isOff) Color(0xFF34C759) else Color(0xFFE54D42)),
    ) {
        Column(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (isOff) "\uD83D\uDD07" else "\uD83D\uDD0A", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(if (isOff) strings.shutterOff else strings.shutterOn, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(if (isOff) strings.shutterOffDesc else strings.shutterOnDesc, fontSize = 14.sp, color = Color(0xFF8E8E93), textAlign = TextAlign.Center)
        }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = vm::toggleShutterSound,
        enabled = !state.toggling && state.shutterSoundOn != null,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isOff) MaterialTheme.colorScheme.primary else Color(0xFF34C759), disabledContainerColor = Color(0xFF3A3A3C)),
    ) {
        if (state.toggling) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(if (isOff) strings.turnOnShutter else strings.turnOffShutter, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    val msg = state.message.resolve(strings)
    if (msg.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 14.sp)
    }

    Spacer(Modifier.height(32.dp))
    Text(strings.mainFooter, color = Color(0xFF636366), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
}

@Composable
fun ErrorScreen(state: UiState, vm: MainViewModel) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Column(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("\u26A0\uFE0F", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(strings.errorTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(state.message.resolve(strings), color = Color(0xFF8E8E93), textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = vm::tryConnect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
        Text(strings.retry, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

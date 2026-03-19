package com.soundless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundlessTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.state.collectAsState()
                SoundlessApp(state, vm)
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
fun SoundlessApp(state: UiState, vm: MainViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
            Text(
                "Soundless",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp,
            )
            Text(
                "카메라 셔터음 제어",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            )

            when (state.screen) {
                AppScreen.CONNECTING -> ConnectingScreen(state)
                AppScreen.PAIRING -> PairingScreen(state, vm)
                AppScreen.MAIN -> MainScreen(state, vm)
                AppScreen.ERROR -> ErrorScreen(state, vm)
            }
        }
    }
}

@Composable
fun ConnectingScreen(state: UiState) {
    Spacer(Modifier.height(48.dp))
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    Text(
        state.message.ifEmpty { "연결 확인 중..." },
        color = Color(0xFF8E8E93),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun PairingScreen(state: UiState, vm: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("무선 디버깅 설정", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "분할 화면으로 설정과 이 앱을 동시에 열어주세요.",
                color = Color(0xFF8E8E93),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "Step 1. 무선 디버깅 화면 상단의 포트",
                color = Color(0xFF4A90D9),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.connectPort,
                onValueChange = vm::updateConnectPort,
                label = { Text("연결 포트") },
                placeholder = { Text("예: 44689") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Step 2. '페어링 코드로 기기 페어링' 클릭 후\n팝업에 표시된 포트와 코드를 입력",
                color = Color(0xFF4A90D9),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.pairingPort,
                onValueChange = vm::updatePairingPort,
                label = { Text("페어링 팝업의 포트") },
                placeholder = { Text("예: 37123 (44689와 다른 번호!)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.pairingCode,
                onValueChange = vm::updatePairingCode,
                label = { Text("페어링 코드 (6자리)") },
                placeholder = { Text("예: 482956") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = vm::pair,
                enabled = !state.isPairing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFF3A3A3C),
                ),
            ) {
                if (state.isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("페어링 및 연결", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (state.message.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            state.message,
            color = if (state.message.contains("실패") || state.message.contains("오류"))
                MaterialTheme.colorScheme.error else Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }

    Spacer(Modifier.height(24.dp))
    Text(
        "ADB 디버깅을 사용하여 시스템 설정을 변경합니다.\nPC 연결 없이 기기 내에서 직접 동작합니다.",
        color = Color(0xFF636366),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
}

@Composable
fun MainScreen(state: UiState, vm: MainViewModel) {
    val isOff = state.shutterSoundOn == false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = CardDefaults.outlinedCardBorder().let {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOff) Color(0xFF34C759) else Color(0xFFE54D42)
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (isOff) "\uD83D\uDD07" else "\uD83D\uDD0A",
                fontSize = 48.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isOff) "셔터음 꺼짐" else "셔터음 켜짐",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isOff) "카메라 셔터음이 비활성화되어 있습니다"
                else "카메라 셔터음이 활성화되어 있습니다",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = vm::toggleShutterSound,
        enabled = !state.toggling && state.shutterSoundOn != null,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOff) MaterialTheme.colorScheme.primary else Color(0xFF34C759),
            disabledContainerColor = Color(0xFF3A3A3C),
        ),
    ) {
        if (state.toggling) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                if (isOff) "셔터음 켜기" else "셔터음 끄기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (state.message.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            state.message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }

    Spacer(Modifier.height(32.dp))
    Text(
        "로컬 ADB를 사용하여 시스템 설정을 변경합니다.\nPC나 USB 연결 없이 바로 적용됩니다.",
        color = Color(0xFF636366),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
}

@Composable
fun ErrorScreen(state: UiState, vm: MainViewModel) {
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
            Text("오류", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                state.message,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = vm::tryConnect,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text("다시 시도", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

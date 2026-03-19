package com.soundless

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen { PAIRING, CONNECTING, MAIN, ERROR }

data class UiState(
    val screen: AppScreen = AppScreen.CONNECTING,
    val pairingCode: String = "",
    val pairingPort: String = "",
    val connectPort: String = "",
    val shutterSoundOn: Boolean? = null,
    val toggling: Boolean = false,
    val message: String = "",
    val isPairing: Boolean = false,
    val isConnecting: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val adb = AdbManager(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        tryConnect()
    }

    fun tryConnect() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(screen = AppScreen.CONNECTING, message = "ADB 서버 시작 중...")
            adb.killServer()
            adb.startServer()
            Thread.sleep(1_000)

            if (adb.isConnected()) {
                loadShutterState()
            } else {
                _state.value = _state.value.copy(
                    screen = AppScreen.PAIRING,
                    message = ""
                )
            }
        }
    }

    private fun extractPort(input: String): Int? {
        val trimmed = input.trim()
        // Handle "IP:port" format (e.g., "172.30.1.22:37123")
        if (trimmed.contains(":")) {
            return trimmed.substringAfterLast(":").toIntOrNull()
        }
        return trimmed.toIntOrNull()
    }

    fun pair() {
        val s = _state.value
        val port = extractPort(s.pairingPort)
        val code = s.pairingCode.trim()
        if (port == null || code.isEmpty()) {
            _state.value = s.copy(message = "페어링 포트와 코드를 입력해주세요")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isPairing = true, message = "페어링 중...")
            val result = adb.pair(port, code)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isPairing = false,
                    message = "페어링 성공! 연결 중..."
                )
                Thread.sleep(1_000)
                connectAfterPair()
            } else {
                _state.value = _state.value.copy(
                    isPairing = false,
                    message = "페어링 실패: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    private fun connectAfterPair() {
        val connectPort = _state.value.connectPort.toIntOrNull()
        if (connectPort == null) {
            _state.value = _state.value.copy(
                message = "무선 디버깅 포트를 입력해주세요"
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isConnecting = true, message = "연결 중...")
            val result = adb.connect(connectPort)
            if (result.isSuccess && adb.isConnected()) {
                loadShutterState()
            } else {
                _state.value = _state.value.copy(
                    isConnecting = false,
                    message = "연결 실패: ${result.exceptionOrNull()?.message ?: "장치를 찾을 수 없습니다"}"
                )
            }
        }
    }

    fun connect() {
        connectAfterPair()
    }

    private fun loadShutterState() {
        val result = adb.getShutterSoundEnabled()
        if (result.isSuccess) {
            _state.value = _state.value.copy(
                screen = AppScreen.MAIN,
                shutterSoundOn = result.getOrNull(),
                message = "",
                isPairing = false,
                isConnecting = false,
            )
        } else {
            val err = result.exceptionOrNull()?.message ?: ""
            if (err.contains("NOT_SUPPORTED")) {
                _state.value = _state.value.copy(
                    screen = AppScreen.ERROR,
                    message = "이 기기는 셔터음 설정을 지원하지 않습니다.\n삼성 갤럭시 한국 출시 기기에서만 사용 가능합니다.",
                    isPairing = false,
                    isConnecting = false,
                )
            } else {
                _state.value = _state.value.copy(
                    screen = AppScreen.MAIN,
                    shutterSoundOn = null,
                    message = "셔터음 상태를 읽을 수 없습니다: $err",
                    isPairing = false,
                    isConnecting = false,
                )
            }
        }
    }

    fun toggleShutterSound() {
        val current = _state.value.shutterSoundOn ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(toggling = true)
            val result = adb.setShutterSoundEnabled(!current)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    shutterSoundOn = !current,
                    toggling = false,
                    message = ""
                )
            } else {
                _state.value = _state.value.copy(
                    toggling = false,
                    message = "설정 변경 실패: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun updatePairingCode(code: String) {
        _state.value = _state.value.copy(pairingCode = code)
    }

    fun updatePairingPort(port: String) {
        _state.value = _state.value.copy(pairingPort = port)
    }

    fun updateConnectPort(port: String) {
        _state.value = _state.value.copy(connectPort = port)
    }

    override fun onCleared() {
        super.onCleared()
        adb.cleanup()
    }
}

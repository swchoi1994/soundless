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

sealed class UiMessage {
    object None : UiMessage()
    object StartingAdb : UiMessage()
    object EnterPairingInfo : UiMessage()
    object Pairing : UiMessage()
    object PairingSuccess : UiMessage()
    data class PairingFailed(val error: String?) : UiMessage()
    object EnterDebugPort : UiMessage()
    object Connecting : UiMessage()
    data class ConnectFailed(val error: String?) : UiMessage()
    object NotSupported : UiMessage()
    data class CannotReadShutter(val error: String) : UiMessage()
    data class SettingChangeFailed(val error: String?) : UiMessage()

    fun isError(): Boolean = when (this) {
        is PairingFailed, is ConnectFailed, is CannotReadShutter,
        is SettingChangeFailed, is EnterPairingInfo, is EnterDebugPort,
        is NotSupported -> true
        else -> false
    }

    fun resolve(s: Strings): String = when (this) {
        is None -> ""
        is StartingAdb -> s.msgStartingAdb
        is EnterPairingInfo -> s.msgEnterPairingInfo
        is Pairing -> s.msgPairing
        is PairingSuccess -> s.msgPairingSuccess
        is PairingFailed -> s.msgPairingFailed(error)
        is EnterDebugPort -> s.msgEnterDebugPort
        is Connecting -> s.msgConnecting
        is ConnectFailed -> s.msgConnectFailed(error)
        is NotSupported -> s.msgNotSupported
        is CannotReadShutter -> s.msgCannotReadShutter(error)
        is SettingChangeFailed -> s.msgSettingChangeFailed(error)
    }
}

data class UiState(
    val screen: AppScreen = AppScreen.CONNECTING,
    val pairingCode: String = "",
    val pairingPort: String = "",
    val connectPort: String = "",
    val shutterSoundOn: Boolean? = null,
    val toggling: Boolean = false,
    val message: UiMessage = UiMessage.None,
    val isPairing: Boolean = false,
    val isConnecting: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val adb = AdbManager(application)
    val nsd = AdbServiceDiscovery(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        tryConnect()
        // Auto-fill ports from NSD discovery
        viewModelScope.launch {
            nsd.connectPort.collect { port ->
                if (port != null && _state.value.connectPort.isEmpty()) {
                    _state.value = _state.value.copy(connectPort = port.toString())
                }
            }
        }
        viewModelScope.launch {
            nsd.pairingPort.collect { port ->
                if (port != null && _state.value.pairingPort.isEmpty()) {
                    _state.value = _state.value.copy(pairingPort = port.toString())
                }
            }
        }
    }

    fun tryConnect() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(screen = AppScreen.CONNECTING, message = UiMessage.StartingAdb)
            adb.killServer()
            adb.startServer()
            Thread.sleep(1_000)

            if (adb.isConnected()) {
                nsd.stopDiscovery()
                loadShutterState()
            } else {
                nsd.startDiscovery()
                _state.value = _state.value.copy(
                    screen = AppScreen.PAIRING,
                    message = UiMessage.None
                )
            }
        }
    }

    private fun extractPort(input: String): Int? {
        val trimmed = input.trim()
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
            _state.value = s.copy(message = UiMessage.EnterPairingInfo)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isPairing = true, message = UiMessage.Pairing)
            val result = adb.pair(port, code)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isPairing = false,
                    message = UiMessage.PairingSuccess
                )
                Thread.sleep(1_000)
                connectAfterPair()
            } else {
                _state.value = _state.value.copy(
                    isPairing = false,
                    message = UiMessage.PairingFailed(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    private fun connectAfterPair() {
        val connectPort = extractPort(_state.value.connectPort)
        if (connectPort == null) {
            _state.value = _state.value.copy(message = UiMessage.EnterDebugPort)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isConnecting = true, message = UiMessage.Connecting)
            val result = adb.connect(connectPort)
            if (result.isSuccess && adb.isConnected()) {
                nsd.stopDiscovery()
                loadShutterState()
            } else {
                _state.value = _state.value.copy(
                    isConnecting = false,
                    message = UiMessage.ConnectFailed(result.exceptionOrNull()?.message)
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
                message = UiMessage.None,
                isPairing = false,
                isConnecting = false,
            )
        } else {
            val err = result.exceptionOrNull()?.message ?: ""
            if (err.contains("NOT_SUPPORTED")) {
                _state.value = _state.value.copy(
                    screen = AppScreen.ERROR,
                    message = UiMessage.NotSupported,
                    isPairing = false,
                    isConnecting = false,
                )
            } else {
                _state.value = _state.value.copy(
                    screen = AppScreen.MAIN,
                    shutterSoundOn = null,
                    message = UiMessage.CannotReadShutter(err),
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
                    message = UiMessage.None
                )
            } else {
                _state.value = _state.value.copy(
                    toggling = false,
                    message = UiMessage.SettingChangeFailed(result.exceptionOrNull()?.message)
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
        nsd.stopDiscovery()
        adb.cleanup()
    }
}

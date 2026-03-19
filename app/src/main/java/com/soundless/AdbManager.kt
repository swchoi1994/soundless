package com.soundless

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintStream

private const val TAG = "SoundlessADB"

class AdbManager(private val context: Context) {

    private val adbPath: String by lazy { getAdbBinaryPath() }
    private val homeDir: File by lazy {
        File(context.filesDir, "adb_home").apply { mkdirs() }
    }
    private val tmpDir: File by lazy {
        File(context.cacheDir, "adb_tmp").apply { mkdirs() }
    }

    private fun getAdbBinaryPath(): String {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        return "$nativeLibDir/libadb.so"
    }

    private fun adb(vararg args: String): Process {
        val command = listOf(adbPath) + args.toList()
        return ProcessBuilder(command)
            .directory(homeDir)
            .apply {
                environment()["HOME"] = homeDir.absolutePath
                environment()["TMPDIR"] = tmpDir.absolutePath
            }
            .redirectErrorStream(true)
            .start()
    }

    private fun runAdb(vararg args: String, timeoutMs: Long = 15_000): Result<String> {
        return try {
            Log.d(TAG, "Running: adb ${args.joinToString(" ")}")
            val process = adb(*args)
            val output = process.inputStream.bufferedReader().readText().trim()
            val exited = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!exited) {
                process.destroyForcibly()
                Log.e(TAG, "Timeout: adb ${args.joinToString(" ")}")
                return Result.failure(Exception("ADB command timed out"))
            }
            Log.d(TAG, "Exit ${process.exitValue()}: $output")
            if (process.exitValue() == 0) {
                Result.success(output)
            } else {
                Result.failure(Exception(output.ifEmpty { "ADB exited with code ${process.exitValue()}" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            Result.failure(e)
        }
    }

    fun startServer(): Result<String> = runAdb("start-server")

    fun killServer(): Result<String> = runAdb("kill-server", timeoutMs = 5_000)

    fun pair(port: Int, code: String): Result<String> {
        return try {
            Log.d(TAG, "Pairing: localhost:$port with code $code")
            val process = adb("pair", "localhost:$port")
            // Wait for the pairing prompt
            Thread.sleep(3_000)
            // Send the pairing code
            PrintStream(process.outputStream).apply {
                println(code)
                flush()
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            val exited = process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                Log.e(TAG, "Pairing timed out")
                return Result.failure(Exception("페어링 시간 초과"))
            }
            Log.d(TAG, "Pair exit ${process.exitValue()}: $output")
            if (output.contains("Successfully paired", ignoreCase = true)) {
                Result.success(output)
            } else {
                Result.failure(Exception(output.ifEmpty { "페어링 실패 (exit ${process.exitValue()})" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pair error: ${e.message}")
            Result.failure(e)
        }
    }

    fun connect(port: Int): Result<String> {
        return runAdb("connect", "localhost:$port")
    }

    fun getDevices(): Result<String> = runAdb("devices")

    fun isConnected(): Boolean {
        val result = getDevices()
        return result.isSuccess && result.getOrDefault("").contains("localhost")
    }

    fun shellCommand(command: String): Result<String> {
        return runAdb("shell", command, timeoutMs = 10_000)
    }

    fun getShutterSoundEnabled(): Result<Boolean> {
        val result = shellCommand("settings get system csc_pref_camera_forced_shuttersound_key")
        return result.map { output ->
            when (output.trim()) {
                "1" -> true
                "0" -> false
                "null" -> throw Exception("NOT_SUPPORTED")
                else -> throw Exception("Unexpected value: $output")
            }
        }
    }

    fun setShutterSoundEnabled(enabled: Boolean): Result<Unit> {
        val value = if (enabled) "1" else "0"
        val result = shellCommand("settings put system csc_pref_camera_forced_shuttersound_key $value")
        return result.map { }
    }

    fun disconnect(): Result<String> = runAdb("disconnect", timeoutMs = 5_000)

    fun cleanup() {
        try { killServer() } catch (_: Exception) {}
    }
}

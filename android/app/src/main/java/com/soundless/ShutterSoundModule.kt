package com.soundless

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class ShutterSoundModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    companion object {
        private const val SHUTTER_SOUND_KEY = "csc_pref_camera_forced_shuttersound_key"
    }

    private var isFirstResume = true

    init {
        reactContext.addLifecycleEventListener(this)
    }

    override fun getName(): String = "ShutterSoundModule"

    @ReactMethod
    fun hasWriteSettingsPermission(promise: Promise) {
        try {
            val canWrite = Settings.System.canWrite(reactContext)
            promise.resolve(canWrite)
        } catch (e: Exception) {
            promise.reject("PERMISSION_CHECK_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun openWriteSettingsPermission(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${reactContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactContext.startActivity(intent)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("OPEN_SETTINGS_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun getShutterSoundEnabled(promise: Promise) {
        try {
            val value = Settings.System.getString(
                reactContext.contentResolver,
                SHUTTER_SOUND_KEY
            )
            if (value == null) {
                promise.reject("NOT_SUPPORTED", "This device does not support the shutter sound setting.")
                return
            }
            promise.resolve(value == "1")
        } catch (e: Exception) {
            promise.reject("READ_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun setShutterSoundEnabled(enabled: Boolean, promise: Promise) {
        try {
            if (!Settings.System.canWrite(reactContext)) {
                promise.reject("NO_PERMISSION", "WRITE_SETTINGS permission not granted.")
                return
            }
            val value = if (enabled) 1 else 0
            val success = Settings.System.putInt(
                reactContext.contentResolver,
                SHUTTER_SOUND_KEY,
                value
            )
            if (!success) {
                promise.reject("WRITE_FAILED", "Failed to write shutter sound setting.")
                return
            }
            // Read back to verify the write actually took effect.
            // Samsung Knox can silently block the write (putInt returns true
            // but the value doesn't change).
            val actual = Settings.System.getString(
                reactContext.contentResolver,
                SHUTTER_SOUND_KEY
            )
            if (actual != value.toString()) {
                promise.reject("WRITE_BLOCKED", "Setting write was blocked by device policy (Knox).")
                return
            }
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("WRITE_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun addListener(@Suppress("UNUSED_PARAMETER") eventName: String) {
        // Required for NativeEventEmitter
    }

    @ReactMethod
    fun removeListeners(@Suppress("UNUSED_PARAMETER") count: Int) {
        // Required for NativeEventEmitter
    }

    private fun sendEvent(eventName: String) {
        if (!reactContext.hasActiveReactInstance()) return
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, null)
    }

    // LifecycleEventListener
    override fun onHostResume() {
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        sendEvent("onAppResume")
    }

    override fun onHostPause() {}

    override fun onHostDestroy() {
        reactContext.removeLifecycleEventListener(this)
    }
}

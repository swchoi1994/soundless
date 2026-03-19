# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Soundless is a native Android (Kotlin + Jetpack Compose) app that lets users toggle the camera shutter sound on Samsung Galaxy devices sold in Korea. It bundles a local ADB server (inspired by [LADB](https://github.com/tytydraco/LADB)) to write the system setting `csc_pref_camera_forced_shuttersound_key` via wireless ADB debugging — no PC or USB required.

The setting persists after the app is closed. Users only need to re-run after firmware updates or factory resets.

## Commands

- `./gradlew :app:assembleDebug` — build debug APK
- `./gradlew :app:assembleRelease` — build release APK (unsigned)
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` — install on device

## Architecture

**Pure Kotlin + Jetpack Compose** (the `android/` directory contains a legacy React Native project that is no longer used — the active app is in `app/`).

### Core Files (`app/src/main/java/com/soundless/`)

- **`MainActivity.kt`** — Single activity with Compose UI. Manages onboarding vs main app flow. Provides language/RTL context via CompositionLocals. Contains all screen composables: ConnectingScreen, PairingScreen, MainScreen, ErrorScreen.
- **`MainViewModel.kt`** — State management (UiState + UiMessage sealed class). Orchestrates ADB pairing, connection, and shutter sound read/write. Integrates NSD auto-discovery.
- **`AdbManager.kt`** — Core ADB logic. Extracts bundled `libadb.so` binary, runs ADB commands via ProcessBuilder (start-server, pair, connect, shell). Reads/writes the shutter sound system setting.
- **`AdbServiceDiscovery.kt`** — mDNS/NSD service discovery. Listens for `_adb-tls-connect._tcp` (connection port) and `_adb-tls-pairing._tcp` (pairing port) to auto-fill port fields.
- **`OnboardingScreen.kt`** — 5-page swipeable setup guide (HorizontalPager). Covers Developer Options, USB Debugging, Wireless Debugging, Pairing, and Usage.
- **`Language.kt`** — Localization system. Defines Language enum, Strings data class with ~60 fields, and full translations for 6 languages. Uses CompositionLocals (LocalStrings, LocalLanguage).
- **`LanguageManager.kt`** — Persists language choice in SharedPreferences. Auto-detects system language on first launch via Locale.
- **`LanguageToggleButton.kt`** — Small dropdown language selector (top-right corner).
- **`SoundlessApp.kt`** — Application class (empty, used for manifest).

### Native Libraries (`app/src/main/jniLibs/`)

Pre-compiled `libadb.so` binaries from LADB (GPLv3) for arm64-v8a, armeabi-v7a, x86, x86_64. Bundled with `useLegacyPackaging = true`.

## Key Details

- **Min SDK:** 30 (Android 11 — required for Wireless ADB Debugging)
- **Target SDK:** 36
- **Kotlin:** 2.1.20, Compose BOM 2025.01.01, Material 3
- **System setting key:** `csc_pref_camera_forced_shuttersound_key` (Samsung Korea-specific). Value `1` = sound on, `0` = sound off, missing key = not supported.
- **ADB approach:** The app bundles a real ADB binary and uses Android's Wireless ADB Debugging to connect locally (localhost). Pairing requires the user to open the wireless debugging pairing dialog in Settings and enter the 6-digit code. Ports are auto-discovered via NSD.
- **Languages:** English, Korean, Japanese, Arabic (RTL), Simplified Chinese, Traditional Chinese. Arabic triggers full RTL layout via LocalLayoutDirection.
- **ViewModel messages** use a sealed `UiMessage` class resolved to localized strings at render time — not hardcoded strings.
- **Onboarding** is shown on first launch (tracked via SharedPreferences `onboarding_done`). Can be re-opened via the help button (book emoji, top-left).

## Legacy Files (not actively used)

The root directory contains React Native files from the original approach (`App.tsx`, `src/`, `package.json`, `android/`). These are kept for reference but the active app is the native Kotlin project in `app/`.

## Next Steps

- **Ad integration** for revenue stream (planned for next session)

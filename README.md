# Soundless

Disable the camera shutter sound on Samsung Galaxy devices (Korea models) — no PC needed.

## What it does

Soundless toggles the system setting `csc_pref_camera_forced_shuttersound_key` using a local ADB server running directly on your phone. Once disabled, the shutter sound stays off even after closing the app. You only need to re-run after firmware updates or factory resets.

## Features

- **One-tap shutter sound toggle** — turn the camera shutter sound on/off instantly
- **No PC or USB cable required** — uses wireless ADB debugging locally on the device
- **Auto port discovery** — detects wireless debugging and pairing ports automatically via mDNS
- **6 languages** — English, 한국어, 日本語, العربية (RTL), 简体中文, 繁體中文
- **5-page onboarding guide** — walks through setup step-by-step
- **Dark theme** with clean, minimal UI

## Requirements

- Android 11+ (API 30)
- Samsung Galaxy device sold in Korea
- Wi-Fi connection (for wireless debugging)

## How it works

1. The app bundles a real ADB binary (from [LADB](https://github.com/tytydraco/LADB))
2. Uses Android's Wireless ADB Debugging to connect to the device locally
3. Runs `adb shell settings put system csc_pref_camera_forced_shuttersound_key 0` to disable the shutter sound
4. The setting persists at the system level — no background service needed

## First-time setup

1. Enable **Developer Options** (Settings > About Phone > tap Build Number 7 times)
2. Turn on **USB Debugging** (if grayed out, disable Auto Blocker in Security settings)
3. Turn on **Wireless Debugging** (requires Wi-Fi)
4. Open Soundless and follow the onboarding guide
5. Use split-screen to pair: open the pairing dialog in Settings, enter the code in Soundless
6. Toggle the shutter sound!

## Building

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Local ADB via bundled `libadb.so` (from LADB, GPLv3)
- mDNS/NSD for automatic port discovery
- CompositionLocal-based localization (no XML string resources)

## License

ADB binaries are from [LADB](https://github.com/tytydraco/LADB) (GPLv3).

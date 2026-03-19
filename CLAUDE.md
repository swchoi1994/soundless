# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Soundless is an Android-only React Native app (TypeScript) that lets users toggle the camera shutter sound on Samsung Galaxy devices sold in Korea. It works by writing to the system setting `csc_pref_camera_forced_shuttersound_key` via a custom native module — no ADB or PC required.

The UI is in Korean. The app is Android-only; the iOS directory exists from the RN template but is unused.

## Commands

- `npm start` — start Metro dev server
- `npm run android` — build and run on Android device/emulator
- `npm test` — run Jest tests
- `npm run lint` — run ESLint

## Architecture

The app is minimal — a single screen with one native module:

**React (TypeScript):**
- `App.tsx` — single-screen UI with status-based rendering (loading, no_permission, not_supported, error, ready)
- `src/useShutterSound.ts` — hook that wraps the native module; manages permission checks, sound state, and toggle logic. Listens for `onAppResume` native events to re-check state when the user returns from system settings.

**Native (Kotlin):**
- `android/.../ShutterSoundModule.kt` — React Native bridge module that reads/writes `Settings.System` for the shutter sound key. Implements `LifecycleEventListener` to emit `onAppResume` events. Requires `WRITE_SETTINGS` permission.
- `android/.../ShutterSoundPackage.kt` — registers the native module

## Key Details

- **Node requirement:** >= 22.11.0
- **React Native:** 0.84.0, React 19.2.3
- The system setting key `csc_pref_camera_forced_shuttersound_key` is Samsung-specific. Devices without this key return `NOT_SUPPORTED`.
- Permission flow: the app checks `Settings.System.canWrite()`, and if denied, opens the system write-settings screen. State is re-checked on resume via native lifecycle events (not AppState).

# Project I.G.I.: I'm Going In (Android Kotlin & Jetpack Compose)

A native **Kotlin + Jetpack Compose** rewrite of the Project I.G.I. Android launcher, Storage Access Framework (SAF) game data verifier, content pack layer manager, and tactical mission briefing/HUD shell.

## Implemented Features

- **Tactical Launcher & SAF Game Folder Verifier**:
  - Preserves the dark tactical phosphor-green UI (`#0A0C08` / `#39E365`) with `img.png` splash artwork and Barlow / JetBrains Mono typography.
  - Uses Android's zero-permission **Storage Access Framework (`OpenDocumentTree`)** to pick a retail Project I.G.I. PC directory, verify signature files (`common`, `missions`, `weapons`, `menusystem`, `sounds`, `humanplayer`, `config.qvm`, `igi.exe`), and copy files into app-private storage (`filesDir/Project IGI`) with determinate byte-level progress (`%` and `MB / MB`).
  - Persists **Renderer Backend** selection (`VULKAN` / `WEBGPU`) and **Target Frame Pacer** (`30 FPS` / `60 FPS`).
- **Content Pack & Load-Order Manager (`Configuration > Content`)**:
  - Installs content packs via SAF into `filesDir/content/<pack-id>/`.
  - Atomically reads and updates `filesDir/content-profile.json` (`schema: 1`, `titles.igi1`) with layer enable/disable switches, up/down priority reordering, rescanning, and one-tap **Disable All Content** recovery.
- **14-Mission Campaign Selector & Cheats Modal**:
  - Loads all 14 campaign missions from `menu-cameras.json`, `fog-presets.json`, and `mission_images/mission1..14.png`.
  - Top-right **Cheats Gear Icon** opens an on-demand modal for **Infinite Health**, **Infinite Ammunition**, and **All Weapons**.
- **Hardware Input, Audio & Diagnostics**:
  - Live `Sensor.TYPE_GYROSCOPE` aim integration (`yaw` / `pitch` deltas) and recenter control.
  - `InputManager` hardware gamepad hot-plug detection and stick/trigger/D-pad monitoring.
  - Looping ambient rain audio (`sounds/rain-loop.wav`) that automatically mutes during pause and cutscenes.
  - On-demand `RECORD_AUDIO` permission request for co-op voice chat and `logcat` export to `Downloads/projectigi_logs.txt` via `MediaStore.Downloads`.
- **Interactive Tactical HUD Preview**:
  - Multi-touch dual virtual sticks, stance/scope/reload/fire controls, weapon slots `1–9`, pause menu (`||`), and hold-to-skip cutscene ring.
  - **TODO (Native Runtime)**: The original precompiled `.NET 10 CoreCLR` (`OpenIGI.Android.dll`) and `C++ NDK` Vulkan/OpenAL bridge (`jni_bridge.cpp`) are omitted in this pure Kotlin/Compose rewrite because neither the C# engine source nor Android NDK/CMake are present.

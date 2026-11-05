# Conshite

**Android GameHub with a PSP-first emulator architecture.**

Conshite is being built as two connected layers:

1. **GameHub (Kotlin + Jetpack Compose):** a local game library, document picker, game metadata, settings, and controller-friendly UI.
2. **Native engine (C++20 + Android NDK):** the shared runtime and the eventual PSP emulation core.

## Current milestone: 0.1.0 foundation

- Android app shell using Kotlin and Jetpack Compose.
- Import and remember user-selected PSP ISO/CSO document URIs.
- C++20 JNI library built for arm64-v8a and armeabi-v7a.
- Explicit Android NDK shared C++ runtime selection (c++_shared).
- GitHub Actions builds a debug APK and checks that libc++_shared.so is packaged for both configured ABIs.

**Important:** the PSP emulator core is not integrated yet. The app can catalogue selected files, but it cannot run games at this milestone. The Play control stays disabled rather than pretending emulation works.

## Console scope

The first target is **PlayStation Portable (PSP)**, matching the PPSSPP-style brief and ISO/CSO formats. This does not mean PS1, PS2, Xbox, Nintendo, or every console format is supported.

The planned PSP core candidate is the upstream PPSSPP C++ project. Its source is GPL-2.0-or-later, so integration and distribution must preserve license notices and provide corresponding source for distributed modifications. Review its upstream AI-use/contribution policy before working inside that code.

## Runtime guarantee

Every configured Android ABI must include the Android NDK C++ runtime required by native libraries. The initial project uses c++_shared because the emulator integration is expected to involve multiple native shared libraries. CI checks the APK for:

- lib/arm64-v8a/libc++_shared.so
- lib/armeabi-v7a/libc++_shared.so

Do not mix different C++ runtime variants across native libraries.

## Games and files

Conshite does not bundle commercial game files. Add only game files you are entitled to use and homebrew titles. The repository contains no ROM/ISO/CSO images.

## Build

Open the repository in Android Studio with Android SDK Platform 37, NDK 28.2.13676358, CMake 3.22.1, and JDK 17. The GitHub Actions workflow builds the debug APK automatically on pushes and pull requests to main.

See [core/README.md](core/README.md) for the PSP-core integration gate.

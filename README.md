# Conshite

**Conshite is an Android PSP GameHub powered by the PPSSPP emulator core.**

## Current architecture

Conshite builds the official **PPSSPP v1.20.4** Android frontend and C++ emulator core from the pinned source revision in the `ppsspp` Git submodule.

- **Android application and GameHub:** Java.
- **Emulator engine:** C++.
- **Graphics:** PPSSPP's Android graphics backends (OpenGL ES and Vulkan where the device/driver supports them).
- **Audio:** PPSSPP's Android audio backend.
- **Input:** PPSSPP's on-screen PSP controls and Android game-controller input, including compatible Bluetooth controllers.
- **Game files:** PSP ISO and CSO files, plus supported homebrew formats.

The upstream Android project already connects its Java activity, native JNI layer, graphics surface, audio callbacks, and controller events to the emulator. Conshite does not use a fake CPU loop or a file scanner as an emulator.

## Native runtime is mandatory

Every packaged ABI uses the Android NDK shared C++ runtime:

- `-DANDROID_STL=c++_shared`
- `lib/arm64-v8a/libc++_shared.so`
- `lib/armeabi-v7a/libc++_shared.so`

GitHub Actions fails the build if either the PPSSPP native library (`libppsspp_jni.so`) or the matching `libc++_shared.so` is missing for either ABI. Future native libraries must use the same shared C++ runtime.

## Build

Clone with submodules enabled:

```bash
git clone --recurse-submodules https://github.com/akaswising-ui/Conshite.git
```

Build requirements:

- JDK 17
- Android SDK Platform 36
- Android NDK 29.0.14206865
- CMake 3.22.1
- Gradle 9.4.1

Build the debug APK:

```bash
gradle --no-daemon :app:assembleNormalDebug
```

The APK is created under `app/build/outputs/apk/normal/debug/`.

## Compatibility and device testing

The emulator core is integrated, but compatibility is not universal. Performance and compatibility vary by game, Android device, graphics driver, and settings. The CI build verifies native compilation and runtime packaging; it does not claim that every commercial game has been tested.

Test only game images you are legally entitled to use or homebrew titles. No commercial game files are included in this repository or APK. Record real-device results in [core/COMPATIBILITY.md](core/COMPATIBILITY.md).

## Languages and third-party notices

Conshite's Android integration layer is Java and its emulator engine is C++. The pinned upstream PPSSPP source also contains third-party C support libraries and build configuration files; those are retained as upstream dependencies rather than rewritten.

PPSSPP is licensed under **GPL-2.0-or-later**. Keep its copyright/license notices with source and distributed builds, and provide the corresponding source for any distributed modifications as required by the license. Conshite is not affiliated with or endorsed by Sony. “PSP” is a Sony trademark.

AI-assisted changes in this repository must be reviewed and tested by the project maintainer before distribution. No AI-generated changes are submitted to the PPSSPP upstream project by this integration.

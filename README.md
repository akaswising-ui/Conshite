# Conshite

**Conshite is an independently branded Android game library with an integrated native emulation engine.**

## Current architecture

Conshite builds the official **PPSSPP v1.20.4** Android frontend and C++ emulator core from the pinned source revision in the `ppsspp` Git submodule.

- **GameHub and Android application:** Java.
- **Game launch controller:** embedded Python 3.11 (Chaquopy), called by Java to validate the selected image and prepare its launch path.
- **PSP emulator engine:** C++ (PPSSPP). Python does not replace the CPU, GPU, audio, or native emulation engine.
- **Graphics:** PPSSPP Android graphics backends (OpenGL ES and Vulkan where supported by the device and driver).
- **Audio:** PPSSPP Android audio backend.
- **Input:** PPSSPP touchscreen controls and Android game-controller input, including compatible Bluetooth controllers.
- **Game files:** PSP ISO and CSO, plus supported homebrew formats.

The Conshite game library imports supported ISO/CSO game images into app-private storage. When a user selects a library tile, Java calls the embedded Python 3.11 controller. Python validates the image and returns a display-session contract naming the PPSSPP C++ native library, Android renderer activity, and SurfaceView/GLSurfaceView host. Java verifies that contract and launches the bundled PPSSPP activity with the game image.

**Phone display path:** PPSSPP's Android activity creates the graphics surface. Its native JNI renderer connects that surface to the PPSSPP C++ graphics/emulation engine, which draws PSP frames on the Android screen. Java hosts the Android lifecycle and surface object; it does not emulate or draw PSP frames. Python controls validation and launch hand-off; it does not generate video frames. This avoids adding a second renderer beside the real emulator.

## Native runtime is mandatory

Every packaged ABI must include the Android NDK shared C++ runtime:

- `-DANDROID_STL=c++_shared`
- `lib/arm64-v8a/libc++_shared.so`
- `lib/armeabi-v7a/libc++_shared.so`
- `libppsspp_jni.so` for both configured ABIs

GitHub Actions fails the build if the emulator library, matching shared C++ runtime, or embedded Python 3.11 library is missing for either configured ABI.

## Build

Clone with submodules enabled:

```bash
git clone --recurse-submodules https://github.com/akaswising-ui/Conshite.git
```

Requirements: JDK 17, Python 3.11, Android SDK 36, NDK 29.0.14206865, CMake 3.22.1, and Gradle 9.4.1. The embedded Python runtime uses Chaquopy 17.2 and raises the app minimum Android API level to 24.

```bash
gradle --no-daemon :app:assembleNormalDebug
```

The debug APK is created under `app/build/outputs/apk/normal/debug/`.

## Compatibility

The PPSSPP engine is integrated into the Android build, but game compatibility is not universal. Performance depends on the title, device, Android version, graphics driver, and settings. CI verifies compilation and packaging; it does not claim that every title has been tested. See [docs/PSP-COMPATIBILITY-TESTING.md](docs/PSP-COMPATIBILITY-TESTING.md).

Use homebrew or game images you are legally entitled to use. No commercial game files are included.

## Languages and license

Conshite's own Android application code is Java and its native integration/emulator code is C++. The upstream PPSSPP source contains supporting C libraries; these remain unchanged upstream dependencies.

PPSSPP is GPL-2.0-or-later. Preserve copyright and license notices and provide corresponding source for distributed modifications as required by that license. Conshite is not affiliated with or endorsed by Sony.


## Conshite-owned C++ runtime

In addition to the upstream PPSSPP C++ emulator submodule, Conshite now includes its own Android native module: `conshite-native`. It builds `libconshite_native.so` with C++17, JNI, Android NDK, and CMake for arm64-v8a and armeabi-v7a.

The native module probes ISO9660/CSO file structure, validates the Python-to-Android renderer hand-off, tracks a thread-safe display-session lifecycle, and attaches an `ANativeWindow` reference to the same `SurfaceView` used by the PPSSPP Android Activity. The C++ surface bridge reports surface attach/detach and dimensions. PPSSPP's own native renderer remains responsible for producing and displaying PSP frames.

The application path is Python control -> Java Android Activity/lifecycle -> Conshite C++17 validation/session/surface bridge -> PPSSPP C++ renderer. See [the native runtime notes](docs/CONSHITE-NATIVE-CPP-RUNTIME.md).

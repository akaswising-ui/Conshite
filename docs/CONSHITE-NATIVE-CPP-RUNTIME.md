# Conshite native C++ runtime

Conshite now builds its own Android native library, `libconshite_native.so`, from the `conshite-native` Android library module.

## C++ responsibilities

- Probe ISO9660 images by checking the primary volume descriptor at sector 16.
- Probe CSO images by checking the CISO header, logical size, block size, index table bounds, and first compressed block offsets.
- Check file readability and ensure its size has not changed since Python validated it.
- Validate the expected PPSSPP renderer library, C++ implementation, Android activity, and SurfaceView host.
- Maintain a thread-safe native display-session state machine.
- Export native build information and session diagnostics through JNI.

## Runtime call sequence

```text
Python 3.11 prepare_game()
        |
        v
Java MainActivity
        |
        v
JNI: libconshite_native.so (C++17)
        |-- probes ISO/CSO structure
        |-- validates renderer/display contract
        |-- creates native display session
        v
Java starts PPSSPP Android Activity
        |
        v
libppsspp_jni.so (upstream PPSSPP C++)
        |
        v
PPSSPP owns the Android graphics surface and draws PSP frames
```

The Conshite C++ module is a native control, file-probe, and session bridge. It does not pretend to replace the PPSSPP emulator or manufacture PSP frames. The upstream PPSSPP C++ renderer remains the video engine.

## Build and verification

The app depends on the `:conshite-native` Android library module. Gradle builds it with Android NDK and CMake for `arm64-v8a` and `armeabi-v7a`. CI checks that `libconshite_native.so`, `libppsspp_jni.so`, `libc++_shared.so`, and `libpython3.11.so` are included in the APK for both ABIs.

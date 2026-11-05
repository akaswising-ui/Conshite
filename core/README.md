# PSP emulator core

Conshite pins the upstream PPSSPP **v1.20.4** source as the `ppsspp` Git submodule.

## Integrated subsystems

- PSP CPU emulation and JIT/interpreter paths: upstream PPSSPP C++ Core.
- PSP graphics emulation: upstream GPU/GE implementation and Android graphics contexts.
- Audio: upstream Android audio backend.
- Android rendering: upstream JNI and Android surface integration.
- Touch controls: upstream on-screen PSP controller.
- External controllers: upstream Android key and joystick event handling.
- Disc images: upstream supported PSP image loaders, including ISO/CSO.

The Android app compiles the PPSSPP `ppsspp_jni` native library from the pinned source. It does not load a separately installed emulator app.

## Runtime rule

The Android CMake configuration explicitly selects `c++_shared`. All configured ABIs must package both `libppsspp_jni.so` and `libc++_shared.so`. CI verifies arm64-v8a and armeabi-v7a.

## Source and license

- Upstream: https://github.com/hrydgard/ppsspp
- Pinned release: v1.20.4
- License: GPL-2.0-or-later

Do not remove upstream copyright notices. Any redistributed modified build must comply with the GPL source and notice requirements.

## Testing boundary

CI proves that the Android app and native emulator library compile and that the C++ runtime is packaged. It does not prove that every game boots or runs at full speed. Commercial game compatibility requires legal test images and real-device testing; no game images are stored in this repository.

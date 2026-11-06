# PSP emulator core

Conshite pins the upstream PPSSPP **v1.20.4** source as the `ppsspp` Git submodule.

## Integrated subsystems

- PSP CPU emulation and JIT/interpreter paths: upstream PPSSPP C++ Core.
- PSP graphics emulation: upstream GPU/GE implementation and Android graphics contexts.
- Audio: upstream Android audio backend.
- Android rendering: upstream JNI and Android surface integration.
- Touch controls: upstream on-screen PSP controller.
- External controllers: upstream Android key and joystick event handling.
- Disc images: upstream supported PSP loaders, including ISO and CSO.

## Python-to-C++ display path

The Java GameHub calls the embedded Python 3.11 controller (`app/src/main/python/conshite_runtime.py`). Python validates the selected ISO/CSO and returns a display-session contract containing the native library name, C++ implementation, Android activity, and surface host. Java verifies that contract and launches the bundled `PpssppActivity` with the game URI.

The phone's PSP picture is rendered by PPSSPP's native C++ graphics path. The Android activity owns the required `SurfaceView`/`GLSurfaceView` lifecycle and connects it to the native renderer through JNI. The C++ emulator produces the frames; Python is the launch/control runtime, not a software video renderer.

## Runtime rule

The Android CMake configuration explicitly selects `c++_shared`. The app also embeds Python 3.11 through Chaquopy. CI verifies `libppsspp_jni.so`, `libc++_shared.so`, and `libpython3.11.so` for `arm64-v8a` and `armeabi-v7a`.

## Source and license

- Upstream: https://github.com/hrydgard/ppsspp
- Pinned release: v1.20.4
- License: GPL-2.0-or-later

Do not remove upstream copyright notices. Distributed modified builds must comply with source and notice requirements.

## Testing boundary

CI proves native compilation and Android packaging only. It does not prove that every game boots or runs at full speed. Record actual results in [the compatibility test plan](../docs/PSP-COMPATIBILITY-TESTING.md). No game images are stored in this repository.

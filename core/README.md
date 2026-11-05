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

The Java GameHub presents a Play action after a library item is selected. Java calls the embedded Python 3.11 controller (`app/src/main/python/conshite_runtime.py`) to validate the selected ISO/CSO path and prepare launch metadata. Java then explicitly launches the bundled `PpssppActivity`. Python is the game-launch controller, not the PSP emulator engine. The upstream activity handles the native emulator lifecycle, Android graphics surface, touch input, and gamepad input.

## Runtime rule

The Android CMake configuration explicitly selects `c++_shared`. The app also embeds Python 3.11 through Chaquopy. CI verifies `libppsspp_jni.so`, `libc++_shared.so`, and `libpython3.11.so` for `arm64-v8a` and `armeabi-v7a`.

## Source and license

- Upstream: https://github.com/hrydgard/ppsspp
- Pinned release: v1.20.4
- License: GPL-2.0-or-later

Do not remove upstream copyright notices. Distributed modified builds must comply with source and notice requirements.

## Testing boundary

CI proves native compilation and Android packaging only. It does not prove that every game boots or runs at full speed. Record actual results in [the compatibility test plan](../docs/PSP-COMPATIBILITY-TESTING.md). No game images are stored in this repository.

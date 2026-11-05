# Emulator core integration

## Initial target: PlayStation Portable (PSP)

Conshite's first console target is PSP because the initial product brief calls for a PPSSPP-style emulator and ISO/CSO game files. This is not a promise that every console format will work.

The Android app currently has a GameHub shell, persistent document-URI library, and a native C++ runtime smoke test. It does **not** contain a PSP CPU/GPU/audio emulator yet, so game launch remains disabled.

## Core candidate

Evaluate the upstream PPSSPP C++ core as the PSP implementation rather than writing a fake interpreter or claiming that a file scanner is an emulator. Before importing or modifying that code:

- Review and follow its GPL-2.0-or-later license.
- Preserve copyright and license notices and provide corresponding source for distributed modifications.
- Review the upstream contribution and AI-use policy before generating or submitting changes to that code.
- Keep proprietary game images out of this repository and APK.

Core integration must connect real CPU execution, GPU rendering, audio, input, save data, and lifecycle handling to the Android app. Add compatibility tests before enabling the Play action.

## Runtime rule

Every Android ABI built by Conshite must package the NDK C++ runtime needed by its native libraries. The initial build selects c++_shared; all future native emulator libraries must use the same shared runtime. CI checks that libc++_shared.so is present for each configured ABI.

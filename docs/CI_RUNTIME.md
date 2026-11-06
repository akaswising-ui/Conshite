# CI Runtime: Conshite adapter

## Official runtime identity

**CI Runtime** is the single runtime name used by Conshite and ALVM Studio. Do not label the Conshite-owned runtime as "C++ Runtime" or "Python Runtime" in the app UI. C++17 and Python 3.11 describe implementation components, not separate product runtimes.

## Conshite's current adapter

Conshite's CI Runtime integration currently coordinates PSP game launch:

1. The Python 3.11 controller validates the selected ISO/CSO image and creates a display-session request.
2. The Java Android host manages the Activity lifecycle, verifies the request, and launches the renderer.
3. The CI Runtime native adapter, implemented in C++17 and exposed through JNI, probes the image, validates the display contract, and tracks the native display session.
4. The PPSSPP native engine performs PSP CPU/GPU/audio emulation and produces the frames shown on the phone.

The native adapter is built as libconshite_native.so. The actual PSP renderer remains libppsspp_jni.so. The Android NDK standard library libc++_shared.so is an implementation dependency; it is not the name or identity of CI Runtime.

## Shared CI Runtime direction

The target is one versioned CI Runtime API and module system used by both apps, with platform services behind adapters:

```text
              Conshite                 ALVM Studio
                  |                         |
                  +-----------+-------------+
                              |
                       CI Runtime API
                              |
                 Runtime Core + Module Loader
                              |
          +-------------------+-------------------+
          |                   |                   |
    PSP/Game Adapter     AL Bytecode Adapter   Core Libraries
          |                   |              Files, UI, Input,
    PPSSPP Engine        .room verifier/VM     Graphics, Audio,
                                             Network, Database
                              |
                Platform adapters (Android first,
                   desktop platforms as targets)
```

The ALVM Studio adapter currently executes verified .room v9 bytecode. The Conshite adapter currently validates and launches PSP game images through PPSSPP. These are different workload formats and must remain separate adapters behind the common runtime contract; an ISO/CSO image is not AL bytecode, and CI Runtime does not replace the PPSSPP emulator.

## Java Runtime analogy

The intended Java-like model requires more than a shared name:

- A stable, versioned runtime API and module ABI.
- A verified, documented execution/package format for supported programs.
- A common module loader, lifecycle, error model, and standard libraries.
- Platform adapters so the same compatible runtime module can use Android or desktop services.
- Compatibility tests that prove a module works on each claimed platform.

## Current status and limits

The runtime identity is standardized as **CI Runtime** in both projects. The underlying engines have not yet been physically consolidated into one shared binary: ALVM Studio currently has a Python 3.11 bytecode executor, while Conshite uses a Python 3.11 launch controller plus a C++17 native adapter and the PPSSPP emulator. The shared API, module ABI, cross-platform core, and general-purpose standard libraries are the next engineering stages. Do not claim Java/JVM-level portability or universal game/app support until those stages are implemented and tested.

See [ALVM Studio's CI Runtime notes](https://github.com/akaswising-ui/ALVM/blob/main/docs/CI_RUNTIME.md) for the current .room execution adapter.

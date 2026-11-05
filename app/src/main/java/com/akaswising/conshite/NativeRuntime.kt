package com.akaswising.conshite

/**
 * Loads the Android NDK shared C++ runtime through the native library.
 * This is a runtime smoke test, not the PSP emulator core.
 */
object NativeRuntime {
    private val libraryLoaded: Boolean = runCatching {
        System.loadLibrary("conshite_runtime")
        true
    }.getOrDefault(false)

    fun status(): String {
        if (!libraryLoaded) {
            return "C++ runtime failed to load"
        }

        return runCatching { nativeRuntimeStatus() }
            .getOrElse { "C++ library loaded; native status check failed" }
    }

    private external fun nativeRuntimeStatus(): String
}

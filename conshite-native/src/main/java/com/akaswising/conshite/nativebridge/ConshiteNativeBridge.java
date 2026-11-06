package com.akaswising.conshite.nativebridge;

/**
 * JNI access to Conshite's own C++17 runtime.
 * PPSSPP remains the actual PSP CPU/GPU/audio emulator and frame renderer.
 */
public final class ConshiteNativeBridge {
    private static final boolean AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("conshite_native");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private ConshiteNativeBridge() { }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static String getBuildInfo() {
        return AVAILABLE ? nativeGetBuildInfo()
            : "{\"available\":false,\"error\":\"conshite_native could not be loaded\"}";
    }

    public static String prepareDisplaySession(
            String gamePath, String engine, String nativeLibrary,
            String implementationLanguage, String androidActivity,
            String surfaceHost, boolean ppssppLoaded, long expectedSize) {
        if (!AVAILABLE) {
            return "{\"ok\":false,\"error\":\"Conshite C++ native runtime is unavailable.\"}";
        }
        return nativePrepareDisplaySession(
            gamePath, engine, nativeLibrary, implementationLanguage,
            androidActivity, surfaceHost, ppssppLoaded, expectedSize
        );
    }

    public static boolean markActivityLaunchRequested(String sessionId) {
        return AVAILABLE && nativeMarkActivityLaunchRequested(sessionId);
    }

    public static void abortDisplaySession(String reason) {
        if (AVAILABLE) nativeAbortDisplaySession(reason);
    }

    public static void endDisplaySession() {
        if (AVAILABLE) nativeEndDisplaySession();
    }

    public static String getDisplayState() {
        return AVAILABLE ? nativeGetDisplayState()
            : "{\"active\":false,\"state\":\"native_runtime_unavailable\"}";
    }

    private static native String nativeGetBuildInfo();
    private static native String nativePrepareDisplaySession(
        String gamePath, String engine, String nativeLibrary,
        String implementationLanguage, String androidActivity,
        String surfaceHost, boolean ppssppLoaded, long expectedSize
    );
    private static native boolean nativeMarkActivityLaunchRequested(String sessionId);
    private static native void nativeAbortDisplaySession(String reason);
    private static native void nativeEndDisplaySession();
    private static native String nativeGetDisplayState();
}

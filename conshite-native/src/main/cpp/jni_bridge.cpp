#include "display_session.h"
#include "native_surface_bridge.h"
#include <jni.h>
#include <cstdint>
#include <string>

namespace {
std::string FromJava(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
jstring ToJava(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeGetBuildInfo(
    JNIEnv* env, jclass) {
    return ToJava(env, conshite::DisplaySessionManager::Instance().BuildInfo());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativePrepareDisplaySession(
    JNIEnv* env, jclass, jstring path, jstring engine, jstring nativeLibrary,
    jstring language, jstring activity, jstring surface, jboolean ppssppLoaded,
    jlong expectedSize) {
    const auto size = expectedSize < 0 ? 0 : static_cast<std::uint64_t>(expectedSize);
    const std::string result = conshite::DisplaySessionManager::Instance().Prepare(
        FromJava(env, path), FromJava(env, engine), FromJava(env, nativeLibrary),
        FromJava(env, language), FromJava(env, activity), FromJava(env, surface),
        ppssppLoaded == JNI_TRUE, size);
    return ToJava(env, result);
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeMarkActivityLaunchRequested(
    JNIEnv* env, jclass, jstring sessionId) {
    return conshite::DisplaySessionManager::Instance().MarkActivityLaunchRequested(
        FromJava(env, sessionId)) ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeAbortDisplaySession(
    JNIEnv* env, jclass, jstring reason) {
    conshite::DisplaySessionManager::Instance().Abort(FromJava(env, reason));
}
extern "C" JNIEXPORT void JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeEndDisplaySession(
    JNIEnv*, jclass) {
    conshite::DisplaySessionManager::Instance().End();
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeGetDisplayState(
    JNIEnv* env, jclass) {
    const auto s = conshite::DisplaySessionManager::Instance().Snapshot();
    std::string json = "{\"active\":";
    json += s.active ? "true" : "false";
    json += ",\"session_id\":" + std::string("\"") + s.session_id + "\"";
    json += ",\"state\":\"" + s.state + "\"";
    json += ",\"engine\":\"" + s.engine + "\"";
    json += ",\"format\":\"" + s.format + "\"";
    json += ",\"image_size_bytes\":" + std::to_string(s.image_size_bytes);
    json += ",\"generation\":" + std::to_string(s.generation);
    json += ",\"android_surface\":" + conshite::NativeSurfaceBridge::Instance().SnapshotJson() + "}";
    return ToJava(env, json);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeOnSurfaceCreated(
    JNIEnv* env, jclass, jobject surface) {
    return conshite::NativeSurfaceBridge::Instance().Attach(env, surface)
        ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeOnSurfaceChanged(
    JNIEnv*, jclass) {
    conshite::NativeSurfaceBridge::Instance().Refresh();
}

extern "C" JNIEXPORT void JNICALL
Java_com_akaswising_conshite_nativebridge_ConshiteNativeBridge_nativeOnSurfaceDestroyed(
    JNIEnv*, jclass) {
    conshite::NativeSurfaceBridge::Instance().Detach();
}

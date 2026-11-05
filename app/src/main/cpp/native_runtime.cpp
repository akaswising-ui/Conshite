#include <jni.h>

#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_akaswising_conshite_NativeRuntime_nativeRuntimeStatus(
    JNIEnv* environment,
    jobject /* receiver */
) {
    const std::string status = "C++20 native runtime loaded successfully";
    return environment->NewStringUTF(status.c_str());
}

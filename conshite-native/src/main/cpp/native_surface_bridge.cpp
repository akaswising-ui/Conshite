#include "native_surface_bridge.h"

#include <sstream>

#include <android/native_window_jni.h>

namespace conshite {

NativeSurfaceBridge& NativeSurfaceBridge::Instance() {
    static NativeSurfaceBridge bridge;
    return bridge;
}

bool NativeSurfaceBridge::Attach(JNIEnv* env, jobject surface) {
    if (env == nullptr || surface == nullptr) return false;

    // ANativeWindow_fromSurface retains a native reference. It does not
    // replace the PPSSPP renderer's own reference or change buffer geometry.
    ANativeWindow* candidate = ANativeWindow_fromSurface(env, surface);
    if (candidate == nullptr) return false;

    std::lock_guard<std::mutex> lock(mutex_);
    if (window_ != nullptr) {
        ANativeWindow_release(window_);
    }
    window_ = candidate;
    ++generation_;
    RefreshLocked();
    return width_ > 0 && height_ > 0;
}

void NativeSurfaceBridge::RefreshLocked() {
    if (window_ == nullptr) {
        width_ = 0;
        height_ = 0;
        format_ = 0;
        return;
    }
    width_ = ANativeWindow_getWidth(window_);
    height_ = ANativeWindow_getHeight(window_);
    format_ = ANativeWindow_getFormat(window_);
}

void NativeSurfaceBridge::Refresh() {
    std::lock_guard<std::mutex> lock(mutex_);
    RefreshLocked();
}

void NativeSurfaceBridge::Detach() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (window_ != nullptr) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
    width_ = 0;
    height_ = 0;
    format_ = 0;
}

std::string NativeSurfaceBridge::SnapshotJson() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ostringstream out;
    out << "{\"attached\":" << (window_ != nullptr ? "true" : "false")
        << ",\"width\":" << width_
        << ",\"height\":" << height_
        << ",\"format\":" << format_
        << ",\"generation\":" << generation_
        << ",\"owner\":\"PPSSPP SurfaceView\"}";
    return out.str();
}

}  // namespace conshite

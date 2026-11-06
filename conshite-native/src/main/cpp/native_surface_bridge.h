#pragma once

#include <cstdint>
#include <jni.h>
#include <mutex>
#include <string>

#include <android/native_window.h>

namespace conshite {

// Retains a reference to the PPSSPP Activity's real Android surface.
// This bridge monitors lifetime and dimensions; PPSSPP remains the renderer.
class NativeSurfaceBridge final {
public:
    static NativeSurfaceBridge& Instance();
    bool Attach(JNIEnv* env, jobject surface);
    void Refresh();
    void Detach();
    std::string SnapshotJson() const;

private:
    NativeSurfaceBridge() = default;
    void RefreshLocked();

    mutable std::mutex mutex_;
    ANativeWindow* window_ = nullptr;
    int width_ = 0;
    int height_ = 0;
    int format_ = 0;
    std::uint64_t generation_ = 0;
};

}  // namespace conshite

#pragma once
#include <cstdint>
#include <mutex>
#include <string>

namespace conshite {
struct DisplaySessionSnapshot {
    bool active = false;
    std::string session_id;
    std::string state = "idle";
    std::string engine;
    std::string native_library;
    std::string format;
    std::string error;
    std::uint64_t image_size_bytes = 0;
    std::uint64_t logical_size_bytes = 0;
    std::uint64_t generation = 0;
};

class DisplaySessionManager final {
public:
    static DisplaySessionManager& Instance();
    std::string BuildInfo() const;
    std::string Prepare(const std::string& path, const std::string& engine,
        const std::string& nativeLibrary, const std::string& language,
        const std::string& activity, const std::string& surface,
        bool ppssppLoaded, std::uint64_t expectedSize);
    bool MarkActivityLaunchRequested(const std::string& sessionId);
    void Abort(const std::string& reason);
    void End();
    DisplaySessionSnapshot Snapshot() const;
private:
    DisplaySessionManager() = default;
    mutable std::mutex mutex_;
    DisplaySessionSnapshot current_;
    std::uint64_t generation_ = 0;
};
}  // namespace conshite

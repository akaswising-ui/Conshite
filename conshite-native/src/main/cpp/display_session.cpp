#include "display_session.h"
#include "image_probe.h"

#include <chrono>
#include <sstream>

namespace conshite {
namespace {
std::uint64_t ClockTicks() {
    return static_cast<std::uint64_t>(
        std::chrono::steady_clock::now().time_since_epoch().count());
}
std::string Quote(const std::string& value) {
    // All fields serialized here are fixed identifiers or generated session IDs.
    return "\"" + value + "\"";
}
std::string SessionJson(const DisplaySessionSnapshot& s, bool ok, const std::string& error) {
    std::ostringstream out;
    out << "{\"ok\":" << (ok ? "true" : "false")
        << ",\"session_id\":" << Quote(s.session_id)
        << ",\"state\":" << Quote(s.state)
        << ",\"engine\":" << Quote(s.engine)
        << ",\"native_library\":" << Quote(s.native_library)
        << ",\"implementation_language\":\"C++17\""
        << ",\"image_format\":" << Quote(s.format)
        << ",\"image_size_bytes\":" << s.image_size_bytes
        << ",\"logical_size_bytes\":" << s.logical_size_bytes
        << ",\"generation\":" << s.generation
        << ",\"error\":" << Quote(error) << "}";
    return out.str();
}
}  // namespace

DisplaySessionManager& DisplaySessionManager::Instance() {
    static DisplaySessionManager manager;
    return manager;
}
std::string DisplaySessionManager::BuildInfo() const {
#if defined(__aarch64__)
    const char* abi = "arm64-v8a";
#elif defined(__arm__)
    const char* abi = "armeabi-v7a";
#elif defined(__x86_64__)
    const char* abi = "x86_64";
#elif defined(__i386__)
    const char* abi = "x86";
#else
    const char* abi = "unknown";
#endif
    std::ostringstream out;
    out << "{\"available\":true,\"runtime\":\"Conshite Native Runtime\","
        << "\"language\":\"C++17\",\"library\":\"conshite_native\","
        << "\"abi\":" << Quote(abi)
        << ",\"display_engine\":\"PPSSPP\","
        << "\"renderer_library\":\"ppsspp_jni\",\"surface_bridge\":\"ANativeWindow\","
        << "\"image_probe\":\"ISO9660-PVD+CSO-CISO\","
        << "\"session_manager\":\"thread-safe\"}";
    return out.str();
}
std::string DisplaySessionManager::Prepare(const std::string& path,
    const std::string& engine, const std::string& nativeLibrary,
    const std::string& language, const std::string& activity,
    const std::string& surface, bool ppssppLoaded, std::uint64_t expectedSize) {
    std::string error;
    if (!ppssppLoaded) error = "PPSSPP native renderer library is not loaded.";
    else if (engine != "PPSSPP") error = "Selected display engine is not PPSSPP.";
    else if (nativeLibrary != "ppsspp_jni") error = "Renderer library does not match PPSSPP JNI.";
    else if (language != "C++") error = "PSP renderer must use the native C++ engine.";
    else if (activity != "com.akaswising.conshite.ConshitePpssppActivity")
        error = "Android renderer activity does not match the bundled PPSSPP activity.";
    else if (surface.find("SurfaceView") == std::string::npos)
        error = "Android display host is not a SurfaceView.";

    GameImageInfo image;
    if (error.empty()) ProbeGameImage(path, expectedSize, &image, &error);

    std::lock_guard<std::mutex> lock(mutex_);
    if (!error.empty()) {
        DisplaySessionSnapshot rejected;
        rejected.state = "rejected";
        rejected.engine = engine;
        rejected.native_library = nativeLibrary;
        return SessionJson(rejected, false, error);
    }

    current_ = DisplaySessionSnapshot{};
    current_.active = true;
    current_.state = "prepared";
    current_.engine = engine;
    current_.native_library = nativeLibrary;
    current_.format = image.format;
    current_.image_size_bytes = image.size_bytes;
    current_.logical_size_bytes = image.logical_size_bytes;
    current_.generation = ++generation_;
    current_.session_id = "conshite-psp-" + std::to_string(generation_)
        + "-" + std::to_string(ClockTicks());
    return SessionJson(current_, true, "");
}
bool DisplaySessionManager::MarkActivityLaunchRequested(const std::string& id) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!current_.active || current_.session_id != id || current_.state != "prepared") return false;
    current_.state = "activity_launch_requested";
    return true;
}
void DisplaySessionManager::Abort(const std::string& reason) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!current_.active) return;
    current_.active = false;
    current_.state = "launch_failed";
    current_.error = reason;
}
void DisplaySessionManager::End() {
    std::lock_guard<std::mutex> lock(mutex_);
    current_.active = false;
    current_.state = "ended";
}
DisplaySessionSnapshot DisplaySessionManager::Snapshot() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return current_;
}
}  // namespace conshite

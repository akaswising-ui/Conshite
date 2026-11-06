#pragma once
#include <cstdint>
#include <string>

namespace conshite {
struct GameImageInfo {
    std::string format;
    std::uint64_t size_bytes = 0;
    std::uint64_t logical_size_bytes = 0;
    std::uint32_t block_size = 0;
    std::uint64_t block_count = 0;
};

bool ProbeGameImage(const std::string& path,
                    std::uint64_t expected_size,
                    GameImageInfo* info,
                    std::string* error);
}  // namespace conshite

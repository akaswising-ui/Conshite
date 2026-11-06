#include "image_probe.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <fstream>
#include <limits>
#include <sys/stat.h>
#include <unistd.h>

namespace conshite {
namespace {
constexpr std::uint64_t kSector = 2048;
constexpr std::uint64_t kPvdSector = 16;
constexpr std::uint64_t kMaxBlockSize = 1024 * 1024;
constexpr std::uint64_t kMaxBlockCount = 100000000;

std::uint32_t ReadLe32(const unsigned char* p) {
    return static_cast<std::uint32_t>(p[0])
        | (static_cast<std::uint32_t>(p[1]) << 8)
        | (static_cast<std::uint32_t>(p[2]) << 16)
        | (static_cast<std::uint32_t>(p[3]) << 24);
}
std::uint64_t ReadLe64(const unsigned char* p) {
    std::uint64_t value = 0;
    for (int i = 7; i >= 0; --i) value = (value << 8) | p[i];
    return value;
}
std::string Extension(const std::string& path) {
    const auto slash = path.find_last_of("/\\");
    const auto dot = path.find_last_of('.');
    if (dot == std::string::npos || (slash != std::string::npos && dot < slash)) return {};
    std::string ext = path.substr(dot);
    std::transform(ext.begin(), ext.end(), ext.begin(),
        [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return ext;
}
bool Fail(std::string* error, const char* message) {
    if (error) *error = message;
    return false;
}
bool ProbeIso(std::ifstream& file, std::uint64_t size,
              GameImageInfo* out, std::string* error) {
    const std::uint64_t offset = kPvdSector * kSector;
    if (size < offset + 7) return Fail(error, "ISO is too small for its volume descriptor.");
    std::array<unsigned char, 7> pvd{};
    file.seekg(static_cast<std::streamoff>(offset), std::ios::beg);
    file.read(reinterpret_cast<char*>(pvd.data()), pvd.size());
    if (file.gcount() != static_cast<std::streamsize>(pvd.size()))
        return Fail(error, "Could not read the ISO volume descriptor.");
    if (pvd[0] != 1 || pvd[6] != 1 || pvd[1] != 'C' || pvd[2] != 'D'
        || pvd[3] != '0' || pvd[4] != '0' || pvd[5] != '1') {
        return Fail(error, "ISO9660 primary volume descriptor not found at sector 16.");
    }
    out->format = "ISO";
    out->size_bytes = size;
    out->logical_size_bytes = size;
    out->block_size = static_cast<std::uint32_t>(kSector);
    out->block_count = (size + kSector - 1) / kSector;
    return true;
}
bool ProbeCso(std::ifstream& file, std::uint64_t size,
              GameImageInfo* out, std::string* error) {
    std::array<unsigned char, 24> h{};
    file.seekg(0, std::ios::beg);
    file.read(reinterpret_cast<char*>(h.data()), h.size());
    if (file.gcount() != static_cast<std::streamsize>(h.size()))
        return Fail(error, "CSO header is incomplete.");
    if (h[0] != 'C' || h[1] != 'I' || h[2] != 'S' || h[3] != 'O')
        return Fail(error, "CSO CISO signature is missing.");

    const std::uint32_t headerSize = ReadLe32(h.data() + 4);
    const std::uint64_t logicalSize = ReadLe64(h.data() + 8);
    const std::uint32_t blockSize = ReadLe32(h.data() + 16);
    const std::uint8_t align = h[21];
    if (headerSize < h.size() || headerSize > size)
        return Fail(error, "CSO header size is outside the file.");
    if (!logicalSize || !blockSize || blockSize > kMaxBlockSize)
        return Fail(error, "CSO logical or block size is invalid.");
    if (align > 31) return Fail(error, "CSO block alignment is invalid.");

    const std::uint64_t blocks = logicalSize / blockSize + (logicalSize % blockSize != 0);
    if (!blocks || blocks > kMaxBlockCount || blocks > (std::numeric_limits<std::uint64_t>::max() / 4) - 1)
        return Fail(error, "CSO block index is too large.");
    const std::uint64_t tableSize = (blocks + 1) * 4;
    if (tableSize > size - headerSize) return Fail(error, "CSO block index table is truncated.");

    std::array<unsigned char, 8> index{};
    file.seekg(static_cast<std::streamoff>(headerSize), std::ios::beg);
    file.read(reinterpret_cast<char*>(index.data()), index.size());
    if (file.gcount() != static_cast<std::streamsize>(index.size()))
        return Fail(error, "CSO first block index is unavailable.");
    const std::uint32_t first = ReadLe32(index.data());
    const std::uint32_t next = ReadLe32(index.data() + 4);
    const std::uint64_t firstOffset = static_cast<std::uint64_t>(first & 0x7fffffffU) << align;
    const std::uint64_t nextOffset = static_cast<std::uint64_t>(next & 0x7fffffffU) << align;
    if (firstOffset < headerSize + tableSize || nextOffset <= firstOffset || nextOffset > size)
        return Fail(error, "CSO first compressed block index is invalid.");

    out->format = "CSO";
    out->size_bytes = size;
    out->logical_size_bytes = logicalSize;
    out->block_size = blockSize;
    out->block_count = blocks;
    return true;
}
}  // namespace

bool ProbeGameImage(const std::string& path, std::uint64_t expectedSize,
                    GameImageInfo* info, std::string* error) {
    if (!info) return Fail(error, "Image probe output is missing.");
    if (path.empty()) return Fail(error, "Game image path is empty.");

    struct stat st {};
    if (::stat(path.c_str(), &st) != 0)
        return Fail(error, "Game image does not exist or cannot be inspected.");
    if (!S_ISREG(st.st_mode)) return Fail(error, "Game image is not a regular file.");
    if (::access(path.c_str(), R_OK) != 0) return Fail(error, "Game image is not readable.");
    if (st.st_size <= 0) return Fail(error, "Game image is empty.");

    const auto actualSize = static_cast<std::uint64_t>(st.st_size);
    if (expectedSize && expectedSize != actualSize)
        return Fail(error, "Game image size changed after Python validation.");

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) return Fail(error, "C++ runtime could not open the game image.");
    const std::string ext = Extension(path);
    if (ext == ".iso") return ProbeIso(file, actualSize, info, error);
    if (ext == ".cso") return ProbeCso(file, actualSize, info, error);
    return Fail(error, "Only PSP ISO and CSO images are supported.");
}
}  // namespace conshite

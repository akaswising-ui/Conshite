#!/usr/bin/env python3
"""Apply the Conshite-owned presentation layer to the pinned native engine.

The upstream source remains a pinned submodule. This script records the exact,
repeatable source modifications used by every Conshite Android build.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NATIVE_APP = ROOT / "ppsspp" / "UI" / "NativeApp.cpp"
GAMEPAD = ROOT / "ppsspp" / "UI" / "GamepadEmu.cpp"
CONFIG = ROOT / "ppsspp" / "Core" / "Config.cpp"


def replace_once(path: Path, old: str, new: str) -> None:
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(
            f"Refusing unsafe patch: expected one match in {path}, found {count}: {old[:100]!r}"
        )
    path.write_text(source.replace(old, new, 1), encoding="utf-8")


# Native title shown by the engine during display/session initialization.
replace_once(
    NATIVE_APP,
    '*app_nice_name = "PPSSPP";',
    '*app_nice_name = "Conshite";',
)

replace_once(
    NATIVE_APP,
    '*app_dir_name = "ppsspp";',
    '*app_dir_name = "conshite";',
)

# On scoped-storage Android builds, upstream intentionally waits for the user
# to choose a memory-stick folder. Conshite owns its data directory, so create
# a private, app-sandboxed path to let direct game launch skip the first-run
# emulator logo/storage setup screen. Do not touch game image content.
replace_once(
    NATIVE_APP,
    '''\t} else {
\t\tINFO_LOG(Log::System, "No memstick directory file found (tried to open '%s')", memStickDirFile.c_str());
\t}

\t// Attempt to create directories after reading the path.''',
    '''\t} else {
\t\tINFO_LOG(Log::System, "No memstick directory file found (tried to open '%s')", memStickDirFile.c_str());
\t}

\tif (g_Config.memStickDirectory.empty()) {
\t\tg_Config.memStickDirectory = Path(savegame_dir) / "ConshiteMemstick";
\t\tif (!File::CreateFullPath(g_Config.memStickDirectory)) {
\t\t\tERROR_LOG(Log::System, "Could not create Conshite private storage: %s", g_Config.memStickDirectory.c_str());
\t\t} else {
\t\t\tCreateSysDirectories();
\t\t}
\t}

\t// Attempt to create directories after reading the path.''',
)

# Original Conshite aqua tint for the existing native touch controls.
# Button hit areas, multi-touch, d-pad, analog stick and input mapping remain
# implemented by the upstream C++ input engine.
replace_once(
    GAMEPAD,
    '''static u32 GetButtonColor() {
\treturn g_Config.iTouchButtonStyle != 0 ? 0xFFFFFF : 0xc0b080;
}''',
    '''static u32 GetButtonColor() {
\treturn g_Config.iTouchButtonStyle != 0 ? 0x42DCCB : 0x2B7180;
}''',
)

# Defaults are applied to fresh installations only; saved per-game preferences
# continue to be respected. Press feedback is animated and controls do not fade.
replace_once(
    CONFIG,
    'ConfigSetting("TouchButtonStyle", SETTING(g_Config, iTouchButtonStyle), 1, CfgFlag::PER_GAME)',
    'ConfigSetting("TouchButtonStyle", SETTING(g_Config, iTouchButtonStyle), 2, CfgFlag::PER_GAME)',
)
replace_once(
    CONFIG,
    'ConfigSetting("TouchButtonOpacity", SETTING(g_Config, iTouchButtonOpacity), 65, CfgFlag::PER_GAME)',
    'ConfigSetting("TouchButtonOpacity", SETTING(g_Config, iTouchButtonOpacity), 76, CfgFlag::PER_GAME)',
)
replace_once(
    CONFIG,
    'ConfigSetting("TouchButtonHideSeconds", SETTING(g_Config, iTouchButtonHideSeconds), 8, CfgFlag::PER_GAME)',
    'ConfigSetting("TouchButtonHideSeconds", SETTING(g_Config, iTouchButtonHideSeconds), 0, CfgFlag::PER_GAME)',
)

print("Conshite native title, private startup storage, and controller defaults applied.")

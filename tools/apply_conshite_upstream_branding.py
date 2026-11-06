#!/usr/bin/env python3
"""Apply the Conshite-owned presentation layer to the pinned native engine.

The upstream source remains a pinned submodule. This script records the exact,
repeatable source modifications used by every Conshite Android build.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NATIVE_APP = ROOT / "ppsspp" / "UI" / "NativeApp.cpp"
GAMEPAD = ROOT / "ppsspp" / "UI" / "GamepadEmu.cpp"
MAIN_SCREEN = ROOT / "ppsspp" / "UI" / "MainScreen.cpp"
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
\t\tINFO_LOG(Log::System, "No memstick directory file found (tried to open '%s')", memstickDirFile.c_str());
\t}

\t// Attempt to create directories after reading the path.''',
    '''\t} else {
\t\tINFO_LOG(Log::System, "No memstick directory file found (tried to open '%s')", memstickDirFile.c_str());
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


# The in-engine library screen may be opened after a game is closed. Give its
# header Conshite's own wordmark instead of upstream icon/logo/version artwork.
replace_once(
    MAIN_SCREEN,
    """\tvoid Draw(UIContext &dc) override {
\t\tusing namespace UI;
\t\tUI::AnchorLayout::Draw(dc);

\t\tconst AtlasImage *iconImg = dc.Draw()->GetAtlas()->getImage(GetIconID());
\t\tconst AtlasImage *logoImg = dc.Draw()->GetAtlas()->getImage(ImageID("I_LOGO"));
\t\tif (!iconImg) {
\t\t\treturn;
\t\t}

\t\tdc.Draw()->DrawImage(GetIconID(), bounds_.x, bounds_.y, 1.0f);

\t\tif (bounds_.w < iconImg->w + logoImg->w + 36) {
\t\t\treturn;
\t\t}

\t\tdc.Draw()->DrawImage(ImageID("I_LOGO"), bounds_.x + iconImg->w + 8, bounds_.y + 4, 1.0f);

\t\tstd::string versionString = PPSSPP_GIT_VERSION;
\t\t// Strip the 'v' from the displayed version, and shorten the commit hash.
\t\tif (versionString.size() > 2) {
\t\t\tif (versionString[0] == 'v' && isdigit(versionString[1])) {
\t\t\t\tversionString = versionString.substr(1);
\t\t\t}
\t\t\tif (CountChar(versionString, '-') == 2) {
\t\t\t\t// Shorten the commit hash.
\t\t\t\tsize_t cutPos = versionString.find_last_of('-') + 8;
\t\t\t\tversionString = versionString.substr(0, std::min(cutPos, versionString.size()));
\t\t\t}
\t\t}
\t\tdc.Flush();

\t\tconst bool tiny = versionString.size() > 10;

\t\tconst FontStyle *style = GetTextStyle(dc, tiny ? TextSize::Tiny : TextSize::Small);
\t\tdc.SetFontStyle(*style);
\t\tdc.DrawText(versionString,
\t\t\tbounds_.x + iconImg->w + 8,
\t\t\tbounds_.y + logoImg->h + (tiny ? 8 : 6),
\t\t\tdc.GetTheme().infoStyle.fgColor);
\t\tdc.SetFontStyle(dc.GetTheme().uiFont);
\t}""",
    """\tvoid Draw(UIContext &dc) override {
\t\tUI::AnchorLayout::Draw(dc);
\t\tdc.Flush();
\t\tconst FontStyle *style = GetTextStyle(dc, TextSize::Small);
\t\tdc.SetFontStyle(*style);
\t\tdc.DrawText("CONSHITE", bounds_.x, bounds_.y + 4, dc.GetTheme().infoStyle.fgColor);
\t\tdc.SetFontStyle(dc.GetTheme().uiFont);
\t}""",
)
replace_once(
    MAIN_SCREEN,
    """\tvoid GetContentDimensions(const UIContext &dc, float &w, float &h) const override {
\t\tconst AtlasImage *iconImg = dc.Draw()->GetAtlas()->getImage(GetIconID());
\t\tw = iconImg->w;
\t\th = iconImg->h;
\t}""",
    """\tvoid GetContentDimensions(const UIContext &, float &w, float &h) const override {
\t\tw = 144.0f;
\t\th = 32.0f;
\t}""",
)
replace_once(
    MAIN_SCREEN,
    'LogoView(bool portrait, UI::LayoutParams *layoutParams) : UI::AnchorLayout(layoutParams), portrait_(portrait) {}',
    'LogoView(bool, UI::LayoutParams *layoutParams) : UI::AnchorLayout(layoutParams) {}',
)
replace_once(
    MAIN_SCREEN,
    """private:
\tImageID GetIconID() const {
\t\treturn System_GetPropertyBool(SYSPROP_APP_GOLD) ? ImageID("I_ICON_GOLD") : ImageID("I_ICON");
\t}

\tconst bool portrait_;
};""",
    """};""",
)
replace_once(
    MAIN_SCREEN,
    """\tbool Touch(const TouchInput &touch) override {
\t\tbool retval = UI::AnchorLayout::Touch(touch);
\t\tif (!portrait_ && (touch.flags & TouchInputFlags::DOWN) && bounds_.Contains(touch.x, touch.y) && touch.y >= bounds_.y2() - 20) {
\t\t\tauto di = GetI18NCategory(I18NCat::DIALOG);
\t\t\tSystem_CopyStringToClipboard(PPSSPP_GIT_VERSION);
\t\t\tg_OSD.Show(OSDType::MESSAGE_INFO, ApplySafeSubstitutions(di->T("Copied to clipboard: %1"), PPSSPP_GIT_VERSION), 0.0f, "copyToClip");
\t\t\treturn true;
\t\t}
\t\treturn retval;
\t}""",
    """\tbool Touch(const TouchInput &touch) override {
\t\treturn UI::AnchorLayout::Touch(touch);
\t}""",
)
replace_once(
    MAIN_SCREEN,
    'new Choice(mm->T("About PPSSPP"))',
    'new Choice(mm->T("Open-source notices"))',
)
replace_once(
    MAIN_SCREEN,
    """\tif (!portrait) {
\t\tparent->Add(new Choice(mm->T("www.ppsspp.org")))->OnClick.Handle(this, &MainScreen::OnPPSSPPOrg);
\t}""",
    """\t// Upstream website shortcuts are not part of Conshite's customer UI.""",
)
replace_once(
    MAIN_SCREEN,
    """\tif (!System_GetPropertyBool(SYSPROP_APP_GOLD) && (System_GetPropertyInt(SYSPROP_DEVICE_TYPE) != DEVICE_TYPE_VR)) {
\t\tChoice *gold = parent->Add(portrait ? new Choice(ImageID("I_ICON_GOLD"), portrait ? new LinearLayoutParams() : nullptr) : new Choice(mm->T("Buy PPSSPP Gold")));
\t\tgold->OnClick.Add([this](UI::EventParams &) {
\t\t\tLaunchBuyGold(this->screenManager());
\t\t});
\t\tgold->SetIconRight(ImageID("I_ICON_GOLD"), 0.5f);
\t\tgold->SetImageScale(0.6f);  // for the left-icon in case of vertical.
\t\tgold->SetShine(true);
\t}""",
    """\t// Upstream add-on promotion is not shown in Conshite.""",
)

print("Conshite native title, private startup storage, controller skin, and menu branding applied.")


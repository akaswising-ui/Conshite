"""Conshite game-launch adapter for the shared CI Runtime identity.

This adapter validates PSP images and prepares the native display session.
Python 3.11 is an implementation component; PPSSPP remains the PSP emulator.
"""
from pathlib import Path
import json
import platform

_SUPPORTED_FORMATS = {".iso", ".cso"}

_DISPLAY_SESSION = {
    "engine": "PPSSPP",
    "native_library": "ppsspp_jni",
    "conshite_native_library": "conshite_native",
    "native_bridge_class": "com.akaswising.conshite.nativebridge.ConshiteNativeBridge",
    "implementation_language": "C++",
    "android_activity": "com.akaswising.conshite.ConshitePpssppActivity",
    "surface_host": "Android SurfaceView/GLSurfaceView",
    "graphics_backends": ["OpenGL ES", "Vulkan"],
}


def display_session_info():
    """Return the CI Runtime native adapter and Android display hand-off contract."""
    return dict(_DISPLAY_SESSION)


def runtime_info():
    """Return machine-readable CI Runtime information for the Conshite adapter."""
    return json.dumps({
        "runtime": "CI Runtime",
        "implementation": "Python " + platform.python_version(),
        "role": "game-launch-controller",
        "display_session": display_session_info(),
        "native_control": {
            "library": "conshite_native",
            "language": "C++17",
            "purpose": "ISO-CSO-probe-and-display-session-manager",
        },
    })


def prepare_game(game_path):
    """Validate a PSP image and prepare the CI Runtime display-session hand-off."""
    if not isinstance(game_path, str) or not game_path.strip():
        raise ValueError("Game path is empty.")

    image = Path(game_path).expanduser().resolve(strict=True)
    if not image.is_file():
        raise ValueError("Game path is not a file.")

    image_format = image.suffix.lower()
    if image_format not in _SUPPORTED_FORMATS:
        raise ValueError("Only PSP ISO and CSO images are supported.")

    size = image.stat().st_size
    if size <= 0:
        raise ValueError("Game image is empty.")

    return json.dumps({
        "path": str(image),
        "format": image_format[1:].upper(),
        "size": size,
        "display_session": display_session_info(),
        "native_control": {
            "library": "conshite_native",
            "language": "C++17",
            "purpose": "ISO-CSO-probe-and-display-session-manager",
        },
    })

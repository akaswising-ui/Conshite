"""Python control runtime for the Conshite PSP display pipeline.

Python validates the selected image and describes the native display session.
The actual PSP emulation and frame rendering remain in PPSSPP's C++ engine.
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
    "android_activity": "org.ppsspp.ppsspp.PpssppActivity",
    "surface_host": "Android SurfaceView/GLSurfaceView",
    "graphics_backends": ["OpenGL ES", "Vulkan"],
}


def display_session_info():
    """Return the native C++ renderer and Android display hand-off contract."""
    return dict(_DISPLAY_SESSION)


def runtime_info():
    """Return machine-readable information about the Python control runtime."""
    return json.dumps({
        "runtime": "Python",
        "version": platform.python_version(),
        "role": "game-launch-controller",
        "display_session": display_session_info(),
        "native_control": {
            "library": "conshite_native",
            "language": "C++17",
            "purpose": "ISO-CSO-probe-and-display-session-manager",
        },
    })


def prepare_game(game_path):
    """Validate a PSP image and prepare the C++ renderer display-session hand-off."""
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

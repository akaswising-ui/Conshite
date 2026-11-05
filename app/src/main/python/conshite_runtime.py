"""Python game-launch controller for Conshite.

This module validates imported PSP image files and prepares a launch descriptor.
PSP CPU/GPU/audio emulation remains in the bundled native PPSSPP engine.
"""
from pathlib import Path
import json
import platform

_SUPPORTED_FORMATS = {".iso", ".cso"}


def runtime_info():
    """Return machine-readable information about the embedded Python runtime."""
    return json.dumps({
        "runtime": "Python",
        "version": platform.python_version(),
        "role": "game-launch-controller",
    })


def prepare_game(game_path):
    """Validate a local PSP image and return its canonical launch metadata."""
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
    })

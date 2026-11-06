import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "app" / "src" / "main" / "python"))

import conshite_runtime


class ConshiteRuntimeTests(unittest.TestCase):
    def test_runtime_info_identifies_python_controller_and_cpp_display(self):
        info = json.loads(conshite_runtime.runtime_info())
        self.assertEqual(info["runtime"], "Python")
        self.assertEqual(info["role"], "game-launch-controller")
        self.assertEqual(info["display_session"]["engine"], "PPSSPP")
        self.assertEqual(info["display_session"]["native_library"], "ppsspp_jni")
        self.assertEqual(info["display_session"]["implementation_language"], "C++")
        self.assertEqual(info["native_control"]["library"], "conshite_native")
        self.assertEqual(info["native_control"]["language"], "C++17")

    def test_display_session_targets_android_native_render_surface(self):
        session = conshite_runtime.display_session_info()
        self.assertEqual(session["android_activity"], "org.ppsspp.ppsspp.PpssppActivity")
        self.assertEqual(session["surface_host"], "Android SurfaceView/GLSurfaceView")
        self.assertIn("OpenGL ES", session["graphics_backends"])
        self.assertIn("Vulkan", session["graphics_backends"])

    def test_prepare_iso_returns_canonical_path_size_and_display_session(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "sample.iso"
            image.write_bytes(b"PSP-IMAGE")
            result = json.loads(conshite_runtime.prepare_game(str(image)))
            self.assertEqual(result["path"], str(image.resolve()))
            self.assertEqual(result["format"], "ISO")
            self.assertEqual(result["size"], 9)
            self.assertEqual(result["display_session"]["native_library"], "ppsspp_jni")
            self.assertEqual(result["display_session"]["conshite_native_library"], "conshite_native")

    def test_prepare_cso_is_supported_case_insensitively(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "sample.CSO"
            image.write_bytes(b"CSO")
            result = json.loads(conshite_runtime.prepare_game(str(image)))
            self.assertEqual(result["format"], "CSO")

    def test_unsupported_extension_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "notes.txt"
            image.write_text("not a game")
            with self.assertRaises(ValueError):
                conshite_runtime.prepare_game(str(image))

    def test_empty_image_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "empty.iso"
            image.write_bytes(b"")
            with self.assertRaises(ValueError):
                conshite_runtime.prepare_game(str(image))

    def test_missing_image_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "missing.iso"
            with self.assertRaises(FileNotFoundError):
                conshite_runtime.prepare_game(str(image))


if __name__ == "__main__":
    unittest.main()

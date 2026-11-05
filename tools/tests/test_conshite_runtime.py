import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "app" / "src" / "main" / "python"))

import conshite_runtime


class ConshiteRuntimeTests(unittest.TestCase):
    def test_runtime_info_identifies_python_controller(self):
        info = json.loads(conshite_runtime.runtime_info())
        self.assertEqual(info["runtime"], "Python")
        self.assertEqual(info["role"], "game-launch-controller")

    def test_prepare_iso_returns_canonical_path_and_size(self):
        with tempfile.TemporaryDirectory() as folder:
            image = Path(folder) / "sample.iso"
            image.write_bytes(b"PSP-IMAGE")
            result = json.loads(conshite_runtime.prepare_game(str(image)))
            self.assertEqual(result["path"], str(image.resolve()))
            self.assertEqual(result["format"], "ISO")
            self.assertEqual(result["size"], 9)

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

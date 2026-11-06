# Conshite C++ PSP display and Python control path

## Runtime roles

- **Python 3.11:** validates the selected ISO/CSO image and returns a display-session contract.
- **Java Android host:** verifies the contract, starts the PPSSPP activity, and supplies the game URI. Android requires an Activity/View host for its window and surface lifecycle.
- **C++ PPSSPP engine:** performs PSP emulation and renders video through the native graphics backend.

## Display hand-off

```text
GameHub selection
      |
      v
Python 3.11 conshite_runtime.prepare_game()
      |  validates file + returns display_session
      v
Java MainActivity verifies native library/activity/surface contract
      |  ACTION_VIEW + PSP image URI
      v
PPSSPP Android Activity
      |  SurfaceView / GLSurfaceView
      v
JNI native graphics bridge
      |
      v
PPSSPP C++ emulator + graphics backend
      |
      v
PSP frames appear on the Android phone display
```

The C++ renderer is the upstream PPSSPP engine bundled as `libppsspp_jni.so`. The Android view is a platform surface host, not a second emulator or a Python-rendered screen. The Python runtime does not produce frames; it controls which game image is validated and handed to the C++ emulator.

## Verification

CI runs Python tests for the display-session contract and checks that `libppsspp_jni.so`, `libc++_shared.so`, and `libpython3.11.so` are packaged for the configured Android ABIs. These checks prove contract consistency and packaging, not that a particular game was tested on a physical phone.

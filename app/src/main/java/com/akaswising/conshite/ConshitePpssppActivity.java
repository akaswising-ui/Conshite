package com.akaswising.conshite;

import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.akaswising.conshite.nativebridge.ConshiteNativeBridge;
import org.ppsspp.ppsspp.PpssppActivity;

/**
 * PPSSPP Activity with a C++ native-window observer attached to its real
 * Android SurfaceView. PPSSPP remains the only frame renderer.
 */
public final class ConshitePpssppActivity extends PpssppActivity
        implements SurfaceHolder.Callback {
    private static final String TAG = "ConshiteSurface";
    private SurfaceView displayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root = findViewById(android.R.id.content);
        displayView = findSurfaceView(root);
        if (displayView != null) {
            displayView.getHolder().addCallback(this);
        } else {
            Log.e(TAG, "PPSSPP did not expose a SurfaceView to Conshite.");
        }
    }

    private SurfaceView findSurfaceView(View view) {
        if (view instanceof SurfaceView) return (SurfaceView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            SurfaceView found = findSurfaceView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Surface surface = holder.getSurface();
        if (!ConshiteNativeBridge.onSurfaceCreated(surface)) {
            Log.e(TAG, "C++ runtime could not attach the PPSSPP Android surface.");
        } else {
            Log.i(TAG, "C++ runtime attached to the PPSSPP Android surface.");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        ConshiteNativeBridge.onSurfaceChanged();
        Log.d(TAG, "PPSSPP surface changed: " + width + "x" + height + " format=" + format);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        ConshiteNativeBridge.onSurfaceDestroyed();
        Log.i(TAG, "C++ runtime detached from the PPSSPP Android surface.");
    }

    @Override
    protected void onDestroy() {
        ConshiteNativeBridge.onSurfaceDestroyed();
        super.onDestroy();
    }
}

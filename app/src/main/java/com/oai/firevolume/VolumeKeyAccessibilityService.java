package com.oai.firevolume;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class VolumeKeyAccessibilityService extends AccessibilityService {
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return false;
        }

        int delta = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                ? AttenuationService.ATTENUATION_STEP_DB
                : -AttenuationService.ATTENUATION_STEP_DB;

        SharedPreferences prefs = getSharedPreferences(AttenuationService.PREFS, MODE_PRIVATE);
        int current = prefs.getInt(AttenuationService.KEY_DB, 0);
        int next = AttenuationService.clampAttenuation(current + delta);

        if (next == current) {
            String boundary = next == 0
                    ? "Attenuation is already off"
                    : "Maximum attenuation: -" + AttenuationService.MAX_ATTENUATION_DB + " dB";
            Toast.makeText(this, boundary, Toast.LENGTH_SHORT).show();
            return true;
        }

        prefs.edit().putInt(AttenuationService.KEY_DB, next).apply();

        Intent intent = new Intent(this, AttenuationService.class);
        intent.setAction(next == 0
                ? AttenuationService.ACTION_DISABLE
                : AttenuationService.ACTION_SET);
        intent.putExtra(AttenuationService.EXTRA_DB, next);
        try {
            startService(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "Could not apply attenuation", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This service only filters compatible hardware volume key events.
    }

    @Override
    public void onInterrupt() {
        // No ongoing spoken or visual accessibility feedback to interrupt.
    }
}

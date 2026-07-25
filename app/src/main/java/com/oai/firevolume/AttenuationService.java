package com.oai.firevolume;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.IBinder;
import android.widget.Toast;

public class AttenuationService extends Service {
    public static final String PREFS = "fire_volume_prefs";
    public static final String KEY_DB = "attenuation_db";
    public static final String KEY_ERROR = "attenuation_error";
    public static final String EXTRA_DB = "db";
    public static final String ACTION_SET = "com.oai.firevolume.SET";
    public static final String ACTION_DISABLE = "com.oai.firevolume.DISABLE";

    private Equalizer equalizer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int db = prefs.getInt(KEY_DB, 0);
        if (intent != null && intent.hasExtra(EXTRA_DB)) {
            db = intent.getIntExtra(EXTRA_DB, db);
        }

        if ((intent != null && ACTION_DISABLE.equals(intent.getAction())) || db <= 0) {
            disableEffect();
            prefs.edit().putInt(KEY_DB, 0).putString(KEY_ERROR, "").apply();
            stopSelf();
            return START_NOT_STICKY;
        }

        applyAttenuation(db);
        return START_STICKY;
    }

    private void applyAttenuation(int db) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            if (equalizer == null) {
                equalizer = new Equalizer(1000, 0);
            }
            short[] range = equalizer.getBandLevelRange();
            int requested = -Math.abs(db) * 100;
            int desired = Math.max(range[0], Math.min(0, requested));
            short bands = equalizer.getNumberOfBands();
            for (short band = 0; band < bands; band++) {
                equalizer.setBandLevel(band, (short) desired);
            }
            equalizer.setEnabled(true);
            int actualDb = Math.abs(desired) / 100;
            prefs.edit().putInt(KEY_DB, actualDb).putString(KEY_ERROR, "").apply();
            Toast.makeText(this, "Global audio attenuation active: -" + actualDb + " dB", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            disableEffect();
            String message = "Global equalizer unsupported on this Fire TV";
            prefs.edit().putString(KEY_ERROR, message).apply();
            Toast.makeText(this, message + ".", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void disableEffect() {
        if (equalizer != null) {
            try { equalizer.setEnabled(false); } catch (Throwable ignored) { }
            try { equalizer.release(); } catch (Throwable ignored) { }
            equalizer = null;
        }
    }

    @Override
    public void onDestroy() {
        disableEffect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

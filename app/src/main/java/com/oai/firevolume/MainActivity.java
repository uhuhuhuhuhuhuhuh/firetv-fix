package com.oai.firevolume;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private AudioManager audioManager;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        buildUi();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(18), dp(28), dp(18));
        root.setBackgroundColor(Color.rgb(20, 22, 26));

        TextView title = text("Fire TV Software Volume", 30, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        TextView explanation = text(
                "Volume Down makes audio quieter by adding 1 dB of attenuation. "
                        + "Volume Up makes it louder by removing 1 dB.",
                17, Color.LTGRAY);
        explanation.setGravity(Gravity.CENTER);
        root.addView(explanation, withTop(dp(8)));

        status = text("", 18, Color.WHITE);
        status.setGravity(Gravity.CENTER);
        root.addView(status, withTop(dp(14)));

        root.addView(label("Android media volume presets"), withTop(dp(14)));
        LinearLayout volumeRow = row();
        volumeRow.addView(button("5%", new View.OnClickListener() {
            @Override public void onClick(View v) { setSystemPercent(5); }
        }));
        volumeRow.addView(button("10%", new View.OnClickListener() {
            @Override public void onClick(View v) { setSystemPercent(10); }
        }));
        volumeRow.addView(button("25%", new View.OnClickListener() {
            @Override public void onClick(View v) { setSystemPercent(25); }
        }));
        volumeRow.addView(button("50%", new View.OnClickListener() {
            @Override public void onClick(View v) { setSystemPercent(50); }
        }));
        root.addView(volumeRow, fullWidth());

        root.addView(label("Software attenuation presets"), withTop(dp(14)));
        LinearLayout attenuationRow = row();
        attenuationRow.addView(button("Off", new View.OnClickListener() {
            @Override public void onClick(View v) { setAttenuation(0); }
        }));
        attenuationRow.addView(button("-6 dB", new View.OnClickListener() {
            @Override public void onClick(View v) { setAttenuation(6); }
        }));
        attenuationRow.addView(button("-12 dB", new View.OnClickListener() {
            @Override public void onClick(View v) { setAttenuation(12); }
        }));
        Button maximum = button("-15 dB", new View.OnClickListener() {
            @Override public void onClick(View v) {
                setAttenuation(AttenuationService.MAX_ATTENUATION_DB);
            }
        });
        attenuationRow.addView(maximum);
        root.addView(attenuationRow, fullWidth());

        LinearLayout keyControlRow = row();
        keyControlRow.addView(button("Enable volume buttons over other apps", new View.OnClickListener() {
            @Override public void onClick(View v) { openAccessibilitySettings(); }
        }));
        root.addView(keyControlRow, withTop(dp(10)));

        TextView note = text(
                "Enable Fire TV Volume Button Control in Accessibility for global control. "
                        + "Some Fire TV remotes send volume commands directly to the television by IR or CEC; "
                        + "those commands never reach an app and cannot be intercepted.",
                14, Color.GRAY);
        note.setGravity(Gravity.CENTER);
        root.addView(note, withTop(dp(10)));

        setContentView(root);
        maximum.requestFocus();
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable t) {
            Toast.makeText(this, "Open Fire TV Settings, then Accessibility", Toast.LENGTH_LONG).show();
        }
    }

    private void setSystemPercent(int percent) {
        int max = Math.max(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int target = Math.round(max * (percent / 100.0f));
        if (percent > 0 && target == 0) target = 1;
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI);
        } catch (Throwable t) {
            Toast.makeText(this, "Fire OS rejected the media-volume change", Toast.LENGTH_SHORT).show();
        }
        refreshStatus();
    }

    private void adjustAttenuation(int deltaDb) {
        int current = getSharedPreferences(AttenuationService.PREFS, MODE_PRIVATE)
                .getInt(AttenuationService.KEY_DB, 0);
        int next = AttenuationService.clampAttenuation(current + deltaDb);

        if (next == current) {
            String boundary = next == 0
                    ? "Attenuation is already off"
                    : "Maximum attenuation: -" + AttenuationService.MAX_ATTENUATION_DB + " dB";
            Toast.makeText(this, boundary, Toast.LENGTH_SHORT).show();
            return;
        }
        setAttenuation(next);
    }

    private void setAttenuation(int db) {
        db = AttenuationService.clampAttenuation(db);
        getSharedPreferences(AttenuationService.PREFS, MODE_PRIVATE)
                .edit().putInt(AttenuationService.KEY_DB, db).apply();

        Intent intent = new Intent(this, AttenuationService.class);
        intent.setAction(db == 0 ? AttenuationService.ACTION_DISABLE : AttenuationService.ACTION_SET);
        intent.putExtra(AttenuationService.EXTRA_DB, db);
        try {
            startService(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "Could not apply attenuation", Toast.LENGTH_LONG).show();
        }
        refreshStatus();
    }

    private void refreshStatus() {
        if (status == null) return;
        int max = Math.max(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int percent = Math.round(current * 100.0f / max);
        boolean fixed = false;
        try { fixed = audioManager.isVolumeFixed(); } catch (Throwable ignored) { }
        int db = getSharedPreferences(AttenuationService.PREFS, MODE_PRIVATE)
                .getInt(AttenuationService.KEY_DB, 0);
        String fixedText = fixed ? " | HDMI volume fixed" : "";
        String attenuationText = db > 0 ? " | attenuation -" + db + " dB" : " | attenuation off";
        status.setText("Media: " + percent + "%" + fixedText + attenuationText);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            adjustAttenuation(AttenuationService.ATTENUATION_STEP_DB);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            adjustAttenuation(-AttenuationService.ATTENUATION_STEP_DB);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1f);
        params.setMargins(dp(5), dp(4), dp(5), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private TextView label(String value) {
        TextView view = text(value, 19, Color.WHITE);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams withTop(int top) {
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = top;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

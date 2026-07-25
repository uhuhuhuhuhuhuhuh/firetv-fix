# Fire TV Software Volume

A small Fire TV utility that attempts to reduce HDMI output volume in software when a television is locked at a fixed or maximum hardware volume.

## Download

Use the APK attached to the latest GitHub release.

## Fixed attenuation

Version 1.2.0 restores the simple fixed-level controls:

- Off
- -6 dB
- -12 dB
- Maximum (-15 dB)

Select a level in the app, then return to the streaming app. The selected attenuation remains active while the attenuation service continues running.

The experimental Accessibility and volume-button interception features from version 1.1.0 were removed because standard Fire TV remote volume buttons normally control the television directly through infrared or HDMI-CEC and do not reach Android apps.

## Notes

- Targets Android 5.0 and newer.
- Does not request internet access.
- Effectiveness depends on the Fire OS audio implementation.
- The global equalizer method may be unavailable on some firmware.

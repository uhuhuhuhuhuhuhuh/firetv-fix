# Fire TV Software Volume

A small Fire TV utility that attempts to reduce HDMI output volume in software when a television is locked at a fixed or maximum hardware volume.

## Download

Use the APK attached to the latest GitHub release.

## Volume-button control

Version 1.1.0 changes compatible volume key events into 1 dB software-attenuation steps:

- Volume Down increases attenuation by 1 dB, making audio quieter.
- Volume Up decreases attenuation by 1 dB, making audio louder.
- Attenuation ranges from off to -15 dB.
- The buttons work while the app is open.
- To use compatible keys over other apps, open the utility and enable **Fire TV Volume Button Control** in Accessibility settings.

Some Fire TV remotes send their volume commands directly to the television through infrared or HDMI-CEC. Those commands do not reach Fire OS and cannot be intercepted by an APK. A Bluetooth remote that sends Android volume key events is more likely to work with the global control service.

## Notes

- Targets Android 5.0 and newer.
- Does not request internet access.
- Effectiveness depends on the Fire OS audio implementation.
- The global equalizer method may be unavailable on some firmware.

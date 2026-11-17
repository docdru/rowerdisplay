# Rower Display

A very simple Android app that shows live data from an FTMS rowing machine, built for the Domyos Woodrower. One landscape screen for phone and tablet:

- **Left:** the goal. A 60-minute timer disc for a time goal, or a bar for a distance goal. After the goal it keeps counting in a second colour.
- **Middle:** one momentary value, such as pace per 500 m, power or stroke rate.
- **Right:** one accumulated value, such as distance, elapsed time or calories.
- **Bottom band:** the middle value over the session, with its target band shaded.

Later: heart rate, then recorded sessions you can compare.

This app is not affiliated with or endorsed by Decathlon or Domyos.

## Status

Milestone M3: connects to the rower over Bluetooth and shows the live session. Goal and display choices are fixed until M4.

## Install

Every pull request and every push to `main` builds a debug APK. Open the CI run under **Actions**, download the `rowerdisplay-debug-apk` artifact, unzip it and install the APK on the device. Signed release builds on GitHub Releases come with milestone M5.

Requires Android 8.0 (API 26) or newer and Bluetooth LE.

On first start the app asks for the Bluetooth permission (on Android 8–11 this is shown as a location permission, because Android requires it for Bluetooth scans; the app has no network access and does not use your location). Turn the rower on by pulling the handle; the app finds it, connects and reconnects by itself.

## How it is built

All code is written by an AI assistant and every build runs in GitHub Actions; nothing is built locally. See [CLAUDE.md](CLAUDE.md) for the conventions.

## Licence

[MIT](LICENSE)

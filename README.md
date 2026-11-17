# Rower Display

A very simple Android app that shows live data from an FTMS rowing machine, built for the Domyos Woodrower. One landscape screen for phone and tablet:

- **Left:** the goal. A 60-minute timer disc for a time goal, or a bar for a distance goal. After the goal it keeps counting in a second colour.
- **Middle:** one momentary value, such as pace per 500 m, power or stroke rate.
- **Right:** one accumulated value, such as distance, elapsed time or calories.
- **Bottom band:** the middle value over the session, with its target band shaded.

Later: heart rate, then recorded sessions you can compare.

This app is not affiliated with or endorsed by Decathlon or Domyos.

## Status

Milestone M2: the rowing screen, driven by a simulated rower. No Bluetooth yet.

## Install

Every pull request and every push to `main` builds a debug APK. Open the CI run under **Actions**, download the `rowerdisplay-debug-apk` artifact, unzip it and install the APK on the device. Signed release builds on GitHub Releases come with milestone M5.

Requires Android 8.0 (API 26) or newer.

## How it is built

All code is written by an AI assistant and every build runs in GitHub Actions; nothing is built locally. See [CLAUDE.md](CLAUDE.md) for the conventions.

## Licence

[MIT](LICENSE)

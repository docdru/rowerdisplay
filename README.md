# Rower Display

A very simple Android app that shows live data from an FTMS rowing machine, built for the Domyos Woodrower. One landscape screen for phone and tablet:

- **Left:** the goal. A 60-minute timer disc for a time goal, or a bar for a distance goal. After the goal it keeps counting in a second colour.
- **Middle:** one momentary value, such as pace per 500 m, power or stroke rate.
- **Right:** one accumulated value, such as distance, elapsed time or calories.
- **Bottom band:** the middle value over the session, with its target band shaded.

<img width="2532" height="1170" alt="image" src="https://github.com/user-attachments/assets/7438afcb-f4ac-4b01-b297-0f3e204ec17f" />
<img width="2532" height="1170" alt="image" src="https://github.com/user-attachments/assets/3ee49294-a821-4bc6-b80f-43f0d34db073" />


Tap a value's label to choose what that slot shows. The start screen sets the goal; the gear opens the settings (target ranges, demo mode). Back or **End** ends the session. **Demo mode** (in the settings) uses a simulated rower instead of Bluetooth, to try the app without the machine.

Later: heart rate, then recorded sessions you can compare.

This app is not affiliated with or endorsed by Decathlon or Domyos.

## Status

Milestone M5: signed releases on GitHub Releases. Next: heart rate (M6), then recorded sessions (M7).

## Install

**Releases:** download `rowerdisplay-vX.Y.Z.apk` from the [Releases page](https://github.com/docdru/rowerdisplay/releases) and install it. The [Obtainium](https://github.com/ImranR98/Obtainium) app can watch that page and install updates for you.

**Test builds:** every pull request builds a debug APK. Open the CI run linked in the PR's screenshot comment, download `rowerdisplay-debug-apk`, unzip it and install. Debug and release builds are signed with different keys: uninstall one before installing the other.

Requires Android 8.0 (API 26) or newer and Bluetooth LE.

On first start the app asks for the Bluetooth permission (on Android 8–11 this is shown as a location permission, because Android requires it for Bluetooth scans; the app has no network access and does not use your location). Turn the rower on by pulling the handle; the app finds it, connects and reconnects by itself.

## How it is built

All code is written by an AI assistant and every build runs in GitHub Actions; nothing is built locally. See [CLAUDE.md](CLAUDE.md) for the conventions.

## Licence

[MIT](LICENSE)

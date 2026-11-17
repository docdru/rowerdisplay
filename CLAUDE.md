# Working on Rower Display

Read this before changing anything. The owner builds this app with AI only and reviews on a phone.

## Rules of the workflow

- **CI is the only builder.** AI sessions may not be able to reach Google's Maven repository, so do not assume a local Gradle build works. Push and read the CI results instead.
- **One milestone or fix = one branch = one pull request.** Never push to `main` directly.
- A pull request is ready only when CI is green. Read the failing step's log, fix, push again.
- Every pull request description says what changed, what the owner should look at (screenshots, APK) and anything to test on the real rower.
- Keep the app simple. When unsure whether a feature is wanted, ask the owner rather than adding it.

## Milestones

| | Scope |
|---|---|
| M0 | Skeleton, CI, licence, this file |
| M1 | Rower data model, metric list, FTMS parser with tests from recorded bytes, simulated rower |
| M2 | Real screen with simulated data: goal disc and bar (incl. past-goal state), two value slots, graph band; phone and tablet screenshots |
| M3 | Bluetooth: scan, connect, auto-reconnect, auto start and pause, keep screen on, sound and vibration at the goal |
| M4 | Start screen (time or distance goal) and settings (slot values, target bands) |
| M5 | Signed release APK on GitHub Releases from a version tag |
| M6 | Heart rate |
| M7 | Recorded sessions, compare against a past session in the graph |
| Later | F-Droid |

## Product decisions (do not change without asking)

- Landscape only, phone and tablet. Dark theme only.
- Top two thirds: goal element (left), momentary value (middle), accumulated value (right). Bottom third: graph of the middle value with its target band shaded. No overlapping elements.
- Time goal: 60-minute disc (full disc = 60 min) that empties. Distance goal: a vertical bar whose remaining part shrinks toward the top.
- At the goal: keep counting, one sound and one vibration. The disc grows back in the opposite direction and the bar fills from the bottom, both in the `goalPassed` colour.
- Middle slot choices are momentary values (pace, power, stroke rate, later heart rate). Right slot choices are accumulated values (distance, elapsed time, calories, stroke count).
- Big numbers never wrap; their font scales down to fit.
- The app name must never contain "Domyos" or "Decathlon".
- No proprietary dependencies (Google Play services, analytics, crash reporting), so F-Droid stays possible.

## Technical conventions

- Kotlin, Jetpack Compose, single `app` module, versions only in `gradle/libs.versions.toml`.
- Package `io.github.docdru.rowerdisplay`. minSdk 26, target and compile SDK 35, JDK 17.
- Custom drawing (disc, bar, graph) with Compose Canvas; no chart libraries. Material 3 is used for controls (buttons, chips, menus, dialogs, switches) only.
- Settings live in `settings/` (`AppSettings`, `SettingsStore` on SharedPreferences). Navigation is a `Screen` enum in `AppViewModel`; no navigation library.
- Colours live in `ui/theme/Theme.kt` (`RowerColors`). Do not hard-code colours elsewhere.
- Planned layers: `ble` (real and simulated rower sources behind one interface), `session` (goal, smoothing, state), `ui`, later `data` (Room).
- Formatting: ktlint (Android Studio style, trailing commas). Composable functions are PascalCase.

## Rower facts (Domyos Woodrower, checked with nRF Connect)

- Advertises as `DOMYOS-ROW-xxxx`, Fitness Machine service 0x1826.
- Rower Data 0x2AD1 (notify): stroke rate, stroke count, average stroke rate, total distance, instantaneous pace, instantaneous power, resistance level, energy (total, per hour, per minute), heart rate, elapsed time.
- Fitness Machine Feature 0x2ACC (read). Training Status 0x2AD3 (notify, read).
- FTMS may split fields across notifications: always parse by the flags field.
- The parser tests use hand-built bytes in the exact layout the rower announced (flags `0x0BAE`). When a log of real notifications exists (nRF Connect export), add those bytes as tests.

## CI

`.github/workflows/ci.yml` runs on every pull request and push to `main`:

1. Gradle wrapper validation
2. `ktlintCheck lintDebug`
3. `testDebugUnitTest -Proborazzi.test.record=true` (unit tests, and screenshots rendered with Roborazzi/Robolectric)
4. `assembleDebug assembleRelease` (the release APK is unsigned here; it only checks that the release build works)

Artifacts: `screenshots` (PNG per screen and device size), `rowerdisplay-debug-apk`, and `reports` on failure.

On pull requests, CI also pushes the screenshots to the `ci-screenshots` branch (`pr-<number>/<short sha>/<name>.png`) and keeps one PR comment that shows them inline. AI sessions usually cannot download Actions artifacts, but they can fetch `https://raw.githubusercontent.com/docdru/rowerdisplay/ci-screenshots/pr-<number>/<short sha>/<name>.png` to look at the result. Never merge `ci-screenshots` into `main`.

`.github/workflows/ai-review.yml` posts an AI review (Mistral, via the `docdru/ai-review` action) as one PR comment, updated on every push. It is advisory, not a required check: verify its findings, fix real ones, and say in the PR when a finding is wrong. Its instructions and checklists are in `.github/ai-review.toml` (read from `main`); update it when this file's rules change. Dependabot keeps the action's pinned commit current.

## Releases

`.github/workflows/release.yml` runs when a tag `vX.Y.Z` is pushed: unit tests, a signed release APK (`versionName` from the tag, `versionCode` = the workflow's run number), a signature check, then a GitHub Release with `rowerdisplay-vX.Y.Z.apk` and generated notes. Create releases only when the owner asks; tag a commit on `main`.

Signing uses four repository secrets (Settings → Secrets and variables → Actions): `RELEASE_KEYSTORE_BASE64` (the `.jks` file, base64), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. The workflow stops with a clear error if one is missing. Never commit a keystore or print a secret. The owner keeps a backup of the keystore: without it, later versions cannot be installed over the existing app. Debug APKs from CI use a different key, so switching between a debug and a release build needs an uninstall.

Screenshot tests live in `app/src/test/.../ScreenshotTest.kt`. Add a test for every new screen state, at phone (`w844dp-h390dp-land-xxhdpi`) and tablet (`w1280dp-h800dp-land-mdpi`) size.

<div align="center">

# AirDeck-Mobile

Android app that turns a phone into a game controller for a Windows PC, with a layout you can edit and settings for almost everything.

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-KTS-02303A?style=flat-square&logo=gradle&logoColor=white)
![License](https://img.shields.io/github/license/horse-3903/AirDeck-Mobile?style=flat-square)
![Last Commit](https://img.shields.io/github/last-commit/horse-3903/AirDeck-Mobile/modern-ui?style=flat-square)

[Releases](https://github.com/horse-3903/AirDeck-Mobile/releases) | [PC server](https://github.com/horse-3903/AirDeck-PC) | [Portfolio page](https://horse-3903.github.io/showcase/airdeck/) | [Upstream project](https://kitswas.github.io/VirtualGamePad/)

</div>

---

## Overview

**AirDeck-Mobile** is the Android client for AirDeck. It draws a full controller on the screen, sends your input to the [PC server](https://github.com/horse-3903/AirDeck-PC) over Wi-Fi, and keeps the connection alive in a foreground service while you switch apps. AirDeck started as a fork of [kitswas/VirtualGamePad-Mobile](https://github.com/kitswas/VirtualGamePad-Mobile) 0.4.2 and was renamed once the changes became large. The work lives on the `modern-ui` branch.

The goal of the fork is a controller that looks and behaves the same as the server's browser page, with more room to tune it. Every control position, size and trigger mode can be changed, and the settings screen covers sticks, D-pad, haptics, display and reconnect behaviour.

---

## Features

- **Full controller** - two analog sticks, a D-pad, four face buttons, bumpers, analog triggers, View, Start and Back, in PlayStation or Xbox style.
- **Same look as the browser version** - matching layout, shapes, symbols, colours, status badge and edge glow, so switching between the app and the browser page feels the same.
- **Layout editor** - move, resize or hide any control, including the connection badge and settings button, change its anchor and pick a trigger mode per trigger. Controls come in default groups (shoulders, left and right clusters, top row) that move together, and you can regroup them. Positions and sizes snap lightly to other controls, there is undo and redo, and a draggable icon toolbar with an Edit and Preview switch. Presets and the JSON import and export carry the groups too.
- **Connection badge** - the top row shows connection state with a chain, a broken chain or a spinner, and your controller number, in a neutral frame. Tap it to disconnect, with a confirmation, and tap again to reconnect.
- **Automatic reconnect** - when the connection drops, the badge shows the hourglass and the app keeps trying for as long as you set, instead of kicking you out.
- **Back keeps the connection** - the Back button returns to the main page straight away and leaves the connection open. Return to controller picks up where you were.
- **Foreground service** - a persistent notification with Wi-Fi and wake locks keeps the link alive with the screen off or another app in front.
- **Sticks that snap** - touch anywhere in a stick's circle and the cap jumps there. Holding still presses the stick like L3 or R3. Dead zone, sensitivity and vertical inversion are adjustable.
- **D-pad** - one touch area for the whole pad. Several directions can be held at once, including diagonals and two fingers, or you can limit it to one direction at a time.
- **Haptics** - custom vibration with adjustable strength and length, or the phone's own touch feedback, with switches for presses, releases, sticks and triggers.
- **Quick settings** - a gear in the top row opens theme, button style and layout import and export without leaving the controller.
- **QR connect** - a full-screen scanner reads the server's QR code. You can also type the address, and a diagnostics screen explains failed connections.
- **Themes** - system, light or dark, plus an edge glow in your slot colour with its own strength.

---

## Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

The app also uses DataStore for settings, CameraX with ZXing for QR scanning, and kotlinx.serialization for layout files.

---

## Getting Started

### Prerequisites

- Android Studio, or a JDK 17 or newer
- Android SDK with platform 36 (the app targets 34 and needs Android 8.0 or newer)
- Git, with submodule support
- A running [PC server](https://github.com/horse-3903/AirDeck-PC) on the same network

### Installation

```bash
git clone --recurse-submodules -b modern-ui https://github.com/horse-3903/AirDeck-Mobile.git
cd AirDeck-Mobile
./gradlew assembleDebug
```

On Windows use `.\gradlew.bat assembleDebug` and set `JAVA_HOME` to a JDK first. The Android Studio bundled runtime works: `C:\Program Files\Android\Android Studio\jbr`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Run locally

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p io.github.horse3903.airdeck -c android.intent.category.LAUNCHER 1
```

Then connect:

1. Start the PC server and open its Connect device tab.
2. In the app, press Connect to PC and scan the App QR code, or type the address as `ip:port`.
3. The controller opens with your slot number in the top row.

To test without a phone, use the Android emulator. It reaches the host PC at `10.0.2.2`, so enter `10.0.2.2:<port>`. The app locks to landscape, and `adb shell settings put system user_rotation 1` forces it in the emulator.

Pre-built debug APKs are on the [releases page](https://github.com/horse-3903/AirDeck-Mobile/releases). They are debug signed, so install them by sideloading.

### Controls

| Control | What it does |
|---|---|
| Face buttons | A, B, X, Y, shown as letters (Xbox) or symbols (PlayStation). |
| D-pad | Touch the pad. Position picks the direction, sliding switches it, two fingers hold two. |
| Sticks | Touch to snap, drag to move, hold still to press. |
| LB and RB | Bumpers, shown as L1 and R1 in PlayStation style. |
| LT and RT | Analog triggers. Button, drag from touch or two zones, set per trigger in the layout editor. |
| View and Start | The two centre buttons. |
| Back arrow | Returns to the main page and keeps the connection. |
| Badge | Shows connection state. Tap to disconnect or reconnect. |
| Gear | Quick settings. |

---

## Settings

Settings are grouped into Appearance, Layout, Display, Sticks, D-pad, Haptics and Connection.

| Setting | Default | Range |
|---|---|---|
| Colour scheme | System | System, light, dark |
| Gamepad style | PlayStation | PlayStation, Xbox |
| Control opacity | 100% | 20 to 100% |
| Edge glow | On | On or off |
| Glow strength | 100% | 10 to 200% |
| Full screen | On | On or off |
| Keep screen awake | On | On or off |
| Snap stick to touch | On | On or off |
| Stick dead zone | 5% | 0 to 40% |
| Stick sensitivity | 100% | 50 to 150% |
| Hold to click a stick | On | On or off |
| Hold time | 450 ms | 200 to 1500 ms |
| Invert left or right stick vertical | Off | On or off, each |
| One direction at a time (D-pad) | Off | On or off |
| Diagonal zone | 38% | 20 to 60% |
| Haptic feedback | On | On or off |
| Custom vibration | On | On or off |
| Vibration strength | 70% | 5 to 100% |
| Vibration length | 20 ms | 5 to 80 ms |
| Presses, releases, sticks, triggers | On | On or off, each |
| Update interval | 80 ms | 20 to 200 ms |
| Remember connection | Off | On or off |
| Reconnect automatically | On | On or off |
| Keep trying for | 120 s | 10 to 600 s |
| Confirm before disconnecting | On | On or off |

---

## Project Structure

```text
AirDeck-Mobile/
├── app/src/main/java/io/github/horse3903/airdeck/
│   ├── data/                 # settings repository, defaults, button configs, layout import
│   ├── network/              # connection view model, foreground service, diagnostics
│   └── ui/
│       ├── composables/      # sticks, D-pad, buttons, triggers, badge, dialogs
│       ├── screens/          # main menu, connect, gamepad, settings, layout editor
│       ├── components/       # QR scanner
│       ├── theme/            # colours, shapes, typography
│       └── utils/            # haptics, orientation lock
├── app/src/main/res/         # strings, icons, drawables
├── VGP_Data_Exchange/        # Colfer wire protocol (submodule)
├── fastlane/metadata/        # store listing text
├── gradle/libs.versions.toml # dependency versions
└── signing.properties.template
```

---

## Deployment

Releases are GitHub pre-releases. The ones before 0.5.0 are named `v0.4.2-modern-ui.N` and use the old package id. Each has a debug-signed APK and a `SHA256SUMS.txt`. A signed release build needs a keystore.

```bash
cp signing.properties.template signing.properties
./gradlew assembleRelease
```

```bash
gh release create v0.5.0 AirDeck-Mobile-0.5.0-debug.apk SHA256SUMS.txt \
  --repo horse-3903/AirDeck-Mobile --prerelease --notes-file notes.md
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JAVA_HOME` | none | JDK used by Gradle. Android Studio's bundled `jbr` works. |
| `ANDROID_HOME` | `%LOCALAPPDATA%\Android\Sdk` on Windows | Android SDK location. |
| `STORE_FILE` | none | Keystore path, set in `signing.properties` for release builds. |
| `STORE_PASSWORD` | none | Keystore password, from `signing.properties`. |
| `KEY_ALIAS` | none | Signing key alias, from `signing.properties`. |
| `KEY_PASSWORD` | none | Signing key password, from `signing.properties`. |

---

## Credits and licence

The app, the protocol and the original interface are the work of [kitswas](https://github.com/kitswas), whose project is [VirtualGamePad](https://kitswas.github.io/VirtualGamePad/). AirDeck adds the matching browser-style layout, the new sticks and D-pad, the connection badge, reconnect and the expanded settings. Released under the GNU General Public License v3.0, see [LICENCE.TXT](LICENCE.TXT).

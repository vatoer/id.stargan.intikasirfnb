# Development & Production Cheatsheet

Quick reference for daily development commands on **IntiKasir F&B**.

---

## ADB — Pairing & Connecting

### Wireless Debugging (Android 11+)

1. On device: **Settings → Developer Options → Wireless Debugging → ON**
2. Tap **Pair device with pairing code** — note the IP:port and code

```bash
# Pair (one-time per device)
adb pair <IP>:<PAIRING_PORT>
# Enter pairing code when prompted

# Connect
adb connect <IP>:<PORT>

# Verify
adb devices
```

### USB Connection

```bash
# List connected devices
adb devices

# If unauthorized, check device screen for USB debugging prompt
```

### Multiple Devices

```bash
# Target specific device
adb -s <DEVICE_SERIAL> install app.apk
adb -s <DEVICE_SERIAL> shell

# List all devices
adb devices -l
```

### Device Info

When multiple devices are connected, prefix with `adb -s <SERIAL>`.

```bash
# List devices with more detail (model, transport)
adb devices -l

# Full build props (model, Android version, manufacturer, etc.)
adb shell getprop

# Common props (single device)
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.brand
adb shell getprop ro.serialno

# All in one (readable summary)
adb shell getprop | grep -E "ro\.(product|build\.version)"
```

**With specific device:**

```bash
SERIAL=R9RTC03NY6F
adb -s $SERIAL shell getprop ro.product.model
adb -s $SERIAL shell getprop ro.build.version.release
```

---

## Build Variants


| Variant       | Application ID                | Desc                |
| ------------- | ----------------------------- | ------------------- |
| `devDebug`    | `id.stargan.intikasirfnb.dev` | Dev + debug logging |
| `devRelease`  | `id.stargan.intikasirfnb.dev` | Dev + optimized     |
| `prodDebug`   | `id.stargan.intikasirfnb`     | Prod + debug        |
| `prodRelease` | `id.stargan.intikasirfnb`     | Production release  |


### Environment Config (`custom.properties`)

Override build config values per machine (gitignored):

```properties
# For physical device, change to your machine's IP
DEV_APPREG_BASE_URL=http://192.168.1.100:8000

# Prod API
PROD_APPREG_BASE_URL=https://appreg.stargan.id
```

If `custom.properties` doesn't exist, defaults are used (`10.0.2.2:8000` for dev, `appreg.stargan.id` for prod).

---

## Build & Install

### Debug Build (Development)

```bash
# Build dev debug APK
./gradlew assembleDevDebug

# Build and install to connected device
./gradlew installDevDebug

# APK location
# app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

### Release Build (Production)

```bash
# Build prod release APK
./gradlew assembleProdRelease

# Build prod release AAB (for Play Store)
./gradlew bundleProdRelease

# APK location
# app/build/outputs/apk/prod/release/app-prod-release.apk
# AAB location
# app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

### Install via ADB

**Note:** With multiple devices, use `adb -s SERIAL` — the `-s` must come **right after** `adb`, not after `install`.

```bash
# Install APK
adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk

# Install with replacement (update existing)
adb install -r app.apk

# Install to specific device (-s right after adb)
adb -s <DEVICE> install -r app.apk

# Example with variable
DEV=adb-ZP22228JNS-GqGncn._adb-tls-connect._tcp
adb -s $DEV install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk

# Uninstall
adb uninstall id.stargan.intikasirfnb.dev
adb uninstall id.stargan.intikasirfnb
```

---

## Run & Debug

### Logcat

When **more than one device** is connected, use `-s <SERIAL>` so adb knows which device to use:

```bash
# 1) List devices and pick a serial (e.g. R9RTC03NY6F or adb-... for wireless)
adb devices

# 2) Logcat for dev app on that device only (replace SERIAL with your device)
adb -s R9RTC03NY6F logcat --pid=$(adb -s R9RTC03NY6F shell pidof id.stargan.intikasirfnb.dev)
```

One-liner with a variable (set `DEV` to the serial you want):

```bash
DEV=adb-ZP22228JNS-GqGncn._adb-tls-connect._tcp
adb -s $DEV logcat --pid=$(adb -s $DEV shell pidof id.stargan.intikasirfnb.dev)
```

Other useful logcat commands:

```bash
# Filter by app package (single device only)
adb logcat --pid=$(adb shell pidof id.stargan.intikasirfnb.dev)

# Filter by tag
adb logcat -s "MyTag"

# Clear and start fresh
adb logcat -c && adb logcat

# Save to file
adb logcat -d > logcat.txt
```

### Launch App via ADB

```bash
# Launch dev debug
adb shell am start -n id.stargan.intikasirfnb.dev/.ui.MainActivity

# Force stop
adb shell am force-stop id.stargan.intikasirfnb.dev
```

---

## Database (Room)

### Inspect Database

```bash
# Pull database file from device
adb shell run-as id.stargan.intikasirfnb.dev cp /data/data/id.stargan.intikasirfnb.dev/databases/intikasir_fnb.db /sdcard/
adb pull /sdcard/intikasir_fnb.db .

# Open with sqlite3 (if installed)
sqlite3 intikasir_fnb.db
```

> Tip: Use **Database Inspector** in Android Studio for live inspection.

### Clear App Data

```bash
adb shell pm clear id.stargan.intikasirfnb.dev
```

---

## Gradle Useful Commands

```bash
# Clean build
./gradlew clean

# Clean + build
./gradlew clean assembleDevDebug

# Run unit tests
./gradlew testDevDebugUnitTest

# Run instrumented tests
./gradlew connectedDevDebugAndroidTest

# Check dependencies
./gradlew app:dependencies --configuration devDebugRuntimeClasspath

# List available tasks
./gradlew tasks

# Build with stacktrace (for debugging build errors)
./gradlew assembleDevDebug --stacktrace

# Build with info logging
./gradlew assembleDevDebug --info

# Offline mode (use cached dependencies)
./gradlew assembleDevDebug --offline
```

---

## Signing

### Debug Keystore (auto-generated)

```
~/.android/debug.keystore
Alias: androiddebugkey
Password: android
```

### Release Keystore Setup

**Step 1:** Generate keystore (one-time)

```bash
keytool -genkey -v -keystore intikasir-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias intikasir

# View keystore info
keytool -list -v -keystore intikasir-release.jks
```

**Step 2:** Create `signing.properties` at project root (gitignored)

```properties
RELEASE_STORE_FILE=../intikasir-release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=intikasir
RELEASE_KEY_PASSWORD=your_key_password
```

- `RELEASE_STORE_FILE` — path relative to `app/` directory, or absolute path
- Place the `.jks` file at project root (same level as `settings.gradle.kts`)
- Do NOT use `local.properties` — Android Studio may overwrite it

**Step 3:** Build signed release

```bash
./gradlew assembleProdRelease
# Output: app/build/outputs/apk/prod/release/app-prod-release.apk (signed)
```

> The signing config is already wired in `app/build.gradle.kts` via `signingConfigs`.
> Both `signing.properties` and `*.jks` are gitignored. **Never commit credentials to git.**

---

## Device Utilities

```bash
# Screenshot
adb shell screencap /sdcard/screenshot.png && adb pull /sdcard/screenshot.png .

# Screen record (max 3 min)
adb shell screenrecord /sdcard/recording.mp4
# Ctrl+C to stop, then:
adb pull /sdcard/recording.mp4 .

# Device info
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release

# Open URL / deeplink
adb shell am start -a android.intent.action.VIEW -d "https://example.com"

# File manager
adb shell ls /sdcard/

# Push file to device
adb push local-file.txt /sdcard/

# Pull file from device
adb pull /sdcard/remote-file.txt .

# Reboot device
adb reboot
```

---

## Git Quick Reference

```bash
# Feature branch
git checkout -b feature/nama-fitur

# Stage & commit
git add -p                     # interactive staging
git commit -m "feat: deskripsi"

# Commit message prefixes
# feat:     new feature
# fix:      bug fix
# refactor: code restructuring
# docs:     documentation
# chore:    build/config changes
# test:     adding tests

# View recent log
git log --oneline -20
```

---

## Troubleshooting


| Problem                              | Solution                                             |
| ------------------------------------ | ---------------------------------------------------- |
| `adb: device unauthorized`           | Check device for USB debugging prompt, tap **Allow** |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall existing app: `adb uninstall <package>`    |
| `Connection refused` (wireless)      | Re-enable Wireless Debugging on device, re-pair      |
| Build fails after dependency update  | `./gradlew clean` then rebuild                       |
| `Could not determine java version`   | Check `JAVA_HOME` points to JDK 17                   |
| Emulator APPREG_BASE_URL             | Uses `10.0.2.2` (host loopback from emulator)        |
| Physical device APPREG_BASE_URL      | Change to your machine's local IP or use prod flavor |



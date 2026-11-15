# AutoDim

Android app for AYN Thor that dims the handheld display while Screen Cast is connected and restores the previous brightness on disconnect. Built with Kotlin, Jetpack Compose Material 3, and Gradle Kotlin DSL.

## Build

Open this folder in Android Studio, use JDK 17, and sync Gradle. Install Android SDK Platform 35 if prompted.

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

AGP 8.10.1, Gradle 8.13, Kotlin/Compose Compiler 2.1.21, Compose BOM 2025.05.01. Minimum SDK 26; compile/target SDK 35. `local.properties` contains the local SDK path and is not committed.

## Usage

The screen contains a single AutoDim toggle.

- Turn on: grant permission to modify system settings if requested, then return to AutoDim. The app automatically continues, requests notification permission on Android 13+, and starts monitoring. Declining settings permission leaves the toggle off. Declining notification permission does not block monitoring.
- Turn off: stop the service, remove its notification, and restore the saved brightness and automatic brightness mode.

The toggle does not connect to a TV. Use Android's Screen Cast controls to connect. The notification also provides a Turn off action, which updates the toggle when the app is open.

Monitoring checks the current route once at startup and then responds to MediaRouter callbacks. There is no periodic polling, active scan, or wake lock. The service remains in the foreground to keep monitoring while the UI is closed. Battery use has not been measured on Thor.

Reopen AutoDim and turn it on after restarting the device. The app does not prevent sleep. If Android force-stops the process, brightness restoration is not guaranteed to run; reopen the app and turn the toggle off before clearing data or uninstalling.

## Structure

- `MainActivity.kt`: permission requests and the service start/stop flow.
- `AutoDimScreen.kt`: Compose screen with a single toggle, English instructions, and a preview.
- `AutoDimTileService.kt`: Quick Settings toggle synchronized with the monitoring service.
- `DimService.kt`: MediaRouter callbacks and foreground notification.
- `BrightnessController.kt`: save, dim to 1/255, and restore system brightness.

Source files are under `app/src/main/java/com/ph/screencast/autodim/`.

Namespace/application ID: `com.ph.screencast.autodim`. This is a separate app from the original `vn.autodim`; permissions and data are not migrated. Restore brightness in the old app before switching.

## Limitations and verification

The firmware must expose its video route through MediaRouter. Vendor-specific brightness control for Thor's secondary screen may not follow the system brightness setting. End-to-end casting and permission flows need verification on Thor with a TV.

Verify: toggle on with settings permission denied, grant permission and return, allow/deny notifications, connect/disconnect Screen Cast, turn off from both the app and notification, and reopen the app while monitoring. Brightness and automatic mode should return to their original values when monitoring stops.

A manifest receiver alone cannot monitor the Wi-Fi display status broadcast: Android sends it with `FLAG_RECEIVER_REGISTERED_ONLY`, requiring a dynamically registered receiver in a live process.

Android source: https://android.googlesource.com/platform/frameworks/base/+/android-13.0.0_r24/services/core/java/com/android/server/display/WifiDisplayAdapter.java

## Quick Settings tile

Swipe down twice, open Edit, and drag AutoDim into the active tiles. Tap to enable/disable monitoring. The tile reflects changes made from the app or notification. Missing settings/notification permissions open the app's existing permission flow; otherwise the tile starts monitoring directly. Turning off stops the monitoring service, removes its notification, and restores brightness. Removing the tile does not stop monitoring.

The tile service is system-bound on demand and does not poll. Test both tile states, permission requests, toggling from the app/notification, and tapping while locked on the target device.

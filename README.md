# Shizuku-Next

[中文版](./README.zh.md)

## Disclaimer

This is a modern fork and enhancement of Shizuku. If you are looking for the official upstream Shizuku developed by Rikka, please visit the [Official Repository](https://github.com/RikkaApps/Shizuku).

---

## Features and Enhancements in Shizuku-Next

### 1. Platform & Toolchain Modernization
- **Android 17 Support (Baklava / API 37)**: Built with `compileSdk = 37`, `targetSdk = 37`, Java 21, and NDK r29.
- **Latest AndroidX Ecosystem**: Upgraded to latest dependencies (`androidx.core: 1.19.0`, `androidx.annotation: 1.10.0`, `androidx.browser: 1.10.0`, `bouncycastle: 1.85`).
- **AGP 8.10.x & Java 21 Compatibility**: Bypassed AGP AAR metadata check constraints to allow modern AndroidX libraries without build incompatibilities.
- **Removed Deprecated Lifecycle Artifacts**: Cleaned up obsolete dependencies to prevent runtime lifecycle errors.

### 2. Multi-Layer Stealth Mode & Component Concealment
- **Per-App Stealth Shield**: Dedicated "Hide from Apps" management interface with real-time package search and state filters.
- **Dynamic Binder Token Guarding**: Blocked applications querying the Shizuku Binder token are rejected at the IPC boundary.
- **Live Real-Time Server Synchronization**: Server-side authorized package table dynamically updates when toggling apps without requiring daemon restarts (`ShizukuService.setHiddenPackages`).
- **PackageManager Component Redaction**: Intercepts `PackageManager` queries so protected apps cannot detect Shizuku manifest components, services, or activities.

### 3. Privileged Battery Optimization Bypass
- **Direct Shell Whitelisting**: Executes `cmd deviceidle whitelist +<pkg>`, `dumpsys deviceidle whitelist +<pkg>`, and `cmd appops set <pkg> RUN_IN_BACKGROUND allow` directly via privileged Shizuku shell or root.
- **Automatic Daemon Whitelisting**: Automatically adds Shizuku to the system battery optimization whitelist upon service connection to prevent OEM background kills.
- **One-Tap Status Tile**: Live battery optimization status card with instant bypass action in Settings.

### 4. Material 3 Jetpack Compose Dashboard & Start Methods
- **Material 3 Segmented Button Row**: Streamlined method selector (`SingleChoiceSegmentedButtonRow`) for Wireless ADB, Root, and Computer ADB.
- **Context-Aware Smart Badging**: Automatically highlights and badges `Root (Recommended)` on rooted devices and `Wireless (Recommended)` on Android 11+ unrooted devices.
- **Interactive 2-Step Wireless ADB Flow**: Numbered step-by-step workflow with live detected ADB port pill (`Port: <port>`).
- **Zero-Config Terminal Integration**: Quick action tile detecting installed terminal applications (Termux, MT Manager, TermOne, etc.) with 1-tap `rish` configuration.
- **Pure SVG Vector Outlined Icons**: Strict SVG vector compliance across the entire user interface with zero emojis.

### 5. Automation, Watchdog & Startup
- **ADB Watchdog Service**: Background monitor that actively tracks daemon health and automatically reconnects upon disconnection.
- **Start on Boot**: Supports rooted devices and Android 11+ Wireless ADB (`WRITE_SECURE_SETTINGS`) to automatically start without a PC.
- **Custom TCP/IP Port**: Configurable ADB port support for resolving port 5555 conflicts.
- **Material You & Pure Black Theme**: Dynamic wallpaper color extraction and deep AMOLED pure black dark mode.

### 6. Full Native Localization
- Comprehensive native translations for Simplified Chinese, Traditional Chinese, Russian, Japanese, Spanish, German, French, Brazilian Portuguese, Indonesian, Vietnamese, Korean, and fallback support for all standard Android locales.

---

## Usage Guide

### Start on Boot (Wireless ADB)
1. Configure Shizuku following the Wireless ADB pairing process.
2. Enable `Start on boot (Wireless ADB)` in Settings.
   - Requires `WRITE_SECURE_SETTINGS` permission.
   - Can be granted automatically when Shizuku starts, or manually via ADB:
     ```bash
     adb shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
     ```

> [!CAUTION]
> `WRITE_SECURE_SETTINGS` is a privileged permission. Use only if you understand the risks.

### Startup Support Details
- **Root Mode**: Automatically loads the service on boot on rooted devices.
- **Wireless ADB Mode**: For Android 11+. Uses `WRITE_SECURE_SETTINGS` to monitor network connectivity and restart Shizuku automatically without a computer.
- **TV Devices**: Specifically optimized for stability and remote navigation in Android TV environments.

---

## How Shizuku Works

Android uses `binder` for inter-process communication (IPC). Shizuku guides users to start a process (Shizuku server) with root or ADB privileges. When an application starts, a `binder` pointing to the Shizuku server is sent to the application.

Shizuku acts as an intermediary: it receives requests from applications, forwards them to the system server, and returns the results. This allows apps to use system APIs with higher privileges, which is almost identical to calling system APIs directly.

---

## Developer Guide

Refer to the official API repository: [RikkaApps/Shizuku-API](https://github.com/RikkaApps/Shizuku-API)

---

## Building Shizuku-Next

### Prerequisites
- JDK 21
- Android SDK (API 37 / Android 17)
- Android NDK (r29 or newer)
- CMake 3.22.1+

### Build Commands
```bash
# Clone repository with submodules
git clone --recurse-submodules https://github.com/Towartz/Shizuku-Next.git

# Build Debug APK
./gradlew :manager:assembleDebug

# Build Release APK
./gradlew :manager:assembleRelease
```

---

## License

Licensed under the Apache 2.0 License.

Under Apache 2.0 section 6:
- FORBIDDEN to use `ic_launcher` images unless for Shizuku itself.
- FORBIDDEN to use `Shizuku` as app name or `moe.shizuku.privileged.api` as ID in third-party distributions.

---

## Credits

- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) (Original upstream)
- [yangFenTuoZi/Shizuku](https://github.com/yangFenTuoZi/Shizuku)
- [pixincreate/Shizuku](https://github.com/pixincreate/Shizuku)
- [thedjchi/Shizuku](https://github.com/thedjchi/Shizuku)
- [HSSkyBoy/Shizuku](https://github.com/HSSkyBoy/Shizuku)

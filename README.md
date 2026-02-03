# YTMusic Pro v2.0.0 🚀

**YTMusic Pro** is a premium, high-performance Android wrapper for YouTube Music, designed to provide a seamless listening experience with advanced features typically reserved for premium subscribers. 

Built with stability, privacy, and user experience in mind, this version (2.0.0) represents a complete overhaul with production-grade fixes and optimizations.

---

## ✨ Core Features

### 🎧 Premium Experience
- **Ad Blocking**: Multi-layer ad blocking (Network level + DOM-based) to remove interrupted listening and visual clutter.
- **Background Playback**: Continue listening even when the app is minimized or the screen is locked, thanks to custom WebView visibility management.
- **Rich Media Notifications**: Fully interactive notification bar with track info, album art, and precise playback controls.
- **Lock Screen & Bluetooth integration**: Full support for system-wide media controls, including smartwatches and Bluetooth headsets.

### 🛠️ Technical Excellence
- **Smart Idle Timer**: Automatically stops the background service after 5 minutes of inactivity to save battery.
- **Auto-Reconnection**: Intelligent network monitoring that automatically reloads the player when your connection is restored.
- **Graceful Error Handling**: Custom designer error page for offline scenarios with a responsive retry system.
- **Optimized Performance**: Pre-configured with ProGuard/R8 for minimal APK size and maximum code security.

---

## 🏗️ Architecture & How It Works

### 1. The Native Layer (Java/Android)
- **`MainActivity`**: Manages the life cycle, runtime permissions (Android 13+ support), and the sophisticated WebView system.
- **`ForegroundService`**: A dedicated background service that maintains the `MediaSessionCompat` to communicate with the Android OS.
- **`YTMusicWebview`**: A specialized WebView component that overrides window visibility rules to prevent the player from pausing when the app is in the background.

### 2. The Injection Layer (JS/CSS)
- **`inject.js`**: Strategically injected into the YouTube Music DOM to:
  - Extract real-time metadata (Artist, Title, Duration).
  - Remove "Promoted" elements and YouTube "Upsell" dialogs.
  - Apply custom CSS for a modern "Glassmorphism" UI on the bottom player bar.

---

## 📦 What's New in v2.0.0 (The Stability Update)

- ✅ **Fixed Critical Bug**: Resolved a major issue where all notification buttons performed the same action due to `PendingIntent` request code collisions.
- ✅ **Android 14 Support**: Added `FOREGROUND_SERVICE_MEDIA_PLAYBACK` support and handled `POST_NOTIFICATIONS` runtime permissions.
- ✅ **Adaptive Icons**: Replaced broken legacy icons with modern vector-based adaptive icons.
- ✅ **Production Signed**: Fully configured signing pipeline for official APK releases.
- ✅ **Memory Optimized**: Rigorous cleanup of BroadcastReceivers and handlers to prevent memory leaks.

---

## 🚀 Getting Started

### Prerequisites
- Android 7.0 (API Level 24) or higher.
- Recommended: Android 13+ for the best notification experience.

### Building from Source
1. Clone the repository.
2. Open with Android Studio.
3. Build the project:
   ```bash
   ./gradlew assembleRelease
   ```
4. Find your signed APK at: `app/build/outputs/apk/release/app-release.apk`

### Installation
1. Enable "Install from Unknown Sources" in your Android settings.
2. Install `app-release.apk`.
3. Grant "Notification Permission" when prompted (for media controls).

---

## 🔒 Security & Privacy
- **Hardened Code**: ProGuard enabled to obfuscate the Java source and protected JavaScript interfaces.
- **No Tracking**: No third-party analytics or invasive tracking.
- **SSL Whitelisting**: Connections are restricted to trusted YouTube and Google domains for security.

---

## 🎨 UI Customization
The app applies a custom dark-theme enhancement with a blurred player bar, creating a modern and premium feel that integrates perfectly with the latest Android design languages.

---

## 📄 License & Disclaimer
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### Disclaimer
This project is for educational purposes only. YouTube Music is a trademark of Google LLC. This app is not affiliated with or endorsed by Google.

---

**Developed with ❤️ for Music Lovers.**  
**Status**: 🟢 Production Ready | **Version**: 2.0.0

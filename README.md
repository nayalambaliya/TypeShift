<div align="center">

# TypeShift

**AI-powered text assistant that works in every app across Android, iOS, and macOS**

Type a trigger like `?fix`, `?improve`, or `?formal` anywhere — TypeShift rewrites your text in-place using Groq's blazing-fast LLaMA 3.3 70B model.

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](android/)
[![iOS](https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white)](ios/)
[![macOS](https://img.shields.io/badge/macOS-000000?style=for-the-badge&logo=apple&logoColor=white)](macos/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](android/)
[![Swift](https://img.shields.io/badge/Swift-FA7343?style=for-the-badge&logo=swift&logoColor=white)](macos/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

</div>

---

## What is TypeShift?

TypeShift is a cross-platform AI writing assistant that hooks directly into the operating system — no copy-paste, no switching apps. It uses the platform's native accessibility API to read and rewrite text in any app you're using.

- **Android** — Accessibility Service runs in the background, detects triggers as you type
- **iOS** — Custom Keyboard Extension, works inside every app that accepts text input
- **macOS** — Menu bar app using the macOS Accessibility API (AXUIElement), works in Notes, Mail, VS Code, browsers, and more

All three platforms use the [Groq API](https://console.groq.com) (free tier) with `llama-3.3-70b-versatile` for near-instant responses.

---

## Platform Support

| Platform | Mechanism | Distribution | Status |
|----------|-----------|--------------|--------|
| Android 8+ | `AccessibilityService` | APK (build from source) | ✅ Live |
| iOS 17+ | `UIInputViewController` (Keyboard Extension) | Build with Xcode | ✅ Live |
| macOS 13+ | `AXUIElement` + `NSMenuBarExtra` | [Download ZIP](#download) | ✅ Live |

---

## Commands

Type any command at the end of your text, then press **Space** (Android/iOS) or **⌃⇧Space** (macOS). Or use the macOS menu bar to apply a command without typing.

| Command | What it does |
|---------|-------------|
| `?fix` | Fix grammar & spelling |
| `?improve` | Improve clarity and flow |
| `?formal` | Rewrite in professional tone |
| `?casual` | Rewrite in friendly tone |
| `?shorter` | Make it concise |
| `?longer` | Expand with more detail |
| `?reply` | Generate a natural reply |
| `?emoji` | Add relevant emojis |
| `?tldr` | One-sentence summary |
| `?bullet` | Convert to bullet points |
| `?subject` | Generate email subject line |
| `?headline` | Write a catchy headline |
| `?tweet` | Shrink to ≤280 characters |
| `?eli5` | Explain like I'm 5 |
| `?human` | Sound more natural |
| `?hinglish` | Rewrite in Hindi + English mix |
| `?roast` | Funny roast of the text |
| `?translate:XX` | Translate to any language (e.g. `?translate:fr`) |
| `?undo` | Restore the original text |

---

## Tech Stack

### Android
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| System Hook | `AccessibilityService` + `AccessibilityNodeInfo` |
| Text Replacement | `performAction(ACTION_SET_TEXT)` with clipboard fallback |
| AI | Groq REST API (`OkHttp`) |
| Design | OxygenOS-inspired, AMOLED dark, `#7B61FF` accent |

### iOS
| Layer | Technology |
|-------|-----------|
| Language | Swift |
| UI | SwiftUI + UIKit |
| System Hook | `UIInputViewController` (Keyboard Extension) |
| Text Access | `UITextDocumentProxy` |
| Data Sharing | App Groups (`UserDefaults` shared suite) |
| AI | Groq REST API (`URLSession`) |

### macOS
| Layer | Technology |
|-------|-----------|
| Language | Swift |
| UI | SwiftUI + AppKit |
| System Hook | `AXUIElementCreateSystemWide()` + `NSEvent.addGlobalMonitorForEvents` |
| Text Reading | `kAXValueAttribute`, `kAXStringForRangeParameterizedAttribute` |
| Text Writing | `AXUIElementSetAttributeValue`, `CGEvent` (Cmd+A / Cmd+V) |
| Entry Point | `NSMenuBarExtra` (menu bar app, no dock icon) |
| AI | Groq REST API (`URLSession`) |

---

## How It Works

```
User types "Hello world?fix " in any app
         │
         ▼
 ┌──────────────────┐
 │  OS Hook Layer   │  AccessibilityService / UITextDocumentProxy / AXUIElement
 │  detects trigger │
 └────────┬─────────┘
          │
          ▼
 ┌──────────────────┐
 │  Text Extracted  │  "Hello world"  (trigger stripped)
 └────────┬─────────┘
          │
          ▼
 ┌──────────────────┐
 │  Groq API Call   │  llama-3.3-70b-versatile  (~300ms)
 └────────┬─────────┘
          │
          ▼
 ┌──────────────────┐
 │  In-Place Replace│  "Hello, world!"  written back to the same field
 └──────────────────┘
```

---

## Download

### macOS
1. Go to [**Releases**](https://github.com/nayalambaliya/TypeShift/releases/latest)
2. Download `TypeShift-macOS.zip` and unzip
3. Move `TypeShift.app` to `/Applications`
4. **First launch:** Right-click → Open (bypasses Gatekeeper for unsigned app)
5. Open TypeShift from the menu bar (`T›`) → Settings → enter your free [Groq API key](https://console.groq.com)
6. Go to **System Settings → Privacy & Security → Accessibility** → enable TypeShift

### Android
Build from source (see below) — APK available in [Releases](https://github.com/nayalambaliya/TypeShift/releases/latest) when published.

### iOS
Build from source with Xcode (see below) — no App Store distribution without a paid Apple Developer account.

---

## Building from Source

### Prerequisites
- [Groq API key](https://console.groq.com) — free, no credit card required

### Android
```bash
# Clone the repo
git clone https://github.com/nayalambaliya/TypeShift.git
cd TypeShift/android

# Open in Android Studio, or build from CLI:
./gradlew assembleDebug

# Install on connected device:
./gradlew installDebug
```
After install: open TypeShift → enter API key → enable Accessibility Service → enable keyboard in Settings.

### iOS
```bash
# Requires Xcode 15+ and a physical iPhone (iOS 17+)
git clone https://github.com/nayalambaliya/TypeShift.git
cd TypeShift/ios

# Install xcodegen if needed:
brew install xcodegen

# Generate Xcode project:
xcodegen generate

# Open in Xcode:
open TypeShift.xcodeproj
```
1. Set your development team in Xcode (free Apple ID works for personal device)
2. Build & run on your iPhone
3. Go to **Settings → General → Keyboard → Keyboards → Add New Keyboard → TypeShift**
4. Enable Full Access → enter your Groq API key in the TypeShift app

### macOS
```bash
git clone https://github.com/nayalambaliya/TypeShift.git
cd TypeShift/macos

# Install xcodegen if needed:
brew install xcodegen

# Generate Xcode project:
xcodegen generate

# Build:
xcodebuild -scheme TypeShiftMac -configuration Release -derivedDataPath build

# Copy to Applications:
cp -R build/Build/Products/Release/TypeShift.app /Applications/
open /Applications/TypeShift.app
```

---

## License

MIT — see [LICENSE](LICENSE)

---

<div align="center">
  <sub>Built with Swift · Kotlin · Groq API · llama-3.3-70b-versatile</sub>
</div>

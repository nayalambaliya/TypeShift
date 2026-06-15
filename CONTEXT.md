# TypeShift — Project Context

**What it is:** A cross-platform AI writing assistant that rewrites text in-place inside any app using trigger commands (e.g. typing `?fix` then Space) — no copy/paste, no switching apps.

---

## Platform Matrix

| Platform | Status | Entry Point | Trigger Mechanism |
|---|---|---|---|
| Android | ✅ Shipped (v1.3) | AccessibilityService | `TYPE_VIEW_TEXT_CHANGED` event |
| macOS | ✅ Shipped | NSMenuBarExtra menu bar app | Global NSEvent key monitor + AX reading |
| Windows | ⚠️ Built, untested | System tray (NotifyIcon) | `SetWindowsHookEx` WH_KEYBOARD_LL |
| iOS | ❌ Removed | — | — |

---

## Repo Layout

```
TypeShift/
├── android/                    Kotlin + Jetpack Compose
│   └── app/src/main/java/com/nayal/aikeyboard/
│       ├── MainActivity.kt         4-tab UI (Aurora design)
│       ├── AiAccessibilityService.kt  Core trigger detection + Groq calls
│       ├── AiKeyboardService.kt    Legacy IME stub (unused, kept for manifest)
│       └── ui/                     Shared UI components
├── macos/
│   ├── project.yml                 XcodeGen spec (ALWAYS run xcodegen before building)
│   ├── TypeShiftMac.xcodeproj/     Generated — never edit directly
│   └── TypeShiftMac/
│       ├── TypeShiftMacApp.swift   App entry + NSMenuBarExtra
│       ├── AppDelegate.swift       App delegate
│       ├── TextMonitor.swift       Core trigger detection + AX reading + Groq calls
│       ├── GroqAPI.swift           callGroq() async function
│       ├── SettingsView.swift      4-tab settings window (TabView)
│       ├── Info.plist              Bundle metadata
│       └── TypeShiftMac.entitlements  app-sandbox: false
├── windows/
│   ├── TypeShift.csproj            .NET 8, UseWPF + UseWindowsForms
│   ├── App.xaml / App.xaml.cs      Dark theme resources + tray icon
│   ├── MainWindow.xaml / .cs       4-tab WPF window
│   ├── CommandDialog.xaml / .cs    Add/edit custom command dialog
│   ├── Core/
│   │   ├── KeyboardHook.cs         SetWindowsHookEx WH_KEYBOARD_LL
│   │   ├── TextProcessor.cs        Trigger detection + clipboard replacement
│   │   ├── GroqApi.cs              HttpClient Groq calls
│   │   └── Settings.cs             JSON settings in %APPDATA%\TypeShift\
│   └── Models/
│       └── CustomCommand.cs        Model + JSON store in %APPDATA%\TypeShift\
├── assets/                         Brand assets (logo, etc.)
├── .github/workflows/
│   ├── release-android.yml         Builds debug APK + uploads to GitHub Release
│   └── release-windows.yml         Builds self-contained EXE + uploads to GitHub Release
├── CONTEXT.md                      ← this file
├── AGENTS.md                       AI agent behavioral contract
└── tasks/
    ├── DEVLOG.md                   Session log (agents write, newest first)
    ├── todo.md                     Phased task list
    └── lessons.md                  Hard-won lessons
```

---

## Tech Stack

| | Android | macOS | Windows |
|---|---|---|---|
| Language | Kotlin | Swift 5.9 | C# (.NET 8) |
| UI | Jetpack Compose + Material 3 | SwiftUI + AppKit | WPF |
| Min OS | Android 7.0 (API 24) | macOS 13.0 Ventura | Windows 10 |
| Build tool | Gradle (gradlew) | XcodeGen + xcodebuild | dotnet |
| Package | `com.nayal.aikeyboard` | `com.nayal.typeshift.mac` | `TypeShift` namespace |

---

## Groq API Integration

- **Endpoint:** `https://api.groq.com/openai/v1/chat/completions`
- **Model:** `llama-3.3-70b-versatile`
- **Timeout:** Android 30s read / macOS 20s / Windows 30s
- **Streaming:** No — all platforms use blocking/async single response
- **Error handling:** On failure, restores original text + shows toast/status message

**Prompt structure:**

| Platform | System message | User message |
|---|---|---|
| Android | None — instruction folded into user message | `"[instruction]\n\nText:\n[text]"` |
| macOS | `instruction` as `role: system` | `text` as `role: user` |
| Windows | `instruction` as `role: system` | `text` as `role: user` |

**API key storage:**

| Platform | Storage | Key name | ⚠️ Gotcha |
|---|---|---|---|
| Android | `SharedPreferences("ai_keyboard_prefs")` | `"gemini_api_key"` | Legacy name — DO NOT rename or users lose their key |
| macOS | `UserDefaults.standard` | `"groq_api_key"` | — |
| Windows | `%APPDATA%\TypeShift\settings.json` | `ApiKey` field | — |

**Temperature storage:**

| Platform | Key | Default |
|---|---|---|
| Android | SharedPreferences `"ai_temperature"` (Float) | 0.7f |
| macOS | UserDefaults `"ai_temperature"` (Double) | 0.7 |
| Windows | `%APPDATA%\TypeShift\settings.json` `Temperature` field | 0.7 |

---

## Command Registry

### Built-in triggers

| Trigger | Instruction summary | Android | macOS | Windows |
|---|---|---|---|---|
| `?fix` | Fix grammar & spelling | ✅ | ✅ | ✅ |
| `?improve` | Improve clarity | ✅ | ✅ | ✅ |
| `?formal` | Professional tone | ✅ | ✅ | ✅ |
| `?casual` | Friendly tone | ✅ | ✅ | ✅ |
| `?shorter` | Make concise | ✅ | ✅ | ✅ |
| `?longer` | Expand | ✅ | ✅ | ✅ |
| `?emoji` | Add emojis | ✅ | ✅ | ✅ |
| `?reply` | Write a reply | ✅ | ✅ | ✅ |
| `?human` | Sound more human | ✅ | ✅ | ✅ |
| `?hinglish` | Hindi+English mix | ✅ | ✅ | ✅ |
| `?roast` | Funny roast | ✅ | ✅ | ✅ |
| `?tweet` | Shrink to tweet | ✅ | ✅ | ✅ |
| `?bullet` | Bullet points | ✅ | ✅ | ✅ |
| `?subject` | Email subject line | ✅ | ✅ | ✅ |
| `?eli5` | Explain like I'm 5 | ✅ | ✅ | ✅ |
| `?tldr` | One-line summary | ✅ | ✅ | ✅ |
| `?headline` | Catchy headline | ✅ | ✅ | ✅ |
| `?simplify` | Simplify text | ✅ | ❌ | ❌ |
| `?joke` | Tell a joke | ✅ | ❌ | ❌ |
| `?undo` | Restore original | ✅ | ✅ | ✅ |
| `?translate:XX` | Translate to language | ✅ | ✅ | ✅ |

**⚠️ Platform divergence:** `?simplify` and `?joke` exist only on Android (added in v1.3 Aurora redesign). Not yet ported to macOS or Windows.

### Custom commands storage format

| Platform | Storage | Format |
|---|---|---|
| Android | SharedPreferences `"custom_commands"` | JSON array: `[{"id":"…","trigger":"?foo","name":"Foo","prompt":"…"}]` |
| macOS | UserDefaults key (JSON Data) | Same JSON schema |
| Windows | `%APPDATA%\TypeShift\custom_commands.json` | Same JSON schema |

---

## Design Systems

### Android — Aurora Palette (v1.3)
```
Bg        = #080812   (near-black, deep space)
Surface1  = #FFFFFF0A (4% white glass)
Surface2  = #FFFFFF14 (8% white glass)
Surface3  = #FFFFFF22 (13% white glass)
Accent    = #8B5CF6   (violet purple)
AccentAlt = #A78BFA   (lighter violet)
TextPrim  = #F1F5F9
TextSec   = #94A3B8
Success   = #34D399
Danger    = #F87171
Warning   = #FBBF24

Category accent colors:
CatEdit     = #60A5FA  (blue)
CatCreative = #FB923C  (orange)
CatUtil     = #34D399  (green)
CatFun      = #F472B6  (pink)
CatLang     = #A78BFA  (lavender)
CatMeta     = #94A3B8  (slate)
```
Font: Outfit (Google Fonts)

### macOS / Windows — Dark Purple
```
Bg      = #000000
Surf1   = #0D0D0D
Surf2   = #1A1A1A
Surf3   = #242424
Accent  = #7B61FF
AccentLt= #9E8FFF
```

---

## Build Instructions

### Android
```bash
cd android
./gradlew assembleDebug          # debug APK (no keystore needed)
./gradlew assembleRelease        # release APK (needs keystore env vars)
./gradlew installDebug           # install directly to connected device
```
Requires: JDK 21, Android SDK

### macOS
```bash
cd macos
xcodegen generate                # REQUIRED if project.yml changed
xcodebuild -project TypeShiftMac.xcodeproj \
           -scheme TypeShiftMac  \
           -configuration Release \
           build
# Or just open in Xcode after xcodegen generate
```
Requires: Xcode 16, XcodeGen (`brew install xcodegen`)

**Distribution:** App is unsigned (CODE_SIGN_IDENTITY: "-"). New machines need `xattr -cr TypeShift.app` or Right-click → Open to bypass Gatekeeper.

### Windows
```bash
cd windows
dotnet build -c Release                     # validate
dotnet publish TypeShift.csproj \
  -c Release -r win-x64 \
  --self-contained true \
  -p:PublishSingleFile=true \
  -o publish/                               # single EXE
```
Requires: .NET 8 SDK. **Must run on Windows** — WPF cannot build on macOS/Linux.

---

## CI/CD

### Workflows (both trigger on `v*` tags + `workflow_dispatch`)

| Workflow | Runner | Output | Signed? |
|---|---|---|---|
| `release-android.yml` | ubuntu-latest | `TypeShift-android.apk` | ❌ Debug build |
| `release-windows.yml` | windows-latest | `TypeShift-windows.exe` | N/A (self-contained) |
| macOS | ❌ No workflow | — | — |

**Android signing situation:** The original release-signing workflow (with keystore secrets) was replaced by a debug build (`assembleDebug`) after CI failures. Play Protect may flag the debug APK on some devices.

**GitHub Secrets configured:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — set but currently unused since CI builds debug.

### Releasing
```bash
git tag v1.x.x
git push origin v1.x.x
# → triggers both workflows → creates GitHub Release with APK + EXE attached
```

---

## Known Bugs / Open Items

1. **`?simplify` and `?joke` missing on macOS + Windows** — Android-only divergence
2. **Android API key SharedPreferences name is `"gemini_api_key"`** — legacy name from before Groq migration; changing it breaks existing users
3. **Android CI builds debug APK** — unsigned, may trigger Play Protect on some devices
4. **No macOS CI workflow** — macOS zip is created manually
5. **Windows untested end-to-end** — built in this session, never run on actual Windows
6. **macOS `?undo` not in standard trigger list** — handled in `detectTrigger()` separately, works correctly
7. **Windows `?joke`/`?simplify` not in command list** — `MainWindow.xaml.cs` BuiltinCommands array missing these two

---

## Hard Constraints

- **Never rename Android SharedPreferences key `"gemini_api_key"`** — breaks existing user data
- **Never commit keystore files or `.env` files**
- **`windows/` cannot be built on macOS** — WPF is Windows-only
- **`macos/TypeShiftMac.xcodeproj/` is committed** — do not gitignore it (generated by XcodeGen but needed for CI)
- **Never change trigger command strings** — they are user-facing identifiers
- **Changing the Groq model requires a DEVLOG note** — performance-critical dependency

---

## Current Focus

Windows platform needs end-to-end testing. macOS and Android are shipping. CI needs a signed Android release build restored.
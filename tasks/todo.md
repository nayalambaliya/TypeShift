# TypeShift — TODO

---

## Phase 0 — DONE ✅

- [x] Initial Android app with AccessibilityService trigger detection
- [x] Groq API integration (`llama-3.3-70b-versatile`)
- [x] 17 built-in trigger commands
- [x] Android Instagram-style 4-tab UI (Home, Commands, Explore, Settings)
- [x] Custom user-defined commands (Android)
- [x] AI temperature slider (all platforms)
- [x] Aurora glassmorphism UI redesign — Android v1.3
- [x] `?joke` and `?simplify` commands (Android)
- [x] macOS menu bar app with NSMenuBarExtra
- [x] macOS 4-tab settings window (TabView)
- [x] macOS custom commands + temperature
- [x] macOS ⌃⇧Space hotkey
- [x] macOS three-strategy AX text reading (native → parameterized range → clipboard)
- [x] Windows WPF app built (untested)
- [x] GitHub Actions: Android APK workflow
- [x] GitHub Actions: Windows EXE workflow (self-contained, no .NET required)
- [x] Repo cleanup (removed legacy icons, added Gradle artifacts to .gitignore)
- [x] CONTEXT.md, AGENTS.md, tasks/ handover files

---

## Phase 1 — Current 🔨

- [ ] **Test Windows end-to-end** — install on actual Windows 10/11 machine, verify trigger → Groq → replacement works
- [ ] **Port `?simplify` to macOS** — add to `TextMonitor.triggers` array + `SettingsView.swift` built-in list
- [ ] **Port `?joke` to macOS** — same as above
- [ ] **Port `?simplify` and `?joke` to Windows** — add to `MainWindow.xaml.cs` `BuiltinCommands` array + `TextProcessor.cs` triggers dict
- [ ] **Restore signed Android release APK** — reactivate `assembleRelease` in `release-android.yml`, use `KEYSTORE_BASE64` secret
- [ ] **macOS CI workflow** — add `.github/workflows/release-macos.yml`: `xcodegen generate` + `xcodebuild archive` + zip + upload to release

---

## Phase 2 — Next 📋

- [ ] **`?undo` on Windows** — verify TextProcessor.cs correctly stores and restores original text
- [ ] **Windows tray: show processing indicator** — currently no visual feedback while Groq processes
- [ ] **Android Play Protect submission** — submit to Google's accessibility app whitelist to avoid "harmful app" warning on debug builds
- [ ] **Unify macOS prompt structure with Android** — macOS uses `role: system` for instruction; Android folds it into `role: user`. Pick one and align.
- [ ] **`?translate:XX` test on all platforms** — verify the regex-based detection works correctly on macOS and Windows
- [ ] **Error handling on Windows** — TextProcessor.cs has no UI feedback when Groq fails; add tray notification or balloon tip
- [ ] **macOS: `?undo` store per-field** — currently `lastOriginal` is a single string; concurrent edits across fields could corrupt it

---

## Phase 3 — Future 💡

- [ ] **Web browser extension** — Chrome/Firefox extension to inject trigger detection without needing OS-level accessibility
- [ ] **iOS version** — was removed; could be revisited as a keyboard extension
- [ ] **Streaming responses** — show Groq response token-by-token as it arrives (would need rethinking the in-place replacement model)
- [ ] **Usage stats** — track how often each command is used per platform
- [ ] **Command marketplace** — share custom commands between users
- [ ] **Offline fallback** — local model (Ollama) when internet is unavailable
- [ ] **Monetization** — Pro tier with more commands or a managed Groq key
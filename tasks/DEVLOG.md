# TypeShift — Dev Log
> Newest entry at top. Agents: append entries, never edit existing ones.

---

## 2026-06-13 · Claude Sonnet 4.6 · Handover — full project onboarding + repo cleanup

**Mode:** Researcher + Builder

**Did:**
- Audited full codebase across all three platforms
- Created `CONTEXT.md`, `AGENTS.md`, `tasks/DEVLOG.md`, `tasks/todo.md`, `tasks/lessons.md`
- Cleaned repo: removed 14 legacy `.webp` launcher icons, 4 legacy adaptive icon XMLs, added Gradle build artifacts (`IDLE`, `Task`, `:app:installDebug`) to `.gitignore`
- Confirmed Android CI switched to debug build (bypassing keystore)
- Identified `?simplify` and `?joke` as Android-only (not on macOS/Windows)
- Confirmed macOS uses different prompt structure (system message) vs Android (user message only)
- Confirmed Windows built but never run end-to-end on real Windows

**State:**
- Android: ✅ Shipping v1.3 (Aurora glassmorphism UI, 19 triggers, custom commands, temperature)
- macOS: ✅ Shipping (17 triggers, custom commands, temperature, menu bar)
- Windows: ⚠️ Code complete, untested on real Windows machine
- CI: Android builds debug APK; Windows builds self-contained EXE; no macOS CI

**Decided:**
- Documented `"gemini_api_key"` as a hard constraint — cannot rename without breaking user data
- Windows platform classified as "built, untested" not "shipped"

**Next:**
- Test Windows end-to-end on a real Windows machine
- Port `?simplify` and `?joke` to macOS and Windows
- Restore signed release APK in Android CI (reactivate `assembleRelease` + keystore secrets)
- Add macOS CI workflow (xcodegen + xcodebuild + zip artifact)

**Modified:**
- `CONTEXT.md` (new)
- `AGENTS.md` (new)
- `tasks/DEVLOG.md` (new)
- `tasks/todo.md` (new)
- `tasks/lessons.md` (new)
- `.gitignore` (added Gradle artifacts)
- `android/app/src/main/res/` (removed legacy icon files)

---

## 2026-06-13 · Claude Sonnet 4.6 · Windows WPF app — complete build

**Mode:** Builder

**Did:**
- Built entire Windows platform from scratch: `TypeShift.csproj`, `App.xaml/cs`, `MainWindow.xaml/cs`, `CommandDialog.xaml/cs`, `Core/KeyboardHook.cs`, `Core/TextProcessor.cs`, `Core/GroqApi.cs`, `Core/Settings.cs`, `Models/CustomCommand.cs`
- Added `release-windows.yml` GitHub Actions workflow with `workflow_dispatch` support and `permissions: contents: write`
- Committed and pushed all Windows files to `origin/main`

**State:**
- Windows: Code complete. Never compiled or run on actual Windows.

**Next:** Test on real Windows machine.

**Modified:** All files in `windows/`, `.github/workflows/release-windows.yml`

---

## 2026-06-13 · Claude Sonnet 4.6 · macOS parity — custom commands + temperature + TabView fix

**Mode:** Builder

**Did:**
- Rewrote `SettingsView.swift` as 4-tab TabView (Home, Commands, Explore, Settings)
- Added custom commands CRUD to macOS (UserDefaults JSON storage)
- Added temperature slider to macOS settings
- Fixed `NavigationSplitView` not rendering sidebar in `Settings` scene — switched to `TabView`
- Updated menu bar dropdown to show custom commands under "MY COMMANDS" section
- Added `processCustomCommand()` to `TextMonitor.swift` for menu bar clicks

**State:**
- macOS: ✅ Parity with Android features. Deployed to `/Applications/TypeShift.app`.

**Modified:** `macos/TypeShiftMac/SettingsView.swift`, `macos/TypeShiftMac/TextMonitor.swift`, `macos/TypeShiftMac/TypeShiftMacApp.swift`, `macos/TypeShiftMac/GroqAPI.swift`

---

## 2026-06-XX · Claude Sonnet 4.6 · Android Aurora redesign v1.3

**Mode:** Builder

**Did:**
- Full Aurora glassmorphism UI redesign (4-tab bottom nav, Aurora palette)
- Added `?joke` and `?simplify` commands (Android only)
- Custom commands with add/edit/delete
- Temperature slider (0.0–1.5)
- Signed release APK workflow (later switched to debug due to CI issues)

**State:**
- Android: ✅ Shipping v1.3

**Modified:** `android/app/src/main/java/com/nayal/aikeyboard/MainActivity.kt`, `android/app/src/main/java/com/nayal/aikeyboard/AiAccessibilityService.kt`
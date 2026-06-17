# TypeShift — Dev Log
> Newest entry at top. Agents: append entries, never edit existing ones.

---

## 2026-06-17 · Claude Opus 4.8 · AI prompt fix + multi-provider support (Android)

**Mode:** Builder

**Did:**
- **Fixed the "Improve returns unrelated text" bug** in `AiAccessibilityService.callAI`: instruction now sent as a `system` message (was folded into the user turn), and the request body is built with `JSONObject`/`JSONArray` (was hand-built string with manual escaping that corrupted on quotes/tabs/newlines/unicode). Improves every command, not just `?improve`.
- **Added multi-provider support** (the CV's "configurable AI provider support with custom API keys and local models" claim, now real):
  - New `Providers.kt`: `AiProvider` model + presets (Groq, OpenAI, OpenRouter, Together, DeepSeek, Mistral, Local Ollama/LM Studio, Custom) + SharedPreferences helpers. Key insight — all are OpenAI-compatible `/chat/completions`, so one network path serves all.
  - `callAI` now resolves endpoint/model/key from the selected provider; Authorization header only sent when a key exists (local needs none).
  - Settings UI: replaced `ApiKeyCard` with `ProviderCard` (provider chips + per-provider key/model, plus Server URL for local/custom). `ModelInfoCard` + Home `StatsRow` reflect the selected provider.
  - Groq key keeps legacy pref name `gemini_api_key` (L-001) so existing users keep their key.
  - Manifest: `usesCleartextTraffic="true"` so local HTTP LLM servers on LAN work.
- Verified with `./gradlew assembleDebug` → BUILD SUCCESSFUL.

**State:**
- Android: ✅ Builds. AI quality fix + full multi-provider live in code (not yet released/tagged).
- macOS/Windows: still Groq-only — multi-provider not yet ported.

**Decided:**
- OpenAI-compatible-only provider set for v1 (covers Groq/OpenAI/OpenRouter/Together/DeepSeek/Mistral/local). Anthropic + Gemini use different schemas — deferred (would need per-provider adapters).
- Model field editable for every provider, not just custom.

**Next:**
- Port multi-provider to macOS + Windows for parity
- Optional: tag a release to ship the signed APK with these changes
- Still open from audit: R8/minification, EncryptedSharedPreferences

**Modified:**
- `android/app/src/main/java/com/nayal/aikeyboard/Providers.kt` (new)
- `android/app/src/main/java/com/nayal/aikeyboard/AiAccessibilityService.kt`
- `android/app/src/main/java/com/nayal/aikeyboard/MainActivity.kt`
- `android/app/src/main/AndroidManifest.xml`

---

## 2026-06-13 · Claude Opus 4.8 · Critical fixes — icons, signed release, versioning

**Mode:** Builder

**Did:**
- **Root-caused the missing launcher icons:** manifest referenced `@mipmap/ic_launcher` but no mipmap resources existed (cleanup commit `c3bbcd7` removed `.webp` + adaptive XMLs; `.png` were already gone). Build was broken.
- Created adaptive icon set (minSdk 26 → vector-only, no PNG buckets):
  - `drawable/ic_launcher_background.xml` — #3D2BFF→#7B61FF diagonal gradient
  - `drawable/ic_launcher_foreground.xml` — white "T›" stroke wordmark, inside safe zone
  - `drawable/ic_launcher_monochrome.xml` — Android 13 themed-icon layer
  - `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`
- Added splash: `core-splashscreen:1.0.1`, `Theme.AIKeyboard.Starting`, `installSplashScreen()` in MainActivity, manifest activity theme switched to `.Starting`
- **Fixed Play Protect root cause:** `release-android.yml` now decodes `KEYSTORE_BASE64` and runs `assembleRelease` (was debug-signed)
- **Fixed versioning:** `build.gradle.kts` reads `VERSION_NAME` (from tag, `v` stripped) + `VERSION_CODE` (from `github.run_number`); CI injects both
- Added `*.jks` / `*.keystore` to `.gitignore`
- Verified with `./gradlew assembleDebug` → BUILD SUCCESSFUL

**State:**
- Android: ✅ Builds clean. Icons render. Next signed release will be a proper release-signed APK with correct version.

**Decided:**
- Vector-only adaptive icons (no density PNGs) — justified by minSdk 26
- Version driven by CI, not hardcoded — prevents the "every release is v1.0" bug recurring

**Next (from the audit, not yet done):**
- HIGH: fix AI prompt structure (system prompt + JSONObject body) — the "Improve returns unrelated text" bug
- HIGH: enable R8 + proguard rules
- HIGH: EncryptedSharedPreferences for API key
- Note: template audit Parts 2 (camera/mic/location perms), Scoped Storage, FileProvider, Room/SQL are N/A — app only declares INTERNET

**Modified:**
- `android/app/src/main/res/drawable/ic_launcher_background.xml` (new)
- `android/app/src/main/res/drawable/ic_launcher_foreground.xml` (new)
- `android/app/src/main/res/drawable/ic_launcher_monochrome.xml` (new)
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (new)
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` (new)
- `android/app/src/main/res/values/themes.xml`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/nayal/aikeyboard/MainActivity.kt`
- `android/app/build.gradle.kts`
- `.github/workflows/release-android.yml`
- `.gitignore`

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
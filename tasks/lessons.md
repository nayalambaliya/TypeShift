# TypeShift — Lessons Learned

---

## L-001 · Android SharedPreferences key is "gemini_api_key" not "groq_api_key"

**Problem:** The Android app stores the Groq API key under `"gemini_api_key"` in SharedPreferences.
**Root cause:** Originally built for Gemini; switched to Groq but the key name was never updated.
**Prevention rule:** NEVER rename this key. Any rename silently wipes the API key for all existing users. The macOS and Windows platforms correctly use `"groq_api_key"` — this divergence is intentional and permanent.

---

## L-002 · NavigationSplitView silently fails inside a macOS Settings scene

**Problem:** Used `NavigationSplitView` in the macOS `Settings {}` scene expecting a sidebar + detail layout. App showed only the detail view — no sidebar rendered.
**Root cause:** SwiftUI `Settings` scene does not support `NavigationSplitView`'s sidebar column in macOS 13/14 — it silently falls back to detail-only.
**Fix:** Replaced with `TabView` inside the Settings scene.
**Prevention rule:** In macOS `Settings {}` scenes, always use `TabView` for multi-section layout. Never use `NavigationSplitView`.

---

## L-003 · macOS app update requires killing the old process and clearing quarantine

**Problem:** After replacing `/Applications/TypeShift.app` with a new build, the old version kept running.
**Root cause:** macOS caches running processes; `cp -R` merges rather than replaces app bundles.
**Fix:** `pkill TypeShift && rm -rf /Applications/TypeShift.app && cp -R <new build> /Applications/ && xattr -cr /Applications/TypeShift.app`
**Prevention rule:** Always `rm -rf` the old app before copying. Always `xattr -cr` after copying. Always `pkill` first.

---

## L-004 · Android CI release signing replaced by debug build

**Problem:** `assembleRelease` failed in CI because the keystore env var decoding was flaky, and the release workflow had no `permissions: contents: write`.
**Root cause:** Two issues: (1) keystore base64 decode path issue on ubuntu runner; (2) GitHub Actions default token lacks `contents: write` for creating releases.
**Fix:** Temporarily switched to `assembleDebug` to unblock releases. Added `permissions: contents: write` to both workflow files.
**Prevention rule:** Every workflow that uploads to GitHub Releases needs `permissions: contents: write` at the job or workflow level. When restoring release signing, test the keystore decode step in isolation first.

---

## L-005 · WPF cannot be compiled on macOS

**Problem:** Tried to build the Windows app from a Mac.
**Root cause:** `<UseWPF>true</UseWPF>` in the `.csproj` requires the Windows SDK. `dotnet build` on macOS fails with "WPF is not supported on this platform."
**Prevention rule:** Windows builds must happen on Windows or via GitHub Actions `windows-latest` runner. Never attempt `dotnet publish` for the Windows target on macOS.

---

## L-006 · GitHub Actions "harmful app" / Play Protect on debug APK

**Problem:** Users downloading the debug APK from GitHub Releases get a Play Protect "harmful app" warning because it accesses AccessibilityService.
**Root cause:** AccessibilityService apps are flagged by Play Protect unless the app is on Google's whitelist or is a signed production release distributed via Play Store.
**Prevention rule:** The long-term fix is submitting to Google's accessibility app whitelist. Short-term: instruct users to dismiss the warning in Play Protect settings. When restoring signed release builds, a properly signed APK reduces (but does not eliminate) the warning.

---

## L-007 · macOS AX reading needs three fallback strategies

**Problem:** A single AX reading strategy doesn't work across all apps — native apps expose `kAXValueAttribute`, but VS Code and browsers don't.
**Root cause:** Each app framework exposes different AX attributes. VS Code/Electron only exposes `kAXStringForRangeParameterizedAttribute`. Some apps expose neither.
**Fix:** TextMonitor uses three strategies in order: (1) direct `kAXValueAttribute`, (2) parameterized range read (`kAXStringForRangeParameterizedAttribute`), (3) `kAXSelectedTextAttribute` fallback.
**Prevention rule:** Never remove or reorder these three strategies. If adding a new reading method, add it as a fourth fallback, not a replacement.

---

## L-008 · Browser clipboard update is slower than native apps

**Problem:** On macOS, the clipboard wasn't populated after `Cmd+C` in Chrome/Brave before the code tried to read it.
**Root cause:** Chromium browsers update the clipboard asynchronously with a longer delay than native AppKit apps.
**Fix:** TextMonitor waits 350ms after `Cmd+C` before reading clipboard (vs ~50ms for native apps). Also clears clipboard before `Cmd+C` so stale content is detectable.
**Prevention rule:** Never reduce the 350ms clipboard read delay. If adding a new browser workaround, document the delay value and the browser it was tested on.
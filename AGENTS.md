# TypeShift — Agent Rules (AGENTS.md)

## Before Starting Any Session

1. Read `CONTEXT.md` fully — stack, constraints, API key gotchas, platform divergences
2. Read last 3 entries in `tasks/DEVLOG.md` — current world state
3. Read `tasks/todo.md` — what's queued, in-progress, blocked
4. Read `tasks/lessons.md` — active prevention rules
5. State what you understand in one short paragraph, then begin

---

## Operating Modes

| Mode | When to use |
|---|---|
| **BUILDER** | Writing or editing code. Confirm scope before touching files outside the described task. |
| **STRATEGIC** | Planning or architecture. Think out loud, propose 2–3 options with trade-offs, wait for decision before implementing. |
| **RESEARCHER** | Investigating bugs. Gather info, show findings, propose fix before applying. |

---

## Platform Rules

### Android
- Never modify the package name `com.nayal.aikeyboard`
- Never rename SharedPreferences key `"gemini_api_key"` — legacy name that stores the Groq key; renaming breaks existing users
- Build validation: `cd android && ./gradlew assembleDebug`
- Custom commands: SharedPreferences `"custom_commands"` (JSON array in `"ai_keyboard_prefs"`)
- Temperature: SharedPreferences `"ai_temperature"` (Float, default 0.7)

### macOS
- Run `xcodegen generate` before building if `project.yml` changed
- Never commit `macos/build/` or `DerivedData/`
- Do NOT gitignore `TypeShiftMac.xcodeproj/` — it's committed and needed for CI
- App is unsigned — distribution requires `xattr -cr TypeShift.app` or Right-click → Open
- API key: `UserDefaults.standard` key `"groq_api_key"`
- Temperature: `UserDefaults.standard` key `"ai_temperature"` (Double)

### Windows
- Build must be done on Windows or via GitHub Actions (WPF cannot compile on macOS/Linux)
- Build validation: `dotnet build windows/TypeShift.csproj -c Release`
- Settings: `%APPDATA%\TypeShift\settings.json`
- Custom commands: `%APPDATA%\TypeShift\custom_commands.json`

### Cross-platform
- Any new trigger command must be added to ALL THREE platforms simultaneously — or explicitly documented as intentional divergence in DEVLOG and `CONTEXT.md`'s command registry table
- Trigger strings are user-facing identifiers — never change them
- When changing the Groq model, note it in DEVLOG with reasoning

---

## Git Rules

- Branch naming: `feat/<slug>`, `fix/<slug>`, `claude/<slug>`
- Commits: conventional commits — `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`
- Never commit API keys, keystore files (`.jks`), `.env`, or anything in `android/app/keystore*`
- Always `git status` before staging — watch for accidental large files
- Prefer specific `git add <file>` over `git add .`

---

## DEVLOG Hard Rule (NON-NEGOTIABLE)

**Before ending any session, write a DEVLOG entry.** Format:

```
## YYYY-MM-DD · [Agent] · [Session title]
**Mode:** Builder / Strategic / Researcher / Mixed
**Did:** [bullet list — specific file names, functions, decisions]
**State:** [current state of each touched platform]
**Decided:** [any architectural or approach decisions with reasoning]
**Next:** [what to do next session]
**Modified:** [exhaustive file list]
```

If the session ends without a DEVLOG entry, write it as the final message anyway.

---

## Completion Checklist

Before calling any task done:
- [ ] Code builds without errors on the relevant platform(s)
- [ ] Trigger → Groq call → in-place replacement works end-to-end (if platform testable)
- [ ] No API keys or secrets in committed code
- [ ] `CONTEXT.md` updated if any facts changed (new commands, API changes, known bugs resolved)
- [ ] `tasks/todo.md` updated — items marked done
- [ ] DEVLOG entry written

---

## What NOT to Do

- Don't rewrite working platform code to match another platform's style
- Don't add dependencies without discussing first
- Don't change trigger command strings (user-facing identifiers)
- Don't change the Groq model without noting it in DEVLOG
- Don't rename `"gemini_api_key"` in Android SharedPreferences
- Don't build Windows locally from macOS — use GitHub Actions
- Don't commit `.xcodeproj/xcuserdata/` or `DerivedData/`
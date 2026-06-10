import Cocoa
import ApplicationServices

@MainActor
final class TextMonitor: ObservableObject {
    static let shared = TextMonitor()

    @Published var isProcessing        = false
    @Published var statusMessage       = ""
    @Published var accessibilityGranted = false

    private var keyMonitor:   Any?
    private var permTimer:    Timer?
    private var lastCheck:    Date   = .distantPast
    private var lastOriginal: String?
    private var pendingIndicatorLen = 0
    private var lastActiveApp: NSRunningApplication?   // tracks app before menu opens

    private let debounce: TimeInterval = 0.08

    // ── Triggers ──────────────────────────────────────────────
    static let triggers: [(String, String)] = [
        ("?fix",      "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else."),
        ("?improve",  "Improve the writing quality and clarity. Return only the improved text, nothing else."),
        ("?formal",   "Rewrite in a formal, professional tone. Return only the rewritten text, nothing else."),
        ("?casual",   "Rewrite in a casual, friendly tone. Return only the rewritten text, nothing else."),
        ("?shorter",  "Make this text more concise. Return only the shortened text, nothing else."),
        ("?longer",   "Expand this text with more detail. Return only the expanded text, nothing else."),
        ("?emoji",    "Add relevant emojis to this text. Return only the text with emojis, nothing else."),
        ("?reply",    "Write a natural reply to this message. Return only the reply, nothing else."),
        ("?human",    "Make this text sound more natural and human. Return only the rewritten text, nothing else."),
        ("?hinglish", "Rewrite in Hinglish (Hindi + English mix). Return only the Hinglish text, nothing else."),
        ("?roast",    "Write a funny roast of this text. Return only the roast, nothing else."),
        ("?tweet",    "Rewrite as a tweet under 280 characters. Return only the tweet, nothing else."),
        ("?bullet",   "Convert this text into bullet points. Return only the bullet points, nothing else."),
        ("?subject",  "Generate an email subject line. Return only the subject line, nothing else."),
        ("?eli5",     "Explain this like I'm 5 years old. Return only the explanation, nothing else."),
        ("?tldr",     "Summarize this in one sentence. Return only the summary, nothing else."),
        ("?headline", "Write a catchy headline for this text. Return only the headline, nothing else."),
    ]

    // ── Setup ────────────────────────────────────────────────
    func setup() {
        accessibilityGranted = AXIsProcessTrusted()

        NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.didActivateApplicationNotification,
            object: nil, queue: .main
        ) { [weak self] note in
            if let app = note.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication,
               app.bundleIdentifier != Bundle.main.bundleIdentifier {
                self?.lastActiveApp = app
            }
        }

        if accessibilityGranted {
            startMonitor()
        } else {
            permTimer = Timer.scheduledTimer(withTimeInterval: 2, repeats: true) { [weak self] t in
                Task { @MainActor in
                    if AXIsProcessTrusted() {
                        self?.accessibilityGranted = true
                        self?.startMonitor()
                        t.invalidate()
                    }
                }
            }
        }

        // Continuously recheck permission — re-grants after binary replacement are detected here
        Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                let granted = AXIsProcessTrusted()
                self.accessibilityGranted = granted
                if granted && self.keyMonitor == nil {
                    self.startMonitor()
                }
            }
        }
    }

    func requestAccessibility() {
        let opts = [kAXTrustedCheckOptionPrompt.takeRetainedValue() as String: true] as CFDictionary
        AXIsProcessTrustedWithOptions(opts)
    }

    private func startMonitor() {
        guard keyMonitor == nil else { return }
        keyMonitor = NSEvent.addGlobalMonitorForEvents(matching: .keyDown) { [weak self] event in
            Task { @MainActor in
                // ⌃⇧Space — universal hotkey that works in every app including browsers
                let mods = event.modifierFlags.intersection([.command, .option, .shift, .control])
                if mods == [.control, .shift], event.keyCode == 0x31 {
                    self?.hotkeyTriggered()
                    return
                }
                self?.onKey()
            }
        }
    }

    func stopMonitor() {
        if let m = keyMonitor { NSEvent.removeMonitor(m); keyMonitor = nil }
    }

    // ── Menu bar command — re-focuses previous app, selects all, processes ──
    func processSelectedText(_ trigger: String) {
        guard !isProcessing else { return }
        guard let instr = Self.triggers.first(where: { $0.0 == trigger })?.1 else { return }

        let target = lastActiveApp
        flash("⟳ Focusing…")
        target?.activate(options: .activateIgnoringOtherApps)

        // 500 ms — browser windows need more time to restore keyboard focus to the text field
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.clipboardProcess(instruction: instr, target: target)
        }
    }

    // ── ⌃⇧Space hotkey ────────────────────────────────────────
    func hotkeyTriggered() {
        guard !isProcessing else { return }
        clipboardProcess(instruction: nil, target: nil)
    }

    // ── Core clipboard-based processor (used by both hotkey and menu) ──
    private func clipboardProcess(instruction explicitInstruction: String?, target: NSRunningApplication?) {
        let pb    = NSPasteboard.general
        let saved = pb.string(forType: .string)

        // Clear clipboard BEFORE Cmd+C so we can detect whether the copy actually succeeded.
        // Without this, stale clipboard content makes it look like the copy worked even when focus was wrong.
        pb.clearContents()

        flash("⟳ Reading…")
        postKey(0x00, flags: .maskCommand)   // Cmd+A
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            guard let self else { return }
            self.postKey(0x08, flags: .maskCommand)  // Cmd+C
            // 350 ms — browsers (Brave, Chrome) update the clipboard slower than native apps
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                guard let text = pb.string(forType: .string), !text.isEmpty else {
                    self.flash("⚠ Click in text field first, then try again")
                    if let s = saved { pb.clearContents(); pb.setString(s, forType: .string) }
                    return
                }

                let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)

                // Determine instruction: explicit (from menu) or detect from trigger suffix
                var instruction = explicitInstruction ?? Self.triggers.first!.1
                var cleanText   = trimmed

                if explicitInstruction == nil {
                    for (trig, instr) in Self.triggers {
                        if trimmed.lowercased().hasSuffix(trig) {
                            instruction = instr
                            cleanText   = String(trimmed.dropLast(trig.count)).trimmingCharacters(in: .whitespaces)
                            break
                        }
                    }
                    if let trigRange = trimmed.range(of: #"\?translate:[a-zA-Z]{2,5}$"#, options: .regularExpression),
                       let codeRange = trimmed.range(of: #"(?<=\?translate:)[a-zA-Z]+$"#, options: .regularExpression) {
                        instruction = "Translate the following text to \(String(trimmed[codeRange])). Return only the translated text, nothing else."
                        cleanText   = String(trimmed[..<trigRange.lowerBound]).trimmingCharacters(in: .whitespaces)
                    }
                }

                guard !cleanText.isEmpty else {
                    self.flash("⚠ No text to process")
                    pb.clearContents(); if let s = saved { pb.setString(s, forType: .string) }
                    return
                }

                let key = UserDefaults.standard.string(forKey: "groq_api_key") ?? ""
                guard !key.isEmpty else {
                    self.flash("⚠ Set API key in Settings")
                    pb.clearContents(); if let s = saved { pb.setString(s, forType: .string) }
                    return
                }

                self.isProcessing  = true
                self.statusMessage = "Thinking…"
                self.lastOriginal  = cleanText

                pb.clearContents()
                pb.setString("⟳ Thinking…", forType: .string)
                self.postKey(0x00, flags: .maskCommand)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    self.postKey(0x09, flags: .maskCommand)  // Cmd+V
                }

                Task {
                    let reactivate = {
                        target?.activate(options: .activateIgnoringOtherApps)
                    }
                    do {
                        let result = try await callGroq(text: cleanText, instruction: instruction, apiKey: key)
                        reactivate()
                        // 400 ms — browser needs time to restore text-field focus after re-activation
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            pb.clearContents()
                            pb.setString(result, forType: .string)
                            self.postKey(0x00, flags: .maskCommand)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                self.postKey(0x09, flags: .maskCommand)
                                self.flash("Done ✓")
                                self.isProcessing = false
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                                    pb.clearContents(); if let s = saved { pb.setString(s, forType: .string) }
                                }
                            }
                        }
                    } catch {
                        reactivate()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            pb.clearContents()
                            pb.setString(cleanText, forType: .string)
                            self.postKey(0x00, flags: .maskCommand)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                self.postKey(0x09, flags: .maskCommand)
                                self.flash("⚠ Error — try again")
                                self.isProcessing = false
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                                    pb.clearContents(); if let s = saved { pb.setString(s, forType: .string) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Key event → debounced check ───────────────────────────
    private func onKey() {
        let now = Date()
        guard now.timeIntervalSince(lastCheck) >= debounce else { return }
        lastCheck = now
        // Small delay so the target app can update its text field
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.06) { [weak self] in
            self?.checkFocused()
        }
    }

    // ── Read text + detect trigger ────────────────────────────
    private func checkFocused() {
        guard !isProcessing else { return }
        guard let element = focusedElement() else { return }

        // Method A: direct value (Notes, Mail, native NSTextView, etc.)
        if let text = directValue(element), !text.isEmpty {
            detectTrigger(text, in: element)
            return
        }

        // Method B: parameterized range read (VS Code, Electron, Chrome, Brave)
        if let text = textBeforeCursor(element), !text.isEmpty {
            detectTrigger(text, in: element)
            return
        }

        // Method C: selected-text attribute fallback — some editors populate this even when
        // the full value is unavailable. Not ideal but catches remaining cases.
        if let text = selectedTextAttr(element), !text.isEmpty {
            detectTrigger(text, in: element)
        }
    }

    private func selectedTextAttr(_ el: AXUIElement) -> String? {
        var ref: CFTypeRef?
        guard AXUIElementCopyAttributeValue(el, kAXSelectedTextAttribute as CFString, &ref) == .success else { return nil }
        return ref as? String
    }

    // MARK: – AX reading helpers

    private func focusedElement() -> AXUIElement? {
        let sys = AXUIElementCreateSystemWide()
        var ref: CFTypeRef?
        guard AXUIElementCopyAttributeValue(sys, kAXFocusedUIElementAttribute as CFString, &ref) == .success,
              let r = ref else { return nil }
        return (r as! AXUIElement)
    }

    private func directValue(_ el: AXUIElement) -> String? {
        var ref: CFTypeRef?
        guard AXUIElementCopyAttributeValue(el, kAXValueAttribute as CFString, &ref) == .success else { return nil }
        return ref as? String
    }

    /// Reads up to 600 chars immediately before the cursor using the
    /// parameterized kAXStringForRangeParameterizedAttribute — this is
    /// what works in VS Code, Chrome, and other Electron/WebKit apps.
    private func textBeforeCursor(_ el: AXUIElement) -> String? {
        var selRef: CFTypeRef?
        guard AXUIElementCopyAttributeValue(el, kAXSelectedTextRangeAttribute as CFString, &selRef) == .success,
              let selVal = selRef else { return nil }

        var cursorRange = CFRange(location: 0, length: 0)
        AXValueGetValue(selVal as! AXValue, AXValueType.cfRange, &cursorRange)
        guard cursorRange.location > 0 else { return nil }

        let from = max(0, cursorRange.location - 600)
        let len  = cursorRange.location - from
        var readRange = CFRange(location: from, length: len)
        guard let rangeVal = AXValueCreate(AXValueType.cfRange, &readRange) else { return nil }

        var textRef: CFTypeRef?
        guard AXUIElementCopyParameterizedAttributeValue(
            el,
            kAXStringForRangeParameterizedAttribute as CFString,
            rangeVal, &textRef
        ) == .success else { return nil }

        return textRef as? String
    }

    // MARK: – Trigger detection

    private func detectTrigger(_ text: String, in el: AXUIElement) {
        // Trim trailing whitespace/newlines for suffix matching,
        // but keep the original `text` for knowing how many chars to replace.
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let lower   = trimmed.lowercased()

        // ?undo
        if lower.hasSuffix("?undo") {
            guard let original = lastOriginal else { flash("Nothing to undo"); return }
            replaceLastChars(el, count: text.count, with: original)
            lastOriginal = nil
            return
        }

        // ?translate:XX
        if let trigRange = trimmed.range(of: #"\?translate:[a-zA-Z]{2,5}$"#, options: .regularExpression),
           let codeRange = trimmed.range(of: #"(?<=\?translate:)[a-zA-Z]+$"#,  options: .regularExpression) {
            let lang  = String(trimmed[codeRange])
            let clean = String(trimmed[..<trigRange.lowerBound]).trimmingCharacters(in: .whitespaces)
            guard !clean.isEmpty else { return }
            let instr = "Translate the following text to \(lang). Return only the translated text, nothing else."
            process(fullText: text, clean: clean, instruction: instr, in: el)
            return
        }

        // Standard triggers
        for (trig, instr) in Self.triggers {
            if lower.hasSuffix(trig) {
                let clean = String(trimmed.dropLast(trig.count)).trimmingCharacters(in: .whitespaces)
                guard !clean.isEmpty else { return }
                // Pass original `text` so replaceLastChars removes trailing spaces too
                process(fullText: text, clean: clean, instruction: instr, in: el)
                return
            }
        }
    }

    // MARK: – Process

    private func process(fullText: String, clean: String, instruction: String, in el: AXUIElement) {
        guard !isProcessing else { return }
        let key = UserDefaults.standard.string(forKey: "groq_api_key") ?? ""
        guard !key.isEmpty else { flash("⚠ Set API key in TypeShift Settings"); return }

        isProcessing = true
        statusMessage = "Thinking…"
        lastOriginal  = clean

        let indicator = "⟳ Thinking…"
        replaceLastChars(el, count: fullText.count, with: indicator)
        pendingIndicatorLen = indicator.count

        Task {
            do {
                let result = try await callGroq(text: clean, instruction: instruction, apiKey: key)
                if let current = focusedElement() {
                    replaceLastChars(current, count: pendingIndicatorLen, with: result)
                }
                flash("Done ✓")
                isProcessing = false
            } catch {
                if let current = focusedElement() {
                    replaceLastChars(current, count: pendingIndicatorLen, with: clean)
                }
                flash("Error — try again")
                isProcessing = false
                lastOriginal = nil
            }
        }
    }

    // MARK: – Text replacement

    /// Replaces the last `count` characters before the cursor with `newText`.
    /// Tries three strategies in order: direct value set → AX range selection + paste → clipboard fallback.
    private func replaceLastChars(_ el: AXUIElement, count: Int, with newText: String) {

        // Strategy 1 — direct AX value (native apps: Notes, Mail, TextEdit, etc.)
        if let current = directValue(el) {
            let drop = min(count, current.count)
            let prefix = String(current.dropLast(drop))
            let full   = prefix + newText
            if AXUIElementSetAttributeValue(el, kAXValueAttribute as CFString, full as CFString) == .success {
                moveCursorToEnd(el, length: full.utf16.count)
                return
            }
        }

        // Strategy 2 — select range + paste (VS Code, Chrome, Electron, Safari)
        var selRef: CFTypeRef?
        if AXUIElementCopyAttributeValue(el, kAXSelectedTextRangeAttribute as CFString, &selRef) == .success,
           let sv = selRef {
            var cur = CFRange(location: 0, length: 0)
            AXValueGetValue(sv as! AXValue, .cfRange, &cur)

            var sel = CFRange(location: max(0, cur.location - count),
                              length: min(count, cur.location))
            if let selVal = AXValueCreate(.cfRange, &sel),
               AXUIElementSetAttributeValue(el, kAXSelectedTextRangeAttribute as CFString, selVal) == .success {
                paste(newText)
                return
            }
        }

        // Strategy 3 — Cmd+A + paste (last resort, replaces entire field)
        selectAllAndPaste(newText)
    }

    private func moveCursorToEnd(_ el: AXUIElement, length: Int) {
        var r = CFRange(location: length, length: 0)
        if let v = AXValueCreate(.cfRange, &r) {
            AXUIElementSetAttributeValue(el, kAXSelectedTextRangeAttribute as CFString, v)
        }
    }

    private func paste(_ text: String) {
        let pb  = NSPasteboard.general
        let old = pb.string(forType: .string)
        pb.clearContents()
        pb.setString(text, forType: .string)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.02) {
            self.postKey(0x09, flags: .maskCommand)   // Cmd+V
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                pb.clearContents()
                if let old { pb.setString(old, forType: .string) }
            }
        }
    }

    private func selectAllAndPaste(_ text: String) {
        let pb  = NSPasteboard.general
        let old = pb.string(forType: .string)
        pb.clearContents()
        pb.setString(text, forType: .string)
        postKey(0x00, flags: .maskCommand)   // Cmd+A
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
            self.postKey(0x09, flags: .maskCommand)   // Cmd+V
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                pb.clearContents()
                if let old { pb.setString(old, forType: .string) }
            }
        }
    }

    private func postKey(_ code: CGKeyCode, flags: CGEventFlags = []) {
        let src = CGEventSource(stateID: .hidSystemState)
        let dn  = CGEvent(keyboardEventSource: src, virtualKey: code, keyDown: true)
        dn?.flags = flags
        dn?.post(tap: .cgAnnotatedSessionEventTap)
        let up  = CGEvent(keyboardEventSource: src, virtualKey: code, keyDown: false)
        up?.post(tap: .cgAnnotatedSessionEventTap)
    }

    private func flash(_ msg: String) {
        statusMessage = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
            if self?.statusMessage == msg { self?.statusMessage = "" }
        }
    }
}

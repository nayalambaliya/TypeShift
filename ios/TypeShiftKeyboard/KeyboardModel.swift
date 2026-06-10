import UIKit
import Combine

@MainActor
final class KeyboardModel: ObservableObject {
    @Published var isShifted    = false
    @Published var showNumbers  = false
    @Published var isProcessing = false
    @Published var status       = ""

    private let proxyGetter:   () -> UITextDocumentProxy?
    private let advanceAction: () -> Void
    private let switchCheck:   () -> Bool
    private var lastOriginal:  String?

    var proxy:         UITextDocumentProxy? { proxyGetter() }
    var showSwitchKey: Bool                 { switchCheck() }

    static let triggers: [(trigger: String, instruction: String)] = [
        ("?fix",      "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else."),
        ("?improve",  "Improve the writing quality and clarity. Return only the improved text, nothing else."),
        ("?formal",   "Rewrite in a formal, professional tone. Return only the rewritten text, nothing else."),
        ("?casual",   "Rewrite in a casual, friendly tone. Return only the rewritten text, nothing else."),
        ("?shorter",  "Make this text more concise while keeping the main point. Return only the shortened text, nothing else."),
        ("?longer",   "Expand this text with more detail. Return only the expanded text, nothing else."),
        ("?emoji",    "Add relevant emojis throughout this text. Return only the text with emojis, nothing else."),
        ("?reply",    "Write a natural and appropriate reply to this message. Return only the reply, nothing else."),
        ("?human",    "Make this text sound more natural and human. Return only the rewritten text, nothing else."),
        ("?hinglish", "Rewrite this in Hinglish (Hindi + English mix). Return only the Hinglish text, nothing else."),
        ("?roast",    "Write a funny roast of this text. Return only the roast, nothing else."),
        ("?tweet",    "Rewrite as a tweet under 280 characters. Return only the tweet, nothing else."),
        ("?bullet",   "Convert this text into bullet points. Return only the bullet points, nothing else."),
        ("?subject",  "Generate an email subject line for this text. Return only the subject line, nothing else."),
        ("?eli5",     "Explain this like I'm 5 years old. Return only the explanation, nothing else."),
        ("?tldr",     "Summarize this in one sentence. Return only the summary, nothing else."),
        ("?headline", "Write a catchy headline for this text. Return only the headline, nothing else."),
    ]

    init(proxy: @escaping () -> UITextDocumentProxy?,
         advanceAction: @escaping () -> Void,
         switchKeyCheck: @escaping () -> Bool) {
        self.proxyGetter  = proxy
        self.advanceAction = advanceAction
        self.switchCheck  = switchKeyCheck
    }

    // MARK: – Key actions

    func tap(_ char: String) {
        let c = isShifted ? char.uppercased() : char
        proxy?.insertText(c)
        if isShifted { isShifted = false }
    }

    func backspace()   { proxy?.deleteBackward() }
    func space()       { proxy?.insertText(" ") }
    func newline()     { proxy?.insertText("\n") }
    func switchMode()  { advanceAction() }
    func toggleShift() { isShifted.toggle() }
    func toggleNumbers() { showNumbers.toggle() }

    // MARK: – Trigger detection

    func onTextChanged() {
        guard !isProcessing else { return }
        guard let raw = proxy?.documentContextBeforeInput, !raw.isEmpty else { return }
        let text = raw  // keep trailing whitespace stripped only for comparison

        // ?undo
        if text.hasSuffix("?undo") || text.hasSuffix("?UNDO") {
            handleUndo(fullText: text)
            return
        }

        // ?translate:XX
        let translatePattern = #"\?translate:([a-zA-Z]{2,5})$"#
        if let range = text.range(of: translatePattern, options: .regularExpression),
           let codeRange = text.range(of: #"(?<=\?translate:)[a-zA-Z]+$"#, options: .regularExpression) {
            let langCode = String(text[codeRange])
            let cleanText = String(text[..<range.lowerBound]).trimmingCharacters(in: .whitespaces)
            guard !cleanText.isEmpty else { return }
            let instruction = "Translate the following text to \(langCode). Return only the translated text, nothing else."
            processText(fullText: text, cleanText: cleanText, instruction: instruction)
            return
        }

        // Standard triggers
        for t in Self.triggers {
            if text.lowercased().hasSuffix(t.trigger) {
                let clean = String(text.dropLast(t.trigger.count)).trimmingCharacters(in: .whitespaces)
                guard !clean.isEmpty else { return }
                processText(fullText: text, cleanText: clean, instruction: t.instruction)
                return
            }
        }
    }

    private func handleUndo(fullText: String) {
        guard let original = lastOriginal else {
            flash("Nothing to undo")
            return
        }
        deleteChars(fullText.count)
        proxy?.insertText(original)
        lastOriginal = nil
    }

    private func processText(fullText: String, cleanText: String, instruction: String) {
        let key = sharedDefaults.groqAPIKey
        guard !key.isEmpty else {
            flash("⚠ Set API key in TypeShift app")
            return
        }

        isProcessing = true
        status = "Thinking..."
        lastOriginal = cleanText

        deleteChars(fullText.count)
        proxy?.insertText("⟳")

        Task {
            do {
                let result = try await callGroq(text: cleanText, instruction: instruction, apiKey: key)
                proxy?.deleteBackward()           // remove spinner
                proxy?.insertText(result)
                status = "Done ✓"
                isProcessing = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 2) { self.status = "" }
            } catch {
                proxy?.deleteBackward()
                proxy?.insertText(cleanText)
                flash("Error — try again")
                isProcessing = false
                lastOriginal = nil
            }
        }
    }

    private func deleteChars(_ n: Int) {
        for _ in 0..<n { proxy?.deleteBackward() }
    }

    private func flash(_ msg: String) {
        status = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { self.status = "" }
    }
}

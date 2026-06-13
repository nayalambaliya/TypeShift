import SwiftUI

// ── Colors ────────────────────────────────────────────────────
private let cBg      = Color(hex: "#000000")
private let cSurf1   = Color(hex: "#141414")
private let cSurf2   = Color(hex: "#1E1E1E")
private let cSurf3   = Color(hex: "#282828")
private let cAccent  = Color(hex: "#7B61FF")
private let cAccLt   = Color(hex: "#9E8FFF")
private let cPrim    = Color.white
private let cSec     = Color(hex: "#8A8A8A")
private let cTert    = Color(hex: "#555555")
private let cGreen   = Color(hex: "#34C759")

extension Color {
    init(hex: String) {
        let h = hex.trimmingCharacters(in: .init(charactersIn: "#"))
        var rgb: UInt64 = 0
        Scanner(string: h).scanHexInt64(&rgb)
        self.init(
            red:   Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8)  & 0xFF) / 255,
            blue:  Double( rgb        & 0xFF) / 255
        )
    }
}

// ── Custom command model ───────────────────────────────────────
struct CustomCommand: Codable, Identifiable {
    var id:      String = UUID().uuidString
    var trigger: String
    var name:    String
    var prompt:  String
}

func loadCustomCommands() -> [CustomCommand] {
    guard let data = UserDefaults.standard.data(forKey: "custom_commands"),
          let cmds = try? JSONDecoder().decode([CustomCommand].self, from: data)
    else { return [] }
    return cmds
}

func saveCustomCommands(_ commands: [CustomCommand]) {
    guard let data = try? JSONEncoder().encode(commands) else { return }
    UserDefaults.standard.set(data, forKey: "custom_commands")
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

// ── Settings root ─────────────────────────────────────────────
struct SettingsView: View {
    @ObservedObject private var monitor = TextMonitor.shared
    @State private var apiKey   = UserDefaults.standard.string(forKey: "groq_api_key") ?? ""
    @State private var saved    = false
    @State private var showKey  = false
    @State private var expanded = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 40)

                Text("TypeShift")
                    .font(.system(size: 36, weight: .heavy, design: .rounded))
                    .foregroundStyle(cPrim)
                    .tracking(-0.5)
                Text("AI text assistant — works in every app on your Mac")
                    .font(.system(size: 14))
                    .foregroundStyle(cSec)
                    .padding(.top, 2)

                Spacer().frame(height: 28)

                StatusCard(monitor: monitor)
                Spacer().frame(height: 14)
                ApiCard(apiKey: $apiKey, saved: $saved, showKey: $showKey)
                Spacer().frame(height: 14)
                TemperatureCard()
                Spacer().frame(height: 14)
                CommandsCard(expanded: $expanded)
                Spacer().frame(height: 14)
                CustomCommandsCard()

                Spacer().frame(height: 40)
            }
            .padding(.horizontal, 28)
        }
        .background(cBg)
        .frame(minWidth: 500, maxWidth: 500)
    }
}

// ── Status card ───────────────────────────────────────────────
struct StatusCard: View {
    @ObservedObject var monitor: TextMonitor

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Circle()
                    .fill(monitor.accessibilityGranted ? cGreen : Color.orange)
                    .frame(width: 9, height: 9)
                Text(monitor.accessibilityGranted ? "Accessibility access granted" : "Accessibility access needed")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(cPrim)
            }

            Text(monitor.accessibilityGranted
                ? "TypeShift is active.\n\n**Native apps** (Notes, Mail, Messages): type ?fix at the end, press Space.\n\n**Browsers & VS Code**: type ?fix at the end, press **⌃⇧Space** (Ctrl+Shift+Space)."
                : "TypeShift needs Accessibility access to read and modify text in other apps. This is equivalent to the Android accessibility service permission.")
                .font(.system(size: 13))
                .foregroundStyle(cSec)
                .lineSpacing(4)

            if !monitor.accessibilityGranted {
                Button {
                    monitor.requestAccessibility()
                } label: {
                    Text("Grant Accessibility Access")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(cPrim)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(cAccent)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(22)
        .background(LinearGradient(colors: [cSurf1, cSurf2], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// ── API key card ──────────────────────────────────────────────
struct ApiCard: View {
    @Binding var apiKey:  String
    @Binding var saved:   Bool
    @Binding var showKey: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("API Key")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(cPrim)

            Text("Powered by Groq — free, no credit card required")
                .font(.system(size: 13))
                .foregroundStyle(cSec)

            HStack {
                Group {
                    if showKey {
                        TextField("gsk_...", text: $apiKey)
                    } else {
                        SecureField("gsk_...", text: $apiKey)
                    }
                }
                .font(.system(size: 13, design: .monospaced))
                .foregroundStyle(cPrim)
                .textFieldStyle(.plain)
                .tint(cAccent)
                .onChange(of: apiKey) { _ in saved = false }

                Button(showKey ? "Hide" : "Show") { showKey.toggle() }
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(cAccent)
                    .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(cSurf2)
            .clipShape(RoundedRectangle(cornerRadius: 14))

            HStack(spacing: 12) {
                Button {
                    UserDefaults.standard.set(apiKey.trimmingCharacters(in: .whitespaces), forKey: "groq_api_key")
                    saved = true
                } label: {
                    Text("Save")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(cPrim)
                        .padding(.horizontal, 24)
                        .frame(height: 40)
                        .background(cAccent)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                if saved {
                    Text("✓  Saved")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(cGreen)
                }
            }

            Text("Get your free key at console.groq.com")
                .font(.system(size: 12))
                .foregroundStyle(cTert)
        }
        .padding(22)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// ── Commands card ─────────────────────────────────────────────
struct CommandsCard: View {
    @Binding var expanded: Bool

    let commands: [(String, String)] = [
        ("?fix",          "Fix grammar & spelling"),
        ("?improve",      "Improve clarity"),
        ("?formal",       "Professional tone"),
        ("?casual",       "Friendly tone"),
        ("?shorter",      "Make it concise"),
        ("?longer",       "Expand with detail"),
        ("?emoji",        "Add relevant emojis"),
        ("?reply",        "Generate a reply"),
        ("?human",        "Sound more human"),
        ("?hinglish",     "Convert to Hinglish"),
        ("?roast",        "Funny roast"),
        ("?tweet",        "Shrink to a tweet"),
        ("?bullet",       "Convert to bullet points"),
        ("?subject",      "Email subject line"),
        ("?eli5",         "Explain like I'm 5"),
        ("?tldr",         "One sentence summary"),
        ("?headline",     "Catchy headline"),
        ("?undo",         "Restore original text"),
        ("?translate:XX", "Translate any language"),
    ]

    var visible: [(String, String)] { expanded ? commands : Array(commands.prefix(7)) }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Commands")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(cPrim)
                Spacer()
                Button(expanded ? "Show less" : "See all \(commands.count)") {
                    expanded.toggle()
                }
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(cAccent)
                .buttonStyle(.plain)
            }

            Text("In any app: type command at end of text + Space\nIn browsers: type command at end + ⌃⇧Space")
                .font(.system(size: 13))
                .foregroundStyle(cSec)
                .padding(.bottom, 8)

            ForEach(visible, id: \.0) { cmd, desc in
                VStack(spacing: 0) {
                    HStack(spacing: 12) {
                        Text(cmd)
                            .font(.system(size: 12, weight: .semibold, design: .monospaced))
                            .foregroundStyle(cAccLt)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(cAccent.opacity(0.15))
                            .clipShape(Capsule())

                        Text(desc)
                            .font(.system(size: 13))
                            .foregroundStyle(cSec)
                        Spacer()
                    }
                    .padding(.vertical, 9)

                    if cmd != visible.last?.0 {
                        Divider().background(cSurf3)
                    }
                }
            }

            if !expanded {
                Text("+ \(commands.count - 7) more commands")
                    .font(.system(size: 12))
                    .foregroundStyle(cTert)
                    .padding(.top, 6)
            }
        }
        .padding(22)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// ── Temperature card ───────────────────────────────────────────
struct TemperatureCard: View {
    @AppStorage("ai_temperature") private var temperature: Double = 0.7

    private var label: String {
        switch temperature {
        case ..<0.4: return "Precise"
        case ..<0.8: return "Balanced"
        case ..<1.1: return "Creative"
        default:     return "Wild"
        }
    }

    private var labelColor: Color {
        switch temperature {
        case ..<0.4: return Color(hex: "#4FC3F7")
        case ..<0.8: return cAccent
        case ..<1.1: return Color(hex: "#FF9F0A")
        default:     return Color(hex: "#FF3B30")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("🌡️  AI Temperature")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(cPrim)
                Spacer()
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(labelColor)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(labelColor.opacity(0.15))
                    .clipShape(Capsule())
            }

            Text("Controls how creative or deterministic the AI output is.")
                .font(.system(size: 13))
                .foregroundStyle(cSec)

            HStack(spacing: 10) {
                Text("0.0")
                    .font(.system(size: 11))
                    .foregroundStyle(cTert)
                Slider(value: $temperature, in: 0.0...1.5, step: 0.1)
                    .tint(labelColor)
                Text("1.5")
                    .font(.system(size: 11))
                    .foregroundStyle(cTert)
                Text(String(format: "%.1f", temperature))
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .foregroundStyle(labelColor)
                    .frame(width: 28)
            }

            Text("Precise  ·  Balanced  ·  Creative  ·  Wild")
                .font(.system(size: 11))
                .foregroundStyle(cTert)
        }
        .padding(22)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// ── Custom commands card ───────────────────────────────────────
struct CustomCommandsCard: View {
    @State private var commands:   [CustomCommand] = loadCustomCommands()
    @State private var showSheet   = false
    @State private var editTarget: CustomCommand?  = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("My Commands")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(cPrim)
                Spacer()
                Button {
                    editTarget = nil
                    showSheet  = true
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(cAccent)
                }
                .buttonStyle(.plain)
            }

            Text("Create custom ?triggers with your own AI prompts. They work in any app, just like built-in commands.")
                .font(.system(size: 13))
                .foregroundStyle(cSec)

            if commands.isEmpty {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Text("✦").font(.system(size: 24))
                        Text("No custom commands yet")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(cPrim)
                        Text("Click + to create your first one")
                            .font(.system(size: 13))
                            .foregroundStyle(cSec)
                    }
                    .padding(.vertical, 16)
                    Spacer()
                }
            } else {
                ForEach(commands) { cmd in
                    VStack(spacing: 0) {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(cmd.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(cPrim)
                                Text(cmd.trigger)
                                    .font(.system(size: 12, weight: .semibold, design: .monospaced))
                                    .foregroundStyle(cAccLt)
                                Text(cmd.prompt)
                                    .font(.system(size: 12))
                                    .foregroundStyle(cSec)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Button {
                                editTarget = cmd
                                showSheet  = true
                            } label: {
                                Image(systemName: "pencil")
                                    .foregroundStyle(cAccent)
                            }
                            .buttonStyle(.plain)
                            Button {
                                commands.removeAll { $0.id == cmd.id }
                                saveCustomCommands(commands)
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundStyle(Color(hex: "#FF3B30"))
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.vertical, 10)

                        if cmd.id != commands.last?.id {
                            Divider().background(cSurf3)
                        }
                    }
                }
            }
        }
        .padding(22)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .sheet(isPresented: $showSheet) {
            AddCommandSheet(existing: editTarget) { saved in
                if editTarget != nil {
                    commands = commands.map { $0.id == saved.id ? saved : $0 }
                } else {
                    commands.append(saved)
                }
                saveCustomCommands(commands)
                showSheet  = false
                editTarget = nil
            }
        }
    }
}

// ── Add / edit command sheet ───────────────────────────────────
struct AddCommandSheet: View {
    let existing: CustomCommand?
    let onSave:   (CustomCommand) -> Void

    @State private var name:    String
    @State private var trigger: String
    @State private var prompt:  String
    @State private var error:   String? = nil
    @Environment(\.dismiss) private var dismiss

    init(existing: CustomCommand?, onSave: @escaping (CustomCommand) -> Void) {
        self.existing = existing
        self.onSave   = onSave
        _name    = State(initialValue: existing?.name    ?? "")
        _trigger = State(initialValue: existing?.trigger ?? "?")
        _prompt  = State(initialValue: existing?.prompt  ?? "")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(existing != nil ? "Edit Command" : "New Command")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(cPrim)

            MacSheetField(label: "Name",    value: $name,    placeholder: "e.g. Formal Email")
            MacSheetField(label: "Trigger", value: $trigger, placeholder: "e.g. ?email")
            MacSheetField(label: "Prompt",  value: $prompt,  placeholder: "e.g. Rewrite as a professional email…", multiline: true)

            if let error {
                Text(error)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#FF3B30"))
            }

            HStack {
                Button("Cancel") { dismiss() }
                    .font(.system(size: 14))
                    .foregroundStyle(cSec)
                    .buttonStyle(.plain)
                Spacer()
                Button("Save Command") {
                    guard !name.isBlank    else { error = "Name is required"; return }
                    guard trigger.count > 1 && trigger.hasPrefix("?") else { error = "Trigger must start with ? plus a keyword"; return }
                    guard !prompt.isBlank  else { error = "Prompt is required"; return }
                    let cmd = CustomCommand(
                        id:      existing?.id ?? UUID().uuidString,
                        trigger: trigger.trimmingCharacters(in: .whitespaces).lowercased(),
                        name:    name.trimmingCharacters(in: .whitespaces),
                        prompt:  prompt.trimmingCharacters(in: .whitespaces)
                    )
                    onSave(cmd)
                }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 20)
                .frame(height: 40)
                .background(cAccent)
                .clipShape(Capsule())
                .buttonStyle(.plain)
            }
        }
        .padding(28)
        .background(cBg)
        .frame(minWidth: 440, maxWidth: 440)
    }
}

struct MacSheetField: View {
    let label:       String
    @Binding var value: String
    let placeholder: String
    var multiline:   Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(cSec)
            Group {
                if multiline {
                    TextEditor(text: $value)
                        .frame(minHeight: 80, maxHeight: 120)
                        .font(.system(size: 13))
                        .foregroundStyle(cPrim)
                        .scrollContentBackground(.hidden)
                } else {
                    TextField(placeholder, text: $value)
                        .font(.system(size: 13))
                        .foregroundStyle(cPrim)
                        .textFieldStyle(.plain)
                }
            }
            .padding(10)
            .background(cSurf2)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .tint(cAccent)
        }
    }
}

import SwiftUI

// ── Colors ────────────────────────────────────────────────────
private let cBg     = Color(hex: "#000000")
private let cSurf1  = Color(hex: "#0D0D0D")
private let cSurf2  = Color(hex: "#1A1A1A")
private let cSurf3  = Color(hex: "#242424")
private let cSurf4  = Color(hex: "#2E2E2E")
private let cAccent = Color(hex: "#7B61FF")
private let cAccLt  = Color(hex: "#9E8FFF")
private let cPrim   = Color.white
private let cSec    = Color(hex: "#8A8A8A")
private let cTert   = Color(hex: "#444444")
private let cGreen  = Color(hex: "#34C759")
private let cRed    = Color(hex: "#FF3B30")
private let cOrange = Color(hex: "#FF9F0A")

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

// ── Custom command model ──────────────────────────────────────
struct CustomCommand: Codable, Identifiable {
    var id: String = UUID().uuidString
    var trigger: String
    var name: String
    var prompt: String
}

func loadCustomCommands() -> [CustomCommand] {
    guard let data = UserDefaults.standard.data(forKey: "custom_commands"),
          let commands = try? JSONDecoder().decode([CustomCommand].self, from: data)
    else { return [] }
    return commands
}

func saveCustomCommands(_ commands: [CustomCommand]) {
    if let data = try? JSONEncoder().encode(commands) {
        UserDefaults.standard.set(data, forKey: "custom_commands")
    }
}

// ── Navigation ────────────────────────────────────────────────
enum NavTab: String, CaseIterable, Identifiable {
    case home, commands, explore, settings
    var id: String { rawValue }

    var label: String {
        switch self {
        case .home:     "Home"
        case .commands: "Commands"
        case .explore:  "Explore"
        case .settings: "Settings"
        }
    }
    var icon: String {
        switch self {
        case .home:     "house.fill"
        case .commands: "bolt.fill"
        case .explore:  "safari.fill"
        case .settings: "gearshape.fill"
        }
    }
}

// ── Root view ─────────────────────────────────────────────────
struct SettingsView: View {
    @ObservedObject private var monitor = TextMonitor.shared

    var body: some View {
        TabView {
            HomeTab(monitor: monitor)
                .tabItem { Label("Home",     systemImage: "house.fill") }
                .tag(NavTab.home)

            CommandsTab()
                .tabItem { Label("Commands", systemImage: "bolt.fill") }
                .tag(NavTab.commands)

            ExploreTab()
                .tabItem { Label("Explore",  systemImage: "safari.fill") }
                .tag(NavTab.explore)

            SettingsTab()
                .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                .tag(NavTab.settings)
        }
        .preferredColorScheme(.dark)
        .frame(width: 600, height: 540)
        .background(cBg)
    }
}

// ─────────────────────────────────────────────
//  HOME TAB
// ─────────────────────────────────────────────
struct HomeTab: View {
    @ObservedObject var monitor: TextMonitor

    let quickCommands = [
        ("?fix", "Fix grammar"), ("?improve", "Improve"),
        ("?formal", "Formal"), ("?casual", "Casual"),
        ("?shorter", "Shorter"), ("?emoji", "Add emojis")
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Spacer().frame(height: 4)

                // Status card
                HomeStatusCard(monitor: monitor)

                // Stats row
                HomeStatsRow()

                // Quick commands
                VStack(alignment: .leading, spacing: 10) {
                    SectionLabel("Quick Commands")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(quickCommands, id: \.0) { trigger, label in
                                QuickCommandChip(trigger: trigger, label: label)
                            }
                        }
                        .padding(.horizontal, 1)
                    }
                }

                // Tip
                SectionLabel("Pro Tip")
                TipCard()

                Spacer().frame(height: 20)
            }
            .padding(24)
        }
        .background(cBg)
    }
}

struct HomeStatusCard: View {
    @ObservedObject var monitor: TextMonitor

    var body: some View {
        let granted = monitor.accessibilityGranted
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Circle()
                    .fill(granted ? cGreen : cRed)
                    .frame(width: 8, height: 8)
                Text(granted ? "Service active" : "Service disabled")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(cPrim)
            }

            Text(granted
                ? "TypeShift is running. Type any trigger in any app and press Space (native) or ⌃⇧Space (browsers)."
                : "Enable TypeShift in System Settings → Privacy & Security → Accessibility.")
                .font(.system(size: 13))
                .foregroundStyle(cSec)
                .lineSpacing(3)

            if !granted {
                HStack(spacing: 10) {
                    Button {
                        monitor.requestAccessibility()
                    } label: {
                        Text("Grant Access")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(cPrim)
                            .padding(.horizontal, 20)
                            .frame(height: 36)
                            .background(cAccent)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)

                    Button {
                        monitor.fixPermission()
                    } label: {
                        Text("Already granted? Tap to fix →")
                            .font(.system(size: 12))
                            .foregroundStyle(cAccLt)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [cSurf2, cSurf1], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(monitor.accessibilityGranted ? cGreen.opacity(0.25) : cRed.opacity(0.2), lineWidth: 1)
        )
    }
}

struct HomeStatsRow: View {
    var customCount: Int { loadCustomCommands().count }
    var hasKey: Bool { !(UserDefaults.standard.string(forKey: "groq_api_key") ?? "").isEmpty }

    var body: some View {
        HStack(spacing: 12) {
            StatChip(icon: "bolt.fill",    value: "19",            label: "Commands")
            StatChip(icon: "star.fill",    value: "\(customCount)", label: "Custom")
            StatChip(icon: "key.fill",     value: hasKey ? "Set" : "None", label: "API Key",
                     valueColor: hasKey ? cGreen : cRed)
        }
    }
}

struct StatChip: View {
    var icon: String
    var value: String
    var label: String
    var valueColor: Color = cPrim

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon).foregroundStyle(cAccent).font(.system(size: 14))
            Text(value).font(.system(size: 18, weight: .bold)).foregroundStyle(valueColor)
            Text(label).font(.system(size: 11)).foregroundStyle(cSec)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(cSurf2)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct QuickCommandChip: View {
    var trigger: String
    var label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(trigger).font(.system(size: 12, weight: .semibold, design: .monospaced)).foregroundStyle(cAccLt)
            Text(label).font(.system(size: 11)).foregroundStyle(cSec)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(cAccent.opacity(0.08))
        .clipShape(Capsule())
        .overlay(Capsule().strokeBorder(cAccent.opacity(0.25), lineWidth: 1))
    }
}

struct TipCard: View {
    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Text("💡").font(.system(size: 22))
            VStack(alignment: .leading, spacing: 4) {
                Text("Tip of the day").font(.system(size: 13, weight: .semibold)).foregroundStyle(cAccLt)
                Text("Type your text, add ?formal at the end and press Space — TypeShift rewrites it in-place in any app on your Mac.")
                    .font(.system(size: 13)).foregroundStyle(cSec).lineSpacing(3)
            }
        }
        .padding(18)
        .background(LinearGradient(colors: [Color(hex: "#1A1040"), Color(hex: "#0D0D1A")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(cAccent.opacity(0.2), lineWidth: 1))
    }
}

// ─────────────────────────────────────────────
//  COMMANDS TAB
// ─────────────────────────────────────────────
struct CommandsTab: View {
    @State private var customCommands: [CustomCommand] = loadCustomCommands()
    @State private var showAddSheet  = false
    @State private var editTarget:    CustomCommand? = nil
    @State private var builtinExpanded = true

    let builtinCommands: [(String, String)] = [
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
        ("?bullet",       "Bullet points"),
        ("?subject",      "Email subject line"),
        ("?eli5",         "Explain like I'm 5"),
        ("?tldr",         "One-line summary"),
        ("?headline",     "Catchy headline"),
        ("?undo",         "Restore original"),
        ("?translate:XX", "Translate any language"),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Spacer().frame(height: 4)

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Commands")
                            .font(.system(size: 26, weight: .heavy))
                            .foregroundStyle(cPrim)
                        Text("\(builtinCommands.count + customCommands.count) total")
                            .font(.system(size: 13)).foregroundStyle(cSec)
                    }
                    Spacer()
                    Button {
                        editTarget = nil; showAddSheet = true
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus")
                            Text("New Command")
                        }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(cPrim)
                        .padding(.horizontal, 16)
                        .frame(height: 34)
                        .background(cAccent)
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }

                // Built-in section
                VStack(alignment: .leading, spacing: 8) {
                    Button {
                        withAnimation { builtinExpanded.toggle() }
                    } label: {
                        HStack {
                            SectionLabel("Built-in  •  \(builtinCommands.count)")
                            Spacer()
                            Image(systemName: builtinExpanded ? "chevron.up" : "chevron.down")
                                .font(.system(size: 11))
                                .foregroundStyle(cSec)
                        }
                    }
                    .buttonStyle(.plain)

                    if builtinExpanded {
                        VStack(spacing: 0) {
                            ForEach(builtinCommands, id: \.0) { trigger, desc in
                                MacCommandRow(trigger: trigger, desc: desc)
                                if trigger != builtinCommands.last?.0 {
                                    Divider().opacity(0.3).padding(.horizontal, 16)
                                }
                            }
                        }
                        .background(cSurf2)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }

                // My Commands section
                VStack(alignment: .leading, spacing: 8) {
                    SectionLabel("My Commands  •  \(customCommands.count)")

                    if customCommands.isEmpty {
                        HStack {
                            Spacer()
                            VStack(spacing: 8) {
                                Text("✦").font(.system(size: 28))
                                Text("No custom commands yet")
                                    .font(.system(size: 14, weight: .semibold)).foregroundStyle(cPrim)
                                Text("Click \"New Command\" to create one")
                                    .font(.system(size: 13)).foregroundStyle(cSec)
                            }
                            .padding(32)
                            Spacer()
                        }
                        .background(cSurf2)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(cSurf4, lineWidth: 1))
                    } else {
                        VStack(spacing: 8) {
                            ForEach(customCommands) { cmd in
                                MacCustomCommandItem(cmd: cmd,
                                    onEdit: { editTarget = cmd; showAddSheet = true },
                                    onDelete: {
                                        customCommands.removeAll { $0.id == cmd.id }
                                        saveCustomCommands(customCommands)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer().frame(height: 20)
            }
            .padding(24)
        }
        .background(cBg)
        .sheet(isPresented: $showAddSheet) {
            AddCommandSheet(existing: editTarget) { cmd in
                if let idx = customCommands.firstIndex(where: { $0.id == cmd.id }) {
                    customCommands[idx] = cmd
                } else {
                    customCommands.append(cmd)
                }
                saveCustomCommands(customCommands)
                showAddSheet = false
                editTarget = nil
            } onCancel: {
                showAddSheet = false
                editTarget = nil
            }
        }
    }
}

struct MacCommandRow: View {
    var trigger: String
    var desc: String

    var body: some View {
        HStack(spacing: 12) {
            Text(trigger)
                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                .foregroundStyle(cAccLt)
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(cAccent.opacity(0.12))
                .clipShape(Capsule())
            Text(desc)
                .font(.system(size: 13)).foregroundStyle(cSec)
            Spacer()
        }
        .padding(.horizontal, 16).padding(.vertical, 11)
    }
}

struct MacCustomCommandItem: View {
    var cmd: CustomCommand
    var onEdit: () -> Void
    var onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(cAccent.opacity(0.12))
                    .frame(width: 38, height: 38)
                Text("✦").font(.system(size: 16))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(cmd.name)
                    .font(.system(size: 14, weight: .semibold)).foregroundStyle(cPrim)
                Text(cmd.trigger)
                    .font(.system(size: 12, design: .monospaced)).foregroundStyle(cAccLt)
                Text(cmd.prompt)
                    .font(.system(size: 12)).foregroundStyle(cSec)
                    .lineLimit(1)
            }
            Spacer()
            HStack(spacing: 4) {
                Button { onEdit() } label: {
                    Image(systemName: "pencil")
                        .foregroundStyle(cAccent)
                        .padding(6)
                        .background(cAccent.opacity(0.1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)

                Button { onDelete() } label: {
                    Image(systemName: "trash")
                        .foregroundStyle(cRed)
                        .padding(6)
                        .background(cRed.opacity(0.1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(14)
        .background(cSurf2)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct AddCommandSheet: View {
    var existing: CustomCommand?
    var onSave: (CustomCommand) -> Void
    var onCancel: () -> Void

    @State private var name: String
    @State private var trigger: String
    @State private var prompt: String
    @State private var error: String? = nil

    init(existing: CustomCommand?, onSave: @escaping (CustomCommand) -> Void, onCancel: @escaping () -> Void) {
        self.existing = existing
        self.onSave = onSave
        self.onCancel = onCancel
        _name    = State(initialValue: existing?.name    ?? "")
        _trigger = State(initialValue: existing?.trigger ?? "?")
        _prompt  = State(initialValue: existing?.prompt  ?? "")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                Text(existing != nil ? "Edit Command" : "New Command")
                    .font(.system(size: 20, weight: .bold)).foregroundStyle(cPrim)
                Spacer()
                Button { onCancel() } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(cSec).font(.system(size: 20))
                }
                .buttonStyle(.plain)
            }

            SheetField(label: "Name", value: $name, placeholder: "e.g. Formal Email")
            SheetField(label: "Trigger", value: $trigger, placeholder: "e.g. ?email")
            SheetField(label: "Prompt", value: $prompt, placeholder: "e.g. Rewrite as a professional email…", multiline: true)

            if let error {
                Text(error).font(.system(size: 13)).foregroundStyle(cRed)
            }

            HStack {
                Spacer()
                Button { onCancel() } label: {
                    Text("Cancel")
                        .font(.system(size: 14)).foregroundStyle(cSec)
                        .padding(.horizontal, 20).frame(height: 38)
                        .background(cSurf3).clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Button {
                    guard !name.isBlank    else { error = "Name is required"; return }
                    guard trigger.count > 1 && trigger.hasPrefix("?") else { error = "Trigger must start with ?"; return }
                    guard !prompt.isBlank  else { error = "Prompt is required"; return }
                    let cmd = CustomCommand(
                        id:      existing?.id ?? UUID().uuidString,
                        trigger: trigger.trimmingCharacters(in: .whitespaces).lowercased(),
                        name:    name.trimmingCharacters(in: .whitespaces),
                        prompt:  prompt.trimmingCharacters(in: .whitespaces)
                    )
                    onSave(cmd)
                } label: {
                    Text("Save Command")
                        .font(.system(size: 14, weight: .semibold)).foregroundStyle(cPrim)
                        .padding(.horizontal, 24).frame(height: 38)
                        .background(cAccent).clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(28)
        .frame(width: 440)
        .background(cSurf1)
        .preferredColorScheme(.dark)
    }
}

struct SheetField: View {
    var label: String
    @Binding var value: String
    var placeholder: String
    var multiline = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 12, weight: .medium)).foregroundStyle(cSec)
            Group {
                if multiline {
                    TextEditor(text: $value)
                        .font(.system(size: 13)).foregroundStyle(cPrim)
                        .frame(height: 80)
                        .scrollContentBackground(.hidden)
                } else {
                    TextField(placeholder, text: $value)
                        .font(.system(size: 13)).foregroundStyle(cPrim)
                        .textFieldStyle(.plain)
                        .frame(height: 36)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, multiline ? 8 : 0)
            .background(cSurf3)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(cAccent.opacity(0.3), lineWidth: 1))
        }
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}

// ─────────────────────────────────────────────
//  EXPLORE TAB
// ─────────────────────────────────────────────
struct ExploreTab: View {
    let useCases: [(String, String, String)] = [
        ("✍️", "Writing",      "Use ?improve and ?formal to polish emails, essays, and reports in seconds."),
        ("💬", "Messaging",    "Add ?casual or ?emoji to make your texts more fun and expressive."),
        ("🐦", "Social Media", "Turn any long thought into a viral tweet with ?tweet."),
        ("📧", "Email",        "Generate the perfect subject line with ?subject — never blank again."),
        ("🌍", "Translate",    "Type ?translate:french to instantly translate to any language."),
        ("🎤", "Content",      "?headline turns plain text into attention-grabbing titles."),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Spacer().frame(height: 4)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Explore")
                        .font(.system(size: 26, weight: .heavy)).foregroundStyle(cPrim)
                    Text("Discover what TypeShift can do")
                        .font(.system(size: 13)).foregroundStyle(cSec)
                }

                // How it works
                VStack(alignment: .leading, spacing: 12) {
                    Text("How it works")
                        .font(.system(size: 15, weight: .bold)).foregroundStyle(cPrim)

                    ForEach(Array([
                        ("1", "Type your text in any app on your Mac"),
                        ("2", "Append a command like ?fix or ?formal"),
                        ("3", "Press Space (native) or ⌃⇧Space (browsers) — text is rewritten instantly"),
                    ].enumerated()), id: \.0) { _, step in
                        HStack(spacing: 12) {
                            Text(step.0)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(cAccLt)
                                .frame(width: 26, height: 26)
                                .background(cAccent.opacity(0.2))
                                .clipShape(Circle())
                            Text(step.1).font(.system(size: 13)).foregroundStyle(cSec)
                        }
                    }
                }
                .padding(20)
                .background(LinearGradient(colors: [Color(hex: "#1A1040"), Color(hex: "#0D1A20")], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(cAccent.opacity(0.15), lineWidth: 1))

                SectionLabel("Use Cases")

                VStack(spacing: 10) {
                    ForEach(useCases, id: \.1) { emoji, title, desc in
                        HStack(alignment: .top, spacing: 14) {
                            Text(emoji)
                                .font(.system(size: 22))
                                .frame(width: 42, height: 42)
                                .background(cSurf3)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            VStack(alignment: .leading, spacing: 3) {
                                Text(title).font(.system(size: 14, weight: .semibold)).foregroundStyle(cPrim)
                                Text(desc).font(.system(size: 13)).foregroundStyle(cSec).lineSpacing(2)
                            }
                            Spacer()
                        }
                        .padding(14)
                        .background(cSurf2)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }

                Spacer().frame(height: 20)
            }
            .padding(24)
        }
        .background(cBg)
    }
}

// ─────────────────────────────────────────────
//  SETTINGS TAB
// ─────────────────────────────────────────────
struct SettingsTab: View {
    @State private var apiKey     = UserDefaults.standard.string(forKey: "groq_api_key") ?? ""
    @State private var saved      = false
    @State private var showKey    = false
    @State private var temperature = UserDefaults.standard.object(forKey: "ai_temperature") as? Double ?? 0.7

    var tempLabel: String {
        switch temperature {
        case ..<0.4: "Precise"
        case ..<0.8: "Balanced"
        case ..<1.1: "Creative"
        default:     "Wild"
        }
    }
    var tempColor: Color {
        switch temperature {
        case ..<0.4: Color(hex: "#4FC3F7")
        case ..<0.8: cAccent
        case ..<1.1: cOrange
        default:     cRed
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Spacer().frame(height: 4)

                Text("Settings")
                    .font(.system(size: 26, weight: .heavy)).foregroundStyle(cPrim)

                // API Key
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 8) {
                        Image(systemName: "key.fill").foregroundStyle(cAccent)
                        Text("Groq API Key")
                            .font(.system(size: 17, weight: .bold)).foregroundStyle(cPrim)
                    }
                    Text("Free, fast, no credit card required")
                        .font(.system(size: 13)).foregroundStyle(cSec)

                    HStack {
                        Group {
                            if showKey {
                                TextField("gsk_...", text: $apiKey)
                            } else {
                                SecureField("gsk_...", text: $apiKey)
                            }
                        }
                        .font(.system(size: 13, design: .monospaced))
                        .foregroundStyle(cPrim).textFieldStyle(.plain)
                        .tint(cAccent).onChange(of: apiKey) { _ in saved = false }

                        Button(showKey ? "Hide" : "Show") { showKey.toggle() }
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(cAccent).buttonStyle(.plain)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 11)
                    .background(cSurf3).clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(cAccent.opacity(0.3), lineWidth: 1))

                    HStack(spacing: 12) {
                        Button {
                            UserDefaults.standard.set(apiKey.trimmingCharacters(in: .whitespaces), forKey: "groq_api_key")
                            saved = true
                        } label: {
                            Text("Save")
                                .font(.system(size: 13, weight: .semibold)).foregroundStyle(cPrim)
                                .padding(.horizontal, 22).frame(height: 36)
                                .background(cAccent).clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        if saved {
                            Text("✓  Saved").font(.system(size: 13, weight: .medium)).foregroundStyle(cGreen)
                        }
                    }
                    Text("Get your free key at console.groq.com")
                        .font(.system(size: 12)).foregroundStyle(cTert)
                }
                .padding(20)
                .background(cSurf2).clipShape(RoundedRectangle(cornerRadius: 18))

                // Temperature
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 8) {
                        Text("🌡️")
                        Text("AI Temperature")
                            .font(.system(size: 17, weight: .bold)).foregroundStyle(cPrim)
                    }
                    Text("Controls how creative or precise the AI output is.")
                        .font(.system(size: 13)).foregroundStyle(cSec)

                    HStack {
                        Text(tempLabel)
                            .font(.system(size: 15, weight: .semibold)).foregroundStyle(tempColor)
                        Spacer()
                        Text(String(format: "%.1f", temperature))
                            .font(.system(size: 13, weight: .bold)).foregroundStyle(tempColor)
                            .padding(.horizontal, 10).padding(.vertical, 3)
                            .background(tempColor.opacity(0.12)).clipShape(Capsule())
                    }

                    Slider(value: $temperature, in: 0...1.5, step: 0.1) {
                        EmptyView()
                    } minimumValueLabel: {
                        Text("0.0").font(.system(size: 11)).foregroundStyle(cTert)
                    } maximumValueLabel: {
                        Text("1.5").font(.system(size: 11)).foregroundStyle(cTert)
                    }
                    .tint(tempColor)
                    .onChange(of: temperature) { val in
                        UserDefaults.standard.set(val, forKey: "ai_temperature")
                    }
                }
                .padding(20)
                .background(cSurf2).clipShape(RoundedRectangle(cornerRadius: 18))

                // Model info
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(LinearGradient(colors: [Color(hex: "#3D2BFF"), cAccent], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 42, height: 42)
                        Text("⚡").font(.system(size: 20))
                    }
                    VStack(alignment: .leading, spacing: 3) {
                        Text("AI Model").font(.system(size: 14, weight: .semibold)).foregroundStyle(cPrim)
                        Text("llama-3.3-70b-versatile").font(.system(size: 13)).foregroundStyle(cAccLt)
                        Text("via Groq — ~300ms response time").font(.system(size: 12)).foregroundStyle(cSec)
                    }
                    Spacer()
                }
                .padding(18)
                .background(cSurf2).clipShape(RoundedRectangle(cornerRadius: 18))

                // About
                VStack(alignment: .leading, spacing: 10) {
                    Text("About").font(.system(size: 17, weight: .bold)).foregroundStyle(cPrim)
                    Divider().opacity(0.3)
                    HStack { Text("Version").foregroundStyle(cSec); Spacer(); Text("1.0").foregroundStyle(cPrim) }
                    HStack { Text("Platform").foregroundStyle(cSec); Spacer(); Text("macOS 13+").foregroundStyle(cPrim) }
                    HStack { Text("Shortcut").foregroundStyle(cSec); Spacer(); Text("⌃⇧Space").foregroundStyle(cPrim) }
                    Divider().opacity(0.3)
                    Text("TypeShift works in every app — Notes, Mail, VS Code, browsers, and more.")
                        .font(.system(size: 13)).foregroundStyle(cSec).lineSpacing(3)
                }
                .font(.system(size: 13))
                .padding(20)
                .background(cSurf2).clipShape(RoundedRectangle(cornerRadius: 18))

                Spacer().frame(height: 20)
            }
            .padding(24)
        }
        .background(cBg)
    }
}

// ── Shared ────────────────────────────────────────────────────
struct SectionLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(cSec)
            .tracking(0.5)
            .textCase(.uppercase)
    }
}

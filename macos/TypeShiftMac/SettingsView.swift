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
                CommandsCard(expanded: $expanded)

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

import SwiftUI

// ── Colors (OxygenOS AMOLED dark) ─────────────────────────────
private let cBg       = Color(hex: "#000000")
private let cSurf1    = Color(hex: "#141414")
private let cSurf2    = Color(hex: "#1E1E1E")
private let cSurf3    = Color(hex: "#282828")
private let cAccent   = Color(hex: "#7B61FF")
private let cAccentLt = Color(hex: "#9E8FFF")
private let cPrim     = Color.white
private let cSec      = Color(hex: "#8A8A8A")
private let cTert     = Color(hex: "#555555")
private let cGreen    = Color(hex: "#34C759")
private let cRed      = Color(hex: "#FF3B30")

extension Color {
    init(hex: String) {
        let h = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var rgb: UInt64 = 0
        Scanner(string: h).scanHexInt64(&rgb)
        self.init(
            red:   Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8)  & 0xFF) / 255,
            blue:  Double( rgb        & 0xFF) / 255
        )
    }
}

// ── Main view ─────────────────────────────────────────────────
struct ContentView: View {
    @State private var apiKey   = sharedDefaults.groqAPIKey
    @State private var saved    = false
    @State private var showKey  = false
    @State private var expanded = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 60)

                // Title
                Text("TypeShift")
                    .font(.system(size: 40, weight: .heavy, design: .rounded))
                    .foregroundStyle(cPrim)
                    .tracking(-0.5)
                Text("AI text assistant — works everywhere")
                    .font(.system(size: 15, weight: .regular))
                    .foregroundStyle(cSec)
                    .padding(.top, 2)

                Spacer().frame(height: 32)

                // Instructions card
                HowToCard()

                Spacer().frame(height: 16)

                // API key card
                ApiCard(apiKey: $apiKey, saved: $saved, showKey: $showKey)

                Spacer().frame(height: 16)

                // Commands card
                CommandsCard(expanded: $expanded)

                Spacer().frame(height: 48)
            }
            .padding(.horizontal, 20)
        }
        .background(cBg.ignoresSafeArea())
    }
}

// ── How to use card ────────────────────────────────────────────
struct HowToCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Circle()
                    .fill(cGreen)
                    .frame(width: 10, height: 10)
                Text("Setup")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(cPrim)
            }
            Text("Go to **Settings → General → Keyboard → Keyboards → Add New Keyboard** and add TypeShift. Then enable Full Access so it can reach the AI.")
                .font(.system(size: 14))
                .foregroundStyle(cSec)
                .lineSpacing(5)

            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text("Open Settings")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(cPrim)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(cAccent)
                    .clipShape(Capsule())
            }
        }
        .padding(24)
        .background(
            LinearGradient(colors: [cSurf1, cSurf2], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

// ── API Key card ───────────────────────────────────────────────
struct ApiCard: View {
    @Binding var apiKey: String
    @Binding var saved:  Bool
    @Binding var showKey: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("API Key")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(cPrim)

            Text("Powered by Groq — free, no credit card needed")
                .font(.system(size: 14))
                .foregroundStyle(cSec)

            // Input
            HStack {
                Group {
                    if showKey {
                        TextField("gsk_...", text: $apiKey)
                    } else {
                        SecureField("gsk_...", text: $apiKey)
                    }
                }
                .font(.system(size: 14, design: .monospaced))
                .foregroundStyle(cPrim)
                .tint(cAccent)
                .onChange(of: apiKey) { saved = false }

                Button(showKey ? "Hide" : "Show") { showKey.toggle() }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(cAccent)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(cSurf2)
            .clipShape(RoundedRectangle(cornerRadius: 16))

            HStack(spacing: 14) {
                Button {
                    sharedDefaults.groqAPIKey = apiKey.trimmingCharacters(in: .whitespaces)
                    saved = true
                } label: {
                    Text("Save")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(cPrim)
                        .padding(.horizontal, 28)
                        .frame(height: 48)
                        .background(cAccent)
                        .clipShape(Capsule())
                }

                if saved {
                    Text("✓  Saved")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(cGreen)
                }
            }

            Text("Get your free key at console.groq.com")
                .font(.system(size: 13))
                .foregroundStyle(cTert)
        }
        .padding(24)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

// ── Commands card ──────────────────────────────────────────────
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

    var visible: [(String, String)] { expanded ? commands : Array(commands.prefix(6)) }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Commands")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(cPrim)
                Spacer()
                Button(expanded ? "Show less" : "See all \(commands.count)") {
                    expanded.toggle()
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(cAccent)
            }

            Text("Type any command at the end of your text")
                .font(.system(size: 14))
                .foregroundStyle(cSec)
                .padding(.bottom, 8)

            ForEach(visible, id: \.0) { cmd, desc in
                VStack(spacing: 0) {
                    HStack(spacing: 14) {
                        Text(cmd)
                            .font(.system(size: 13, weight: .semibold, design: .monospaced))
                            .foregroundStyle(cAccentLt)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .background(cAccent.opacity(0.15))
                            .clipShape(Capsule())

                        Text(desc)
                            .font(.system(size: 14))
                            .foregroundStyle(cSec)

                        Spacer()
                    }
                    .padding(.vertical, 10)

                    if cmd != visible.last?.0 {
                        Divider()
                            .background(cSurf3)
                    }
                }
            }

            if !expanded {
                Text("+ \(commands.count - 6) more commands")
                    .font(.system(size: 13))
                    .foregroundStyle(cTert)
                    .padding(.top, 8)
            }
        }
        .padding(24)
        .background(cSurf1)
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

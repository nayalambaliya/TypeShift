import SwiftUI

@main
struct TypeShiftMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var monitor = TextMonitor.shared

    var body: some Scene {
        MenuBarExtra {
            MenuBarContent(monitor: monitor)
        } label: {
            MenuBarIcon(monitor: monitor)
        }
        .menuBarExtraStyle(.menu)

        Settings {
            SettingsView()
                .frame(width: 500)
                .preferredColorScheme(.dark)
        }
    }
}

// ── Menu bar label ─────────────────────────────────────────────
struct MenuBarIcon: View {
    @ObservedObject var monitor: TextMonitor

    var body: some View {
        if monitor.isProcessing {
            Text("⟳ T").font(.system(size: 12, weight: .bold))
        } else {
            Text("T›").font(.system(size: 13, weight: .heavy))
        }
    }
}

// ── Menu bar dropdown ──────────────────────────────────────────
struct MenuBarContent: View {
    @ObservedObject var monitor: TextMonitor

    var body: some View {
        // Status
        if monitor.isProcessing {
            Text("⟳  \(monitor.statusMessage)").foregroundStyle(.secondary)
            Divider()
        } else if !monitor.statusMessage.isEmpty {
            Text(monitor.statusMessage).foregroundStyle(.secondary)
            Divider()
        }

        if !monitor.accessibilityGranted {
            Button("⚠  Enable Accessibility Access…") { monitor.requestAccessibility() }
            Divider()
        }

        // Commands — click any command to process text in the current app.
        // Works in browsers, VS Code, and all native apps.
        Text("PROCESS TEXT IN CURRENT APP")
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(.secondary)

        Button("Fix Grammar")    { monitor.processSelectedText("?fix") }
        Button("Improve")        { monitor.processSelectedText("?improve") }
        Button("Make Formal")    { monitor.processSelectedText("?formal") }
        Button("Make Casual")    { monitor.processSelectedText("?casual") }
        Button("Shorten")        { monitor.processSelectedText("?shorter") }
        Button("Expand")         { monitor.processSelectedText("?longer") }
        Button("Write Reply")    { monitor.processSelectedText("?reply") }
        Button("Add Emojis")     { monitor.processSelectedText("?emoji") }
        Button("Make Human")     { monitor.processSelectedText("?human") }
        Button("Hinglish")       { monitor.processSelectedText("?hinglish") }

        Divider()

        Button("Write Tweet")    { monitor.processSelectedText("?tweet") }
        Button("Bullet Points")  { monitor.processSelectedText("?bullet") }
        Button("Email Subject")  { monitor.processSelectedText("?subject") }
        Button("Headline")       { monitor.processSelectedText("?headline") }
        Button("Summarize")      { monitor.processSelectedText("?tldr") }
        Button("Explain (ELI5)") { monitor.processSelectedText("?eli5") }
        Button("Roast It")       { monitor.processSelectedText("?roast") }

        let customCmds = loadCustomCommands()
        if !customCmds.isEmpty {
            Divider()
            Text("MY COMMANDS")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.secondary)
            ForEach(customCmds) { cc in
                Button(cc.name) { monitor.processSelectedText(cc.trigger) }
            }
        }

        Divider()

        if #available(macOS 14.0, *) {
            SettingsLink { Text("Settings…") }
        } else {
            Button("Settings…") {
                NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
            }
        }
        Divider()
        Button("Quit TypeShift") { NSApp.terminate(nil) }
    }
}

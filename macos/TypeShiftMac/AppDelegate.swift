import Cocoa
import ApplicationServices

class AppDelegate: NSObject, NSApplicationDelegate {

    func applicationDidFinishLaunching(_ notification: Notification) {
        // No dock icon — lives only in the menu bar
        NSApp.setActivationPolicy(.accessory)

        // Start monitoring (will check permission internally)
        TextMonitor.shared.setup()
    }
}

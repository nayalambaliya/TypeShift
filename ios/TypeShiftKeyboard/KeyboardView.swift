import SwiftUI

// ── Colors ──────────────────────────────────────────────────────
private let kBg     = Color(red: 0.06, green: 0.06, blue: 0.06)
private let kKey    = Color(red: 0.17, green: 0.17, blue: 0.17)
private let kKeyDk  = Color(red: 0.10, green: 0.10, blue: 0.10)
private let kAccent = Color(red: 0.48, green: 0.38, blue: 1.00)
private let kText   = Color.white
private let kTextSec = Color(white: 0.55)
private let kGreen  = Color(red: 0.20, green: 0.78, blue: 0.35)

// ── Root wrapper (needed by UIHostingController) ─────────────────
struct KeyboardRootView: View {
    @ObservedObject var model: KeyboardModel

    var body: some View {
        KeyboardView(model: model)
            .frame(height: keyboardHeight)
            .background(kBg)
    }

    private var keyboardHeight: CGFloat {
        let screen = UIScreen.main.bounds.height
        return screen > 800 ? 280 : 250   // taller on larger phones
    }
}

// ── Main keyboard view ───────────────────────────────────────────
struct KeyboardView: View {
    @ObservedObject var model: KeyboardModel

    var body: some View {
        VStack(spacing: 0) {
            statusBar
            if model.showNumbers {
                NumbersView(model: model)
            } else {
                LettersView(model: model)
            }
            bottomBar
        }
    }

    // ── Top status bar ──────────────────────────────────────────
    private var statusBar: some View {
        HStack {
            // Brand
            HStack(spacing: 5) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(kAccent)
                    .frame(width: 4, height: 16)
                Text("TypeShift")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(kText)
            }

            Spacer()

            // Status message or idle hint
            if model.isProcessing {
                HStack(spacing: 6) {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .scaleEffect(0.65)
                        .tint(kAccent)
                    Text(model.status)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(kAccent)
                }
            } else if !model.status.isEmpty {
                Text(model.status)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(model.status.hasPrefix("⚠") ? .orange : kGreen)
            } else {
                Text("type ?fix, ?improve…")
                    .font(.system(size: 11))
                    .foregroundStyle(kTextSec)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(Color(red: 0.04, green: 0.04, blue: 0.04))
    }

    // ── Bottom bar (space, return, switch) ─────────────────────
    private var bottomBar: some View {
        HStack(spacing: 8) {
            if model.showSwitchKey {
                KeyButton(label: "🌐", width: 44, color: kKeyDk) { model.switchMode() }
            }
            KeyButton(label: "123", width: 48, color: kKeyDk, fontSize: 13) {
                model.toggleNumbers()
            }
            KeyButton(label: "space", width: nil, color: kKey) { model.space() }
                .frame(maxWidth: .infinity)
            KeyButton(label: "return", width: 80, color: kKeyDk, fontSize: 13) { model.newline() }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 6)
        .background(kBg)
    }
}

// ── Letters layout (QWERTY) ──────────────────────────────────────
struct LettersView: View {
    @ObservedObject var model: KeyboardModel

    let rows = [
        ["q","w","e","r","t","y","u","i","o","p"],
        ["a","s","d","f","g","h","j","k","l"],
        ["z","x","c","v","b","n","m"],
    ]

    var body: some View {
        VStack(spacing: 6) {
            // Row 1
            HStack(spacing: 5) {
                ForEach(rows[0], id: \.self) { k in
                    KeyButton(label: model.isShifted ? k.uppercased() : k) {
                        model.tap(k)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 4)

            // Row 2 (slightly indented)
            HStack(spacing: 5) {
                Spacer().frame(width: 14)
                ForEach(rows[1], id: \.self) { k in
                    KeyButton(label: model.isShifted ? k.uppercased() : k) {
                        model.tap(k)
                    }
                    .frame(maxWidth: .infinity)
                }
                Spacer().frame(width: 14)
            }
            .padding(.horizontal, 4)

            // Row 3 + shift/backspace
            HStack(spacing: 5) {
                // Shift
                KeyButton(label: model.isShifted ? "⬆" : "⇧", width: 42, color: model.isShifted ? kAccent : kKeyDk) {
                    model.toggleShift()
                }
                Spacer().frame(width: 2)
                ForEach(rows[2], id: \.self) { k in
                    KeyButton(label: model.isShifted ? k.uppercased() : k) {
                        model.tap(k)
                    }
                    .frame(maxWidth: .infinity)
                }
                Spacer().frame(width: 2)
                // Backspace
                KeyButton(label: "⌫", width: 42, color: kKeyDk) { model.backspace() }
            }
            .padding(.horizontal, 4)
        }
        .padding(.top, 6)
    }
}

// ── Numbers layout ───────────────────────────────────────────────
struct NumbersView: View {
    @ObservedObject var model: KeyboardModel

    let numRows = [
        ["1","2","3","4","5","6","7","8","9","0"],
        ["-","/",":",";","(",")","$","&","@","\""],
        [".",",","?","!","'"]
    ]

    var body: some View {
        VStack(spacing: 6) {
            ForEach(numRows.indices, id: \.self) { i in
                HStack(spacing: 5) {
                    if i == 2 {
                        KeyButton(label: "ABC", width: 42, color: kKeyDk, fontSize: 12) {
                            model.toggleNumbers()
                        }
                        Spacer().frame(width: 2)
                    }
                    ForEach(numRows[i], id: \.self) { k in
                        KeyButton(label: k) { model.tap(k) }
                            .frame(maxWidth: .infinity)
                    }
                    if i == 2 {
                        Spacer().frame(width: 2)
                        KeyButton(label: "⌫", width: 42, color: kKeyDk) { model.backspace() }
                    }
                }
                .padding(.horizontal, 4)
            }
        }
        .padding(.top, 6)
    }
}

// ── Reusable key button ──────────────────────────────────────────
struct KeyButton: View {
    let label:    String
    var width:    CGFloat? = nil
    var color:    Color    = kKey
    var fontSize: CGFloat  = 16
    let action:   () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: fontSize, weight: .regular))
                .foregroundStyle(kText)
                .frame(width: width, height: 42)
                .frame(maxWidth: width == nil ? nil : .none)
                .background(color)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

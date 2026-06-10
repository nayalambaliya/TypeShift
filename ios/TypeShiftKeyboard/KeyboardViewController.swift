import UIKit
import SwiftUI

class KeyboardViewController: UIInputViewController {

    private var model: KeyboardModel!
    private var host: UIHostingController<KeyboardRootView>?

    override func viewDidLoad() {
        super.viewDidLoad()

        model = KeyboardModel(
            proxy:          { [weak self] in self?.textDocumentProxy },
            advanceAction:  { [weak self] in self?.advanceToNextInputMode() },
            switchKeyCheck: { [weak self] in self?.needsInputModeSwitchKey ?? false }
        )

        let root = KeyboardRootView(model: model)
        let hc = UIHostingController(rootView: root)
        hc.view.translatesAutoresizingMaskIntoConstraints = false
        hc.view.backgroundColor = .clear

        addChild(hc)
        view.addSubview(hc.view)
        hc.didMove(toParent: self)

        NSLayoutConstraint.activate([
            hc.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hc.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            hc.view.topAnchor.constraint(equalTo: view.topAnchor),
            hc.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        host = hc
    }

    override func textDidChange(_ textInput: (any UITextInput)?) {
        super.textDidChange(textInput)
        model.onTextChanged()
    }
}

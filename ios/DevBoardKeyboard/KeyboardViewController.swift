import UIKit

/// Native iOS keyboard extension. No microphone, file or network access is requested.
final class KeyboardViewController: UIInputViewController {
    private enum Layer { case letters, numbers, symbols, emoji }
    private struct Key {
        let label: String
        let id: String
        let weight: CGFloat
        init(_ label: String, _ id: String? = nil, _ weight: CGFloat = 1) {
            self.label = label
            self.id = id ?? label
            self.weight = weight
        }
    }

    private let vertical = UIStackView()
    private let popup = UIStackView()
    private var popupHeightAdded = false
    private var keyboardHeightConstraint: NSLayoutConstraint!
    private var layer: Layer = .letters
    private var shift = true
    private var capsLock = false
    private var ctrl = false
    private var alt = false
    private var lastShiftTap = Date.distantPast
    private let accentedNumbers: [String: String] = [
        "1": "ą", "2": "č", "3": "ę", "4": "ė", "5": "į",
        "6": "š", "7": "ų", "8": "ū", "9": "ž"
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        keyboardHeightConstraint = view.heightAnchor.constraint(equalToConstant: 312)
        keyboardHeightConstraint.isActive = true
        vertical.axis = .vertical
        vertical.spacing = 5
        vertical.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(vertical)
        NSLayoutConstraint.activate([
            vertical.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 4),
            vertical.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -4),
            vertical.topAnchor.constraint(equalTo: view.topAnchor, constant: 4),
            vertical.bottomAnchor.constraint(lessThanOrEqualTo: view.bottomAnchor, constant: -4)
        ])
        render()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        render()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        let before = textDocumentProxy.documentContextBeforeInput ?? ""
        if before.isEmpty || before.hasSuffix("\n") { shift = true }
        render()
    }

    private func palette() -> (UIColor, UIColor, UIColor) {
        switch SharedSettings.theme {
        case "light": return (.systemGray6, .white, .black)
        case "matrix": return (.black, UIColor(red: 0.08, green: 0.18, blue: 0.10, alpha: 1), .systemGreen)
        default: return (UIColor(red: 0.13, green: 0.15, blue: 0.18, alpha: 1),
                         UIColor(red: 0.25, green: 0.28, blue: 0.33, alpha: 1), .white)
        }
    }

    private func makeButton(_ key: Key) -> UIButton {
        let (_, buttonColor, foreground) = palette()
        let button = UIButton(type: .system)
        button.setTitle(key.label, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: key.label.contains("\n") ? 14 : 17, weight: .medium)
        button.titleLabel?.numberOfLines = 2
        button.titleLabel?.textAlignment = .center
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.setTitleColor(foreground, for: .normal)
        button.backgroundColor = buttonColor
        button.layer.cornerRadius = 6
        button.accessibilityIdentifier = key.id
        button.addAction(UIAction { [weak self] _ in self?.keyPressed(key.id) }, for: .touchUpInside)
        if ["comma", "period"].contains(key.id) || accentedNumbers[key.id] != nil {
            let hold = UILongPressGestureRecognizer(target: self, action: #selector(longPressed(_:)))
            hold.minimumPressDuration = 0.4
            button.addGestureRecognizer(hold)
        }
        return button
    }

    private func addRow(_ keys: [Key]) {
        let row = UIStackView()
        row.axis = .horizontal
        row.distribution = .fill
        row.spacing = 4
        let total = keys.reduce(CGFloat(0)) { $0 + $1.weight }
        for key in keys {
            let button = makeButton(key)
            row.addArrangedSubview(button)
            let ratio = key.weight / total
            button.widthAnchor.constraint(equalTo: row.widthAnchor, multiplier: ratio,
                constant: -(CGFloat(keys.count - 1) * row.spacing * ratio)).isActive = true
        }
        row.heightAnchor.constraint(equalToConstant: 45).isActive = true
        vertical.addArrangedSubview(row)
    }

    private func addToolbar() {
        let scroll = UIScrollView()
        scroll.showsHorizontalScrollIndicator = false
        scroll.heightAnchor.constraint(equalToConstant: 39).isActive = true
        let toolbar = UIStackView()
        toolbar.axis = .horizontal
        toolbar.spacing = 4
        toolbar.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(toolbar)
        NSLayoutConstraint.activate([
            toolbar.leadingAnchor.constraint(equalTo: scroll.contentLayoutGuide.leadingAnchor),
            toolbar.trailingAnchor.constraint(equalTo: scroll.contentLayoutGuide.trailingAnchor),
            toolbar.topAnchor.constraint(equalTo: scroll.contentLayoutGuide.topAnchor),
            toolbar.bottomAnchor.constraint(equalTo: scroll.contentLayoutGuide.bottomAnchor),
            toolbar.heightAnchor.constraint(equalTo: scroll.frameLayoutGuide.heightAnchor)
        ])
        for key in [
            Key("Esc", "esc"), Key("Tab", "tab"), Key(ctrl ? "Ctrl●" : "Ctrl", "ctrl"),
            Key(alt ? "Alt●" : "Alt", "alt"), Key("🎙️", "mic"),
            Key("←", "left"), Key("↑", "up"), Key("↓", "down"), Key("→", "right"),
            Key("Home", "home"), Key("Del", "delete"), Key("Pg↑", "pageup"),
            Key("Pg↓", "pagedown"), Key("📋", "snippet")
        ] {
            let button = makeButton(key)
            button.widthAnchor.constraint(greaterThanOrEqualToConstant: key.id == "mic" ? 39 : 47).isActive = true
            toolbar.addArrangedSubview(button)
        }
        vertical.addArrangedSubview(scroll)
    }

    private func render() {
        let (background, _, foreground) = palette()
        view.backgroundColor = background
        vertical.arrangedSubviews.forEach {
            vertical.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }
        addToolbar()
        popup.axis = .horizontal
        popup.spacing = 4
        popup.isHidden = true
        // Keep popup attached to the keyboard view; popups cannot extend beyond its bounds.
        if popup.superview != nil { popup.removeFromSuperview() }
        vertical.addArrangedSubview(popup)
        if !popupHeightAdded {
            popup.heightAnchor.constraint(equalToConstant: 35).isActive = true
            popupHeightAdded = true
        }

        switch layer {
        case .letters:
            let nums = (0...9).map { index -> Key in
                let digit = index == 9 ? "0" : String(index + 1)
                let label = digit + (accentedNumbers[digit].map { "\n" + $0 } ?? "")
                return Key(label, digit)
            }
            addRow(nums)
            addRow(Array("qwertyuiop").map { Key(shift || capsLock ? String($0).uppercased() : String($0), String($0)) })
            addRow(Array("asdfghjkl").map { Key(shift || capsLock ? String($0).uppercased() : String($0), String($0)) })
            let lowerChars = Array("zxcvbnm").map { String($0) }
            let lowerKeys: [Key] = lowerChars.map { character in
                let label = (shift || capsLock) ? character.uppercased() : character
                return Key(label, character)
            }
            var bottomKeys: [Key] = [Key(capsLock ? "⇪" : "⇧", "shift", 1.5)]
            bottomKeys.append(contentsOf: lowerKeys)
            bottomKeys.append(Key("⌫", "backspace", 1.5))
            addRow(bottomKeys)
        case .numbers:
            addRow(Array("1234567890").map { Key(String($0)) })
            addRow(["@", "#", "$", "%", "&", "-", "+", "(", ")", "/"].map { Key($0) })
            addRow(["*", "\"", "'", ":", ";", "!", "?", "=", "_", "\\"].map { Key($0) })
            addRow([Key("#+=", "symbols", 1.5), Key("<", "<"), Key(">", ">"),
                    Key("[", "["), Key("]", "]"), Key("{", "{"), Key("}", "}"),
                    Key("⌫", "backspace", 1.5)])
        case .symbols:
            addRow(["~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"].map { Key($0) })
            addRow(["£", "€", "¥", "^", "°", "=", "≠", "≤", "≥", "§"].map { Key($0) })
            addRow(["_", "—", "…", "©", "®", "™", "✓", "±", "%", "\\"].map { Key($0) })
            addRow([Key("123", "numbers", 1.5), Key("<", "<"), Key(">", ">"),
                    Key("[", "["), Key("]", "]"), Key("{", "{"), Key("}", "}"),
                    Key("⌫", "backspace", 1.5)])
        case .emoji:
            addRow(Array("😀😁😂🤣😊😍🥰😘😎😃").map { Key(String($0)) })
            addRow(Array("😢😭😡👍👎👏🙏💪🔥❤️").map { Key(String($0)) })
            addRow(Array("🎉✨✅❌💻⌨️🚀🐱🌍☕").map { Key(String($0)) })
            addRow([Key("ABC", "letters", 2), Key("😀", "emoji", 2), Key("⌫", "backspace", 2)])
        }
        addRow([
            Key(layer == .letters ? "123" : "ABC", layer == .letters ? "numbers" : "letters"),
            Key("🌐", "globe"), Key(layer == .emoji ? "ABC" : "😀", "emoji"),
            Key(",", "comma"), Key("space", "space", 4),
            Key(".", "period"), Key("↵", "return", 1.4)
        ])
        // Palette configured before key creation; used for future accessibility contrast tweaks.
        _ = foreground
    }

    @objc private func longPressed(_ recognizer: UILongPressGestureRecognizer) {
        guard recognizer.state == .began,
              let id = recognizer.view?.accessibilityIdentifier else { return }
        if id == "comma" {
            showChoices([",", ";", ":", "!", "?"])
        } else if id == "period" {
            showChoices([".", "…", "?", "!", "@"])
        } else if let letter = accentedNumbers[id] {
            showChoices([id, letter, letter.uppercased()])
        }
    }

    private func showChoices(_ options: [String]) {
        popup.arrangedSubviews.forEach { popup.removeArrangedSubview($0); $0.removeFromSuperview() }
        for option in options {
            let button = makeButton(Key(option, "choice:" + option))
            popup.addArrangedSubview(button)
        }
        popup.isHidden = false
        keyboardHeightConstraint.constant = 350
    }

    private func terminalSequence(_ key: String) -> String? {
        guard SharedSettings.profile == "terminal" else { return nil }
        return [
            "up": "\u{1B}[A", "down": "\u{1B}[B", "right": "\u{1B}[C",
            "left": "\u{1B}[D", "home": "\u{1B}[H",
            "pageup": "\u{1B}[5~", "pagedown": "\u{1B}[6~",
            "esc": "\u{1B}", "tab": "\t"
        ][key]
    }

    private func keyPressed(_ key: String) {
        let proxy = textDocumentProxy
        switch key {
        case "shift":
            if Date().timeIntervalSince(lastShiftTap) < 0.4 {
                capsLock.toggle()
                shift = capsLock
            } else {
                shift = !shift
                capsLock = false
            }
            lastShiftTap = Date()
        case "backspace":
            proxy.deleteBackward()
        case "delete":
            // UITextDocumentProxy has no forward-delete API; reposition then delete.
            proxy.adjustTextPosition(byCharacterOffset: 1)
            proxy.deleteBackward()
        case "ctrl": ctrl.toggle()
        case "alt": alt.toggle()
        case "globe", "mic":
            // Dictation belongs to the system keyboard. This switches keyboards.
            advanceToNextInputMode()
        case "123", "numbers": layer = .numbers
        case "#+=", "symbols": layer = .symbols
        case "ABC", "letters": layer = .letters
        case "emoji": layer = layer == .emoji ? .letters : .emoji
        case "snippet":
            let saved = SharedSettings.snippet
            if !saved.isEmpty { proxy.insertText(saved) }
        case "comma": proxy.insertText(",")
        case "period": proxy.insertText(".")
        case "space":
            let previous = (proxy.documentContextBeforeInput ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            proxy.insertText(" ")
            if [".", "!", "?"].contains(where: { previous.hasSuffix($0) }) {
                shift = true
            }
        case "return":
            proxy.insertText("\n")
            shift = true
        case "left", "right":
            if let sequence = terminalSequence(key) {
                proxy.insertText(sequence)
            } else {
                proxy.adjustTextPosition(byCharacterOffset: key == "left" ? -1 : 1)
            }
        case "up", "down", "home", "pageup", "pagedown", "esc", "tab":
            if let sequence = terminalSequence(key) { proxy.insertText(sequence) }
        default:
            let value = key.hasPrefix("choice:") ? String(key.dropFirst(7)) : key
            var text = value
            if value.count == 1, let char = value.first, char.isLetter {
                if shift || capsLock { text = value.uppercased() }
                if ctrl, let ascii = char.lowercased().utf8.first, (97...122).contains(ascii) {
                    text = String(UnicodeScalar(Int(ascii) - 96)!)
                }
                if !capsLock { shift = false }
            }
            if alt && SharedSettings.profile == "terminal" { text = "\u{1B}" + text }
            ctrl = false
            alt = false
            proxy.insertText(text)
        }
        popup.isHidden = true
        keyboardHeightConstraint.constant = 312
        render()
    }
}

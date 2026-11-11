import UIKit

/// Native iOS keyboard extension. No microphone, file or network access is requested.
private final class EmojiCell: UICollectionViewCell {
    let emojiLabel = UILabel()
    override init(frame: CGRect) {
        super.init(frame: frame)
        emojiLabel.translatesAutoresizingMaskIntoConstraints = false
        emojiLabel.font = .systemFont(ofSize: 27)
        emojiLabel.textAlignment = .center
        contentView.addSubview(emojiLabel)
        NSLayoutConstraint.activate([
            emojiLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            emojiLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            emojiLabel.topAnchor.constraint(equalTo: contentView.topAnchor),
            emojiLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor)
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }
}

/// Records the original touch location for the three zones inside Space.
private final class SpaceKeyButton: UIButton {
    private(set) var touchFraction: CGFloat = 0.5

    override func beginTracking(_ touch: UITouch, with event: UIEvent?) -> Bool {
        touchFraction = touch.location(in: self).x / max(bounds.width, 1)
        return super.beginTracking(touch, with: event)
    }
}

final class KeyboardViewController: UIInputViewController, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
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
    private var emojiCategory = 1
    private var emojiItems: [[String]] = []
    private weak var emojiGrid: UICollectionView?
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
        if layer != .emoji { render() }
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
        let button: UIButton = key.id == "space" ? SpaceKeyButton(frame: .zero) : UIButton(type: .system)
        button.setTitle(key.id == "space" ? nil : key.label, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: key.label.contains("\n") ? 14 : 17, weight: .medium)
        button.titleLabel?.numberOfLines = 2
        button.titleLabel?.textAlignment = .center
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.setTitleColor(foreground, for: .normal)
        button.backgroundColor = buttonColor
        button.layer.cornerRadius = 6
        button.accessibilityIdentifier = key.id
        if key.id == "shift" {
            button.accessibilityLabel = capsLock ? "Caps Lock on. Tap to disable" : "Shift. Double tap or hold for Caps Lock"
        }
        if key.id == "space" {
            button.accessibilityLabel = "Less than, space, greater than"
            let labels = UIStackView()
            labels.axis = .horizontal
            labels.distribution = .equalCentering
            labels.alignment = .center
            labels.isUserInteractionEnabled = false
            labels.translatesAutoresizingMaskIntoConstraints = false
            for title in ["<", "space", ">"] {
                let item = UILabel()
                item.text = title
                item.textColor = foreground
                item.font = .systemFont(ofSize: 15, weight: .medium)
                item.textAlignment = .center
                item.numberOfLines = 1
                item.adjustsFontSizeToFitWidth = true
                item.minimumScaleFactor = 0.8
                labels.addArrangedSubview(item)
            }
            button.addSubview(labels)
            NSLayoutConstraint.activate([
                labels.leadingAnchor.constraint(equalTo: button.leadingAnchor, constant: 10),
                labels.trailingAnchor.constraint(equalTo: button.trailingAnchor, constant: -10),
                labels.centerYAnchor.constraint(equalTo: button.centerYAnchor)
            ])
        }
        button.addAction(UIAction { [weak self, weak button] _ in
            guard let self = self else { return }
            if let space = button as? SpaceKeyButton {
                let id = space.touchFraction < 0.22 ? "spaceLess" :
                         (space.touchFraction > 0.78 ? "spaceGreater" : "space")
                self.keyPressed(id)
            } else {
                self.keyPressed(key.id)
            }
        }, for: .touchUpInside)
        if ["comma", "period", "shift"].contains(key.id) || accentedNumbers[key.id] != nil {
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

    private func addEmojiPanel() {
        let available = EmojiCatalog.groups
        guard !available.isEmpty else {
            addRow([Key("ABC", "letters")])
            return
        }
        let categoryScroll = UIScrollView()
        categoryScroll.showsHorizontalScrollIndicator = false
        categoryScroll.heightAnchor.constraint(equalToConstant: 39).isActive = true
        let categories = UIStackView()
        categories.axis = .horizontal
        categories.spacing = 3
        categories.translatesAutoresizingMaskIntoConstraints = false
        categoryScroll.addSubview(categories)
        NSLayoutConstraint.activate([
            categories.leadingAnchor.constraint(equalTo: categoryScroll.contentLayoutGuide.leadingAnchor),
            categories.trailingAnchor.constraint(equalTo: categoryScroll.contentLayoutGuide.trailingAnchor),
            categories.topAnchor.constraint(equalTo: categoryScroll.contentLayoutGuide.topAnchor),
            categories.bottomAnchor.constraint(equalTo: categoryScroll.contentLayoutGuide.bottomAnchor),
            categories.heightAnchor.constraint(equalTo: categoryScroll.frameLayoutGuide.heightAnchor)
        ])
        for (index, icon) in EmojiCatalog.icons.enumerated() {
            let button = makeButton(Key(icon, "emojiGroup:" + String(index)))
            button.alpha = index == emojiCategory ? 1 : 0.65
            button.widthAnchor.constraint(equalToConstant: 41).isActive = true
            categories.addArrangedSubview(button)
        }
        vertical.addArrangedSubview(categoryScroll)

        let flow = UICollectionViewFlowLayout()
        flow.scrollDirection = .vertical
        flow.minimumInteritemSpacing = 1
        flow.minimumLineSpacing = 2
        let collection = UICollectionView(frame: .zero, collectionViewLayout: flow)
        collection.register(EmojiCell.self, forCellWithReuseIdentifier: "emoji")
        collection.dataSource = self
        collection.delegate = self
        collection.keyboardDismissMode = .none
        collection.backgroundColor = .clear
        collection.showsVerticalScrollIndicator = false
        collection.heightAnchor.constraint(equalToConstant: 221).isActive = true
        emojiGrid = collection
        emojiItems = emojiCategory == 0 ? EmojiCatalog.recent() : available[emojiCategory - 1].items
        vertical.addArrangedSubview(collection)
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        emojiItems.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "emoji", for: indexPath) as! EmojiCell
        let item = emojiItems[indexPath.item]
        cell.emojiLabel.text = item[0]
        cell.isAccessibilityElement = true
        cell.accessibilityLabel = item[1]
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let selected = emojiItems[indexPath.item][0]
        textDocumentProxy.insertText(selected)
        EmojiCatalog.remember(selected)
        if emojiCategory == 0 {
            emojiItems = EmojiCatalog.recent()
            collectionView.reloadData()
        }
    }

    func collectionView(_ collectionView: UICollectionView, layout: UICollectionViewLayout,
                        sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = max(30, floor((collectionView.bounds.width - 14) / 8))
        return CGSize(width: width, height: 40)
    }

    private func render() {
        let (background, _, foreground) = palette()
        keyboardHeightConstraint.constant = layer == .emoji ? 366 : 312
        view.backgroundColor = background
        emojiGrid = nil
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
            addEmojiPanel()
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
        if id == "shift" {
            capsLock.toggle()
            shift = capsLock
            lastShiftTap = .distantPast
            render()
        } else if id == "comma" {
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
        case let group where group.hasPrefix("emojiGroup:"):
            if let index = Int(group.dropFirst("emojiGroup:".count)),
               (0...EmojiCatalog.groups.count).contains(index) {
                emojiCategory = index
                emojiItems = index == 0 ? EmojiCatalog.recent() : EmojiCatalog.groups[index - 1].items
                emojiGrid?.reloadData()
                emojiGrid?.setContentOffset(.zero, animated: false)
            }
        case "snippet":
            let saved = SharedSettings.snippet
            if !saved.isEmpty { proxy.insertText(saved) }
        case "comma": proxy.insertText(",")
        case "period": proxy.insertText(".")
        case "spaceLess": proxy.insertText("<")
        case "spaceGreater": proxy.insertText(">")
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

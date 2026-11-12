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
    private var repeatTimer: Timer?
    private var heldKey: String?
    private var repeated = false
    private var repeatTicks = 0
    private var heldModifiers = (false, false, false)
    private var fn = SharedSettings.functionKeys
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
        fn = SharedSettings.functionKeys
        if SharedSettings.profile == "terminal" { shift = false }
        render()
    }

    override func viewWillDisappear(_ animated: Bool) {
        stopRepeat()
        super.viewWillDisappear(animated)
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        let before = textDocumentProxy.documentContextBeforeInput ?? ""
        if !capsLock && SharedSettings.profile != "terminal" && (before.isEmpty || before.hasSuffix("\n")) { shift = true }
        if layer != .emoji && heldKey == nil { render() }
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
            button.accessibilityLabel = "Space. Hold to choose language. Brackets are decorative"
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
        button.addAction(UIAction { [weak self] _ in
            guard let self else { return }
            self.stopRepeat()
            if self.repeated { self.repeated = false; return }
            self.keyPressed(key.id)
        }, for: .touchUpInside)
        button.addAction(UIAction { [weak self, weak button] _ in
            guard let self else { return }
            self.repeated = false
            if SharedSettings.pressAnimation, let button {
                UIView.animate(withDuration: 0.07) { button.transform = CGAffineTransform(scaleX: 0.95, y: 0.9); button.alpha = 0.79 }
            }
            if ["left", "right", "up", "down", "backspace"].contains(key.id) { self.startRepeat(key.id) }
        }, for: .touchDown)
        button.addAction(UIAction { [weak self, weak button] _ in
            self?.stopRepeat()
            if let button { UIView.animate(withDuration: 0.12) { button.transform = .identity; button.alpha = 1 } }
        }, for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        if ["comma", "period", "shift", "space", "mic"].contains(key.id) ||
            (layer == .letters && SharedSettings.language == "lt-LT" && SharedSettings.variant == "pc" && accentedNumbers[key.id] != nil) {
            let hold = UILongPressGestureRecognizer(target: self, action: #selector(longPressed(_:)))
            hold.minimumPressDuration = 0.5
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
        let toolbarKeys = [Key("Fn", "fn"), Key("📋", "clipboard"), Key("⭐", "snippet"),
                           Key(SharedSettings.language == "lt-LT" ? "LT 🎙" : "EN 🎙", "mic"),
                           Key("LT/EN", "language"), Key("🌐", "globe")]
        for key in toolbarKeys {
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
        stopRepeat()
        addToolbar()
        if fn { addRow((1...8).map { Key("F\($0)") }) }
        let combos = SharedSettings.combos.filter { $0.enabled }
        if !combos.isEmpty {
            let scroll = UIScrollView(); scroll.showsHorizontalScrollIndicator = false
            scroll.heightAnchor.constraint(equalToConstant: 35).isActive = true
            let strip = UIStackView(); strip.axis = .horizontal; strip.spacing = 4
            strip.translatesAutoresizingMaskIntoConstraints = false; scroll.addSubview(strip)
            for combo in combos {
                let button = makeButton(Key(combo.name, "combo:" + combo.id.uuidString))
                button.widthAnchor.constraint(equalToConstant: 92).isActive = true; strip.addArrangedSubview(button)
            }
            NSLayoutConstraint.activate([strip.leadingAnchor.constraint(equalTo: scroll.contentLayoutGuide.leadingAnchor),
                strip.trailingAnchor.constraint(equalTo: scroll.contentLayoutGuide.trailingAnchor),
                strip.topAnchor.constraint(equalTo: scroll.contentLayoutGuide.topAnchor), strip.bottomAnchor.constraint(equalTo: scroll.contentLayoutGuide.bottomAnchor),
                strip.heightAnchor.constraint(equalTo: scroll.frameLayoutGuide.heightAnchor)])
            vertical.addArrangedSubview(scroll)
        }
        if layer != .emoji {
            for row in SharedSettings.rows.prefix(2) { addRow(row.map(mappedKey)) }
        }
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
            for row in SharedSettings.rows.dropFirst(2).dropLast() { addRow(row.map(mappedKey)) }
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
        if layer == .letters { addRow(SharedSettings.rows.last!.map(mappedKey)) }
        else { addRow([Key("ABC", "letters"), Key("🌐", "globe"), Key("😀", "emoji"),
                       Key(",", "comma"), Key("space", "space", 4), Key(".", "period"), Key("↵", "return")]) }
        keyboardHeightConstraint.constant = CGFloat(vertical.arrangedSubviews.filter { !$0.isHidden }.count * 50 + 10) + (layer == .emoji ? 171 : 0)
        // Palette configured before key creation; used for future accessibility contrast tweaks.
        _ = foreground
    }

    @objc private func longPressed(_ recognizer: UILongPressGestureRecognizer) {
        guard recognizer.state == .began,
              let id = recognizer.view?.accessibilityIdentifier else { return }
        if id == "space" || id == "mic" {
            showChoices(["language:lt-LT", "language:en-US"])
        } else if id == "shift" {
            capsLock.toggle()
            shift = false
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
            let title = option == "language:lt-LT" ? "Lithuanian" : option == "language:en-US" ? "English" : option
            let button = makeButton(Key(title, option.hasPrefix("language:") ? option : "choice:" + option))
            popup.addArrangedSubview(button)
        }
        popup.isHidden = false
        keyboardHeightConstraint.constant += 40
    }

    private func terminalSequence(_ key: String) -> String? {
        guard SharedSettings.profile == "terminal" else { return nil }
        return KeyboardMapping.terminalSequence(key, ctrl: ctrl, alt: alt, shift: shift)
    }

    private func mappedKey(_ label: String) -> Key {
        let ids = ["Esc": "esc", "Ctrl": "ctrl", "Alt": "alt", "Tab": "tab", "Del": "delete", "End": "end",
                   "PgUp": "pageup", "PgDn": "pagedown", "Home": "home", "←": "left", "→": "right", "↑": "up", "↓": "down",
                   "⇧": "shift", "⌫": "backspace", "Space": "space", "↵": "return", "😀": "emoji", ",": "comma", ".": "period"]
        var title = KeyboardMapping.output(label, variant: SharedSettings.variant, shift: shift, caps: capsLock)
        if label == "⇧" { title = capsLock ? "⇪" : "⇧" }
        if layer == .letters && SharedSettings.language == "lt-LT" && SharedSettings.variant == "pc", let hint = accentedNumbers[label] { title = hint + "\n" + title }
        return Key(title, ids[label] ?? label, label == "Space" ? 4 : 1)
    }

    private func setLanguage(_ value: String) {
        SharedSettings.sessionLanguage = value
        if hasFullAccess { SharedSettings.language = value }
    }

    private func startRepeat(_ key: String) {
        stopRepeat(); heldKey = key; repeatTicks = 0; heldModifiers = (ctrl, alt, shift)
        repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.065, repeats: true) { [weak self] _ in
            guard let self, let held = self.heldKey else { return }
            self.repeatTicks += 1
            if self.repeatTicks < 7 { return }
            self.repeated = true
            (self.ctrl, self.alt, self.shift) = self.heldModifiers
            self.keyPressed(held, refresh: false)
        }
    }
    private func stopRepeat() { repeatTimer?.invalidate(); repeatTimer = nil; heldKey = nil }

    private func showItems(_ items: [String]) {
        if items.isEmpty { showMessage("No saved items. Add snippets in the DevBoard app."); return }
        popup.arrangedSubviews.forEach { popup.removeArrangedSubview($0); $0.removeFromSuperview() }
        let scroll = UIScrollView()
        let list = UIStackView(); list.axis = .horizontal; list.spacing = 4
        list.translatesAutoresizingMaskIntoConstraints = false; scroll.addSubview(list)
        for item in items {
            let button = makeButton(Key(String(item.prefix(24)), "choice:" + item))
            button.widthAnchor.constraint(equalToConstant: 160).isActive = true; list.addArrangedSubview(button)
        }
        NSLayoutConstraint.activate([list.leadingAnchor.constraint(equalTo: scroll.contentLayoutGuide.leadingAnchor),
            list.trailingAnchor.constraint(equalTo: scroll.contentLayoutGuide.trailingAnchor),
            list.topAnchor.constraint(equalTo: scroll.contentLayoutGuide.topAnchor), list.bottomAnchor.constraint(equalTo: scroll.contentLayoutGuide.bottomAnchor),
            list.heightAnchor.constraint(equalTo: scroll.frameLayoutGuide.heightAnchor)])
        popup.addArrangedSubview(scroll); popup.isHidden = false; keyboardHeightConstraint.constant += 40
    }
    private func showMessage(_ message: String) {
        popup.arrangedSubviews.forEach { popup.removeArrangedSubview($0); $0.removeFromSuperview() }
        let label = UILabel(); label.text = message; label.font = .systemFont(ofSize: 11); label.numberOfLines = 2
        label.textColor = palette().2; popup.addArrangedSubview(label); popup.isHidden = false; keyboardHeightConstraint.constant += 40
    }
    private func runCombo(_ combo: SavedCombo) {
        let aliases = ["Enter": "return", "Esc": "esc", "Space": "space"]
        let key = aliases[combo.key] ?? mappedKey(combo.key).id
        if SharedSettings.profile != "terminal" && (combo.ctrl || combo.alt || combo.shift || key.hasPrefix("F")) {
            showMessage("This shortcut requires a compatible terminal. iOS cannot send hardware shortcuts."); return
        }
        ctrl = combo.ctrl; alt = combo.alt; shift = combo.shift
        keyPressed(key)
        ctrl = false; alt = false; if !capsLock { shift = false }; render()
    }

    private func keyPressed(_ key: String, refresh: Bool = true) {
        let proxy = textDocumentProxy
        if key.hasPrefix("choice:") {
            proxy.insertText(String(key.dropFirst(7))); ctrl = false; alt = false; if !capsLock { shift = false }; render(); return
        }
        if key.hasPrefix("combo:"), let combo = SharedSettings.combos.first(where: { "combo:" + $0.id.uuidString == key }) { runCombo(combo); return }
        if key.hasPrefix("language:") { setLanguage(String(key.dropFirst(9))); render(); return }
        switch key {
        case "fn":
            fn.toggle(); if hasFullAccess { SharedSettings.functionKeys = fn }
        case "language":
            setLanguage(SharedSettings.language == "lt-LT" ? "en-US" : "lt-LT")
        case "clipboard":
            render()
            guard hasFullAccess else {
                let extensionInfo = Bundle.main.object(forInfoDictionaryKey: "NSExtension") as? [String: Any]
                let attributes = extensionInfo?["NSExtensionAttributes"] as? [String: Any]
                showMessage(attributes?["RequestsOpenAccess"] as? Bool == true ? "Clipboard requires Allow Full Access in iOS keyboard settings." : "Clipboard is disabled: this build does not request Full Access.")
                return
            }
            let current = UIPasteboard.general.string ?? ""
            if SharedSettings.captureClipboard && !current.isEmpty {
                SharedSettings.clips = [String(current.prefix(2000))] + SharedSettings.clips.filter { $0 != current }
            }
            showItems((current.isEmpty ? [] : [current]) + SharedSettings.clips.filter { $0 != current }); return
        case "shift":
            if capsLock { capsLock = false; shift = false; lastShiftTap = .distantPast }
            else if Date().timeIntervalSince(lastShiftTap) < 0.42 { capsLock = true; shift = false; lastShiftTap = .distantPast }
            else { shift.toggle(); lastShiftTap = Date() }
        case "backspace":
            proxy.deleteBackward()
        case "delete":
            if let sequence = terminalSequence(key) { proxy.insertText(sequence) }
            else if let after = proxy.documentContextAfterInput, !after.isEmpty {
                // At the document end do nothing; moving there then deleting would erase the previous character.
                proxy.adjustTextPosition(byCharacterOffset: 1); proxy.deleteBackward()
            }
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
            render(); showItems(SharedSettings.snippets); return
        case "comma": proxy.insertText(KeyboardMapping.output(",", variant: SharedSettings.variant, shift: layer == .letters && shift, caps: false)); if !capsLock { shift = false }
        case "period": proxy.insertText(KeyboardMapping.output(".", variant: SharedSettings.variant, shift: layer == .letters && shift, caps: false)); if !capsLock { shift = false }
        case "space":
            let previous = (proxy.documentContextBeforeInput ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            proxy.insertText(" ")
            shift = false
            if !capsLock && SharedSettings.profile != "terminal" && [".", "!", "?"].contains(where: { previous.hasSuffix($0) }) {
                shift = true
            }
        case "return":
            proxy.insertText("\n")
            shift = SharedSettings.profile != "terminal" && !capsLock
        case "left", "right":
            if let sequence = terminalSequence(key) {
                proxy.insertText(sequence)
            } else {
                proxy.adjustTextPosition(byCharacterOffset: key == "left" ? -1 : 1)
            }
        case "up", "down", "home", "end", "pageup", "pagedown", "esc", "tab", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8":
            if let sequence = terminalSequence(key) { proxy.insertText(sequence) }
            else if key == "tab" { proxy.insertText("\t") }
            else {
                let before = proxy.documentContextBeforeInput ?? ""
                let after = proxy.documentContextAfterInput ?? ""
                if key == "home" { proxy.adjustTextPosition(byCharacterOffset: -(before.components(separatedBy: "\n").last?.count ?? 0)) }
                else if key == "end" { proxy.adjustTextPosition(byCharacterOffset: after.components(separatedBy: "\n").first?.count ?? 0) }
                else { if refresh { render(); showMessage("iOS cannot send this key outside a compatible terminal.") }; return }
            }
            ctrl = false; alt = false
        default:
            let value = key.hasPrefix("choice:") ? String(key.dropFirst(7)) : key
            var text = key.hasPrefix("choice:") ? value : KeyboardMapping.output(value, variant: layer == .letters ? SharedSettings.variant : "qwerty", shift: shift, caps: capsLock)
            if value.count == 1, let char = value.first, char.isLetter {
                if !key.hasPrefix("choice:") && (shift || capsLock) { text = value.uppercased() }
                if ctrl, let ascii = char.lowercased().utf8.first, (97...122).contains(ascii) {
                    text = String(UnicodeScalar(Int(ascii) - 96)!)
                }
                if !capsLock { shift = false }
            }
            if ctrl && SharedSettings.profile != "terminal" { render(); showMessage("Ctrl shortcuts require a compatible terminal on iOS."); ctrl = false; alt = false; return }
            if !capsLock { shift = false }
            if alt && SharedSettings.profile == "terminal" { text = "\u{1B}" + text }
            ctrl = false
            alt = false
            proxy.insertText(text)
        }
        if !["shift", "ctrl", "alt"].contains(key) && !key.hasPrefix("emojiGroup:") { ctrl = false; alt = false }
        popup.isHidden = true
        if refresh { render() }
    }
}

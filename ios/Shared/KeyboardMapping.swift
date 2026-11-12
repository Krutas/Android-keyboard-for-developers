import Foundation

/// Key identifiers remain stable when their displayed Shift legend changes.
enum KeyboardMapping {
    static let ltRows = [
        ["ą", "ž", "e", "r", "t", "y", "u", "i", "o", "p", "į", "w"],
        ["a", "s", "d", "š", "g", "h", "j", "k", "l", "ų", "ė", "q"],
        ["⇧", "z", "ū", "c", "v", "b", "n", "m", "č", "f", "x", "ę", "⌫"]
    ]
    static let baseRows = [
        ["Esc", "Ctrl", "Alt", "Tab", "Del", "End", "PgUp", "PgDn"],
        ["←", "↓", "↑", "→", "Home", "/", "⌫"],
        Array("1234567890").map(String.init),
        Array("qwertyuiop").map(String.init),
        Array("asdfghjkl").map(String.init),
        ["⇧"] + Array("zxcvbnm").map(String.init) + ["⌫"],
        ["123", "😀", ",", "Space", ".", "↵"]
    ]
    static let shifted = Dictionary(uniqueKeysWithValues: zip(Array("1234567890-=[]\\;',./`"), Array("!@#$%^&*()_+{}|:\"<>?~")).map { (String($0), String($1)) })
    static func output(_ key: String, variant: String, shift: Bool, caps: Bool) -> String {
        if key.count == 1, let c = key.first, c.isLetter { return shift || caps ? key.uppercased() : key.lowercased() }
        return variant == "pc" && shift ? (shifted[key] ?? key) : key
    }
    static func initialRows(language: String, variant: String) -> [[String]] {
        var rows = baseRows
        if language == "lt-LT" && variant == "qwerty" { for i in 0..<3 { rows[i + 3] = ltRows[i] } }
        return rows
    }
    static func terminalSequence(_ key: String, ctrl: Bool = false, alt: Bool = false, shift: Bool = false) -> String? {
        let modifier = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0)
        let cursor = ["left": "D", "right": "C", "up": "A", "down": "B", "home": "H", "end": "F"]
        if let suffix = cursor[key] { return modifier == 1 ? "\u{1B}[" + suffix : "\u{1B}[1;\(modifier)" + suffix }
        let tilde = ["insert": 2, "delete": 3, "pageup": 5, "pagedown": 6, "F5": 15, "F6": 17, "F7": 18, "F8": 19]
        if let number = tilde[key] { return "\u{1B}[\(number)" + (modifier == 1 ? "~" : ";\(modifier)~") }
        if let index = ["F1", "F2", "F3", "F4"].firstIndex(of: key) {
            let suffix = ["P", "Q", "R", "S"][index]
            return modifier == 1 ? "\u{1B}O" + suffix : "\u{1B}[1;\(modifier)" + suffix
        }
        if key == "esc" { return "\u{1B}" }
        if key == "tab" { return shift ? "\u{1B}[Z" : "\t" }
        return nil
    }
}

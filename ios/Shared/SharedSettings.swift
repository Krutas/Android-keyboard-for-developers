import Foundation

/// Host app writes preferences; keyboard extension reads them from the App Group.
enum SharedSettings {
    // Optional Full Access expansion awaits explicit user approval.
    static let requestsFullAccess = false
    static var sessionLanguage: String?
    static let appGroupID = "group.lt.krutas.devboard"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroupID) ?? .standard
    }

    static var profile: String {
        get { defaults.string(forKey: "selectedProfile") ?? "terminal" }
        set { defaults.set(newValue, forKey: "selectedProfile") }
    }

    static var theme: String {
        get { defaults.string(forKey: "selectedTheme") ?? "dark" }
        set { defaults.set(newValue, forKey: "selectedTheme") }
    }

    static var snippet: String {
        get { defaults.string(forKey: "savedSnippet") ?? "" }
        set { defaults.set(newValue, forKey: "savedSnippet") }
    }
}

struct SavedCombo: Codable, Identifiable {
    var id = UUID()
    var name: String
    var key: String
    var ctrl = false
    var alt = false
    var shift = false
    var enabled = true
    var summary: String { ([ctrl ? "Ctrl" : nil, alt ? "Alt" : nil, shift ? "Shift" : nil, key].compactMap { $0 }).joined(separator: "+") }
}

extension SharedSettings {
    static var language: String {
        get { sessionLanguage ?? defaults.string(forKey: "language") ?? "lt-LT" }
        set { defaults.set(newValue, forKey: "language") }
    }
    static var variant: String {
        get { defaults.string(forKey: "variant_" + language) ?? (language == "lt-LT" ? "pc" : "qwerty") }
        set { defaults.set(newValue, forKey: "variant_" + language) }
    }
    static var functionKeys: Bool {
        get { defaults.bool(forKey: "functionKeys") }
        set { defaults.set(newValue, forKey: "functionKeys") }
    }
    static var pressAnimation: Bool {
        get { defaults.object(forKey: "pressAnimation") as? Bool ?? true }
        set { defaults.set(newValue, forKey: "pressAnimation") }
    }
    static var captureClipboard: Bool {
        get { defaults.bool(forKey: "captureClipboard") }
        set { defaults.set(newValue, forKey: "captureClipboard") }
    }
    static var snippets: [String] {
        get { defaults.stringArray(forKey: "snippets") ?? (snippet.isEmpty ? [] : [snippet]) }
        set { defaults.set(Array(newValue.prefix(30)), forKey: "snippets") }
    }
    static var clips: [String] {
        get { defaults.stringArray(forKey: "clips") ?? [] }
        set { defaults.set(Array(newValue.prefix(20)), forKey: "clips") }
    }
    static var combos: [SavedCombo] {
        get { (defaults.data(forKey: "combos").flatMap { try? JSONDecoder().decode([SavedCombo].self, from: $0) }) ?? [] }
        set { defaults.set(try? JSONEncoder().encode(Array(newValue.prefix(24))), forKey: "combos") }
    }
    private static var rowsKey: String { "rows_\(profile)_\(language)_\(variant)" }
    static var rows: [[String]] {
        get {
            if let rows = defaults.array(forKey: rowsKey) as? [[String]], rows.count == 7,
               rows.allSatisfy({ (3...16).contains($0.count) && $0.allSatisfy({ !$0.isEmpty && $0.count <= 24 }) }) { return rows }
            return KeyboardMapping.initialRows(language: language, variant: variant)
        }
        set { defaults.set(newValue, forKey: rowsKey) }
    }
}

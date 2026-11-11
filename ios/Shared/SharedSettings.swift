import Foundation

/// Host app writes preferences; keyboard extension reads them from the App Group.
enum SharedSettings {
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

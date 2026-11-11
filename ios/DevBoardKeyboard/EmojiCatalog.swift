import Foundation

/// The same offline Unicode RGI emoji catalog used by Android and the simulator.
enum EmojiCatalog {
    struct Group: Decodable {
        let name: String
        let items: [[String]] // [emoji, Unicode name]
    }
    private struct Document: Decodable {
        let version: String
        let groups: [Group]
    }

    static let icons = ["🕘", "😀", "🧑", "🐻", "🍔", "🚗", "⚽", "💡", "❤️", "🏳️"]
    static let groups: [Group] = {
        guard let path = Bundle(for: KeyboardViewController.self).url(forResource: "emoji", withExtension: "json"),
              let data = try? Data(contentsOf: path),
              let parsed = try? JSONDecoder().decode(Document.self, from: data) else {
            return []
        }
        return parsed.groups
    }()

    static func recent() -> [[String]] {
        let values = UserDefaults.standard.stringArray(forKey: "recentEmoji") ?? []
        let byValue = Dictionary(uniqueKeysWithValues: groups.flatMap { $0.items }.map { ($0[0], $0) })
        return values.compactMap { byValue[$0] }
    }

    static func remember(_ value: String) {
        var values = UserDefaults.standard.stringArray(forKey: "recentEmoji") ?? []
        values.removeAll { $0 == value }
        values.insert(value, at: 0)
        UserDefaults.standard.set(Array(values.prefix(48)), forKey: "recentEmoji")
    }
}

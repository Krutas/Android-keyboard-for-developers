import Foundation

@main
struct MappingTests {
    static func main() {
        for (key, symbol) in KeyboardMapping.shifted {
            precondition(KeyboardMapping.output(key, variant: "pc", shift: true, caps: false) == symbol)
            precondition(KeyboardMapping.output(key, variant: "pc", shift: false, caps: true) == key)
            precondition(KeyboardMapping.output(key, variant: "qwerty", shift: true, caps: false) == key)
        }
        for key in "ąčęėįšųūžqwerty" {
            precondition(KeyboardMapping.output(String(key), variant: "pc", shift: false, caps: true) == String(key).uppercased())
        }
        for language in ["lt-LT", "en-US"] { for variant in ["pc", "qwerty"] {
            let rows = KeyboardMapping.initialRows(language: language, variant: variant)
            precondition(rows.count == 7)
            if language == "lt-LT" && variant == "qwerty" {
                let letters = rows[3...5].flatMap { $0 }.joined()
                for letter in "abcdefghijklmnopqrstuvwxyząčęėįšųūž" { precondition(letters.contains(letter)) }
            } else { precondition(rows == KeyboardMapping.baseRows) }
        } }
        precondition(KeyboardMapping.terminalSequence("F8") == "\u{1B}[19~")
        precondition(KeyboardMapping.terminalSequence("left", ctrl: true, shift: true) == "\u{1B}[1;6D")
        precondition(KeyboardMapping.terminalSequence("end") == "\u{1B}[F")
        precondition(KeyboardMapping.terminalSequence("unknown") == nil)
        print("Swift mapping tests passed")
    }
}

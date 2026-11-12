import SwiftUI

struct KeyboardPreview: View {
    let rows: [[String]]
    let theme: String
    let functionKeys: Bool
    let combos: [SavedCombo]
    var body: some View {
        VStack(spacing: 3) {
            previewRow(["⚙", "+ Combo", "Fn", "📋", "⭐", SharedSettings.language == "lt-LT" ? "LT 🎙" : "EN 🎙"])
            if functionKeys { previewRow((1...8).map { "F\($0)" }) }
            ForEach(Array(combos.filter(\.enabled).enumerated()), id: \.offset) { _, combo in previewRow([combo.name]) }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in previewRow(row) }
        }.padding(5).background(theme == "light" ? Color.gray.opacity(0.15) : theme == "matrix" ? Color.green.opacity(0.2) : Color.black.opacity(0.3)).accessibilityLabel("Live keyboard preview")
    }
    private func previewRow(_ row: [String]) -> some View {
        HStack(spacing: 2) {
            ForEach(Array(row.enumerated()), id: \.offset) { _, key in
                Text(key == "Space" ? "< Space >" : KeyboardMapping.output(key, variant: SharedSettings.variant, shift: false, caps: false))
                    .font(.system(size: 9, design: .monospaced)).lineLimit(1).minimumScaleFactor(0.5)
                    .frame(maxWidth: .infinity, minHeight: 24).background(Color.secondary.opacity(0.2)).cornerRadius(3)
            }
        }
    }
}

struct LayoutEditor: View {
    @Binding var rows: [[String]]
    @State private var selected: [Int]?
    @State private var reset = false
    var body: some View {
        ScrollView {
            Text("Tap two keys to swap. Hold a key for Delete or Reset.").padding()
            ForEach(rows.indices, id: \.self) { r in
                HStack(spacing: 2) {
                    ForEach(rows[r].indices, id: \.self) { c in
                        Button(rows[r][c]) {
                            if let first = selected {
                                let old = rows[first[0]][first[1]]
                                rows[first[0]][first[1]] = rows[r][c]; rows[r][c] = old
                                selected = nil; SharedSettings.rows = rows
                            } else { selected = [r, c] }
                        }
                        .font(.system(size: 11)).frame(maxWidth: .infinity, minHeight: 44)
                        .background(selected == [r, c] ? Color.blue.opacity(0.5) : Color.gray.opacity(0.2))
                        .contextMenu {
                            Button("Delete key", role: .destructive) {
                                rows[r].remove(at: c); selected = nil; SharedSettings.rows = rows
                            }.disabled(rows[r].count <= 3)
                            Button("Reset to defaults", role: .destructive) { reset = true }
                        }
                    }
                }
            }
            Button("Reset to defaults", role: .destructive) { reset = true }.padding()
        }.navigationTitle("Edit \(SharedSettings.variant.uppercased())")
        .confirmationDialog("Reset this layout? Custom positions and deletions will be lost.", isPresented: $reset, titleVisibility: .visible) {
            Button("Reset", role: .destructive) {
                rows = KeyboardMapping.initialRows(language: SharedSettings.language, variant: SharedSettings.variant)
                selected = nil; SharedSettings.rows = rows
            }
        }
    }
}

struct ComboEditor: View {
    @Binding var combos: [SavedCombo]
    @State private var draft = SavedCombo(name: "", key: "")
    @State private var editing: UUID?
    private let keys = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890").map(String.init) + ["Tab", "Enter", "Esc", "Space", "⌫", "Del", "Home", "End", "←", "↑", "↓", "→"] + (1...8).map { "F\($0)" }
    var body: some View {
        Form {
            TextField("Button name", text: $draft.name)
            Text("Action: \(draft.summary)")
            Toggle("Ctrl", isOn: $draft.ctrl)
            Toggle("Alt", isOn: $draft.alt)
            Toggle("Shift", isOn: $draft.shift)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6)) {
                ForEach(keys, id: \.self) { key in Button(key) { draft.key = key }.buttonStyle(.bordered) }
            }
            Button("Save combo") {
                if let id = editing, let i = combos.firstIndex(where: { $0.id == id }) { combos[i] = draft }
                else if combos.count < 24 { combos.append(draft) }
                SharedSettings.combos = combos; draft = SavedCombo(name: "", key: ""); editing = nil
            }.disabled(draft.key.isEmpty || draft.name.trimmingCharacters(in: .whitespaces).isEmpty)
            ForEach(combos) { combo in
                HStack {
                    Button("\(combo.name) · \(combo.summary)") { draft = combo; editing = combo.id }
                    Toggle("Enabled", isOn: Binding(get: { combos.first(where: { $0.id == combo.id })?.enabled ?? false }, set: { enabled in
                        if let i = combos.firstIndex(where: { $0.id == combo.id }) { combos[i].enabled = enabled; SharedSettings.combos = combos }
                    })).labelsHidden()
                    Button("Delete") {
                        combos.removeAll { $0.id == combo.id }; SharedSettings.combos = combos
                        draft = SavedCombo(name: "", key: ""); editing = nil
                    }
                }
            }
            Text("Terminal profile sends ANSI/control text to compatible terminal apps. iOS cannot send universal hardware shortcuts or access the host editing menu. Standard profile shows an explanation for unsupported combos.").font(.caption)
        }.buttonStyle(.borderless).navigationTitle("+ Combo")
    }
}

import SwiftUI
import UniformTypeIdentifiers

@main
struct DevBoardApp: App {
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

struct ContentView: View {
    @State private var profile = SharedSettings.profile
    @State private var theme = SharedSettings.theme
    @State private var snippet = SharedSettings.snippet
    @State private var language = SharedSettings.language
    @State private var variant = SharedSettings.variant
    @State private var functionKeys = SharedSettings.functionKeys
    @State private var animation = SharedSettings.pressAnimation
    @State private var captureClipboard = SharedSettings.captureClipboard
    @State private var snippets = SharedSettings.snippets
    @State private var snippetIndex: Int?
    @State private var combos = SharedSettings.combos
    @State private var rows = SharedSettings.rows
    @State private var showFilePicker = false
    @State private var sharedFile: URL?
    @State private var importError: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Enable DevBoard") {
                    Text("1. Open Settings → General → Keyboard → Keyboards.")
                    Text("2. Tap Add New Keyboard and select DevBoard.")
                    Text("3. In a text field, press and hold 🌐 to choose DevBoard.")
                    Text("This build does not request Full Access. Clipboard is unavailable; basic typing and companion settings remain local.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("Keyboard layout") {
                    Picker("Profile", selection: $profile) {
                        Text("Terminal").tag("terminal")
                        Text("Standard").tag("standard")
                    }
                    .onChange(of: profile) { SharedSettings.profile = $0; rows = SharedSettings.rows }
                    Picker("Language", selection: $language) {
                        Text("Lithuanian").tag("lt-LT")
                        Text("English").tag("en-US")
                    }.onChange(of: language) { SharedSettings.language = $0; variant = SharedSettings.variant; rows = SharedSettings.rows }
                    Picker("Layout", selection: $variant) {
                        Text("PC").tag("pc")
                        Text("QWERTY").tag("qwerty")
                    }.onChange(of: variant) { SharedSettings.variant = $0; rows = SharedSettings.rows }
                    Toggle("Show F1–F8", isOn: $functionKeys).onChange(of: functionKeys) { SharedSettings.functionKeys = $0 }
                    Toggle("Button Press Animation", isOn: $animation).onChange(of: animation) { SharedSettings.pressAnimation = $0 }
                    NavigationLink("Edit layout") { LayoutEditor(rows: $rows) }
                    NavigationLink("+ Combo") { ComboEditor(combos: $combos) }
                    KeyboardPreview(rows: rows, theme: theme, functionKeys: functionKeys, combos: combos)
                    Picker("Theme", selection: $theme) {
                        Text("Dark").tag("dark")
                        Text("Light").tag("light")
                        Text("Matrix").tag("matrix")
                    }
                    .onChange(of: theme) { SharedSettings.theme = $0 }
                }
                Section("Saved snippets") {
                    TextField("Text to insert", text: $snippet, axis: .vertical).lineLimit(2...5)
                    Button(snippetIndex == nil ? "Save snippet" : "Update snippet") {
                        guard !snippet.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                        if let i = snippetIndex, snippets.indices.contains(i) { snippets[i] = String(snippet.prefix(2000)) }
                        else if snippets.count < 30 { snippets.insert(String(snippet.prefix(2000)), at: 0) }
                        SharedSettings.snippets = snippets; snippet = ""; snippetIndex = nil
                    }
                    ForEach(snippets.indices, id: \.self) { i in
                        HStack {
                            Button(String(snippets[i].prefix(80))) { snippet = snippets[i]; snippetIndex = i }
                            Spacer()
                            Button("Delete") { snippets.remove(at: i); SharedSettings.snippets = snippets; snippetIndex = nil; snippet = "" }
                        }
                    }
                }
                Section("Clipboard") {
                    Toggle("Keep explicitly pasted items", isOn: $captureClipboard).disabled(!SharedSettings.requestsFullAccess).onChange(of: captureClipboard) {
                        SharedSettings.captureClipboard = $0
                        if !$0 { SharedSettings.clips = [] }
                    }
                    Button("Clear clipboard history") { SharedSettings.clips = [] }
                    Text("Clipboard is unavailable in this build because Full Access is disabled. No background recording.").font(.caption)
                }
                Section("Share a file") {
                    Button("Choose any file") { showFilePicker = true }
                    if let sharedFile {
                        Text(sharedFile.lastPathComponent).font(.caption)
                        ShareLink(item: sharedFile) {
                            Label("Share selected file", systemImage: "square.and.arrow.up")
                        }
                    }
                    if let importError {
                        Text(importError).foregroundStyle(.red).font(.caption)
                    }
                    Text("The keyboard extension cannot attach files directly to another app. Use the iOS Share Sheet from this app.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("iOS limits") {
                    Text("🎙️ on the keyboard switches to the next keyboard, where iOS system dictation may be available. Apple does not give third-party keyboard extensions microphone access.")
                    Text("iOS uses the system keyboard for passwords and some phone fields. Per-app auto-profiles and hardware-style Ctrl/Alt/F-keys are not supported universally.")
                }
            }
            .buttonStyle(.borderless)
            .navigationTitle("DevBoard")
            .fileImporter(isPresented: $showFilePicker, allowedContentTypes: [.item]) { result in
                switch result {
                case .success(let url):
                    let access = url.startAccessingSecurityScopedResource()
                    defer { if access { url.stopAccessingSecurityScopedResource() } }
                    let copy = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString + "-" + url.lastPathComponent)
                    do {
                        try FileManager.default.copyItem(at: url, to: copy)
                        sharedFile = copy
                        importError = nil
                    } catch {
                        importError = "Could not open this file: \(error.localizedDescription)"
                    }
                case .failure(let error):
                    importError = error.localizedDescription
                }
            }
        }
    }
}

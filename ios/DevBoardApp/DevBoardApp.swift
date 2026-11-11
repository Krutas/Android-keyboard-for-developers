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
                    Text("You do not need to enable Full Access for the basic keyboard.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("Keyboard layout") {
                    Picker("Profile", selection: $profile) {
                        Text("Terminal").tag("terminal")
                        Text("Standard").tag("standard")
                    }
                    .onChange(of: profile) { SharedSettings.profile = $0 }
                    Picker("Theme", selection: $theme) {
                        Text("Dark").tag("dark")
                        Text("Light").tag("light")
                        Text("Matrix").tag("matrix")
                    }
                    .onChange(of: theme) { SharedSettings.theme = $0 }
                }
                Section("Quick snippet") {
                    TextField("Saved text or command", text: $snippet, axis: .vertical)
                        .lineLimit(2...5)
                    Button("Save snippet") { SharedSettings.snippet = snippet }
                    Text("Use 📋 on the keyboard to insert the saved text.")
                        .font(.caption).foregroundStyle(.secondary)
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

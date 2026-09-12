import Foundation

struct LiveCloudSyncFileStore {
    let runDirectory: URL

    init(runID: String) throws {
        let documentsDirectory = try FileManager.default.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        runDirectory = documentsDirectory.appendingPathComponent(runID, isDirectory: true)
        try FileManager.default.createDirectory(at: runDirectory, withIntermediateDirectories: true)
    }

    var commandURL: URL { runDirectory.appendingPathComponent("command.json") }
    private var processedURL: URL { runDirectory.appendingPathComponent("last-processed.json") }

    func readCommand() -> LiveCloudSyncCommand? {
        guard let data = try? Data(contentsOf: commandURL) else { return nil }
        return try? JSONDecoder().decode(LiveCloudSyncCommand.self, from: data)
    }

    private func processedCommands() throws -> LiveCloudSyncProcessedCommands {
        guard FileManager.default.fileExists(atPath: processedURL.path) else {
            return LiveCloudSyncProcessedCommands(ids: [])
        }
        return try JSONDecoder().decode(LiveCloudSyncProcessedCommands.self, from: Data(contentsOf: processedURL))
    }

    func hasProcessed(_ id: String) throws -> Bool {
        try processedCommands().ids.contains(id)
    }

    // Record before UI work so a restarted runner cannot perform the same command twice.
    func recordProcessed(_ id: String) throws {
        var commands = try processedCommands()
        commands.ids.insert(id)
        let encoded = try JSONEncoder().encode(commands)
        try encoded.write(to: processedURL, options: .atomic)
    }

    func write(_ response: LiveCloudSyncResponse) throws {
        let data = try JSONEncoder().encode(response)
        let responseURL = runDirectory.appendingPathComponent("response.json")
        try data.write(to: responseURL, options: .atomic)
    }

    func writeScreenshot(_ data: Data, id: String) throws -> String {
        let filename = "\(id).png"
        try data.write(to: runDirectory.appendingPathComponent(filename), options: .atomic)
        return filename
    }
}

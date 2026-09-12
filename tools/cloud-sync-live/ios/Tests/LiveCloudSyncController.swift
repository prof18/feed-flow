import XCTest

final class LiveCloudSyncController {
    private let configuration: LiveCloudSyncConfiguration
    private let fileStore: LiveCloudSyncFileStore
    private let targetApp: XCUIApplication

    init(configuration: LiveCloudSyncConfiguration, fileStore: LiveCloudSyncFileStore, targetApp: XCUIApplication) {
        self.configuration = configuration
        self.fileStore = fileStore
        self.targetApp = targetApp
    }

    func run() throws {
        let deadline = Date().addingTimeInterval(configuration.timeoutSeconds)

        while Date() < deadline {
            defer { Thread.sleep(forTimeInterval: 0.25) }
            guard let command = fileStore.readCommand(), try !fileStore.hasProcessed(command.id) else { continue }
            guard command.runID == configuration.runID, UUID(uuidString: command.id) != nil else { continue }

            try fileStore.recordProcessed(command.id)
            try fileStore.write(perform(command))

            if command.action == "stop" { return }
        }
    }

    private func perform(_ command: LiveCloudSyncCommand) -> LiveCloudSyncResponse {
        var screenshot: String?
        do {
            switch command.action {
            case "inspect":
                break
            case "activate":
                targetApp.activate()
                guard targetApp.wait(for: .runningForeground, timeout: 10) else {
                    throw LiveCloudSyncError.invalidConfiguration("The configured app did not reach the foreground.")
                }
            case "background":
                XCUIDevice.shared.press(.home)
                return response(for: command, ok: true, error: nil, tree: "App sent to background.", screenshot: nil)
            case "tap":
                try element(for: command).tap()
            case "longPress":
                try element(for: command).press(forDuration: 1)
            case "typeText":
                guard let text = command.text else { throw LiveCloudSyncError.missingText }
                targetApp.typeText(text)
            case "swipeUp":
                try swipe(command, up: true)
            case "swipeDown":
                try swipe(command, up: false)
            case "refresh":
                try refresh()
            case "screenshot":
                screenshot = try fileStore.writeScreenshot(targetApp.screenshot().pngRepresentation, id: command.id)
            case "stop":
                break
            default:
                throw LiveCloudSyncError.unsupportedAction(command.action)
            }
            Thread.sleep(forTimeInterval: 0.5)
            return response(for: command, ok: true, error: nil, tree: targetApp.debugDescription, screenshot: screenshot)
        } catch {
            return response(for: command, ok: false, error: error.localizedDescription, tree: targetApp.debugDescription, screenshot: screenshot)
        }
    }

    private func refresh() throws {
        let list = targetApp.collectionViews.firstMatch
        guard list.exists && list.isHittable else {
            throw LiveCloudSyncError.targetNotFound("Visible timeline collection view")
        }
        let start = list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.25))
        let end = list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.9))
        start.press(forDuration: 0.1, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.5)
    }

    private func response(for command: LiveCloudSyncCommand, ok: Bool, error: String?, tree: String, screenshot: String?) -> LiveCloudSyncResponse {
        LiveCloudSyncResponse(id: command.id, runID: configuration.runID, ok: ok, error: error, tree: tree, screenshot: screenshot)
    }

    private func element(for command: LiveCloudSyncCommand) throws -> XCUIElement {
        guard let target = command.target?.trimmingCharacters(in: .whitespacesAndNewlines), !target.isEmpty else {
            throw LiveCloudSyncError.missingTarget
        }
        let predicate = NSPredicate(format: "identifier == %@ OR label == %@", target, target)
        let buttons = targetApp.buttons.matching(predicate).allElementsBoundByIndex
        let hittableButtons = buttons.filter(\.isHittable)
        if hittableButtons.count == 1 { return hittableButtons[0] }
        if hittableButtons.count > 1 { throw LiveCloudSyncError.ambiguousTarget(target) }

        let matches = targetApp.descendants(matching: .any).matching(predicate).allElementsBoundByIndex
        let hittableMatches = matches.filter(\.isHittable)
        if hittableMatches.count == 1 { return hittableMatches[0] }
        if hittableMatches.count > 1 { throw LiveCloudSyncError.ambiguousTarget(target) }
        if !matches.isEmpty || !buttons.isEmpty { throw LiveCloudSyncError.targetNotHittable(target) }
        throw LiveCloudSyncError.targetNotFound(target)
    }

    private func swipe(_ command: LiveCloudSyncCommand, up: Bool) throws {
        if command.target == nil || command.target?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == true {
            if up { targetApp.swipeUp() } else { targetApp.swipeDown() }
            return
        }
        let element = try element(for: command)
        if up { element.swipeUp() } else { element.swipeDown() }
    }
}

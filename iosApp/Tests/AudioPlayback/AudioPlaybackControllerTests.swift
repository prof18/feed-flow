import AVFoundation
import Foundation
import MediaPlayer
import XCTest

final class AudioPlaybackControllerTests: XCTestCase {
    @MainActor
    func testTogglingEpisodeWithChangedDisplayMetadataPausesWithoutReplacingAudio() {
        let store = TestAudioPlaybackPositionStore()
        store.positions["episode"] = 12000
        let controller = AudioPlaybackController(positionStore: store)
        defer { controller.stop() }
        let selected = episode("episode")
        controller.toggle(selected)
        let originalItem = controller.player.currentItem
        let updated = AudioEpisode(
            itemId: selected.itemId,
            url: selected.url,
            title: "Updated episode title",
            subtitle: "Renamed feed",
            artworkURL: URL(fileURLWithPath: "/updated-artwork.png")
        )

        controller.toggle(updated)

        XCTAssertFalse(controller.isPlaying)
        XCTAssertTrue(controller.player.currentItem === originalItem)
        XCTAssertEqual(controller.position, 12)
        XCTAssertEqual(store.positions["episode"], 12000)

        controller.toggle(updated)

        XCTAssertTrue(controller.isPlaying)
        XCTAssertTrue(controller.player.currentItem === originalItem)
    }

    @MainActor
    func testTogglingEpisodeWithChangedAudioURLReplacesAudio() {
        let store = TestAudioPlaybackPositionStore()
        store.positions["episode"] = 12000
        let controller = AudioPlaybackController(positionStore: store)
        defer { controller.stop() }
        let selected = episode("episode")
        controller.toggle(selected)
        let originalItem = controller.player.currentItem
        let updated = AudioEpisode(
            itemId: selected.itemId,
            url: URL(fileURLWithPath: "/replacement-audio.mp3"),
            title: selected.title,
            subtitle: selected.subtitle,
            artworkURL: selected.artworkURL
        )

        controller.toggle(updated)

        XCTAssertTrue(controller.isPlaying)
        XCTAssertTrue(controller.player.currentItem !== originalItem)
        XCTAssertEqual(controller.episode, updated)
        XCTAssertEqual(controller.position, 12)
    }

    @MainActor
    func testSystemProgressIsAvailableWhenPreparationFinishesWhilePaused() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let controller = AudioPlaybackController(positionStore: TestAudioPlaybackPositionStore())
        defer { controller.stop() }
        let selected = AudioEpisode(itemId: "wave", url: url, title: "Episode", subtitle: "Feed", artworkURL: nil)
        controller.toggle(selected)
        controller.pause()

        for _ in 0 ..< 100 where !controller.canSeek {
            try await Task.sleep(for: .milliseconds(20))
        }

        XCTAssertTrue(controller.canSeek)
        let infoCenter = controller.nowPlayingSession.nowPlayingInfoCenter
        let info = infoCenter.nowPlayingInfo
        XCTAssertEqual(info?[MPMediaItemPropertyTitle] as? String, "Episode")
        XCTAssertEqual(info?[MPMediaItemPropertyArtist] as? String, "Feed")
        XCTAssertEqual(info?[MPMediaItemPropertyPlaybackDuration] as? Double, 6)
        XCTAssertEqual(info?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 0)
        XCTAssertEqual((info?[MPNowPlayingInfoPropertyPlaybackRate] as? NSNumber)?.doubleValue, 0)
        XCTAssertTrue(controller.nowPlayingSession.remoteCommandCenter.changePlaybackPositionCommand.isEnabled)

        controller.seek(to: 4)
        XCTAssertEqual(
            infoCenter.nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double,
            4
        )
        controller.setPlaybackSpeed(1.5)
        controller.play()
        XCTAssertEqual(
            (infoCenter.nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] as? NSNumber)?.doubleValue,
            1.5
        )
        controller.pause()
        XCTAssertEqual(
            (infoCenter.nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] as? NSNumber)?.doubleValue,
            0
        )
        controller.stop()
        XCTAssertNil(controller.nowPlayingSession.nowPlayingInfoCenter.nowPlayingInfo)
        XCTAssertFalse(controller.nowPlayingSession.remoteCommandCenter.changePlaybackPositionCommand.isEnabled)
    }

    @MainActor
    func testStartingAndRetryingPlaybackConfigureMediaAudioSession() throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let session = AVAudioSession.sharedInstance()
        let previousCategory = session.category
        let previousMode = session.mode
        let previousOptions = session.categoryOptions
        let controller = AudioPlaybackController(positionStore: TestAudioPlaybackPositionStore())
        defer {
            controller.stop()
            try? session.setCategory(previousCategory, mode: previousMode, options: previousOptions)
        }
        let selected = AudioEpisode(itemId: "wave", url: url, title: "wave", subtitle: nil, artworkURL: nil)
        try session.setActive(false)
        try session.setCategory(.soloAmbient, mode: .default)

        controller.toggle(selected)

        XCTAssertEqual(session.category, .playback)
        XCTAssertEqual(session.mode, .spokenAudio)
        XCTAssertFalse(controller.hasFailed)

        controller.failPlayback()
        try session.setActive(false)
        try session.setCategory(.soloAmbient, mode: .default)

        controller.toggle(selected)

        XCTAssertEqual(session.category, .playback)
        XCTAssertEqual(session.mode, .spokenAudio)
        XCTAssertFalse(controller.hasFailed)
    }

    @MainActor
    func testRestoredSeekFinishesWhilePausedAndPauseDoesNotReplacePendingSeek() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let store = TestAudioPlaybackPositionStore()
        store.positions["wave"] = 2000
        let controller = AudioPlaybackController(positionStore: store)
        defer { controller.stop() }
        controller.start(AudioEpisode(itemId: "wave", url: url, title: "wave", subtitle: nil, artworkURL: nil))
        controller.pause()

        for _ in 0 ..< 100 where !controller.canSeek {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertTrue(controller.canSeek)
        XCTAssertFalse(controller.isPlaying)
        XCTAssertEqual(controller.position, 2, accuracy: 0.05)

        controller.seek(to: 4)
        controller.pause()
        XCTAssertEqual(store.positions["wave"], 4000)
        try await Task.sleep(for: .milliseconds(100))
        XCTAssertEqual(controller.position, 4, accuracy: 0.05)
    }

    @MainActor
    func testPausingAndClosingDuringPreparationPreserveResumePosition() {
        let store = TestAudioPlaybackPositionStore()
        store.positions["episode"] = 72000
        let controller = AudioPlaybackController(positionStore: store)
        controller.start(episode("episode"))

        controller.pause()
        XCTAssertEqual(controller.position, 72)
        XCTAssertEqual(store.positions["episode"], 72000)
        XCTAssertFalse(controller.isPlaying)

        controller.stop()
        XCTAssertNil(controller.episode)
        XCTAssertEqual(store.positions["episode"], 72000)
    }

    @MainActor
    func testReplacingEpisodeSavesPreviousAndRestoresSelectedEpisode() {
        let store = TestAudioPlaybackPositionStore()
        store.positions = ["first": 15000, "second": 95000]
        let controller = AudioPlaybackController(positionStore: store)
        controller.start(episode("first"))
        controller.start(episode("second"))

        XCTAssertEqual(store.positions["first"], 15000)
        XCTAssertEqual(controller.episode?.itemId, "second")
        XCTAssertEqual(controller.position, 95)
        controller.stop()
    }

    @MainActor
    func testToggleSameEpisodePausesAndFailureCanBeRetried() {
        let store = TestAudioPlaybackPositionStore()
        store.positions["episode"] = 12000
        let controller = AudioPlaybackController(positionStore: store)
        let selected = episode("episode")
        controller.toggle(selected)
        XCTAssertTrue(controller.isPlaying)
        controller.toggle(selected)
        XCTAssertFalse(controller.isPlaying)
        controller.failPlayback()
        XCTAssertTrue(controller.hasFailed)
        controller.toggle(selected)
        XCTAssertFalse(controller.hasFailed)
        XCTAssertEqual(controller.position, 12)
        controller.stop()
    }

    @MainActor
    func testPlaybackSpeedSelectionIsBoundedAndDoesNotStartPausedPlayback() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let controller = AudioPlaybackController(positionStore: TestAudioPlaybackPositionStore())
        defer { controller.stop() }
        controller.start(AudioEpisode(itemId: "wave", url: url, title: "wave", subtitle: nil, artworkURL: nil))

        controller.pause()
        for _ in 0 ..< 100 where !controller.canSeek {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertTrue(controller.canSeek)
        XCTAssertFalse(controller.isPlaying)
        controller.setPlaybackSpeed(3.0)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 1.0)

        controller.setPlaybackSpeed(1.5)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 1.5)
        XCTAssertEqual(controller.player.defaultRate, 1.5)
        XCTAssertFalse(controller.isPlaying)
        XCTAssertEqual(controller.player.rate, 0)
        let info = controller.nowPlayingSession.nowPlayingInfoCenter.nowPlayingInfo
        XCTAssertEqual(
            info?[MPNowPlayingInfoPropertyDefaultPlaybackRate] as? Double,
            1.5
        )

        controller.play()
        for _ in 0 ..< 100 where controller.player.rate != 1.5 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 1.5, accuracy: 0.01)
        controller.setPlaybackSpeed(0.5)
        XCTAssertEqual(controller.player.rate, 0.5, accuracy: 0.01)
        controller.pause()
        XCTAssertEqual(controller.player.rate, 0)
        controller.play()
        XCTAssertEqual(controller.player.rate, 0.5, accuracy: 0.01)
    }

    @MainActor
    func testPlaybackSpeedSurvivesEpisodeReplacementStopAndRetry() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let controller = AudioPlaybackController(positionStore: TestAudioPlaybackPositionStore())
        defer { controller.stop() }
        let first = AudioEpisode(itemId: "first", url: url, title: "first", subtitle: nil, artworkURL: nil)
        let second = AudioEpisode(itemId: "second", url: url, title: "second", subtitle: nil, artworkURL: nil)

        controller.setPlaybackSpeed(2.0)
        controller.start(first)
        for _ in 0 ..< 100 where controller.player.rate != 2.0 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 2.0, accuracy: 0.01)

        controller.start(second)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 2.0)
        for _ in 0 ..< 100 where controller.player.rate != 2.0 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 2.0, accuracy: 0.01)

        controller.stop()
        XCTAssertEqual(controller.selectedPlaybackSpeed, 2.0)
        controller.toggle(second)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 2.0)
        for _ in 0 ..< 100 where controller.player.rate != 2.0 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 2.0, accuracy: 0.01)
        controller.failPlayback()
        controller.toggle(second)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 2.0)
        for _ in 0 ..< 100 where controller.player.rate != 2.0 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 2.0, accuracy: 0.01)
    }

    @MainActor
    func testMediaServicesResetRecreatesPlayerAndWaitsForUserToResume() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }
        let store = TestAudioPlaybackPositionStore()
        store.positions["wave"] = 4000
        let controller = AudioPlaybackController(positionStore: store)
        defer { controller.stop() }
        let selected = AudioEpisode(itemId: "wave", url: url, title: "wave", subtitle: nil, artworkURL: nil)
        controller.start(selected)
        for _ in 0 ..< 100 where !controller.canSeek {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertTrue(controller.canSeek)
        controller.pause()
        controller.setPlaybackSpeed(1.5)
        controller.seek(to: 4)
        let oldPlayer = controller.player
        let oldSession = controller.nowPlayingSession
        let remoteTargetCount = controller.remoteTargets.count
        let systemObserverCount = controller.systemObservers.count

        NotificationCenter.default.post(name: AVAudioSession.mediaServicesWereResetNotification, object: nil)
        for _ in 0 ..< 100 where !controller.hasFailed {
            try await Task.sleep(for: .milliseconds(20))
        }

        XCTAssertTrue(controller.player !== oldPlayer)
        XCTAssertTrue(controller.nowPlayingSession !== oldSession)
        XCTAssertTrue(controller.nowPlayingSession.players.first === controller.player)
        XCTAssertNil(oldPlayer.currentItem)
        XCTAssertNil(controller.player.currentItem)
        XCTAssertEqual(controller.episode, selected)
        XCTAssertEqual(controller.position, 4, accuracy: 0.05)
        XCTAssertEqual(controller.selectedPlaybackSpeed, 1.5)
        XCTAssertEqual(store.positions["wave"], 4000)
        XCTAssertTrue(controller.hasFailed)
        XCTAssertFalse(controller.isPlaying)
        XCTAssertFalse(controller.wantsPlayback)
        XCTAssertEqual(controller.remoteTargets.count, remoteTargetCount)
        XCTAssertEqual(controller.systemObservers.count, systemObserverCount)

        let replacementPlayer = controller.player
        controller.toggle(selected)
        XCTAssertTrue(controller.player === replacementPlayer)
        XCTAssertNotNil(controller.player.currentItem)
        XCTAssertFalse(controller.hasFailed)
        XCTAssertEqual(controller.position, 4, accuracy: 0.05)
        for _ in 0 ..< 100 where !controller.canSeek {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertTrue(controller.canSeek)
        XCTAssertTrue(controller.isPlaying)
        for _ in 0 ..< 100 where controller.player.rate != 1.5 {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(controller.player.rate, 1.5, accuracy: 0.01)
        XCTAssertEqual(controller.player.defaultRate, 1.5)
    }

    @MainActor
    func testSpeechPreparationStopsPodcastAndPreservesPodcastPosition() async throws {
        let podcastURL = try waveURL()
        defer { try? FileManager.default.removeItem(at: podcastURL) }
        let store = TestAudioPlaybackPositionStore()
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: store, generator: generator)
        defer { controller.stop() }
        controller.start(AudioEpisode(itemId: "article", url: podcastURL, title: "Article", subtitle: nil, artworkURL: nil))
        try await waitUntilCanSeek(controller)
        controller.seek(to: 2)

        controller.toggleSpeech(itemId: "article", title: "Article", content: "body", subtitle: nil, artworkURL: nil)

        XCTAssertNil(controller.player.currentItem)
        XCTAssertEqual(controller.episode?.kind, .speech)
        XCTAssertEqual(store.positions["article"], 2_000)
        XCTAssertTrue(controller.isPreparingSpeech)
        try await generator.waitForRequestCount(1)
        generator.fail(request: 0)
        try await waitUntilFailed(controller)
    }

    @MainActor
    func testPausedSpeechPreparationLoadsWAVDurationAndEnablesSeek() async throws {
        let outputURL = try waveURL()
        defer { try? FileManager.default.removeItem(at: outputURL) }
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        defer { controller.stop() }
        controller.toggleSpeech(itemId: "article", title: "Article", content: "body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        controller.pause()
        generator.complete(request: 0, with: outputURL)
        try await waitUntilCanSeek(controller)

        XCTAssertFalse(controller.isPreparingSpeech)
        XCTAssertFalse(controller.isPlaying)
        XCTAssertEqual(controller.duration, 6, accuracy: 0.05)
        XCTAssertTrue(controller.canSeek)
        XCTAssertEqual(controller.player.rate, 0)
    }

    @MainActor
    func testSpeechPlaybackUsesSeparatePositionNamespaceFromPodcast() async throws {
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        defer { controller.stop() }
        controller.toggleSpeech(itemId: "article", title: "Article", content: "body", subtitle: nil, artworkURL: nil)

        let speechEpisode = try XCTUnwrap(controller.episode)
        let podcastEpisode = AudioEpisode(
            itemId: "article", url: URL(fileURLWithPath: "/podcast.mp3"), title: "Article", subtitle: nil, artworkURL: nil
        )
        XCTAssertEqual(speechEpisode.kind, .speech)
        XCTAssertNotEqual(speechEpisode.playbackId, podcastEpisode.playbackId)
        try await generator.waitForRequestCount(1)
        controller.stop()
        let lateURL = try waveURL()
        generator.complete(request: 0, with: lateURL)
        try await waitForScheduledCallbacks()
        try? FileManager.default.removeItem(at: lateURL)
    }

    @MainActor
    func testSpeechSeekPodcastSwitchAndReturnRestoresCachedFileAndProgress() async throws {
        let speechURL = try waveURL()
        let podcastURL = try waveURL()
        defer {
            try? FileManager.default.removeItem(at: speechURL)
            try? FileManager.default.removeItem(at: podcastURL)
        }
        let store = TestAudioPlaybackPositionStore()
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: store, generator: generator)
        defer { controller.stop() }
        let request = (itemId: "article", title: "Article", content: "body")
        controller.toggleSpeech(itemId: request.itemId, title: request.title, content: request.content, subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        generator.complete(request: 0, with: speechURL)
        try await waitUntilCanSeek(controller)
        let originalSpeechEpisode = try XCTUnwrap(controller.episode)
        controller.seek(to: 3)
        XCTAssertEqual(store.positions[originalSpeechEpisode.playbackId], 3_000)

        let podcastEpisode = AudioEpisode(itemId: request.itemId, url: podcastURL, title: request.title, subtitle: nil, artworkURL: nil)
        controller.start(podcastEpisode, autoplay: false)
        controller.toggleSpeech(itemId: request.itemId, title: request.title, content: request.content, subtitle: nil, artworkURL: nil)
        try await waitUntilCanSeek(controller)

        XCTAssertEqual(generator.requests.count, 1)
        XCTAssertEqual(controller.episode?.url, originalSpeechEpisode.url)
        XCTAssertEqual(controller.position, 3, accuracy: 0.05)
        XCTAssertNotEqual(controller.episode?.playbackId, podcastEpisode.playbackId)
    }

    @MainActor
    func testLateSpeechResultAfterReplacementCannotReplaceNextSource() async throws {
        let outputURL = try waveURL()
        let replacementURL = try waveURL()
        defer {
            try? FileManager.default.removeItem(at: outputURL)
            try? FileManager.default.removeItem(at: replacementURL)
        }
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        defer { controller.stop() }
        controller.toggleSpeech(itemId: "first", title: "First", content: "body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        let replacement = AudioEpisode(itemId: "replacement", url: replacementURL, title: "Replacement", subtitle: nil, artworkURL: nil)
        controller.start(replacement, autoplay: false)
        generator.complete(request: 0, with: outputURL)
        try await waitForScheduledCallbacks()

        XCTAssertEqual(controller.episode, replacement)
        XCTAssertEqual((controller.player.currentItem?.asset as? AVURLAsset)?.url, replacementURL)
        XCTAssertFalse(controller.isPreparingSpeech)
    }

    @MainActor
    func testFailedSpeechGenerationCanBeRetried() async throws {
        let outputURL = try waveURL()
        defer { try? FileManager.default.removeItem(at: outputURL) }
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        defer { controller.stop() }
        controller.toggleSpeech(itemId: "article", title: "Article", content: "body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        generator.fail(request: 0)
        for _ in 0 ..< 100 where !controller.hasFailed { try await Task.sleep(for: .milliseconds(10)) }
        XCTAssertTrue(controller.hasFailed)

        controller.play()
        try await generator.waitForRequestCount(2)
        generator.complete(request: 1, with: outputURL)
        try await waitUntilCanSeek(controller)

        XCTAssertEqual(generator.requests.count, 2)
        XCTAssertFalse(controller.hasFailed)
        XCTAssertEqual(controller.episode?.kind, .speech)
    }

    @MainActor
    func testChangedSpeechContentGeneratesNewCachedAudio() async throws {
        let firstURL = try waveURL()
        let secondURL = try waveURL()
        defer {
            try? FileManager.default.removeItem(at: firstURL)
            try? FileManager.default.removeItem(at: secondURL)
        }
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        defer { controller.stop() }
        controller.toggleSpeech(itemId: "article", title: "Article", content: "first body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        generator.complete(request: 0, with: firstURL)
        try await waitUntilCanSeek(controller)
        let firstPlaybackId = controller.episode?.playbackId

        controller.toggleSpeech(itemId: "article", title: "Article", content: "changed body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(2)
        let changedContentPlaybackId = SpeechPlaybackRequest(
            itemId: "article", title: "Article", content: "changed body", subtitle: nil, artworkURL: nil
        ).episode.playbackId
        XCTAssertNotEqual(controller.episode?.playbackId, firstPlaybackId)
        XCTAssertEqual(controller.episode?.playbackId, changedContentPlaybackId)
        generator.complete(request: 1, with: secondURL)
        try await waitUntilCanSeek(controller)

        XCTAssertEqual(generator.requests.count, 2)
        XCTAssertEqual(controller.episode?.url, secondURL)
    }

    @MainActor
    func testLateSpeechResultAfterCloseDoesNotAutoplay() async throws {
        let outputURL = try waveURL()
        defer { try? FileManager.default.removeItem(at: outputURL) }
        let generator = TestSpeechAudioGenerator()
        let controller = speechController(store: TestAudioPlaybackPositionStore(), generator: generator)
        controller.toggleSpeech(itemId: "article", title: "Article", content: "body", subtitle: nil, artworkURL: nil)
        try await generator.waitForRequestCount(1)
        controller.stop()
        generator.complete(request: 0, with: outputURL)
        try await waitForScheduledCallbacks()

        XCTAssertNil(controller.episode)
        XCTAssertNil(controller.player.currentItem)
        XCTAssertFalse(controller.isPlaying)
        XCTAssertEqual(controller.player.rate, 0)
    }

    @MainActor
    private func speechController(store: TestAudioPlaybackPositionStore, generator: TestSpeechAudioGenerator) -> AudioPlaybackController {
        AudioPlaybackController(
            positionStore: store,
            speechGenerator: generator,
            speechText: { _, content in [content] }
        )
    }

    @MainActor
    private func waitUntilCanSeek(_ controller: AudioPlaybackController) async throws {
        for _ in 0 ..< 200 {
            if controller.canSeek { return }
            try await Task.sleep(for: .milliseconds(10))
        }
        XCTFail("Timed out waiting for prepared audio to become seekable")
    }

    @MainActor
    private func waitUntilFailed(_ controller: AudioPlaybackController) async throws {
        for _ in 0 ..< 100 {
            if controller.hasFailed { return }
            try await Task.sleep(for: .milliseconds(10))
        }
        XCTFail("Timed out waiting for speech generation failure")
    }

    @MainActor
    private func waitForScheduledCallbacks() async throws {
        for _ in 0 ..< 50 { try await Task.sleep(for: .milliseconds(10)) }
    }

    private func waveURL() throws -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).wav")
        try AudioTestWaveFile.write(to: url)
        return url
    }

    private func episode(_ itemId: String) -> AudioEpisode {
        AudioEpisode(
            itemId: itemId,
            url: URL(fileURLWithPath: "/missing-feedflow-test-audio.mp3"),
            title: itemId,
            subtitle: nil,
            artworkURL: nil
        )
    }
}

import AVFoundation
import Foundation
import MediaPlayer
import Observation
import UIKit

@Observable
@MainActor
final class AudioPlaybackController {
    static let supportedPlaybackSpeeds = [0.5, 1.0, 1.5, 2.0]

    private(set) var episode: AudioEpisode?
    private(set) var isPlaying = false
    private(set) var isLoading = false
    private(set) var isPreparingSpeech = false
    private(set) var hasFailed = false
    private(set) var position = 0.0
    private(set) var duration = 0.0
    private(set) var selectedPlaybackSpeed = 1.0
    private var isPreparing = false
    var canSeek: Bool {
        episode != nil && !isPreparingSpeech && !isPreparing && duration > 0 && !hasFailed
    }

    @ObservationIgnored private(set) var player: AVPlayer
    @ObservationIgnored private(set) var nowPlayingSession: MPNowPlayingSession
    @ObservationIgnored let positionStore: AudioPlaybackPositionStore
    @ObservationIgnored var wantsPlayback = false
    @ObservationIgnored var resumeAfterInterruption = false
    @ObservationIgnored var systemObservers: [NSObjectProtocol] = []
    @ObservationIgnored var remoteTargets: [(MPRemoteCommand, Any)] = []
    @ObservationIgnored var artwork: MPMediaItemArtwork?
    @ObservationIgnored var artworkTask: Task<Void, Never>?
    @ObservationIgnored private var statusObserver: NSKeyValueObservation?
    @ObservationIgnored private var durationObserver: NSKeyValueObservation?
    @ObservationIgnored private var playbackObserver: NSKeyValueObservation?
    @ObservationIgnored private var timeObserver: Any?
    @ObservationIgnored private var itemObservers: [NSObjectProtocol] = []
    @ObservationIgnored private var lastSavedAt = 0.0
    @ObservationIgnored private var isSeeking = false
    @ObservationIgnored private var seekID = UUID()
    @ObservationIgnored private var hasEnded = false

    @ObservationIgnored private let speechGenerator: SpeechAudioGenerating
    @ObservationIgnored private let speechText: (String?, String) async throws -> [String]
    @ObservationIgnored private var speechTask: Task<Void, Never>?
    @ObservationIgnored private var speechGeneration = UUID()
    @ObservationIgnored private var speechRequest: SpeechPlaybackRequest?
    @ObservationIgnored private var cachedSpeech: AudioEpisode?
    @ObservationIgnored private var speechFile: URL?

    init(
        positionStore: AudioPlaybackPositionStore,
        player: AVPlayer = AVPlayer(),
        speechGenerator: SpeechAudioGenerating? = nil,
        speechText: @escaping (String?, String) async throws -> [String] = { _, _ in
            throw SpeechAudioGenerationError.synthesisFailed
        }
    ) {
        self.speechGenerator = speechGenerator ?? SpeechAudioGenerator()
        self.speechText = speechText
        self.positionStore = positionStore
        self.player = player
        nowPlayingSession = MPNowPlayingSession(players: [player])
        configureSystemControls()
        installPlayerObservers()
    }

    private func installPlayerObservers() {
        let observedPlayer = player
        playbackObserver = observedPlayer.observe(
            \.timeControlStatus, options: [.new]
        ) { [weak self, weak observedPlayer] _, _ in
            Task { @MainActor in
                guard let self, let observedPlayer, self.player === observedPlayer else { return }
                self.updatePlaybackState()
            }
        }
        timeObserver = observedPlayer.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 1, preferredTimescale: 600), queue: .main
        ) { [weak self, weak observedPlayer] _ in
            Task { @MainActor in
                guard let self, let observedPlayer, self.player === observedPlayer else { return }
                self.updateProgress()
            }
        }
    }

    deinit {
        if let timeObserver {
            player.removeTimeObserver(timeObserver)
        }
        itemObservers.forEach(NotificationCenter.default.removeObserver)
        systemObservers.forEach(NotificationCenter.default.removeObserver)
        remoteTargets.forEach { command, target in command.removeTarget(target) }
        artworkTask?.cancel()
    }

    func toggle(_ newEpisode: AudioEpisode) {
        if episode?.playbackId == newEpisode.playbackId, episode?.url == newEpisode.url, !hasFailed {
            togglePlayback()
        } else {
            start(newEpisode)
        }
    }

    func togglePlayback() {
        if isPlaying {
            pause()
        } else {
            play()
        }
    }

    func start(_ newEpisode: AudioEpisode, autoplay: Bool = true) {
        cancelSpeechPreparation()
        savePosition()
        player.pause()
        clearItemObservers()
        artworkTask?.cancel()
        artwork = nil
        episode = newEpisode
        hasFailed = false
        duration = 0
        position = Double(max(0, positionStore.position(for: newEpisode.playbackId))) / 1_000
        lastSavedAt = position
        wantsPlayback = autoplay
        isPlaying = autoplay
        isLoading = autoplay
        isPreparing = true
        isSeeking = false
        hasEnded = false
        let item = AVPlayerItem(url: newEpisode.url)
        item.audioTimePitchAlgorithm = .spectral
        player.replaceCurrentItem(with: item)
        statusObserver = item.observe(\.status, options: [.initial, .new]) { [weak self, weak item] _, _ in
            Task { @MainActor in
                guard let self, let item, self.player.currentItem === item else { return }
                self.itemStatusChanged(item)
            }
        }
        durationObserver = item.observe(\.duration, options: [.initial, .new]) { [weak self, weak item] _, _ in
            Task { @MainActor in
                guard let self, let item, self.player.currentItem === item else { return }
                self.duration = AudioPlaybackProgress.seconds(item.duration.seconds)
                self.updateNowPlaying()
            }
        }
        observeItem(item)
        loadArtwork(for: newEpisode)
        if autoplay { play() }
    }

    func play() {
        guard let episode else { return }
        if isPreparingSpeech {
            wantsPlayback = true
            isPlaying = true
            return
        }
        if hasFailed, episode.kind == .speech,
           speechFile == nil || !FileManager.default.fileExists(atPath: episode.url.path),
           let speechRequest {
            prepareSpeech(speechRequest)
            return
        }
        if hasFailed {
            start(episode)
            return
        }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio)
            try session.setActive(true)
            wantsPlayback = true
            if hasEnded {
                hasEnded = false
                player.seek(to: .zero)
            }
            beginPlaybackIfReady()
            updatePlaybackState()
            nowPlayingSession.becomeActiveIfPossible { _ in }
        } catch {
            failPlayback()
        }
    }

    func setPlaybackSpeed(_ speed: Double) {
        guard Self.supportedPlaybackSpeeds.contains(speed) else { return }
        selectedPlaybackSpeed = speed
        player.defaultRate = Float(speed)
        if wantsPlayback, !isPreparing, !hasFailed, player.currentItem?.status == .readyToPlay {
            player.rate = Float(speed)
        }
        updateNowPlaying()
    }

    func pause() {
        wantsPlayback = false
        resumeAfterInterruption = false
        player.pause()
        if !isPreparing, !isSeeking, !hasEnded, player.currentItem?.status == .readyToPlay {
            position = AudioPlaybackProgress.seconds(player.currentTime().seconds)
        }
        isPlaying = false
        isLoading = isPreparingSpeech
        savePosition()
        updateNowPlaying()
    }

    func stop() {
        cancelSpeechPreparation()
        pause()
        clearItemObservers()
        player.replaceCurrentItem(with: nil)
        artworkTask?.cancel()
        artwork = nil
        episode = nil
        position = 0
        duration = 0
        hasFailed = false
        nowPlayingSession.nowPlayingInfoCenter.nowPlayingInfo = nil
        updateRemoteCommandAvailability()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        clearSpeechFile()
        speechRequest = nil
    }

    func seek(to value: Double) {
        guard canSeek else { return }
        let target = AudioPlaybackProgress.seekPosition(value, duration: duration)
        hasEnded = false
        isSeeking = true
        let requestID = UUID()
        seekID = requestID
        position = target
        savePosition()
        updateNowPlaying()
        let currentItem = player.currentItem
        player.seek(to: CMTime(seconds: target, preferredTimescale: 600)) { [weak self] finished in
            Task { @MainActor in
                guard let self, self.player.currentItem === currentItem, self.seekID == requestID else { return }
                self.isSeeking = false
                guard finished else { return }
                self.updateNowPlaying()
            }
        }
    }

    func savePosition() {
        guard let episode else { return }
        positionStore.save(
            itemId: episode.playbackId,
            positionMs: AudioPlaybackProgress.milliseconds(position),
            durationMs: AudioPlaybackProgress.milliseconds(duration)
        )
        lastSavedAt = position
    }

    func failPlayback() {
        pause()
        hasFailed = true
        updateNowPlaying()
    }

    func resetAfterMediaServicesLoss() {
        cancelSpeechPreparation()
        wantsPlayback = false
        resumeAfterInterruption = false
        player.pause()
        isPlaying = false
        isLoading = false
        savePosition()
        hasFailed = true
        isPreparing = false
        isSeeking = false
        seekID = UUID()
        clearItemObservers()
        player.replaceCurrentItem(with: nil)
        removePlayerObservers()
        player = AVPlayer()
        nowPlayingSession.nowPlayingInfoCenter.nowPlayingInfo = nil
        nowPlayingSession = MPNowPlayingSession(players: [player])
        reconfigureRemoteCommands()
        installPlayerObservers()
        updateNowPlaying()
    }

    private func itemStatusChanged(_ item: AVPlayerItem) {
        switch item.status {
        case .readyToPlay:
            duration = AudioPlaybackProgress.seconds(item.duration.seconds)
            let target = AudioPlaybackProgress.seekPosition(position, duration: duration)
            position = target
            item.seek(to: CMTime(seconds: target, preferredTimescale: 600)) { [weak self, weak item] finished in
                Task { @MainActor in
                    guard let self, let item, finished, self.player.currentItem === item else { return }
                    self.isPreparing = false
                    if self.wantsPlayback {
                        self.beginPlaybackIfReady()
                    }
                    self.updateNowPlaying()
                }
            }
        case .failed:
            failPlayback()
        default:
            break
        }
    }

    private func observeItem(_ item: AVPlayerItem) {
        let center = NotificationCenter.default
        itemObservers.append(center.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime, object: item, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.player.currentItem === item else { return }
                self.pause()
                self.hasEnded = true
                self.position = 0
                self.player.seek(to: .zero)
                self.savePosition()
                self.updateNowPlaying()
            }
        })
        itemObservers.append(center.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime, object: item, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.player.currentItem === item else { return }
                self.failPlayback()
            }
        })
    }

    private func clearItemObservers() {
        statusObserver = nil
        durationObserver = nil
        itemObservers.forEach(NotificationCenter.default.removeObserver)
        itemObservers.removeAll()
    }

    private func updateProgress() {
        guard !isPreparingSpeech, !isPreparing, !isSeeking, !hasEnded,
              let item = player.currentItem, item.status == .readyToPlay else { return }
        position = AudioPlaybackProgress.seconds(player.currentTime().seconds)
        duration = AudioPlaybackProgress.seconds(item.duration.seconds)
        if abs(position - lastSavedAt) >= 5 {
            savePosition()
        }
        updateNowPlaying()
    }

    private func updatePlaybackState() {
        if isPreparingSpeech { return }
        isPlaying = wantsPlayback && !hasFailed
        isLoading = isPlaying && player.timeControlStatus != .playing
        updateNowPlaying()
    }

    private func beginPlaybackIfReady() {
        guard wantsPlayback, !isPreparing, player.currentItem?.status == .readyToPlay else { return }
        player.defaultRate = Float(selectedPlaybackSpeed)
        player.play()
    }
}

extension AudioPlaybackController {
    private func removePlayerObservers() {
        playbackObserver = nil
        if let timeObserver {
            player.removeTimeObserver(timeObserver)
            self.timeObserver = nil
        }
    }
}

extension AudioPlaybackController {
    func isSpeechFor(itemId: String?, title: String?, content: String?) -> Bool {
        episode?.kind == .speech && speechRequest?.episode.itemId == itemId &&
            speechRequest?.title == title && speechRequest?.content == content
    }

    func toggleSpeech(
        itemId: String,
        title: String?,
        displayTitle: String? = nil,
        content: String,
        subtitle: String?,
        artworkURL: URL?
    ) {
        let request = SpeechPlaybackRequest(
            itemId: itemId,
            title: title,
            displayTitle: displayTitle,
            content: content,
            subtitle: subtitle,
            artworkURL: artworkURL
        )
        if episode?.playbackId == request.episode.playbackId, !hasFailed {
            togglePlayback()
            return
        }
        speechRequest = request
        if let cachedSpeech, cachedSpeech.playbackId == request.episode.playbackId,
           FileManager.default.fileExists(atPath: cachedSpeech.url.path) {
            start(cachedSpeech)
        } else {
            prepareSpeech(request)
        }
    }

    private func prepareSpeech(_ request: SpeechPlaybackRequest) {
        cancelSpeechPreparation()
        pause()
        clearItemObservers()
        player.replaceCurrentItem(with: nil)
        artworkTask?.cancel()
        artwork = nil
        episode = request.episode
        position = Double(max(0, positionStore.position(for: request.episode.playbackId))) / 1_000
        duration = 0
        hasFailed = false
        isPreparingSpeech = true
        isPreparing = true
        wantsPlayback = true
        isPlaying = true
        isLoading = true
        resumeAfterInterruption = false
        updateNowPlaying()
        let generation = speechGeneration
        speechTask = Task { [weak self] in
            guard let self else { return }
            var output: URL?
            defer {
                if let output { self.deleteSpeechFile(output) }
            }
            do {
                let segments = try await self.speechText(request.title, request.content)
                try Task.checkCancellation()
                output = try await self.speechGenerator.generate(segments: segments)
                try Task.checkCancellation()
                guard self.speechGeneration == generation, let file = output else { return }
                let autoplay = self.wantsPlayback
                self.clearSpeechFile()
                self.speechFile = file
                var prepared = request.episode
                prepared = AudioEpisode(
                    itemId: prepared.itemId, url: file, title: prepared.title,
                    subtitle: prepared.subtitle, artworkURL: prepared.artworkURL,
                    kind: .speech, contentKey: prepared.contentKey
                )
                self.cachedSpeech = prepared
                output = nil
                self.speechTask = nil
                self.start(prepared, autoplay: autoplay)
            } catch {
                guard self.speechGeneration == generation else { return }
                self.speechTask = nil
                self.isPreparingSpeech = false
                self.isPreparing = false
                self.failPlayback()
            }
        }
    }

    private func cancelSpeechPreparation() {
        speechGeneration = UUID()
        speechTask?.cancel()
        speechTask = nil
        isPreparingSpeech = false
    }

    private func clearSpeechFile() {
        if let speechFile { deleteSpeechFile(speechFile) }
        speechFile = nil
        cachedSpeech = nil
    }

    private func deleteSpeechFile(_ url: URL) {
        try? FileManager.default.removeItem(at: url)
        let parent = url.deletingLastPathComponent()
        if parent.lastPathComponent.hasPrefix("reader-speech-") {
            try? FileManager.default.removeItem(at: parent)
        }
    }
}

import AVFoundation
import Foundation
import MediaPlayer
import UIKit

extension AudioPlaybackController {
    func configureSystemControls() {
        let center = NotificationCenter.default
        systemObservers.append(center.addObserver(
            forName: AVAudioSession.interruptionNotification, object: nil, queue: .main
        ) { [weak self] notification in
            let type = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let options = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            Task { @MainActor in self?.handleInterruption(type: type, options: options) }
        })
        systemObservers.append(center.addObserver(
            forName: AVAudioSession.routeChangeNotification, object: nil, queue: .main
        ) { [weak self] notification in
            let reason = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt
            Task { @MainActor in
                if reason == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue {
                    self?.pause()
                }
            }
        })
        systemObservers.append(center.addObserver(
            forName: UIApplication.didEnterBackgroundNotification, object: nil, queue: .main
        ) { [weak self] _ in Task { @MainActor in self?.savePosition() } })
        systemObservers.append(center.addObserver(
            forName: AVAudioSession.mediaServicesWereResetNotification, object: nil, queue: .main
        ) { [weak self] _ in Task { @MainActor in self?.resetAfterMediaServicesLoss() } })
        configureRemoteCommands()
    }

    private func configureRemoteCommands() {
        let commands = nowPlayingSession.remoteCommandCenter
        remoteTargets.append((commands.playCommand, commands.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.play() }
            return .success
        }))
        remoteTargets.append((commands.pauseCommand, commands.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.pause() }
            return .success
        }))
        remoteTargets.append((
            commands.togglePlayPauseCommand,
            commands.togglePlayPauseCommand.addTarget { [weak self] _ in
                Task { @MainActor in self?.togglePlayback() }
                return .success
            }
        ))
        remoteTargets.append((
            commands.changePlaybackPositionCommand,
            commands.changePlaybackPositionCommand.addTarget { [weak self] event in
                guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
                let position = event.positionTime
                Task { @MainActor in self?.seek(to: position) }
                return .success
            }
        ))
        updateRemoteCommandAvailability()
    }

    func reconfigureRemoteCommands() {
        remoteTargets.forEach { command, target in command.removeTarget(target) }
        remoteTargets.removeAll()
        configureRemoteCommands()
    }

    func updateRemoteCommandAvailability() {
        let commands = nowPlayingSession.remoteCommandCenter
        commands.playCommand.isEnabled = episode != nil
        commands.pauseCommand.isEnabled = episode != nil
        commands.togglePlayPauseCommand.isEnabled = episode != nil
        commands.changePlaybackPositionCommand.isEnabled = canSeek
        commands.nextTrackCommand.isEnabled = false
        commands.previousTrackCommand.isEnabled = false
        commands.skipForwardCommand.isEnabled = false
        commands.skipBackwardCommand.isEnabled = false
    }

    private func handleInterruption(type: UInt?, options: UInt) {
        if type == AVAudioSession.InterruptionType.began.rawValue {
            let shouldResume = wantsPlayback
            pause()
            resumeAfterInterruption = shouldResume
        } else if type == AVAudioSession.InterruptionType.ended.rawValue {
            let shouldResume = resumeAfterInterruption
            resumeAfterInterruption = false
            if shouldResume, AVAudioSession.InterruptionOptions(rawValue: options).contains(.shouldResume) {
                play()
            }
        }
    }

    func updateNowPlaying() {
        guard let episode else { return }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: episode.title,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: position,
            MPNowPlayingInfoPropertyPlaybackRate: Double(player.rate),
            MPNowPlayingInfoPropertyDefaultPlaybackRate: selectedPlaybackSpeed,
            MPNowPlayingInfoPropertyMediaType: MPNowPlayingInfoMediaType.audio.rawValue
        ]
        if let subtitle = episode.subtitle {
            info[MPMediaItemPropertyArtist] = subtitle
        }
        if duration > 0 {
            info[MPMediaItemPropertyPlaybackDuration] = duration
        }
        if let artwork {
            info[MPMediaItemPropertyArtwork] = artwork
        }
        nowPlayingSession.nowPlayingInfoCenter.nowPlayingInfo = info
        updateRemoteCommandAvailability()
    }

    func loadArtwork(for episode: AudioEpisode) {
        guard let url = episode.artworkURL, ["https", "http"].contains(url.scheme?.lowercased() ?? "") else { return }
        artworkTask = Task { [weak self] in
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  !Task.isCancelled,
                  let image = UIImage(data: data),
                  let self, self.episode == episode else { return }
            self.artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
            self.updateNowPlaying()
        }
    }
}

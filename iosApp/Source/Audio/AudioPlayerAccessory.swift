import SwiftUI

struct AudioPlayerAccessory: View {
    @Environment(AudioPlaybackController.self)
    private var playback
    @State private var expanded = false
    @State private var scrubPosition = 0.0
    @State private var isScrubbing = false
    let openExternal: (URL) -> Void
    var onOpenEpisode: ((AudioEpisode) -> Void)?

    var body: some View {
        if let episode = playback.episode {
            surface(episode: episode)
                .padding(.horizontal, 16)
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.player)
                .onAppear {
                    expanded = false
                    isScrubbing = false
                }
                .onChange(of: episode) { _, _ in
                    isScrubbing = false
                }
        }
    }

    @ViewBuilder
    private func surface(episode: AudioEpisode) -> some View {
        if #available(iOS 26, *) {
            GlassEffectContainer {
                controls(episode: episode)
                    .glassEffect(.regular, in: .rect(cornerRadius: 26))
            }
        } else {
            controls(episode: episode)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 26))
        }
    }

    private func controls(episode: AudioEpisode) -> some View {
        VStack(spacing: 0) {
            header(episode: episode)
            if expanded {
                expandedControls(episode: episode)
            } else {
                GeometryReader { geometry in
                    Capsule().fill(.secondary.opacity(0.18)).overlay(alignment: .leading) {
                        Capsule().fill(Color.accentColor)
                            .frame(width: geometry.size.width * progress)
                    }
                }.frame(height: 2).padding(.top, 3).accessibilityHidden(true)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }

    private func header(episode: AudioEpisode) -> some View {
        HStack(spacing: 10) {
            Button {
                if let onOpenEpisode {
                    onOpenEpisode(episode)
                } else {
                    withAnimation(.smooth) { expanded.toggle() }
                }
            } label: {
                HStack(spacing: 10) {
                    AudioEpisodeArtwork(url: episode.artworkURL)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(episode.title).font(.subheadline.weight(.semibold)).lineLimit(1)
                        if playback.hasFailed {
                            Text(episode.kind == .speech
                                ? feedFlowStrings.readerModeTtsError : feedFlowStrings.audioPlaybackFailed)
                                .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        } else if episode.kind == .speech, playback.isPreparingSpeech {
                            Text(feedFlowStrings.readerModeTtsPreparing)
                                .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        } else if let subtitle = episode.subtitle {
                            Text(subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }.contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(episode.title)
            .accessibilityHint(onOpenEpisode != nil ? feedFlowStrings.audioReturnToEpisode : feedFlowStrings.audioSeek)
            .accessibilityIdentifier(onOpenEpisode != nil
                ? AudioPlayerAccessibilityIdentifiers.dockReturnToEpisode : AudioPlayerAccessibilityIdentifiers.expand)

            Button { playback.togglePlayback() } label: {
                ZStack {
                    if playback.isLoading {
                        ProgressView().accessibilityLabel(feedFlowStrings.audioLoading)
                    } else {
                        Image(systemName: playback.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 19))
                    }
                }.frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(playback.isPlaying ? feedFlowStrings.audioPause : feedFlowStrings.audioPlay)
            .accessibilityIdentifier(playback.isPlaying
                ? AudioPlayerAccessibilityIdentifiers.pause : AudioPlayerAccessibilityIdentifiers.play)

            if episode.kind == .speech, playback.hasFailed {
                Button(feedFlowStrings.retryButton) { playback.togglePlayback() }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.retrySpeech)
            }

            Button { playback.stop() } label: {
                Image(systemName: "xmark").font(.system(size: 16)).frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(feedFlowStrings.audioClosePlayer)
            .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.close)
        }
    }

    private func expandedControls(episode: AudioEpisode) -> some View {
        VStack(spacing: 0) {
            Slider(
                value: Binding(
                    get: { isScrubbing ? scrubPosition : min(playback.position, max(playback.duration, 1)) },
                    set: {
                        scrubPosition = $0
                        if !isScrubbing {
                            playback.seek(to: $0)
                        }
                    }
                ),
                in: 0 ... max(playback.duration, 1),
                onEditingChanged: { editing in
                    if editing {
                        scrubPosition = playback.position
                        isScrubbing = true
                    } else {
                        playback.seek(to: scrubPosition)
                        isScrubbing = false
                    }
                }
            )
            .disabled(!playback.canSeek)
            .accessibilityLabel(feedFlowStrings.audioSeek)
            .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.seek)
            .padding(.top, 2)
            HStack {
                Text(timeLabel(isScrubbing ? scrubPosition : playback.position))
                    .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.position)
                Spacer()
                Text(timeLabel(playback.duration))
                    .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.duration)
            }
            .font(.caption2.monospacedDigit())
            .foregroundStyle(.secondary)
            .padding(.bottom, 5)
            HStack(spacing: 8) {
                speedSelector
                if episode.kind != .speech {
                    Button {
                        playback.pause()
                        openExternal(episode.url)
                    } label: {
                        Label(feedFlowStrings.audioOpenExternal, systemImage: "arrow.up.forward.square")
                            .font(.caption)
                            .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.external)
                }
            }
        }
    }

    private var speedSelector: some View {
        Menu {
            ForEach(AudioPlaybackController.supportedPlaybackSpeeds, id: \.self) { speed in
                Button {
                    playback.setPlaybackSpeed(speed)
                } label: {
                    if playback.selectedPlaybackSpeed == speed {
                        Label(speedValueLabel(speed), systemImage: "checkmark")
                    } else {
                        Text(speedValueLabel(speed))
                    }
                }
                .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.speedOption(speed))
            }
        } label: {
            Text(speedValueLabel(playback.selectedPlaybackSpeed))
                .font(.caption.weight(.semibold))
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(feedFlowStrings.audioPlaybackSpeed)
        .accessibilityValue(speedValueLabel(playback.selectedPlaybackSpeed))
        .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.speed)
    }

    private func speedValueLabel(_ speed: Double) -> String {
        return "\(speed.formatted(.number.precision(.fractionLength(0...1))))×"
    }

    private var progress: Double {
        playback.duration > 0 ? min(1, playback.position / playback.duration) : 0
    }

    private func timeLabel(_ seconds: Double) -> String {
        let value = Int(AudioPlaybackProgress.seconds(seconds))
        if value >= 3_600 {
            return String(format: "%d:%02d:%02d", value / 3_600, value / 60 % 60, value % 60)
        }
        return String(format: "%d:%02d", value / 60, value % 60)
    }
}

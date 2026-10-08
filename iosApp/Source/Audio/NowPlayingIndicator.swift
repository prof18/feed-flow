import SwiftUI

struct NowPlayingIndicator: View {
    @Environment(AudioPlaybackController.self)
    private var playback

    let onOpenEpisode: (AudioEpisode) -> Void

    var body: some View {
        if let episode = playback.episode {
            miniPlayer(episode: episode)
                .padding(.horizontal, 16)
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.nowPlaying)
        }
    }

    @ViewBuilder
    private func miniPlayer(episode: AudioEpisode) -> some View {
        if #available(iOS 26, *) {
            content(episode: episode)
                .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 20))
        } else {
            content(episode: episode)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        }
    }

    private func content(episode: AudioEpisode) -> some View {
        HStack(spacing: 10) {
            Button {
                onOpenEpisode(episode)
            } label: {
                HStack(spacing: 10) {
                    AudioEpisodeArtwork(url: episode.artworkURL)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(playback.isPlaying ? feedFlowStrings.audioNowPlaying : feedFlowStrings.audioPaused)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                        Text(episode.title)
                            .font(.subheadline.weight(.semibold))
                            .lineLimit(1)
                            .foregroundStyle(.primary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(episode.title)
            .accessibilityHint(feedFlowStrings.audioReturnToEpisode)
            .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.returnToEpisode)

            Button {
                playback.togglePlayback()
            } label: {
                Image(systemName: playback.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(playback.isPlaying ? feedFlowStrings.audioPause : feedFlowStrings.audioPlay)
            .accessibilityIdentifier(playback.isPlaying
                ? AudioPlayerAccessibilityIdentifiers.miniPause : AudioPlayerAccessibilityIdentifiers.miniPlay)
        }
        .padding(.leading, 10)
        .padding(.trailing, 6)
        .padding(.vertical, 6)
    }
}

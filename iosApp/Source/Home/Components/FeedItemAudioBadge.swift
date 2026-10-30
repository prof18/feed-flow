import SwiftUI

struct FeedItemAudioBadge: View {
  let feedItemId: String

  var body: some View {
    Image(systemName: "headphones")
      .font(.system(size: 14))
      .foregroundStyle(.secondary)
      .accessibilityLabel(feedFlowStrings.audioEpisodeBadgeContentDescription)
      .accessibilityIdentifier(FeedItemAccessibilityIdentifiers.audio(feedItemId))
  }
}

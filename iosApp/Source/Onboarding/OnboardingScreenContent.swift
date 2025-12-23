import SwiftUI
import FeedFlowKit

struct OnboardingScreenContent: View {
    @ObservedObject var viewModel: OnboardingViewModel
    var onOnboardingComplete: () -> Void

    @State private var categories: [SuggestedFeedCategory] = []
    @State private var selectedFeeds: Set<String> = []
    @State private var expandedCategories: Set<String> = []
    @State private var isLoading: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: Spacing.large) {
                    OnboardingHeroView()

                    ForEach(categories, id: \.id) { category in
                        ModernCategoryCard(
                            category: category,
                            isExpanded: expandedCategories.contains(category.id),
                            selectedFeeds: selectedFeeds,
                            onCategoryToggle: {
                                toggleCategoryExpansion(categoryId: category.id)
                            },
                            onFeedToggle: { feedUrl in
                                toggleFeedSelection(feedUrl: feedUrl)
                            }
                        )
                    }

                    Spacer().frame(height: 80)
                }
                .padding(Spacing.large)
            }

            ModernOnboardingFooter(
                selectedFeedsCount: selectedFeeds.count,
                isLoading: isLoading,
                onComplete: {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                },
                onSkip: {
                    viewModel.skipOnboarding()
                    onOnboardingComplete()
                }
            )
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            observeState()
        }
    }

    private func observeState() {
        Task {
            for await state in viewModel.suggestedCategoriesState {
                categories = state
                if !state.isEmpty && expandedCategories.isEmpty {
                    expandedCategories.insert(state[0].id)
                }
            }
        }

        Task {
            for await state in viewModel.selectedFeedsState {
                selectedFeeds = state
            }
        }

        Task {
            for await state in viewModel.expandedCategoriesState {
                expandedCategories = state
            }
        }

        Task {
            for await state in viewModel.isLoadingState {
                isLoading = state.boolValue
            }
        }
    }

    private func toggleFeedSelection(feedUrl: String) {
        viewModel.toggleFeedSelection(feedUrl: feedUrl)
    }

    private func toggleCategoryExpansion(categoryId: String) {
        viewModel.toggleCategoryExpansion(categoryId: categoryId)
    }
}

struct OnboardingHeroView: View {
    var body: some View {
        VStack(spacing: Spacing.regular) {
            Text("👋")
                .font(.system(size: 80))

            Text("Welcome to FeedFlow")
                .font(.largeTitle)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            Text("Discover quality content from curated sources.\nSelect your interests to get started!")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
        }
        .padding(.vertical, Spacing.extraLarge)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [
                    Color.accentColor.opacity(0.15),
                    Color.clear
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .cornerRadius(20)
    }
}

struct ModernCategoryCard: View {
    let category: SuggestedFeedCategory
    let isExpanded: Bool
    let selectedFeeds: Set<String>
    let onCategoryToggle: () -> Void
    let onFeedToggle: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onCategoryToggle) {
                HStack {
                    HStack(spacing: Spacing.regular) {
                        Text(category.icon)
                            .font(.title)

                        VStack(alignment: .leading, spacing: 4) {
                            Text(category.name)
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)

                            let selectedInCategory = category.feeds.filter { selectedFeeds.contains($0.url) }.count
                            if selectedInCategory > 0 {
                                Text("\(selectedInCategory) of \(category.feeds.count) selected")
                                    .font(.subheadline)
                                    .foregroundColor(.accentColor)
                                    .fontWeight(.medium)
                            }
                        }
                    }

                    Spacer()

                    Image(systemName: "chevron.down")
                        .foregroundColor(.secondary)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                        .animation(.spring(), value: isExpanded)
                }
                .padding(Spacing.large)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                ModernFeedList(
                    feeds: category.feeds,
                    selectedFeeds: selectedFeeds,
                    onFeedToggle: onFeedToggle
                )
                .padding([.horizontal, .bottom], Spacing.large)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(20)
        .shadow(color: Color.black.opacity(isExpanded ? 0.1 : 0.05), radius: isExpanded ? 8 : 4, y: 2)
        .animation(.spring(), value: isExpanded)
    }
}

struct ModernFeedList: View {
    let feeds: [SuggestedFeed]
    let selectedFeeds: Set<String>
    let onFeedToggle: (String) -> Void

    var body: some View {
        FlowLayout(spacing: Spacing.small) {
            ForEach(feeds, id: \.url) { feed in
                FeedChipView(
                    feed: feed,
                    isSelected: selectedFeeds.contains(feed.url),
                    onToggle: { onFeedToggle(feed.url) }
                )
            }
        }
    }
}

struct FeedChipView: View {
    let feed: SuggestedFeed
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 8) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.caption)
                        .fontWeight(.semibold)
                }
                Text(feed.name)
                    .font(.body)
                    .fontWeight(isSelected ? .semibold : .regular)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(isSelected ? Color.accentColor : Color(UIColor.tertiarySystemGroupedBackground))
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.clear : Color.gray.opacity(0.3), lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3), value: isSelected)
    }
}

struct ModernOnboardingFooter: View {
    let selectedFeedsCount: Int
    let isLoading: Bool
    let onComplete: () -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: Spacing.regular) {
            if selectedFeedsCount > 0 {
                HStack(spacing: 4) {
                    Text("✨")
                        .font(.title3)
                    Text("\(selectedFeedsCount) feed\(selectedFeedsCount > 1 ? "s" : "") selected")
                        .font(.headline)
                        .foregroundColor(.accentColor)
                }
            }

            HStack(spacing: Spacing.regular) {
                Button(action: onSkip) {
                    Text("Skip")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                }
                .buttonStyle(.bordered)
                .disabled(isLoading)

                Button(action: onComplete) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                    } else {
                        Text("Continue")
                            .font(.headline)
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(selectedFeedsCount == 0 || isLoading)
            }
        }
        .padding(Spacing.large)
        .background(Color(UIColor.systemGroupedBackground))
        .shadow(color: Color.black.opacity(0.1), radius: 8, y: -2)
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x, y: bounds.minY + result.positions[index].y), proposal: .unspecified)
        }
    }

    struct FlowResult {
        var size: CGSize
        var positions: [CGPoint]

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var positions: [CGPoint] = []
            var size: CGSize = .zero
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let subviewSize = subview.sizeThatFits(.unspecified)

                if currentX + subviewSize.width > maxWidth && currentX > 0 {
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                lineHeight = max(lineHeight, subviewSize.height)
                currentX += subviewSize.width + spacing
                size.width = max(size.width, currentX - spacing)
            }

            size.height = currentY + lineHeight
            self.size = size
            self.positions = positions
        }
    }
}

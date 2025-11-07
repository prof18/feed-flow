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
                VStack(spacing: Spacing.regular) {
                    OnboardingHeaderView()

                    ForEach(categories, id: \.id) { category in
                        CategoryCardView(
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
                }
                .padding(Spacing.regular)
            }

            OnboardingFooterView(
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
        .navigationTitle("Welcome")
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

struct OnboardingHeaderView: View {
    var body: some View {
        VStack(spacing: Spacing.small) {
            Text("Welcome to FeedFlow")
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            Text("Get started by selecting some feeds to follow")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, Spacing.regular)
    }
}

struct CategoryCardView: View {
    let category: SuggestedFeedCategory
    let isExpanded: Bool
    let selectedFeeds: Set<String>
    let onCategoryToggle: () -> Void
    let onFeedToggle: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onCategoryToggle) {
                HStack {
                    HStack(spacing: Spacing.small) {
                        if let icon = category.icon {
                            Text(icon)
                                .font(.title2)
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(category.name)
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)

                            let selectedInCategory = category.feeds.filter { selectedFeeds.contains($0.url) }.count
                            if selectedInCategory > 0 {
                                Text("\(selectedInCategory) selected")
                                    .font(.caption)
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }

                    Spacer()

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
                .padding(Spacing.regular)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                FeedListView(
                    feeds: category.feeds,
                    selectedFeeds: selectedFeeds,
                    onFeedToggle: onFeedToggle
                )
                .padding([.horizontal, .bottom], Spacing.regular)
            }
        }
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

struct FeedListView: View {
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
            HStack(spacing: 6) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.caption)
                }
                Text(feed.name)
                    .font(.subheadline)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? Color.accentColor : Color(UIColor.tertiarySystemGroupedBackground))
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(isSelected ? Color.clear : Color.gray.opacity(0.3), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

struct OnboardingFooterView: View {
    let selectedFeedsCount: Int
    let isLoading: Bool
    let onComplete: () -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: Spacing.small) {
            if selectedFeedsCount > 0 {
                Text("\(selectedFeedsCount) feed\(selectedFeedsCount > 1 ? "s" : "") selected")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: Spacing.regular) {
                Button(action: onSkip) {
                    Text("Skip")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.bordered)
                .disabled(isLoading)

                Button(action: onComplete) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    } else {
                        Text("Continue")
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(selectedFeedsCount == 0 || isLoading)
            }
        }
        .padding(Spacing.regular)
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
            subview.place(at: result.positions[index], proposal: .unspecified)
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

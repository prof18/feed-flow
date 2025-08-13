//
//  ScrollToTopView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 10/08/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//
import SwiftUI

struct ScrollToTopView: View {
    let onScrollToTop: () -> Void
    let isVisible: Bool

    var body: some View {
        if isVisible {
            if #available(iOS 26.0, *) {
                iOS26ScrollToTopView(onScrollToTop: onScrollToTop)
            } else {
                LegacyScrollToTopView(onScrollToTop: onScrollToTop)
            }
        }
    }
}

@available(iOS 26.0, *)
private struct iOS26ScrollToTopView: View {
    @Namespace private var namespace
    let onScrollToTop: () -> Void

    var body: some View {
        GlassEffectContainer(spacing: Spacing.regular) {
            VStack(alignment: .center, spacing: Spacing.regular) {
                Button {
                    onScrollToTop()
                } label: {
                    Label("Scroll to Top", systemImage: "arrow.up.circle.fill")
                        .foregroundStyle(.primary)
                        .labelStyle(.iconOnly)
                        .fontWeight(.medium)
                        .imageScale(.large)
                }
                .buttonStyle(.glass)
                .glassEffectID("scrolltotop", in: namespace)
            }
            .frame(width: 74)
        }
    }
}

private struct LegacyScrollToTopView: View {
    let onScrollToTop: () -> Void

    var body: some View {
        Button {
            onScrollToTop()
        } label: {
            Label("Scroll to Top", systemImage: "arrow.up.circle.fill")
                .foregroundStyle(.primary)
                .labelStyle(.iconOnly)
                .fontWeight(.medium)
                .imageScale(.large)
        }
        .frame(width: 50, height: 50)
        .background(.regularMaterial)
        .clipShape(Circle())
        .shadow(radius: 5)
    }
}

private struct ShowsScrollToTopViewModifier: ViewModifier {
    let onScrollToTop: () -> Void
    @State private var showButton: Bool = false

    func body(content: Content) -> some View {
        ZStack {
            content

            HStack {
                Spacer()
                VStack {
                    Spacer()
                    ScrollToTopView(onScrollToTop: onScrollToTop, isVisible: showButton)
                        .padding()
                        .transition(.scale.combined(with: .opacity))
                }
            }
        }
    }
}

extension View {
    /// A function that returns a view after it applies `ShowsScrollToTopViewModifier` to it.
    func showsScrollToTop(onScrollToTop: @escaping () -> Void) -> some View {
        modifier(ShowsScrollToTopViewModifier(onScrollToTop: onScrollToTop))
    }

    /// A function that returns a view after it applies `ShowsScrollToTopViewModifier` with custom visibility control.
    func showsScrollToTop(isVisible: Bool, onScrollToTop: @escaping () -> Void) -> some View {
        modifier(ShowsScrollToTopViewModifierWithBinding(isVisible: isVisible, onScrollToTop: onScrollToTop))
    }
}

private struct ShowsScrollToTopViewModifierWithBinding: ViewModifier {
    let isVisible: Bool
    let onScrollToTop: () -> Void

    func body(content: Content) -> some View {
        ZStack {
            content

            HStack {
                Spacer()
                VStack {
                    Spacer()
                    ScrollToTopView(onScrollToTop: onScrollToTop, isVisible: isVisible)
                        .padding()
                        .transition(.scale.combined(with: .opacity))
                }
            }
        }
    }
}

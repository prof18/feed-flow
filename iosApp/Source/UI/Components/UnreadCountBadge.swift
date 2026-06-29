//
//  UnreadCountBadge.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/06/26.
//  Copyright © 2026. All rights reserved.
//

import SwiftUI

struct UnreadCountBadge: View {
    private let value: String

    @ScaledMetric(relativeTo: .caption2) private var horizontalPadding: CGFloat = 6
    @ScaledMetric(relativeTo: .caption2) private var verticalPadding: CGFloat = 2
    @ScaledMetric(relativeTo: .caption2) private var minHeight: CGFloat = 18
    @ScaledMetric(relativeTo: .caption2) private var minWidth: CGFloat = 22

    init(count: Int) {
        self.value = count.formatted()
    }

    init(count: Int64) {
        self.value = count.formatted()
    }

    var body: some View {
        Text(value)
            .font(.caption2.weight(.semibold))
            .monospacedDigit()
            .foregroundStyle(.secondary)
            .lineLimit(1)
            .fixedSize(horizontal: true, vertical: true)
            .padding(.horizontal, horizontalPadding)
            .padding(.vertical, verticalPadding)
            .frame(minWidth: minWidth, minHeight: minHeight)
            .background(Color.secondary.opacity(0.15), in: Capsule())
    }
}

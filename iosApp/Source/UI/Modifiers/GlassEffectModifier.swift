//
//  GlassEffectModifier.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 10/08/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import SwiftUI

struct GlassEffectModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect()
        } else {
            content
        }
    }
}
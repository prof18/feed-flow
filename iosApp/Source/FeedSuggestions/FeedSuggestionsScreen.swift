//
//  FeedSuggestionsScreen.swift
//  FeedFlow
//
//  Created by Claude Code
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSuggestionsScreen: VMStoreOwner {
    typealias VM = OnboardingViewModel

    var storeOwner: StateObject<ObservableVMStore<OnboardingViewModel>>

    init(completion: @escaping () -> Void) {
        self.completion = completion
        _storeOwner = StateObject(wrappedValue: ObservableVMStore(store: Deps.shared.getOnboardingViewModel()))
    }

    let completion: () -> Void

    var body: some View {
        FeedSuggestionsContent(
            viewModel: storeOwner.wrappedValue.instance,
            onBackClick: completion
        )
    }
}

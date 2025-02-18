//
//  VMStoreOwner.swift
//  FeedFlow
//
//  Created by marco.gomiero on 04.08.24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

class VMStoreOwner<VM: ViewModel>: ObservableObject, ViewModelStoreOwner {
    var viewModelStore: ViewModelStore = .init()

    private let key: String = .init(describing: type(of: VM.self))

    init(_ viewModel: VM) {
        viewModelStore.put(key: key, viewModel: viewModel)
    }

    // swiftlint:disable force_cast
    var instance: VM {
        return viewModelStore.get(key: key) as! VM
    }

    // swiftlint:enable force_cast

    deinit {
        print("Deinit vm store of \(VM.self)")
        viewModelStore.clear()
    }
}

//
//  SharedViewModelStoreOwner.swift
//  FeedFlow
//
//  Created by marco.gomiero on 04.08.24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import FeedFlowKit

class VMStoreOwner<VM: ViewModel>: ObservableObject, ViewModelStoreOwner {
    internal var viewModelStore: ViewModelStore = ViewModelStore()

    private let key: String = String(describing: type(of: VM.self))

    init(_ viewModel: VM) {
        viewModelStore.put(key: key, viewModel: viewModel)
    }

    // swiftlint:disable force_cast
    // swiftlint:disable implicit_getter
    var instance: VM {
        get {
            return viewModelStore.get(key: key) as! VM
        }
    }
    // swiftlint:enable force_cast
    // swiftlint:enable implicit_getter

    deinit {
        print("Deinit vm store of \(VM.self)")
        viewModelStore.clear()
    }
}

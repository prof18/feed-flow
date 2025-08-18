//
//  ShareViewController.swift
//  ShareExtension
//
//  Created by Marco Gomiero on 06/01/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import Foundation
import Social
import SwiftUI
import UIKit

@objc(ShareViewController)
class ShareViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        startKoin()
        let contentView = UIHostingController(
            rootView: ShareView(extensionContext: extensionContext)
        )
        addChild(contentView)
        view.addSubview(contentView.view)

        contentView.view.translatesAutoresizingMaskIntoConstraints = false
        contentView.view.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        contentView.view.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        contentView.view.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        NSLayoutConstraint.activate([
            contentView.view.heightAnchor.constraint(equalToConstant: 450) // Increased from 300
        ])

        contentView.view.layer.cornerRadius = 16
        contentView.view.clipsToBounds = true
    }
}

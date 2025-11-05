// swift-tools-version: 5.6
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Reader",
    platforms: [.iOS("17.0")],
    products: [
        .library(
            name: "Reader",
            targets: ["Reader"]
        )
    ],
    targets: [
        .target(
            name: "Reader",
        )
    ]
)

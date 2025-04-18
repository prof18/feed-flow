// swift-tools-version: 5.6
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Reader",
    platforms: [.iOS("15.0"), .macOS("12.0")],
    products: [
        .library(
            name: "Reader",
            targets: ["Reader"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/cezheng/Fuzi", from: "3.1.3"),
        .package(url: "https://github.com/scinfu/SwiftSoup.git", from: "2.4.3")
    ],
    targets: [
        .target(
            name: "Reader",
            dependencies: ["Fuzi", "SwiftSoup"],
            resources: [.process("JS")]
        )
    ]
)

// swift-tools-version:6.0

//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import PackageDescription

let rustBuildDir = "../target/debug/"

let package = Package(
    name: "LibZonaRosaClient",
    platforms: [
        .macOS(.v10_15), .iOS(.v13),
    ],
    products: [
        .library(
            name: "LibZonaRosaClient",
            targets: ["LibZonaRosaClient"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-docc-plugin", from: "1.4.3")
    ],
    targets: [
        .systemLibrary(name: "ZonaRosaFfi"),
        .target(
            name: "LibZonaRosaClient",
            dependencies: ["ZonaRosaFfi"],
            swiftSettings: [.enableExperimentalFeature("StrictConcurrency")],
            linkerSettings: [
                .linkedLibrary("stdc++", .when(platforms: [.linux]))
            ]
        ),
        .testTarget(
            name: "LibZonaRosaClientTests",
            dependencies: ["LibZonaRosaClient"],
            resources: [.process("Resources")],
            swiftSettings: [.enableExperimentalFeature("StrictConcurrency")],
            linkerSettings: [.unsafeFlags(["-L\(rustBuildDir)"])]
        ),
    ]
)

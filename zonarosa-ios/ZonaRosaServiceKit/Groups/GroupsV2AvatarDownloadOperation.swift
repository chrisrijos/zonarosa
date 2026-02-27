//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

enum GroupsV2AvatarDownloadOperation {
    static func run(urlPath: String, maxDownloadSize: UInt64) async throws -> Data {
        return try await Retry.performWithBackoff(maxAttempts: 4) {
            return try await CDNDownloadOperation.tryToDownload(urlPath: urlPath, maxDownloadSize: maxDownloadSize)
        }
    }
}

//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

/// Describes a message that is being replied to in a draft.
public struct ThreadReplyInfo: Codable {
    public let timestamp: UInt64
    @AciUuid public var author: Aci

    public init(timestamp: UInt64, author: Aci) {
        self.timestamp = timestamp
        self._author = author.codableUuid
    }
}

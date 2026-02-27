//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

public struct SMKUDAccessKey {

    public static let kUDAccessKeyLength: Int = 16

    public let keyData: Data

    public init(profileKey: Aes256Key) {
        self.keyData = ProfileKey(profileKey).deriveAccessKey()
    }

    private init(keyData: Data) {
        self.keyData = keyData
    }

    // Unrestricted UD recipients should have a zeroed access key sent to the multi-recipient endpoint
    // For a collection of mixed recipients, a zeroed key will have no effect composing keys with xor
    // For a collection of only unrestricted UD recipients, the server expects a zero access key
    public static var zeroedKey: SMKUDAccessKey {
        .init(keyData: Data(repeating: 0, count: kUDAccessKeyLength))
    }
}

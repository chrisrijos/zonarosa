//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

extension PublicKey {
    public convenience init(keyData: Data) throws {
        if keyData.count != Constants.keyLengthDJB {
            throw ZonaRosaError.invalidKey("invalid number of public key bytes (expected \(Constants.keyLengthDJB), was \(keyData.count))")
        }
        try self.init([Constants.keyTypeDJB] + keyData)
    }

    public enum Constants {
        public static let keyTypeDJB: UInt8 = 0x05
        public static let keyLengthDJB: Int = 32
    }
}

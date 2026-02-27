//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public class ReceiptSerial: ByteArray, @unchecked Sendable {
    public static let SIZE: Int = 16

    public required init(contents: Data) throws {
        try super.init(newContents: contents, expectedLength: ReceiptSerial.SIZE)
    }
}

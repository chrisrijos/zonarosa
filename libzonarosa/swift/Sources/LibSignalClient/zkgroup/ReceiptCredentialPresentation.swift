//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ReceiptCredentialPresentation: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_receipt_credential_presentation_check_valid_contents)
    }

    public func getReceiptExpirationTime() throws -> UInt64 {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningInteger {
                zonarosa_receipt_credential_presentation_get_receipt_expiration_time($0, contents)
            }
        }
    }

    public func getReceiptLevel() throws -> UInt64 {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningInteger {
                zonarosa_receipt_credential_presentation_get_receipt_level($0, contents)
            }
        }
    }

    public func getReceiptSerial() throws -> ReceiptSerial {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningSerialized {
                zonarosa_receipt_credential_presentation_get_receipt_serial($0, contents)
            }
        }
    }
}

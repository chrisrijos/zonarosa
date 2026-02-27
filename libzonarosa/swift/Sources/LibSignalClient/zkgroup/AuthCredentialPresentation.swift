//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class AuthCredentialPresentation: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_auth_credential_presentation_check_valid_contents)
    }

    public func getUuidCiphertext() throws -> UuidCiphertext {
        return try withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningSerialized {
                zonarosa_auth_credential_presentation_get_uuid_ciphertext($0, buffer)
            }
        }
    }

    public func getPniCiphertext() throws -> UuidCiphertext {
        return try withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningSerialized {
                zonarosa_auth_credential_presentation_get_pni_ciphertext($0, buffer)
            }
        }
    }

    public func getRedemptionTime() throws -> Date {
        let secondsSinceEpoch = try withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningInteger {
                zonarosa_auth_credential_presentation_get_redemption_time($0, buffer)
            }
        }
        return Date(timeIntervalSince1970: TimeInterval(secondsSinceEpoch))
    }
}

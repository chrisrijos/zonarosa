//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ProfileKeyCredentialPresentation: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_profile_key_credential_presentation_check_valid_contents)
    }

    public func getUuidCiphertext() throws -> UuidCiphertext {
        return try withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningSerialized {
                zonarosa_profile_key_credential_presentation_get_uuid_ciphertext($0, buffer)
            }
        }
    }

    public func getProfileKeyCiphertext() throws -> ProfileKeyCiphertext {
        return try withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningSerialized {
                zonarosa_profile_key_credential_presentation_get_profile_key_ciphertext($0, buffer)
            }
        }
    }
}

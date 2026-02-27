//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ExpiringProfileKeyCredential: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_expiring_profile_key_credential_check_valid_contents)
    }

    public var expirationTime: Date {
        let timestampInSeconds = failOnError {
            try self.withUnsafePointerToSerialized { contents in
                try invokeFnReturningInteger {
                    zonarosa_expiring_profile_key_credential_get_expiration_time($0, contents)
                }
            }
        }
        return Date(timeIntervalSince1970: TimeInterval(timestampInSeconds))
    }
}

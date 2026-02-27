//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ProfileKey: ByteArray, @unchecked Sendable {
    public static let SIZE: Int = 32

    public required init(contents: Data) throws {
        try super.init(newContents: contents, expectedLength: ProfileKey.SIZE)
    }

    public func getCommitment(userId: Aci) throws -> ProfileKeyCommitment {
        return try withUnsafePointerToSerialized { contents in
            try userId.withPointerToFixedWidthBinary { userId in
                try invokeFnReturningSerialized {
                    zonarosa_profile_key_get_commitment($0, contents, userId)
                }
            }
        }
    }

    public func getProfileKeyVersion(userId: Aci) throws -> ProfileKeyVersion {
        return try withUnsafePointerToSerialized { contents in
            try userId.withPointerToFixedWidthBinary { userId in
                try invokeFnReturningSerialized {
                    zonarosa_profile_key_get_profile_key_version($0, contents, userId)
                }
            }
        }
    }

    public func deriveAccessKey() -> Data {
        return failOnError {
            try withUnsafePointerToSerialized { contents in
                try invokeFnReturningFixedLengthArray {
                    zonarosa_profile_key_derive_access_key($0, contents)
                }
            }
        }
    }
}
